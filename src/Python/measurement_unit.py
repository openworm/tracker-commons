# -*- coding: utf-8 -*-
"""
MeasurementUnit

"""

from scipy.constants import F2C, K2C, C2F, C2K

def C2C(x):
    """
    An identity function, to convert Celsius to Celsius

    """
    return x


class MeasurementUnit():
    """
    Encapsulates the notion of a measurement unit, with an optional SI prefix.
    
    Provides methods to convert to and from the canonical representation:
        Temporal data: seconds ('s')
        Spatial data: millimetres ('mm')
        Temperature data: degrees Celsius ('C')
        Dimensionless data: (no units) ('')

    Usage
    -----------
    
    Initialize with a specific unit string like this: 
        u = MeasurementUnit('cm')

    In this case the prefix is 'c' (centi), and the suffix is 'm' (metres):
    
        assert(MeasurementUnit('centimetres') == u)
        
    To get the canonical (i.e. 'mm') representation of 1 cm:
    
        u.to_canon(1)
        
    This returns `10.0`.
    
    Attributes
    -----------

    prefix: str
        The original prefix
    suffix: str
        The original suffix
    unit_string: str
        The original string    
    canonical_prefix: str
        The canonical form for the prefix.
    canonical_suffix: str
        The canonical form for the suffix.
    canonical_unit_string: str
        canonical_prefix + canonical_suffix
    all_suffixes: list
        All available suffixes
    all_prefixes: list
        All available prefixes

    Methods
    ------------
    __init__
    __eq__
    __ne__
    to_canon
        transforms v from original units to canonical units
    from_canon
        the inverse of to_canon

    """    
    
    # PREFIXES
    SI_prefixes = {'': 1,
                   'c': 1e-2, 'centi': 1e-2,
                   'm': 1e-3, 'milli': 1e-3,
                   # Two ways to get the Greek Mu character in unicode:
                   'u': 1e-6, '\u00B5': 1e-6, '\u03BC': 1e-6, 'micro': 1e-6,
                   'n': 1e-9, 'nano': 1e-9,
                   'k': 1e+3, 'K': 1e+3, 'kilo': 1e+3,
                   'M': 1e+6, 'mega': 1e+6,
                   'G': 1e+9, 'giga': 1e+9}

    # "SUFFIXES" (i.e. units without prefixes)
    dimensionless_units = {'percent': 0.01, '%': 0.01, 
                           '': 1}

    temporal_units = {'s':1, 'sec':1, 'second':1, 'seconds':1,
                             'min':60*60, 'minute':60*60, 'minutes':60*60,
                      'h':60*60, 'hr':60*60, 'hour':60*60, 'hours':60*60,
                      'd':60*60*24, 'day':60*60*24, 'days':60*60*24}

    spatial_units = {'in': 0.0254, 'inch': 0.0254, 'inches': 0.0254,
                     'm': 1, 'metre': 1, 'meter': 1, 'metres': 1, 'meters': 1,
                     'micron': 1e-6, 'microns': 1e-6}

    # Converting between temperature units requires an affine function rather
    # than just a linear (scalar) multiple in the case of the temporal, 
    # spatial, and dimensionless units.  So here we store in the dictionary
    # a duple of functions:
    #    (to the canonical form, from the canonical form)
    temperature_units = {'F': (F2C, C2F),
                         'fahrenheit': (F2C, C2F),
                         'K': (K2C, C2K),
                         'kelvin': (K2C, C2K),
                         'C': (C2C, C2C),
                         'celsius': (C2C, C2C),
                         'centigrade': (C2C, C2C)}

    def __init__(self, unit_string):
        """
        Canonical units: 
           time: 's', 
           space: 'mm', 
           temperature: 'C'

        """
        self.unit_string = unit_string

        # Parse the string into a valid prefix and suffix
        self.prefix, self.suffix = self._parse_unit_string(unit_string)
        
        # Validate that our prefix and suffix don't mix abbreviations and 
        # long form, which is forbidden by the WCON specification
        self._validate_no_mixed_abbreviations()
        
        self._obtain_canonical_representation()
        
    def __repr__(self):
        """
        Pretty-print a nice summary of this unit.

        """
        unit_types = {'m': 'spatial', 's': 'temporal', 'C': 'temperature',
                      '': 'dimensionless'}

        cur_type = unit_types[self.canonical_suffix]
        
        return ("MeasurementUnit type:" + cur_type + " " +
                "original form:'" + self.unit_string + "' canonical "
                "form:'" + self.canonical_unit_string + "'")

    def _validate_no_mixed_abbreviations(self):
        """
        CHECK: (from the WCON spec:)
        Abbreviated and full versions must not be mixed. 
        For instance, both "ms" and "milliseconds" are okay, but 
                           "msecond" and "millis" are not.

        If a problem is found, an AssertionError exception is raised.

        """
        plen = len(self.prefix)
        slen = len(self.suffix)
        if slen > 3:
            # (all prefixes <= 3 chars other than 'day' are abbreviations)
            if (0 < plen <= 3) and self.prefix != 'day':
                raise AssertionError("Error with '" + self.unit_string +"': "
                                     "suffix is a full word but prefix "
                                     "is not.")
        else:
            if plen > 3 or self.prefix == 'day':
                raise AssertionError("Error with '" + self.unit_string +"': "
                                     "suffix is an abbreviation but prefix "
                                     "is the full word.")

    def _obtain_canonical_representation(self):
        """
        Define the functions self.to_canon and self.from_canon, to convert
        values back and forth between the canonical representation.

        Depending on the unit:
        If time, use 's' (seconds)
        If length, use 'mm' (millimetres)
        If temperature, use 'C' (celsius)
        If dimensionless, just convert the value to have no multiplier
            (e.g. if %, divide by 100)
        If dimensionless, return the value again
        
        Parameters
        ------------
        value: float
            The value to be converted
        
        Returns
        ------------
        None.  But it creates the member attributes:
            canonical_prefix: str
            canonical_suffix: str
            to_canon: function
            from_canon: function
        
        """

        # Now obtain the standard form of the prefix & suffix, and 
        # the conversion factor needed.
        if self.suffix in list(self.temporal_units.keys()):
            self.canonical_prefix = ''
            self.canonical_suffix = 's'
            to_canon_func  = lambda x: x * self.temporal_units[self.suffix]
            from_canon_func = lambda x: x / self.temporal_units[self.suffix]

        elif self.suffix in list(self.spatial_units.keys()):
            self.canonical_prefix = 'm'
            self.canonical_suffix = 'm'
            to_canon_func   = lambda x: x * self.spatial_units[self.suffix]
            from_canon_func = lambda x: x / self.spatial_units[self.suffix]

        elif self.suffix in list(self.temperature_units.keys()):
            self.canonical_prefix = ''
            self.canonical_suffix = 'C'
            to_canon_func   = lambda x: self.temperature_units[self.suffix][0](x)
            from_canon_func = lambda x: self.temperature_units[self.suffix][1](x)

        else:
            self.canonical_prefix = ''
            self.canonical_suffix = ''
            to_canon_func   = lambda x: x * self.dimensionless_units[self.suffix]
            from_canon_func = lambda x: x / self.dimensionless_units[self.suffix]

        # Obtain the conversion it will take to make the units standard
        prefix_conversion_constant = \
            self._SI_prefix_conversion_constant(self.prefix, 
                                                self.canonical_prefix)

        self.to_canon = \
            lambda x: to_canon_func(x) * prefix_conversion_constant

        self.from_canon = \
            lambda x: from_canon_func(x) / prefix_conversion_constant

    def _parse_unit_string(self, unit_string):
        """
        Parse a unit_string into a valid prefix and suffix:

            prefix + suffix = unit_string
        
        If no prefix / suffix combination works, raise an AssertionError.
        
        Parameters
        -------------
        unit_string: str
        
        Returns
        -------------
        (prefix, suffix): A duple of strings
        
        """
        # We can't have any ambiguous unit names or SI prefixes, so first we 
        # confirm that there are no duplicate unit names
        assert(len(self.all_suffixes) == len(set(self.all_suffixes)))
        assert(len(self.all_prefixes) == len(set(self.all_prefixes)))

        # CASE 1: unit_string is just a suffix
        if unit_string in self.all_suffixes:
            # e.g. Careful to avoid ripping off the 'm' of 'metre' 
            #      thinking it's the SI prefix milli, leaving us with 
            #      a suffix of 'etre', which will not be found.
            prefix = ''
            suffix = unit_string
        else:
            # CASE 2: unit_string starts with an SI prefix
            # We must have a prefix, otherwise the unit_string is invalid
            candidate_prefixes = list(self.SI_prefixes.keys())
            # This prefix will always be found so remove it from consideration
            candidate_prefixes.remove('')
            # We wish to find the LONGEST prefix at start of unit_string
            longest_prefix_len = -1
            
            for candidate_prefix in candidate_prefixes:
                # If this prefix isn't at least longer than the current 
                # longest, there is no point in even considering it.
                if len(candidate_prefix) <= longest_prefix_len:
                    continue

                # If we find the candidate prefix at the start of unit_string,
                # this is our new prefix.
                if unit_string.find(candidate_prefix) == 0:
                    prefix = candidate_prefix
                    suffix = unit_string[len(prefix):]
                    longest_prefix_len = len(prefix)
        
            # CASE 3: unit_string is invalid.  
            # (It not a valid suffix, nor does it start with a valid prefix)
            if longest_prefix_len == -1 or not suffix in self.all_suffixes:
                raise AssertionError("Error: '" + unit_string + "' is not a "
                                     "valid unit")
        
        return prefix, suffix

    def _SI_prefix_conversion_constant(self, from_prefix, to_prefix = ''):
        """
        Get the conversion constant to convert from any SI prefix to any other
    
        """
        if from_prefix is None:
            from_prefix = ''
        # Make sure we are using valid prefixes
        for prefix in [from_prefix, to_prefix]:
            if not prefix in self.SI_prefixes:
                raise AssertionError("Error: " + prefix + " is not a valid "
                                     "SI prefix")
    
        return self.SI_prefixes[from_prefix] / self.SI_prefixes[to_prefix]    

    @property    
    def all_suffixes(self):
        try:
            return self._all_suffixes
        except AttributeError:
            self._all_suffixes = (list(self.temporal_units.keys()) + 
                                  list(self.spatial_units.keys()) + 
                                  list(self.temperature_units.keys()) +
                                  list(self.dimensionless_units.keys()))

            return self._all_suffixes

    @property    
    def all_prefixes(self):
        try:
            return self._all_prefixes
        except AttributeError:
            self._all_prefixes = list(self.SI_prefixes.keys())
        
            return self._all_prefixes

    
    @property
    def canonical_unit_string(self):
        """
        Return one of 's', 'mm', 'C', or '' (for dimensionless)

        """
        return self.canonical_prefix + self.canonical_suffix
 
    def __eq__(self, other):
        """
        Returns if the units are the same.
        
        That is 'm' and 'metre' will return True
        But 'um' and 'm' will return False
        """
        # Any value would work except the point where Celsius == Fahrenheit,
        # i.e. 0 or 32
        return self.to_canon(10) == other.to_canon(10)

    def __ne__(self, other):
        return self.to_canon(10) != other.to_canon(10)
 
