#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
A module of methods to create the pandas DataFrame representing the "data"
array.

"""
import six
import warnings
import numpy as np
import pandas as pd
import itertools
from collections import OrderedDict
idx = pd.IndexSlice

# "Aspect" is my term for the articulation points at a given time frame
# So if you track 49 skeleton points per frame, then x and y have
# 49 aspects per frame, but ox is only recorded once per frame, or
# even once across the whole video.
elements_with_aspect = ['x', 'y']
elements_without_aspect = ['ox', 'oy', 'cx', 'cy', 'head', 'ventral']
basic_data_keys = elements_with_aspect + elements_without_aspect
supported_data_keys = basic_data_keys + ['id', 't']


def get_mask(arr, desired_key):
    """
    In an array of dicts, obtain a mask on arr of elements having
    the desired key

    Parameters
    ----------
    arr: list-like array of dicts
    desired_key: str

    Returns
    ----------
    Boolean numpy array of same size as arr

    """
    # Find elements having a time aspect
    desired_indexes = [i for (i, s) in enumerate(arr) if desired_key in s]
    desired_indexes = np.array(desired_indexes)

    # Transform our list of indexes into a mask on the data array
    return np.bincount(desired_indexes, minlength=len(arr)).astype(bool)


def df_upsert(src, dest):
    """
    Append src to dest, doing an "update/insert" where
    conflicts cause an AssertionError to be raised.

    Return the new dest

    Parameters
    -----------
    src, dest: pandas DataFrames
        src is the one to be added to dest
        dest is the one that is modified in place

    # TODO: concatenate using a list comprehension as calling it
    # iteratively like this causes a performance hit (see
    # http://pandas.pydata.org/pandas-docs/stable/merging.html#concatenating-objects
    # "Note It is worth noting however, that concat (and therefore append)
    # makes a full copy of the data, and that constantly reusing this
    # function can create a signifcant performance hit. If you need
    # to use the operation over several datasets, use a list comprehension."

    """
    dest.sort_index(axis=1, inplace=True)
    # Append src to dest

    # Add all rows that don't currently exist in dest
    dest = pd.concat([dest, src[~src.index.isin(dest.index)]])

    dest.sort_index(axis=1, inplace=True)

    # Obtain a sliced version of dest, showing only
    # the columns and rows shared with the src
    dest_sliced = \
        dest.loc[dest.index.isin(src.index),
                 dest.columns.isin(src.columns)]

    src_sliced = \
        src.loc[src.index.isin(dest.index),
                src.columns.isin(dest.columns)]

    # Sort our slices so they will be lined up for comparison
    dest_sliced.sort_index(inplace=True)
    src_sliced.sort_index(inplace=True)

    # Obtain a mask of the conflicts in the current segment
    # as compared with all previously loaded data.  That is:
    # NaN NaN = False
    # NaN 2   = False
    # 2   2   = False
    # 2   3   = True
    # 2   NaN = True
    data_conflicts = (pd.notnull(dest_sliced) &
                      (dest_sliced != src_sliced))

    if data_conflicts.any().any():
        raise AssertionError("Data from this segment conflicted "
                             "with previously loaded data:\n",
                             data_conflicts)

    # Replace any rows that do exist with the src version
    dest.update(src)

    # Sort the time series indices
    dest.sort_index(axis=0, inplace=True)

    return dest


def convert_origin(df):
    """
    Offset the coordinates and centroid by the offsets if available.

    In pseudocode:
    
    For each worm and time frame:
        If 'ox' is not NaN:
            Add 'ox' to 'x'
            If 'cx' is not NaN:
                Add 'ox' to 'cx'

        (Also do the same for y)

    After this is done, drop the offset columns 'ox', and 'oy'
    entirely from the dataframe.

    Parameters
    ------------
    df: Pandas DataFrame

    Returns
    ------------
    None.  Modifies `df` in place.

    """
    offset_keys = ['ox', 'oy']
    centroid_keys = ['cx', 'cy']
    coord_keys = ['x', 'y']

    for worm_id in df.columns.get_level_values(0).unique():
        cur_worm = df.loc[:, (worm_id)]

        for offset, centroid, coord in zip(offset_keys,
                                           centroid_keys, coord_keys):

            # Note: This code block uses `x` as the stylized example for 
            # variable naming purposes, but be assured that the enclosing 
            # `for` loop loops through both `x` and `y`.

            if offset in cur_worm.columns.get_level_values(0):
                # Consider offset is 0 if not available in a certain frame
                ox_column = cur_worm.loc[:, (offset)].fillna(0)
    
                # Shift our 'x' values by offset
                all_x_columns = cur_worm.loc[:, (coord)]
                ox_affine_change = (np.array(ox_column) *
                                   np.ones(all_x_columns.shape))
                all_x_columns += ox_affine_change
                # Now assign these values back to the passed dataframe df
                df.loc[:, (worm_id, coord)] = all_x_columns.values

                if centroid in cur_worm.columns.get_level_values(0):
                    cx_column = cur_worm.loc[:, (centroid)]
                    # Shift the centroid by the offset
                    cx_column += ox_column
                    # Now assign these values back to the passed dataframe df
                    df.loc[:, (worm_id, centroid)] = cx_column.values
    
                # Now reset our 'ox' values to zero.
                if offset in cur_worm.columns.get_level_values(0):
                    df.loc[:, (worm_id, offset)] = np.zeros(ox_column.shape)

    # Drop the offset columns entirely from the dataframe.
    # This is so DataFrames with and without offsets
    # will show as comparing identically.
    for offset_key in offset_keys:
        df.drop(offset_key, axis=1, level='key', inplace=True)

    # Because of a known issue in Pandas
    # (https://github.com/pydata/pandas/issues/2770), the dropped columns
    # remain in the "levels" attribute of MultiIndex, even if they don't
    # appear in the "labels" and are thus not 'observed'.
    # The workaround is to reconstitute the MultiIndex from tuples:
    df.columns = pd.MultiIndex.from_tuples(df.columns.values,
                                           names=df.columns.names)


def reverse_backwards_worms(df, coord_keys=['x', 'y']):
    """
    Reverse all worms in all time frames with head == 'R':

    - Reverse the coordinates
    - Change head to 'L'

    Parameters
    ------------
    df: Pandas DataFrame
    coord_keys: list of strings
        The corresponding coordinates to be reversed

    Returns
    ------------
    None.  Modifies `df` in place.

    """
    if 'head' not in df.columns.get_level_values(level='key'):
        return

    mask_of_times_to_reverse = df.loc[:, idx[:, 'head', :]] == 'R'

    for worm_id in df.columns.get_level_values(level='id').unique():
        cur_worm = df.loc[:, idx[worm_id, :, :]]
        cur_keys = cur_worm.columns.get_level_values(level='key')

        if 'head' not in cur_keys:
            # Nothing to do
            continue

        cur_mask = mask_of_times_to_reverse.loc[:, idx[1, 'head', 0]]

        for key in coord_keys:
            if key not in cur_keys:
                continue

            thing_to_reverse = df.loc[cur_mask, idx[1, 'x', :]]

            # TODO: I need to reverse "thing_to_reverse", accounting for
            # aspect size.  [::-1] just reverses the row order.
            pass

    pass


def parse_data(data):
    """
    Parse the an array of entries conforming to the WCON schema definition
    for the "data" array in the root object.  The canonical example is
    that of worm "skeleton" (midline) information over time.

    This could be the standard "data" array from the root object, or
    some custom array that needs to be processed

    Note that all elements are required to have "id", "t", and "x" and "y"
    entries.

    Return an object encapsulating that data.

    """
    # If data is single-valued, wrap it in a list so it will be just
    # a special case of the array case.
    if isinstance(data, dict):
        data = [data]

    data = np.array(data)

    # We only care about data that have all the mandatory fields.
    # the Custom Feature Type 2 objects are suppresed by this filter:
    is_time_series_mask = get_mask(data, 't')
    has_id_mask = get_mask(data, 'id')
    data = data[is_time_series_mask & has_id_mask]

    # Clean up and validate all time-series data segments
    _validate_time_series_data(data)

    time_df = _obtain_time_series_data_frame(data)

    return time_df


def _obtain_time_series_data_frame(time_series_data):
    """
    Obtain a time-series pandas DataFrame

    Parameters
    ------------
    time_series_data: list
        All entries must be lists of dicts, all of which
        must have 'id' and 't'

    Returns
    ----------
    pandas dataframe
        Time-series data goes into this Pandas DataFrame
        The dataframe will have t as index, and multilevel columns
        with id at the first level and all other keys at second level.

    """

    # Our DataFrame to return
    time_df = None

    # Consider only time-series data stamped with an id:
    for data_segment in time_series_data:
        # Add this data_segment to a Pandas dataframe
        segment_id = data_segment['id']
        segment_keys = np.array([k for k in data_segment.keys()
                                 if k not in ['t', 'id']])

        cur_timeframes = np.array(data_segment['t']).astype(float)

        # Create our column names as the cartesian product of
        # the segment's keys and the id of the segment
        # Only elements articulated across the skeleton, etc, of the worm
        # have the "aspect" key though
        cur_elements_with_aspect = \
            [k for k in elements_with_aspect if k in segment_keys]
        cur_elements_without_aspect = ['aspect_size'] + \
            [k for k in elements_without_aspect if k in segment_keys]

        # We want to be able to fit the largest aspect size in our
        # DataFrame
        max_aspect_size = int(max([k[0] for k in data_segment['aspect_size']]))

        key_combos = list(itertools.product([segment_id],
                                            cur_elements_with_aspect,
                                            range(max_aspect_size)))
        key_combos.extend(list(itertools.product([segment_id],
                                                 cur_elements_without_aspect,
                                                 [0])))

        column_type_names = ['id', 'key', 'aspect']
        cur_columns = pd.MultiIndex.from_tuples(key_combos,
                                                names=column_type_names)

        # e.g. if this segment has only 'x', 'y', that's what we'll be
        # looking to add to our dataframe data staging in the next step
        cur_data_keys = cur_elements_with_aspect + \
            cur_elements_without_aspect

        # We must pad the timeframes where the data doesn't have maximal
        # aspect or else the concatenation step below will fail.
        for k in cur_elements_with_aspect:
            for i in range(len(cur_timeframes)):
                data_segment[k][i] = (
                    data_segment[k][i] +
                    [np.NaN] * (max_aspect_size - len(data_segment[k][i])))

        # Stage the data for addition to our DataFrame.
        # Shape KxI where K is the number of keys and
        #                 I is the number of "aspects"
        cur_data = np.array(
            [np.concatenate(
                [data_segment[k][i] for k in cur_data_keys]
            ) for i in range(len(cur_timeframes))]
        )

        cur_df = pd.DataFrame(cur_data, columns=cur_columns)

        cur_df.index = cur_timeframes
        cur_df.index.names = 't'

        # We want the index (time) to be in order.
        cur_df.sort_index(axis=0, inplace=True)

        # Apparently multiindex must be sorted to work properly:
        cur_df.sort_index(axis=1, inplace=True)

        if time_df is None:
            time_df = cur_df
        else:
            time_df = df_upsert(src=cur_df, dest=time_df)

    # We want the index (time) to be in order.
    time_df.sort_index(axis=0, inplace=True)

    # We have to do this because somehow with entire worms who have 'head' or
    # 'ventral' columns, all their columns (even the numeric ones)
    # have dtype = object !  We want dtype=float64 (or some float anyway)
    # (Ignore the FutureWarning here about convert_objects being deprecated,
    # because to_numeric only acts on Series and I don't know which ones
    # we need to convert.)
    with warnings.catch_warnings():
        warnings.filterwarnings(action="ignore", category=FutureWarning)
        time_df = time_df.convert_objects(convert_numeric=True)

    # If 'head' or 'ventral' is NaN, we must specify '?' since
    # otherwise, when saving this object, to specify "no value" we would
    # have to create a new data segment with no 'head' column at all.
    # But since both 'head' and 'ventral' have this issue, we might need
    # segments with just 'head', just 'ventral', and both 'head' and
    # 'ventral', and none.  That's too unwieldy for a rare use case.
    # So instead we treat nan as '?'.

    # We must replace NaN with None, otherwise the JSON encoder will
    # save 'NaN' as the string and this will get rejected by our schema
    # on any subsequent loads
    # Note we can't use .fillna(None) due to this issue:
    # https://github.com/pydata/pandas/issues/1972
    df_keys = set(time_df.columns.get_level_values('key'))
    for k in ['head', 'ventral']:
        if k in df_keys:
            cur_slice = time_df.loc[:, idx[:, k, :]]
            time_df.loc[:, idx[:, k, :]] = cur_slice.fillna(value=np.nan)

    # Make sure aspect_size is a float, since only floats are nullable:
    if 'aspect_size' in df_keys:
        time_df.loc[:, idx[:, 'aspect_size', :]] = \
            time_df.loc[:, idx[:, 'aspect_size', :]].astype(float)

    return time_df


def _validate_time_series_data(time_series_data):
    """
    Clean up and validate all time-series data segments in the
    data dictionary provided

    Parameters
    -----------
    data_series_data: array
        Dictionary extracted from JSON, but only the entries with
        a time series.
        All elements must be lists of dicts, all of which
        must have 't' entries

    """
    canonical_elements = ['id', 't', 'x', 'y', 'cx', 'cy', 'ox', 'oy',
                          'head', 'ventral', 'aspect_size']
    for (data_segment_index, data_segment) in enumerate(time_series_data):
        # Filter the data_segment to ignore non-canonical elements
        if six.PY3:
            data_segment = {k:v for (k,v) in data_segment.items()
                            if k in canonical_elements}
        else:
            data_segment = {k:v for (k,v) in data_segment.iteritems()
                            if k in canonical_elements}

        segment_keys = [k for k in data_segment.keys() if k != 'id']

        # If the 't' element is single-valued, wrap it and all other
        # elements into an array so single-valued elements
        # don't need special treatment in our later processing steps
        if not isinstance(data_segment['t'], list):
            for subkey in segment_keys:
                data_segment[subkey] = [data_segment[subkey]]

        # Further, we have to wrap the elements without an aspect
        # in a further list so that when we convert to a dataframe,
        # our staging data list comprehension will be able to see
        # everything as a list and not as single-valued
        for subkey in elements_without_aspect:
            if subkey in data_segment:
                if not isinstance(data_segment[subkey], list):
                    data_segment[subkey] = [[data_segment[subkey]]]
                else:
                    if not isinstance(data_segment[subkey][0], list):
                        data_segment[subkey] = [[x]
                                                for x in data_segment[subkey]]

        subelement_length = len(data_segment['t'])

        # Broadcast aspectless elements to be
        # length n = subelement_length if it's
        # just being shown once right now  (e.g. for 'ox', 'oy', etc.)
        for k in elements_without_aspect:
            if k in segment_keys:
                k_length = len(data_segment[k])
                if k_length not in [1, subelement_length]:
                    raise AssertionError(k + " for each segment "
                                         "must either have length 1 or "
                                         "length equal to the number of "
                                         "time elements.")
                elif k_length == 1:
                    # Broadcast the origin across all time points
                    # in this segment
                    data_segment[k] = data_segment[k] * subelement_length

        # Validate that all sub-elements have the same length
        subelement_lengths = [len(data_segment[key])
                              for key in segment_keys]

        # Now we can assure ourselves that subelement_length is
        # well-defined; if not, raise an error.
        if len(set(subelement_lengths)) > 1:
            raise AssertionError("Error: Subelements must all have "
                                 "the same length.")

        # In each time, the aspect size could change, so we need to keep
        # track of it since the dataframe will ultimately have columns
        # for the maximal aspect size and it won't otherwise be possible
        # to determine what the aspect size is in each timeframe

        # First let's validate that the aspect size is identical
        # across data elements in each time frame:
        aspect_size_over_time = []
        for t in range(subelement_length):
            # The x and y arrays for element i of the data segment
            # must have the same length
            try:
                cur_aspect_sizes = [len(data_segment[k][t]) for k in
                                    elements_with_aspect]
            except TypeError as err:
                raise TypeError("In the following data segment, an "
                                "element with aspect (x, y, etc.) was not "
                                "double-wrapped in arrays, even "
                                "though time ('t') was. {0}".format(err))

            if len(set(cur_aspect_sizes)) > 1:
                raise AssertionError(
                    "Error: Aspects x and y, etc. must have same "
                    "length for data segment " + str(data_segment_index) +
                    " and time index " + str(data_segment['t'][t]))
            else:
                aspect_size_over_time.append([cur_aspect_sizes[0]])

        # We need aspect_size to be float rather than int since it will
        # be in a DataFrame that may be compared with others and so we
        # want to force all numeric dtypes to be float so the comparison
        # won't fail simply because e.g. int 1 isn't equal to float 1
        # More information about this:
        # Pandas DataFrames are stored internally as a series of "blocks".
        # You can see these blocks by looking at time_df._data, for instance.
        # We want to avoid any of the columns being stored within an IntBlock,
        # because it will then fail to be .equals() another DataFrame with the
        # same data but stored within a FloatBlock.
        # See http://stackoverflow.com/questions/17141828/ and
        # http://stackoverflow.com/questions/19912611/
        aspect_size_over_time = np.array(aspect_size_over_time, dtype=float)

        data_segment['aspect_size'] = aspect_size_over_time
        
        # Update the data segment
        time_series_data[data_segment_index] = data_segment


"""
===============================================================================
SAVING DATA
===============================================================================
"""

precision = 2  # TODO


def _data_segment_as_odict(worm_id, df_segment):
    """
    Convert a pandas dataframe into an ordered_dictionary.  This is a support
    method of data_as_array.

    """
    data_segment = [("id", worm_id)]

    # We only care about time indices for which we have some "aspect" or
    # else which have some non-aspect
    df_segment = df_segment[(~df_segment.isnull()).any(axis=1)]

    # We must make the array "jagged" according to the "aspect size",
    # since aspect size may differ frame-by-frame
    worm_aspect_size = df_segment.loc[:, ('aspect_size')]

    data_segment.append(('t', list(df_segment.index)))

    keys_used = [k for k in df_segment.columns.get_level_values('key')
                 if k != 'aspect_size']

    # e.g. ox, oy, head, ventral
    for key in [k for k in keys_used if k in elements_without_aspect]:
        cur_segment_slice = df_segment.loc[:, idx[key, 0]]
        # We must replace NaN with None, otherwise the JSON encoder will
        # save 'NaN' as the string and this will get rejected by our schema
        # on any subsequent loads
        # Note we can't use .fillna(None) due to this issue:
        # https://github.com/pydata/pandas/issues/1972
        if key in ['head', 'ventral']:
            cur_segment_slice = \
                cur_segment_slice.where(pd.notnull(cur_segment_slice), None)
        cur_list = list(np.array(cur_segment_slice))
        data_segment.append((key, cur_list))

    # e.g. x, y
    for key in [k for k in keys_used if k in elements_with_aspect]:
        non_jagged_array = df_segment.loc[:, (key)]

        jagged_array = []

        for t in non_jagged_array.index:
            # If aspect size isn't defined, don't bother adding data here:
            if np.isnan(worm_aspect_size.loc[t, 0]):
                jagged_array.append([])
            else:
                cur_aspect_size = int(worm_aspect_size.loc[t, 0])
                # For some reason loc's slice notation is INCLUSIVE!
                # so we must subtract one from cur_aspect_size, so if
                # it's 3, for instance, we get only entries
                # 0, 1, and 2, as required.
                cur_entry = non_jagged_array.loc[t, 0:cur_aspect_size - 1]
                cur_entry = list(np.array(cur_entry))
                jagged_array.append(cur_entry)

        data_segment.append((key, jagged_array))

    return OrderedDict(data_segment)


def data_as_array(df):
    """
    Convert a pandas dataframe into an array of objects conforming
    to the WCON standard.

    Parameters
    ------------
    df: Pandas DataFrame

    Returns
    ------------
    A list of dictionaries
        Conforms to WCON standard

    """
    arr = []

    # We'd like to use df.to_dict(),
    # but you'll first have to simplify the multiindex, by taking slices
    # (across time) and then interating over those slices
    for worm_id in set(df.columns.get_level_values('id')):
        try:
            # Downconvert any numpy data types (which are not
            # JSON-serializable) into native Python data types
            worm_id = worm_id.item()
        except AttributeError:
            # If the item wasn't a numpy data type, .item() won't be available
            # just ignore this error
            pass

        df_segment = df.xs(worm_id, level='id', axis=1)

        arr.append(_data_segment_as_odict(worm_id, df_segment))

    return arr


def get_sorted_ordered_dict(d):
    """
    Recursively sort all levels of a potentially nested dict.

    From http://stackoverflow.com/questions/22721579/

    Parameters
    -----------
    d: a potentially nested dict

    Returns
    -----------
    A potentially nested OrderedDict object
        All keys at each nesting level are sorted alphabetically

    """
    od = OrderedDict()
    for k, v in sorted(d.items()):
        if isinstance(v, dict):
            od[k] = get_sorted_ordered_dict(v)
        else:
            od[k] = v
    return od
