# -*- coding: utf-8 -*-
"""
WCON Parser

This parser accepts any valid WCON file, but the output is always "canonical"
WCON, which makes specific choices about how to arrange and format the WCON
file.  This way the functional equality of any two WCON files can be tested 
by this:

w1 = WCONWorm.load('file1.wcon')
w2 = WCONWorm.load('file2.wcon')

assert(w1 == w2)

# or:

w1.output('file1.wcon')
w2.output('file2.wcon')

import filecmp
assert(filecmp.cmp('file1.wcon', file2.wcon'))

"""

import pdb
import warnings, itertools
from io import StringIO
import numpy as np
import pandas as pd
import json, jsonschema

from measurement_unit import MeasurementUnit

def df_upsert(src, dest):
    """
    Append src to dest, doing an "update/insert" where
    conflicts cause an AssertionError to be raised.
    
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


def reject_duplicates(ordered_pairs):
    """Reject duplicate keys."""
    unique_dict = {}
    for key, val in ordered_pairs:
        if key in unique_dict:
           raise KeyError("Duplicate key: %r" % (key,))
        else:
           unique_dict[key] = val

    return unique_dict


def restore(dct):
    """
    """

    if "py/dict" in dct:
        return dict(dct["py/dict"])
    if "py/tuple" in dct:
        return tuple(dct["py/tuple"])
    if "py/set" in dct:
        return set(dct["py/set"])
    if "py/collections.namedtuple" in dct:
        data = dct["py/collections.namedtuple"]
        return namedtuple(data["type"], data["fields"])(*data["values"])
    if "py/numpy.ndarray" in dct:
        data = dct["py/numpy.ndarray"]
        return np.array(data["values"], dtype=data["dtype"])
    if "py/collections.OrderedDict" in dct:
        return OrderedDict(dct["py/collections.OrderedDict"])
    return dct


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



class WormData():
    """
    A worm's data (stripped of units and metadata)

    Time-series and aggregate data
    Tagged by worm or tagged by plate    
    
    Attributes
    -------------
    time_df: Pandas DataFrame
        Indexed by time
        All worms
        Plate data uses id:'plate'
    
    
    """
    pass


class WCONWorm():
    """
    A worm as specified by the WCON standard

    Attributes
    -------------
    units: dict
        May be empty, but is never None since 'units' is required 
        to be specified.
    metadata: dict
        If 'metadata' was not specified, metadata is None.
        The values in this dict might be nested into further dicts or other
        data types.
    data: Pandas DataFrame or None
        If 'data' was not specified, data is None.

    Usage
    -------------
    # From a file:
    with open('my_worm.wcon', 'r') as infile:
        w1 = WCONWorm.load(infile)
    
    # From a string literal:
    from io import StringIO
    w2 = WCONWorm.load(StringIO('{"tracker-commons":true, '
                                '"units":{},"data":{}}'))

    Custom WCON versions
    --------------------
    
    Any top-level key other than the basic:
    
    - tracker-commons
    - files
    - units
    - metadata
    - data
    
    Is assigned to the dictionary object special_root
    
    To process these items, this class should be subclassed and this method
    overwritten.
    
    """
    basic_keys = ['tracker-commons', 'files', 'units', 'metadata', 'data']

    @property
    def schema(self):
        try:
            return self._schema
            
        except AttributeError:
            # Only load _schema if this method gets called.  Once
            # it's loaded, though, persist it in memory and don't lose it
            with open("../../wcon_schema.json", "r") as wcon_schema_file:
                self._schema = json.loads(wcon_schema_file.read())

            # Now that the schema has been loaded, we can try again
            return self._schema

    @classmethod
    def validate_from_schema(cls, wcon_string):
        with open("../../wcon_schema.json", "r") as wcon_schema_file:
            wcon_schema = json.loads(wcon_schema_file.read())

        jsonschema.validate(json.load(StringIO(wcon_string)), wcon_schema)

    @classmethod
    def does_data_clash(cls, w1, w2):
        """
        """
        #TODO
        return False

    @classmethod
    def merge(cls, w1, w2):
        """
        Merge two worms

        Any clashing data found will cause an exception to be raised.
        
        Clashes are checked at a low level of granularity: 
        e.g. if two worms have different metadata but the individual metadata
        entries do not conflict, this method will still fail and raise an
        AssertionError.
        
        """
        if cls.does_data_clash(w1, w2):
            raise AssertionError("Data conflicts between worms to be merged")
            
        # TODO: implement this properly
        return w1

    def __eq__(self, other):
        """
        Comparison operator (overloaded)
        
        """
        return True

    @classmethod
    def load_from_file(cls, JSON_path, 
                       load_prev_chunks=True, 
                       load_next_chunks=True):
        """
        Factory method returning a merged WCONWorm instance of the file
        located at JSON_path and all related "chunks" as specified in the 
        "files" element of the file.

        Uses recursion if there are multiple chunks.
        
        Parameters
        -------------
        JSON_path: a file path to a file that can be opened
            
        load_prev_chunks: bool
            If a "files" key is present, load the previous chunks and merge
            them with this one.  If not present, return only the current 
            file's worm.

        load_next_chunks: bool
            If a "files" key is present, load the next chunks and merge
            them with this one.  If not present, return only the current 
            file's worm.

        """
        print("Loading file: " + JSON_path)

        with open(JSON_path, 'r') as infile:
            w_current = cls.load(infile)

        # CASE 1: NO "files" OBJECT, hence no multiple files.  We are done.
        if w_current.files is None:
            return w_current

        # OTHERWISE, CASE 2: MULTIPLE FILES

        # The schema guarantees that if "files" is present, 
        # "this", "prev" and "next" will exist.  Also, that "this" is not null.
        cur_ext = w_current.files['this']

        if cur_ext == '':
            raise AssertionError('["files"]["this"] == "", which is not '
                                 'a valid file extension for multichunk data')

        # e.g. cur_filename = 'filename_2.wcon'
        # cur_ext = '_2', prefix = 'filename', suffix = '.wcon'
        cur_filename = JSON_path
        if cur_filename.find(cur_ext) == -1:
            raise AssertionError('Cannot find the current extension "'
                                 + cur_ext + '" within the current filename "'
                                 + cur_filename + '".')
        prefix = cur_filename[:cur_filename.find(cur_ext)]
        suffix = cur_filename[cur_filename.find(cur_ext)+len(cur_ext):]
        
        load_chunks= {'prev': load_prev_chunks,
                      'next': load_next_chunks}
        
        for direction in ['prev', 'next']:
            # If we are supposed to load the previous chunks, and one exists, 
            # load it and merge it with the current chunk
            # Same with the "next" chunks
            if (load_chunks[direction] and 
                not w_current.files is None and 
                not w_current.files[direction] is None):
                    cur_load_prev_chunks = (direction == 'prev')
                    cur_load_next_chunks = (direction == 'next')
    
                    new_file_name = (prefix + w_current.files[direction][0] + 
                                     suffix)
                    w_new = cls.load_from_file(new_file_name,
                                               cur_load_prev_chunks,
                                               cur_load_next_chunks)
                    w_current = cls.merge(w_current, w_new)

        return w_current
        


    @classmethod
    def load(cls, JSON_stream):
        """
        Factory method to create a WCONWorm instance
        
        This does NOT load chunks, because a file stream does not 
        have a file name.  In order to load chunks, you must invoke the
        factory method load_from_file.  You will be passing it a file path 
        from which it can find the other files/chunks.
        
        Parameters
        -------------
        JSON_stream: a text stream implementing .read()
            e.g. an object inheriting from TextIOBase
                    
        """
        w = cls()

        serialized_data = JSON_stream.read()

        # Load the whole JSON file into a nested dict.  Any duplicate
        # keys raise an exception since we've hooked in reject_duplicates
        root = json.loads(serialized_data, object_hook=restore,
                          object_pairs_hook=reject_duplicates)
    
        # ===================================================
        # BASIC TOP-LEVEL VALIDATION AGAINST THE SCHEMA

        # Validate the raw file against the WCON schema
        jsonschema.validate(root, w.schema)
    
        if not ('tracker-commons' in root):
            warnings.warn('{"tracker-commons":true} was not present. '
                          'Nevertheless proceeding under the assumption '
                          'this is a WCON file.')

        # ===================================================
        # HANDLE THE REQUIRED ELEMENTS: 'units', 'data'    
    
        w.units = root['units']
        
        for key in w.units:
            w.units[key] = MeasurementUnit(w.units[key])

        
        if len(root['data']) > 0:
            w.data = w._parse_data(root['data'])
            
            # Shift the coordinates by the amount in the offsets 'ox' and 'oy'
            w._convert_origin()
        else:
            # "data": {}
            w.data = None

        # ===================================================
        # HANDLE THE OPTIONAL ELEMENTS: 'files', 'metadata'

        if 'files' in root:
            w.files = root['files']
        else:
            w.files = None

        if 'metadata' in root:
            w.metadata = w._parse_metadata(root['metadata'])
        else:
            w.metadata = None
        
        # ===================================================
        # Any special top-level keys are sent away for later processing
        w.special_keys = [k for k in root.keys() if k not in w.basic_keys]
        if w.special_keys:
            w.special_root = {k: root[k] for k in w.special_keys}

        # DEBUG: temporary
        w.root = root

        return w


    def _parse_metadata(self, metadata):
        """
        Parse the 'metadata' element
        
        Return an object encapsulating that data
        
        """
        return metadata


    def _convert_origin(self):
        """
        Add the offset values 'ox' and 'oy' to the 'x' and 'y' 
        coordinates in the dataframe

        Offset values that are NaN are considered to be zero.
        
        After this is done, set all offset values to zero.
        
        """
        for worm_id in self.data.columns.get_level_values(0).unique():
            cur_worms = self.data.loc[:,(worm_id)]

            for offset, coord in zip(['ox', 'oy'], ['x', 'y']):
                if offset in cur_worms.columns.get_level_values(0):
                    all_x_columns = cur_worms.loc[:,(coord)]
                    ox_column = cur_worms.loc[:,(offset)].fillna(0)
                    affine_change = (np.array(ox_column) * 
                                     np.ones(all_x_columns.shape))
                    # Shift our 'x' values by the amount in 'ox'
                    all_x_columns += affine_change
                    self.data.loc[:,(worm_id,coord)] = all_x_columns.values
                    
                    # Now reset our 'ox' values to zero.
                    self.data.loc[:,(worm_id,offset)] = \
                                                    np.zeros(ox_column.shape)

    # "Aspect" is my term for the articulation points at a given time frame
    # So if you track 49 skeleton points per frame, then x and y have
    # 49 aspects per frame, but ox is only recorded once per frame, or
    # even once across the whole video.
    @property
    def elements_with_aspect(self):
        return ['x', 'y']
    
    @property
    def elements_without_aspect(self):
        return ['ox', 'oy', 'head', 'ventral']
    
    @property
    def basic_data_keys(self):
        return self.elements_with_aspect + self.elements_without_aspect

    @property    
    def supported_data_keys(self):
        return self.basic_data_keys + ['id', 't']

    def _parse_data(self, data):
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

        # Clean up and validate all time-series data segments
        self._validate_time_series_data(data)

        # Obtain a numpy array of all unique timestamps used
        #timeframes = []
        #for data_segment in data[is_time_series_mask]:
        #    timeframes.extend(data_segment['t'])
        #timeframes = np.array(list(set(timeframes)))

        time_df = self._obtain_time_series_data_frame(data)

        return time_df


    def _obtain_time_series_data_frame(self, time_series_data):
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
                [k for k in self.elements_with_aspect if k in segment_keys]
            cur_elements_without_aspect = ['aspect_size'] + \
                [k for k in self.elements_without_aspect if k in segment_keys]

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
                df_upsert(src=cur_df, dest=time_df)

        # We want the index (time) to be in order.
        time_df.sort_index(axis=0, inplace=True)

        return time_df

    def _validate_time_series_data(self, time_series_data):
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
            for subkey in self.elements_without_aspect:
                if subkey in data_segment:
                    if type(data_segment[subkey]) != list:
                        data_segment[subkey] = [[data_segment[subkey]]]
                    else:
                        data_segment[subkey] = [[x] for x in data_segment[subkey]]

            subelement_length = len(data_segment['t'])

            # Broadcast aspectless elements to be 
            # length n = subelement_length if it's 
            # just being shown once right now  (e.g. for 'ox', 'oy', etc.)
            for k in self.elements_without_aspect:
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
                cur_aspect_sizes = [len(data_segment[k][t]) for k in 
                                         self.elements_with_aspect]
                
                if len(set(cur_aspect_sizes)) > 1:
                    raise AssertionError(
                        "Error: Aspects x and y, etc. must have same "
                        "length for data segment " + str(data_segment_index) +
                        " and time index " + str(data_segment['t'][t]))
                else:
                    aspect_size_over_time.append([cur_aspect_sizes[0]])
            
            data_segment['aspect_size'] = aspect_size_over_time



pd.set_option('display.expand_frame_repr', False)

# Suppress RuntimeWarning warnings in Spider because it's a known bug
# http://stackoverflow.com/questions/30519487/
warnings.simplefilter(action = "ignore", category = RuntimeWarning)


if __name__ == '__main__':

    JSON_path = '../../tests/hello_world_simple.wcon'

    print("Loading " + JSON_path)

    w2 = WCONWorm.load_from_file(JSON_path)

    with open(JSON_path, 'r') as infile:
        w1 = WCONWorm.load(infile)
    
    u = MeasurementUnit('cm')



    
if __name__ == '__main__3':
    w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                '"data":[{"id":3, "t":1.3, '
                                         '"x":[3,4,4,3.2], '
                                         '"y":[5.4,3,1,-3]}]}'))


