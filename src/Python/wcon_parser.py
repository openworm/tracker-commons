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
import warnings
import itertools
import json
import numpy as np
import pandas as pd

from io import StringIO

from measurement_unit import MeasurementUnit

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
    w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}}'))
    
    """
    basic_keys = ['tracker-commons', 'units', 'metadata', 'data']


    @classmethod
    def load(cls, JSON_stream):
        """
        Factory method to create a WCONWorm instance
        
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
        # BASIC TOP-LEVEL VALIDATION
        
        if not ('tracker-commons' in root):
            warnings.warn('{"tracker-commons":true} was not present. '
                          'Nevertheless proceeding under the assumption '
                          'this is a WCON file.')
        
        if 'tracker-commons' in root and root['tracker-commons'] == False:
            raise AssertionError('"tracker-commons" is set to false.')
    
        # ===================================================
        # HANDLE THE BASIC TAGS: 'units', 'metadata', 'data'    
    
        if not ('units' in root):
            raise AssertionError('"units" is required')
        else:
            w.units = root['units']
            
            for key in w.units:
                w.units[key] = MeasurementUnit(w.units[key])

        if 'metadata' in root:
            w.metadata = w._parse_metadata(root['metadata'])
        else:
            w.metadata = None
        
        if 'data' in root and len(root['data']) > 0:
            w.data = w._parse_data(root['data'])
        else:
            w.data = None
        
        # ===================================================    
        # Any special top-level keys are sent away for later processing
        w.special_keys = [k for k in root.keys() if k not in w.basic_keys]
        if w.special_keys:
            special_root = {k: root[k] for k in w.special_keys}
            w._parse_special_top_level_objects(special_root)

        # DEBUG: temporary
        w.root = root

        return w


    def _parse_metadata(self, metadata):
        """
        Parse the 'metadata' element
        
        Return an object encapsulating that data
        
        """
        # TODO
        pass

    def _parse_data(self, data):
        """
        Parse the 'data' element
        
        Return an object encapsulating that data
        
        """
        # If data is single-valued, wrap it in a list so it will be just
        # a special case of the array case.
        if type(data) == dict:
            data = [data]

        data = np.array(data)

        basic_data_keys = ['id', 't', 'x', 'y', 'ox', 'oy', 'head', 'ventral']

        # Get a list of all ids in data
        ids = list(set([x['id'] for x in data if 'id' in x]))

        is_time_series_mask = get_mask(data, 't')
        has_id_mask = get_mask(data, 'id')

        # Time-series data goes into this Pandas DataFrame
        # The dataframe will have t as index, and multilevel columns
        # with id at the first level and all other keys at second level.
        time_df = None

        # Clean up and validate all time-series data segments
        for data_segment in data[is_time_series_mask]:
            segment_keys = [k for k in data_segment.keys() if k != 'id']

            # If the 't' element is single-valued, wrap it and all other
            # elements into an array so single-valued elements
            # don't need special treatment in our later processing steps
            if type(data_segment['t']) != list:
                for subkey in segment_keys:
                    data_segment[subkey] = [data_segment[subkey]]

            # Validate that all sub-elements have the same length
            subelement_lengths = [len(data_segment[key]) 
                                  for key in segment_keys]

            if len(set(subelement_lengths)) > 1:
                raise AssertionError("Error: Subelements must all have "
                                     "the same length.")

            # Now we can speak of a subelement_length, as it is 
            # now well-defined.
            subelement_length = subelement_lengths[0]

            for i in range(subelement_length):
                # The x and y arrays for element i of the data segment
                # must have the same length
                if len(data_segment['x'][i]) != len(data_segment['y'][i]):
                    raise AssertionError("Error: x and y must have same "
                                         "length for index " + str(i))
                else:
                    subelement_xylength = len(data_segment['x'][i])


        # Obtain a numpy array of all unique timestamps used
        timeframes = []
        for data_segment in data[is_time_series_mask]:
            timeframes.extend(data_segment['t'])
        timeframes = np.array(list(set(timeframes)))


        # Consider only time-series data stamped with an id:
        for data_segment in data[is_time_series_mask & has_id_mask]:
            # TODO
            # Add this data_segment to a Pandas dataframe
            segment_id = data_segment['id']
            segment_keys = np.array([k for k in data_segment.keys() 
                                       if not k in ['t','id']])
            # We want x before y
            segment_keys = np.sort(segment_keys)
            
            cur_timeframes = np.array(data_segment['t'])

            # Create our column names as the cartesian product of
            # the segment's keys and the id of the segment
            key_combos = list(itertools.product([segment_id], 
                                                segment_keys, 
                                                range(subelement_xylength)))
            column_type_names = ['id', 'key', 'aspect']
            cur_columns = pd.MultiIndex.from_tuples(key_combos,
                                                    names=column_type_names)

            #pdb.set_trace()
            
            #cur_df = pd.DataFrame([5.4,3,1,-3,3,4,4,3.2],
            #                      columns=cur_columns)

            #cur_df = pd.DataFrame(np.random.randn(8,8), columns=cur_columns)
            #cur_df = pd.DataFrame(np.random.randn(16,8), columns=cur_columns)

            df_data = np.array([data_segment[key] for key in segment_keys])

            # Shape KxI where K is the number of keys and 
            #                 I is the number of "aspects"
            df_data2 = np.array(
                [np.concatenate(
                                    [data_segment[k][i] for k in ['x', 'y']]
                               ) for i in range(len(cur_timeframes))])


            cur_df = pd.DataFrame(df_data2, columns=cur_columns)

            cur_df.index = cur_timeframes
            cur_df.index.names = 't'
            
            if time_df is None:
                time_df = cur_df
            else:
                # Append cur_df to time_df
                #pdb.set_trace()

                # DEBUG: doesn't merge indexes
                #time_df = pd.concat([time_df, cur_df])

                # DEBUG: doesn't merge columns
                #time_df = pd.concat([time_df, cur_df], axis=1)

                #pdb.set_trace()

                # Add all rows that don't currently exist in time_df
                time_df = pd.concat([time_df, cur_df[~cur_df.index.isin(time_df.index)]])

                # Obtain a sliced version of time_df, showing only
                # the columns and rows shared with the cur_df
                time_df_sliced = \
                    time_df.loc[time_df.index.isin(cur_df.index),
                                time_df.columns.isin(cur_df.columns)]
                
                # Obtain a mask of the conflicts in the current segment
                # as compared with all previously loaded data.  That is:
                # NaN NaN = False
                # NaN 2   = False
                # 2   2   = False
                # 2   3   = True
                # 2   NaN = True
                data_conflicts = (pd.notnull(time_df_sliced) & 
                                  (time_df_sliced != cur_df))
                                  
                if data_conflicts.any().any():
                    print("Data conflicts:")
                    print(data_conflicts)
                    raise Exception("Data from this segment conflicted "
                                    "with previously loaded data.")

                # Replace any rows that do exist with the cur_df version
                time_df.update(cur_df)
                
                #time_df = pd.merge(time_df, cur_df, 
                #                   left_index=True, right_index=True, 
                #                   how='outer')
                
                # TODO: concatenate using a list comprehension as calling it
                # iteratively like this causes a performance hit (see
                # http://pandas.pydata.org/pandas-docs/stable/merging.html#concatenating-objects
                # "Note It is worth noting however, that concat (and therefore append) 
                # makes a full copy of the data, and that constantly reusing this 
                # function can create a signifcant performance hit. If you need 
                # to use the operation over several datasets, use a list comprehension."

                #time_df = time_df.append(cur_df)#, 
                                         # If True, raise ValueError on 
                                         # creating index with duplicates.
                                         # This is a check that the data isn't
                                         # duplicated and thus ambiguous
                                         #verify_integrity=True)

            # TODO:
            # Check that a segment doesn't overwrite a previously read one
            pass

        # We want the index (time) to be in order.
        time_df.sort_index(inplace=True)

        return time_df




    def _parse_special_top_level_objects(self, special_root):
        """
        Any top-level key other than the basic:
        
        - tracker-commons
        - units
        - metadata
        - data
        
        Is passed here for further processing.  
        
        To accomplish this, this class should be subclassed and 
        this method overwritten.
        
        """
        pass



pd.set_option('display.expand_frame_repr', False)


if __name__ == '__main__':

    JSON_path = '../../tests/hello_world_simple.wcon'
    f = open(JSON_path, 'r')
    #w1 = WCONWorm_old.load(JSON_path)
    with open(JSON_path, 'r') as infile:
        w1 = WCONWorm.load(infile)
    
    u = MeasurementUnit('cm')
    
if __name__ == '__main__2':
    w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}}'))

if __name__ == '__main__3':
    w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                '"data":[{"id":3, "t":1.3, '
                                         '"x":[3,4,4,3.2], '
                                         '"y":[5.4,3,1,-3]}]}'))


# Suppress RuntimeWarning warnings in Spider because it's a known bug
# http://stackoverflow.com/questions/30519487/
warnings.simplefilter(action = "ignore", category = RuntimeWarning)
