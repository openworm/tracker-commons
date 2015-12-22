#!usr/bin/env python
# -*- coding: utf-8 -*-
"""
A module of methods to create the pandas DataFrame representing the "data"
array.

"""

import numpy as np
import pandas as pd
import itertools

# "Aspect" is my term for the articulation points at a given time frame
# So if you track 49 skeleton points per frame, then x and y have
# 49 aspects per frame, but ox is only recorded once per frame, or
# even once across the whole video.
elements_with_aspect = ['x', 'y']
elements_without_aspect = ['ox', 'oy', 'head', 'ventral']
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
    desired_indexes = [i for (i,s) in enumerate(arr) if desired_key in s]
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
    
    return dest


def convert_origin(data, offset_keys=['ox', 'oy'], coord_keys=['x', 'y']):
    """
    Add the offset values 'ox' and 'oy' to the 'x' and 'y' 
    coordinates in the dataframe

    Offset values that are NaN are considered to be zero.
    
    After this is done, set all offset values to zero.

    Parameters
    ------------
    data: Pandas DataFrame
    offset_keys: list of strings
        The offsets
    coord_keys: list of strings
        The corresponding coordinates to be offset
    
    Returns
    ------------
    None.  Modifies `data` in place.
    
    
    """
    for worm_id in data.columns.get_level_values(0).unique():
        cur_worms = data.loc[:,(worm_id)]

        for offset, coord in zip(offset_keys, coord_keys):
            if offset in cur_worms.columns.get_level_values(0):
                all_x_columns = cur_worms.loc[:,(coord)]
                ox_column = cur_worms.loc[:,(offset)].fillna(0)
                affine_change = (np.array(ox_column) * 
                                 np.ones(all_x_columns.shape))
                # Shift our 'x' values by the amount in 'ox'
                all_x_columns += affine_change
                data.loc[:,(worm_id,coord)] = all_x_columns.values
                
                # Now reset our 'ox' values to zero.
                data.loc[:,(worm_id,offset)] = np.zeros(ox_column.shape)

    # For simplicity let's actually just drop the offset columns entirely
    # from the dataframe.  This is so DataFrames with and without offsets
    # will show as comparing identically.
    for offset_key in offset_keys:
        data.drop(offset_key, axis=1, level='key', inplace=True)


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
    if type(data) == dict:
        data = [data]

    data = np.array(data)

    # Get a list of all ids in data
    #ids = list(set([x['id'] for x in data if 'id' in x]))

    is_time_series_mask = get_mask(data, 't')
    has_id_mask = get_mask(data, 'id')

    # We only care about data that have all the mandatory fields.
    # the Custom Feature Type 2 objects are suppresed by this filter:
    data = data[is_time_series_mask & has_id_mask]

    #pdb.set_trace()
    # Clean up and validate all time-series data segments
    _validate_time_series_data(data)

    # Obtain a numpy array of all unique timestamps used
    #timeframes = []
    #for data_segment in data[is_time_series_mask]:
    #    timeframes.extend(data_segment['t'])
    #timeframes = np.array(list(set(timeframes)))

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
                                   if not k in ['t','id']])
        
        cur_timeframes = np.array(data_segment['t'])

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
        max_aspect_size = max([k[0] for k in data_segment['aspect_size']])
        
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
    for (data_segment_index, data_segment) in enumerate(time_series_data):
        segment_keys = [k for k in data_segment.keys() if k != 'id']

        # If the 't' element is single-valued, wrap it and all other
        # elements into an array so single-valued elements
        # don't need special treatment in our later processing steps
        if type(data_segment['t']) != list:
            for subkey in segment_keys:
                data_segment[subkey] = [data_segment[subkey]]

        # Further, we have to wrap the elements without an aspect
        # in a further list so that when we convert to a dataframe,
        # our staging data list comprehension will be able to see
        # everything as a list and not as single-valued
        for subkey in elements_without_aspect:
            if subkey in data_segment:
                if type(data_segment[subkey]) != list:
                    data_segment[subkey] = [[data_segment[subkey]]]
                else:
                    if type(data_segment[subkey][0]) != list:
                        data_segment[subkey] = [[x] for x in data_segment[subkey]]

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
        
        data_segment['aspect_size'] = aspect_size_over_time


def dataframes_are_same(d1, d2):
    """
    Compare two dataframes.
    
    Works with MultiIndexed dataframes
    
    Returns True if they are the same, False otherwise.
    
    Treats NaNs "properly", that is, treating NaN == NaN.
    
    Returns False if the index or columns differ in any way.
    
    Parameters
    --------------
    d1, d2: Pandas DataFrames
    
    Returns
    --------------
    bool
    
    """
    for (cur_axis1, cur_axis2) in [(d1.index, d2.index), (d1.columns, d2.columns)]:
        if type(cur_axis1) != type(cur_axis2):
            return False
        
        if type(cur_axis1) == pd.core.index.MultiIndex:
            # For some reason you can't just do a comparison of two 
            # MultiIndex objects using "==", so here we do it in three steps:

            # e.g. FrozenList(['id', 'key', 'aspect'])
            if cur_axis1.names != cur_axis2.names:
                return False
            
            import pdb
            pdb.set_trace()            
            
            # e.g. FrozenList([[1, 2, 4], ['aspect_size', 'ox', 'x', 'y'], [0, 1, 2]])
            if any(cur_axis1.levels != cur_axis2.levels):
                return False
                
            if cur_axis1.labels != cur_axis2.labels:
                return False
        else:
            # The axis just has a simple index like Float64Index, so we
            # can do a direct comparison
            # e.g. Float64Index([1.3, 1.4, 1.5], dtype='float64')
            if any(cur_axis1 != cur_axis2):
                return False

    # Now we know that both axes have the same dimensions and labels.
    # Now let's compare the actual data.

    # Entries where both are nan, i.e. are NOT different
    # (despite the special behaviour of nan, that is, nan != nan)            
    nan_not_diff = np.logical_and(np.isnan(d1), np.isnan(d2))
    # Dataframes with no differences should be all True,
    # except NaN entries, which will show as False since nan != nan
    not_diff = d1 == d2

    # Now combine the two above to get the truth result of the whole
    # comparison
    return np.logical_or(not_diff, nan_not_diff).any().any()



THRESHOLD = 1e-08

def obtain_differences(df1, df2, index_columns):
    """
    Obtain differences between two dataframes
    Parameters
    ---------------
    df1, df2: DataFrame objects
        The must have the same dtypes and columns
    index_columns: list of strings
        The columns that are the indexes, which matter for 
        old_only and new_only, since it's these that we use to decide
    
    Returns
    ---------------
    old_only, new_only, differences: DataFrame objects
    
    """
    # Make sure our data types agree
    assert(all(df2.dtypes == df1.dtypes))
    # Make sure all the column names agree
    assert(all(df2.columns == df1.columns))
    
    # Re-index to unique values again
    df1.index = range(len(df1.index))
    df2.index = range(len(df2.index))
    
    # Index these tables by their index columns
    df1 = df1.set_index(index_columns)
    df2 = df2.set_index(index_columns)
    # Sort by this index
    df1.sort_index(axis=0, inplace=True)
    df2.sort_index(axis=0, inplace=True)

    # Concatenate the new and ond elements into one DataFrame
    combined = pd.concat({'old': df1, 'new': df2}, names=['NewOrOld'])
    combined = combined.reset_index()    

    # -----------------------------------------------------------------
    # FIND ROWS EXISTING ON NEW XOR OLD

    # Find entries that appear only in df1
    old_only = combined.reset_index().drop_duplicates(subset=index_columns, 
                                                      take_last=False)
    old_only = old_only[old_only['NewOrOld'] == 'old']
    old_only = old_only.set_index(index_columns)
    
    # Find entries that appear only in df2
    new_only = combined.reset_index().drop_duplicates(subset=index_columns, 
                                                      take_last=True)
    new_only = new_only[new_only['NewOrOld'] == 'new']
    new_only = new_only.set_index(index_columns)

    # -----------------------------------------------------------------
    # FIND ROWS EXISTING ON NEW AND OLD

    df1_common = df1[~df1.index.isin(old_only.index)]
    df2_common = df2[~df2.index.isin(new_only.index)]
    
    df1_common = df1_common.sort_index(axis=0, inplace=False)
    df2_common = df2_common.sort_index(axis=0, inplace=False)
    
    # True if the element is different between old/new, and False otherwise
    diff_mask = abs(df2_common - df1_common) >= THRESHOLD
    
    # Entries where one is nan but the other is not are different
    nan_diff = np.logical_xor(np.isnan(df2_common), np.isnan(df1_common))
    # Entries where both are nan, i.e. are NOT different (despite the special
    # behaviour of nan, that is, nan != nan)
    nan_not_diff = np.logical_and(np.isnan(df2_common), np.isnan(df1_common))

    # if nan_not_diff is true, then diff_mask must be false.
    diff_mask[nan_not_diff] = False
    diff_mask[nan_diff] = True
    
    # only rows containing differences.
    df1_common_different = df1_common[diff_mask.any(axis=1)]
    df2_common_different = df2_common[diff_mask.any(axis=1)]

    # get the elementwise diffs for just the rows that are different
    # these are the cells that should be highlighted red, btw
    diff_mask_common = diff_mask[diff_mask.any(axis=1)]

    shared3 = pd.concat({'old': df1_common_different, 
                         'new': df2_common_different}, 
                        names=['NewOrOld'])

    # Clear away the results in new that had no differences, so that they
    # aren't displayed in the excel sheet
    shared3.ix['new'][~diff_mask_common] = np.NaN

    if shared3.size == 0:
        # For some reason we can't reorder_levels with an empty dataframe
        # so we'll skip the reordering if the dataframe is empty
        shared4 = shared3
    else:
        # Now move new, old to the inner index so we see the differences 
        # close together
        num_levels = len(index_columns) + 1 # +1 for the NewOrOld index level
        reordered_levels = np.array((range(num_levels)))
        # Reorder so we go from [0,1,2,3], say, to [1,2,3,0]
        reordered_levels = ((reordered_levels + 1) % num_levels).tolist()
        
        shared4 = shared3.reorder_levels(reordered_levels, axis=0)
        shared4.sort_index(axis=0, inplace=True)

    return old_only, new_only, shared4