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

def isnamedtuple(obj):
    """
    Heuristic check if an object is a namedtuple.

    """
    return isinstance(obj, tuple) \
           and hasattr(obj, "_fields") \
           and hasattr(obj, "_asdict") \
           and callable(obj._asdict)

def serialize(data):
    """
    """

    if data is None or isinstance(data, (bool, int, float, str)):
        return data
    if isinstance(data, list):
        return [serialize(val) for val in data]
    if isinstance(data, OrderedDict):
        return {"py/collections.OrderedDict":
                [[serialize(k), serialize(v)] for k, v in data.items()]}
    if isnamedtuple(data):
        return {"py/collections.namedtuple": {
            "type":   type(data).__name__,
            "fields": list(data._fields),
            "values": [serialize(getattr(data, f)) for f in data._fields]}}
    if isinstance(data, dict):
        if all(isinstance(k, str) for k in data):
            return {k: serialize(v) for k, v in data.items()}
        return {"py/dict": [[serialize(k), serialize(v)] for k, v in data.items()]}
    if isinstance(data, tuple):
        return {"py/tuple": [serialize(val) for val in data]}
    if isinstance(data, set):
        return {"py/set": [serialize(val) for val in data]}
    if isinstance(data, np.ndarray):
        return {"py/numpy.ndarray": {
            "values": data.tolist(),
            "dtype":  str(data.dtype)}}
    raise TypeError("Type %s not data-serializable" % type(data))

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

def data_to_json(data):
    """
    """

    return json.dumps(serialize(data))

def json_to_data(s):
    """
    """

    return json.loads(s, object_hook=restore)


class JSON_Serializer():
    """
    A class that can save all of its attributes to a JSON file, or 
    load them from a JSON file.
    
    """
    def __init__(self):
        pass
    
    def save_to_JSON(self, JSON_path):
        serialized_data = data_to_json(list(self.__dict__.items()))

        with open(JSON_path, 'w') as outfile:
            outfile.write(serialized_data)

    def load_from_JSON(self, JSON_path):
        with open(JSON_path, 'r') as infile:
            serialized_data = infile.read()

        member_list = json_to_data(serialized_data)
        
        for member in member_list:
            setattr(self, member[0], member[1])



class WCONWorm_old(JSON_Serializer):

    @classmethod
    def load(cls, JSON_path):
        """
        Factory method to create a WCONWorm_old instance

        """
        w = cls()
        w.load_from_JSON(JSON_path)
        
        return w



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
        
        root = json.loads(serialized_data, object_hook=restore)
    
        # TODO: ensure it's the first thing, not just that it's present
        # if it's not the first thing, raise a warning
        if not ('tracker-commons' in root and root['tracker-commons']):
            raise AssertionError("'tracker-commons':true was not present")
    
        # Check for empty tracker file?
    
        if not ('units' in root):
            pass
            #raise AssertionError("'units' is required")
        else:
            w.units = root['units']
            
            for key in w.units:
                w.units[key] = MeasurementUnit(w.units[key])

        if 'metadata' in root:
            # TODO
            pass
        
        if 'data' in root:
            # TODO
            pass

        # DEBUG: temporary
        w.root = root

        return w

    

JSON_path = '../../tests/hello_world.wcon'
f = open(JSON_path, 'r')
#w1 = WCONWorm_old.load(JSON_path)
with open(JSON_path, 'r') as infile:
    w1 = WCONWorm.load(infile)

u = MeasurementUnit('cm')