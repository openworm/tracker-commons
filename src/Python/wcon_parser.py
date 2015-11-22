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

assert()

"""

import json
import numpy as np
import pandas as pd

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


class WCONWorm():
    """
    A worm as specified by the WCON standard

    Usage
    -------------
    # From a file:
    with open('my_worm.wcon', 'r') as infile:
        w1 = WCONWorm.load(infile)
    
    # From a string literal:
    from io import StringIO
    w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}}'))
    
    """

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
        
        root = json.loads(serialized_data, object_hook=restore,
                          object_pairs_hook=reject_duplicates)
    
        # TODO: ensure it's the first thing, not just that it's present
        # if it's not the first thing, raise a warning
        if not ('tracker-commons' in root and root['tracker-commons']):
            raise AssertionError("'tracker-commons':true was not present")
    
        # Check for empty tracker file?
    
        if not ('units' in root):
            raise AssertionError("'units' is required")
        else:
            w.units = root['units']
            
            for key in w.units:
                w.units[key] = MeasurementUnit(w.units[key])

        if 'metadata' in root:
            # TODO
            pass
        
        if 'data' in root and len(root['data']) > 0:
            data = root['data']

            # Time-series data goes into this Pandas DataFrame
            # The dataframe will have t as index, and multilevel columns
            # with id at the first level and all other keys at second level.
            time_df = pd.DataFrame()

            # Get a list of all ids in data
            ids = list(set([x['id'] for x in data if 'id' in x]))

            # If data is single-valued, wrap it in a list so it will be just
            # a special case of the array case.
            if type(data) == dict:
                data = [data]

            
            time_free_indexes = []
            timeframes = []
            # Go through all data segments, flagging the non-time-series data
            # for later processing, and adding all time-series data to our
            # dataframe
            for (data_index, data_segment) in enumerate(data):
                if not 't' in data_segment:
                    # Grab the locations of the time-free data
                    time_free_indexes.append(data_index)
                else:
                    # Find all time segments, adding them to our dataframe
                    if(type(data_segment['t']) == list):
                        timeframes.extend(data_segment['t'])
                    else:
                        timeframes.append(data_segment['t'])

                    if not 'id' in data_segment:
                        # TIME-SERIES PLATE FEATURE
                        pass
                    else:
                        # TIME-SERIES WORM FEATURE
                        segment_id = data_segment['id']
                        
                        segment_keys = list(data_segment.keys())
                        segment_keys.remove('t')
                        segment_keys.remove('id')

                        # if t is arrayed, validate that all other attributes
                        # are either single-valued or 

                        """

                        import pdb
                        pdb.set_trace()

        
                        # Create our column names as the cartesian product of
                        # the segment's keys and the id of the segment
                        cur_columns = pd.MultiIndex.from_tuples(
                            [x for x in itertools.product(segment_keys, 
                                                          segment_id)])
                        
                        cur_df = pd.DataFrame([data[key] for key in segment_keys],
                                              columns=cur_columns)
                        cur_df.set_index(['t',data['t']])
                        
                        import pdb
                        pdb.set_trace()
                        #time_df.columns.append(cur_columns)
                        """

        # DEBUG: temporary
        w.root = root

        return w

    

JSON_path = '../../tests/hello_world.wcon'
f = open(JSON_path, 'r')
#w1 = WCONWorm_old.load(JSON_path)
with open(JSON_path, 'r') as infile:
    w1 = WCONWorm.load(infile)

u = MeasurementUnit('cm')