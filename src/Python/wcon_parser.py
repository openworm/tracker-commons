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
import scipy.constants

def C2C(x):
    """
    An identity function, to convert Celsius to Celsius

    """
    return x


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


class MeasurementUnit():
    temporal_units = {'s':1, 'sec':1, 'second':1, 'seconds':1,
                      'h':60*60, 'hr':60*60, 'hour':60*60, 'hours':60*60,
                      'd':60*60*24, 'day':60*60*24, 'days':60*60*24}

    spatial_units = {'i': 0.0254, 'in': 0.0254, 'inch': 0.0254, 'inches': 0.0254,
                     'm': 1, 'metre': 1, 'meter': 1, 'metres': 1, 'meters': 1,
                     'micron': 1e-6}

    temperature_units = {'F': scipy.constants.F2C,
                         'fahrenheit': scipy.constants.F2C,
                         'K': scipy.constants.K2C,
                         'kelvin': scipy.constants.K2C,
                         'C': C2C,
                         'celsius': C2C,
                         'centigrade': C2C}

    dimensionless_units = {'percent': 0.01, '%': 0.01, 
                           '': 1}

    SI_prefixes = {'': 1,
                   'c': 1e-2, 'centi': 1e-2,
                   'm': 1e-3, 'milli': 1e-3,
                   # Two ways to get the Greek Mu character in unicode:
                   'u': 1e-6, '\u00B5': 1e-6, '\u03BC': 1e-6, 'micro': 1e-6,
                   'n': 1e-9, 'nano': 1e-9,
                   'k': 1e+3, 'K': 1e+3, 'kilo': 1e+3,
                   'M': 1e+6, 'mega': 1e+6,
                   'G': 1e+9, 'giga': 1e+9}

    def __init__(self, unit_string):
        """
        Standard units: 
           time: 's', 
           space: 'um', 
           temperature: 'C'

        """
        self.unit_string = unit_string

        all_suffixes = (list(self.temporal_units.keys()) + 
                        list(self.spatial_units.keys()) + 
                        list(self.temperature_units.keys()) +
                        list(self.dimensionless_units.keys()))

        all_prefixes = list(self.SI_prefixes.keys())

        # We can't have any ambiguous unit names or SI prefixes, so first we 
        # confirm that there are no duplicate unit names
        assert(len(all_suffixes) == len(set(all_suffixes)))
        assert(len(all_prefixes) == len(set(all_prefixes)))

        # CASE 1: unit_string is just a suffix
        if unit_string in all_suffixes:
            # e.g. Careful to avoid ripping off the 'm' of 'metre' 
            #      thinking it's the SI prefix milli, leaving us with 
            #      a suffix of 'etre', which will not be found.
            prefix = ''
            suffix = unit_string
        else:
            # CASE 2: unit_string starts with an SI prefix
            # We must have a prefix, otherwise the unit_string is invalid
            found_prefix = False
            candidate_prefixes = list(self.SI_prefixes.keys())
            # This prefix will apply in all cases so get rid of it
            candidate_prefixes.remove('')

            for candidate_prefix in candidate_prefixes:
                # If this candidate_prefix is at the beginning of the 
                # unit_string, we are done
                if unit_string.find(candidate_prefix) == 0:
                    prefix = candidate_prefix
                    suffix = unit_string[len(prefix):]
                    found_prefix = True
        
            # CASE 3: unit_string is invalid.  
            # (It not a valid suffix, nor does it start with a valid prefix)
            if not found_prefix or not suffix in all_suffixes:
                raise AssertionError("Error: '" + unit_string + "' is not a "
                                     "valid unit")
        
        # Now obtain the standard form of the prefix & suffix, and 
        # the conversion factor needed.
        if suffix in list(self.temporal_units.keys()):
            self.standard_prefix = ''
            self.standard_suffix = 's'
            suffix_func = lambda x: x * self.temporal_units[suffix]

        elif suffix in list(self.spatial_units.keys()):
            self.standard_prefix = 'u'
            self.standard_suffix = 'm'
            suffix_func = lambda x: x * self.spatial_units[suffix]

        elif suffix in list(self.temperature_units.keys()):
            self.standard_prefix = ''
            self.standard_suffix = 'C'
            suffix_func = lambda x: self.temperature_units[suffix](x)

        else:
            self.standard_prefix = ''
            self.standard_suffix = ''
            suffix_func = lambda x: x * self.dimensionless_units[suffix]

        # Obtain the conversion it will take to make the units standard
        self.prefix_conversion_constant = \
            self.SI_prefix_conversion_constant(prefix, self.standard_prefix)

        self.conversion_func = \
            lambda x: suffix_func(x) * self.prefix_conversion_constant
    
        self.prefix = prefix
        self.suffix = suffix

    @property
    def standard_unit_string(self):
        """
        Return one of 's', 'um', 'C', or '' (for dimensionless)

        """
        return self.standard_prefix + self.standard_suffix
        
    def convert_to_standard_value(self, value):
        """
        Depending on the unit:
        If time, use 's' (seconds)
        If length, use 'u' (microns)
        If temperature, use 'c' (celsius)
        If percent, just convert the value to be dimensionless 
            (i.e. divide by 100)
        If dimensionless, return the value again
        
        Parameters
        ------------
        value: float
            The value to be converted
        
        Returns
        ------------
        standard_value: float
        
        """
        return self.conversion_func(value)
 
    def __eq__(self, other):
        """
        Returns if the units are the same.
        
        That is 'm' and 'metre' will return True
        But 'um' and 'm' will return False
        """
        # Any value would work except the point where Celsius == Fahrenheit,
        # i.e. 0 or 32
        return self.conversion_func(10) == other.conversion_func(10)

    def __ne__(self, other):
        return self.conversion_func(10) != other.conversion_func(10)
 
    def SI_prefix_conversion_constant(self, from_prefix, to_prefix='u'):
        """
        Get the conversion constant to convert from any SI prefix to any other
    
        Actually converts inches, which are not technically SI
    
        """
        if from_prefix is None:
            from_prefix = ''
        # Make sure we are using valid prefixes
        for prefix in [from_prefix, to_prefix]:
            if not prefix in self.SI_prefixes:
                raise AssertionError("Error: " + prefix + " is not a valid "
                                     "SI prefix")
    
        return self.SI_prefixes[from_prefix] / self.SI_prefixes[to_prefix]       


class WCONWorm():

    temporal_units = ['s', 'sec', 'second', 'seconds',
                      'm', 'min', 'minute', 'minutes',
                      'h', 'hr', 'hour', 'hours',
                      'd', 'day', 'days']
    
    @classmethod
    def load(cls, JSON_path):
        """
        Factory method to create a WCONWorm instance

        """
        w = cls()
        with open(JSON_path, 'r') as infile:
            serialized_data = infile.read()
        
        root = json.loads(serialized_data, object_hook=restore)
    
        if not ('tracker-commons' in root and root['tracker-commons']):
            raise AssertionError("'tracker-commons':true was not present")
    
        # Check for empty tracker file?
    
        if not ('units' in root):
            raise AssertionError("'units' is required")
        else:
            w.units = root['units']

        if 'metadata' in root:
            # TODO
            pass
        
        if 'data' in root:
            # TODO
            pass

        return w

    

JSON_path = '../../tests/hello_world.wcon'
#w1 = WCONWorm_old.load(JSON_path)
w1 = WCONWorm.load(JSON_path)

