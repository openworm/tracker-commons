#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Two classes:  (only the second of which is generally public-facing)

- MeasurementUnitAtom, which handles just a single prefix and suffix
   e.g. mm, celsius, kilograms, etc.

- MeasurementUnit, which can handle the composite units as well
  e.g. mm^2, m/s, etc.

  Note that it cannot handle composite units involving Fahrenheit or Kelvin,
  since they require an affine transformation to the canonical unit expression
  e.g. F^2, K/s will not have to_canon and from_canon attributes that
  work properly.

  Single units involving Fahrenheit and Kelvin will work fine:
  e.g. F, K.

"""
import six
import ast
import operator as op
from scipy.constants import F2C, K2C, C2F, C2K


def C2C(x):
    """
    An identity function, to convert Celsius to Celsius

    """
    return x


class MeasurementUnitAtom():
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
        u = MeasurementUnitAtom('cm')

    In this case the prefix is 'c' (centi), and the suffix is 'm' (metres):

        assert(MeasurementUnitAtom('centimetres') == u)

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
                   'k': 1e+3, 'kilo': 1e+3,
                   'M': 1e+6, 'mega': 1e+6,
                   'G': 1e+9, 'giga': 1e+9}

    # "SUFFIXES" (i.e. units without prefixes)
    dimensionless_units = {'percent': 0.01, '%': 0.01,
                           '': 1}

    temporal_units = {
        's': 1,
        'sec': 1,
        'second': 1,
        'seconds': 1,
        'min': 60,
        'minute': 60,
        'minutes': 60,
        'h': 60 * 60,
        'hr': 60 * 60,
        'hour': 60 * 60,
        'hours': 60 * 60,
        'd': 60 * 60 * 24,
        'day': 60 * 60 * 24,
        'days': 60 * 60 * 24}

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

    unit_types = {'m': 'spatial', 's': 'temporal', 'C': 'temperature',
                  '': 'dimensionless'}

    def __init__(self, unit_string):
        """
        Canonical units:
           time: 's',
           space: 'mm',
           temperature: 'C'

        """
        # Reversing the kludge to fix that ast can't handle the reserved
        # word 'in'; see MeasurementUnit.create below for more details
        self.unit_string = unit_string.replace('_in', 'in')

        # Parse the string into a valid prefix and suffix
        self.prefix, self.suffix = self._parse_unit_string(self.unit_string)

        # Validate that our prefix and suffix don't mix abbreviations and
        # long form, which is forbidden by the WCON specification
        self._validate_no_mixed_abbreviations()

        self._obtain_canonical_representation()

    @property
    def unit_type(self):
        return self.unit_types[self.canonical_suffix]

    def __repr__(self):
        """
        Pretty-print a nice summary of this unit.

        """
        return ("||MeasurementUnitAtom type " + self.unit_type + " " +
                "original form '" + self.unit_string + "' canonical "
                "form '" + self.canonical_unit_string + "'||")

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
                raise AssertionError("Error with '" + self.unit_string + "': "
                                     "suffix is a full word but prefix "
                                     "is not.")
        else:
            if plen > 3 or self.prefix == 'day':
                raise AssertionError("Error with '" + self.unit_string + "': "
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

            def to_canon_func(x):
                return x * self.temporal_units[self.suffix]

            def from_canon_func(x):
                return x / self.temporal_units[self.suffix]

        elif self.suffix in list(self.spatial_units.keys()):
            self.canonical_prefix = 'm'
            self.canonical_suffix = 'm'

            def to_canon_func(x):
                return x * self.spatial_units[self.suffix]

            def from_canon_func(x):
                return x / self.spatial_units[self.suffix]

        elif self.suffix in list(self.temperature_units.keys()):
            self.canonical_prefix = ''
            self.canonical_suffix = 'C'

            def to_canon_func(x):
                return self.temperature_units[self.suffix][0](x)

            def from_canon_func(x):
                return self.temperature_units[self.suffix][1](x)

        else:
            self.canonical_prefix = ''
            self.canonical_suffix = ''

            def to_canon_func(x):
                return x * self.dimensionless_units[self.suffix]

            def from_canon_func(x):
                return x / self.dimensionless_units[self.suffix]

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
            if longest_prefix_len == -1 or suffix not in self.all_suffixes:
                raise AssertionError("Error: '" + unit_string + "' is not a "
                                     "valid unit")

        return prefix, suffix

    def _SI_prefix_conversion_constant(self, from_prefix, to_prefix=''):
        """
        Get the conversion constant to convert from any SI prefix to any other

        """
        if from_prefix is None:
            from_prefix = ''
        # Make sure we are using valid prefixes
        for prefix in [from_prefix, to_prefix]:
            if prefix not in self.SI_prefixes:
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
        Returns if the units are the same, and the type of measurement being
        done is the same (temperature, distance, etc).

        That is 'm' and 'metre' will return True
        But 'um' and 'm' will return False

        """
        # Any value would work except the point where Celsius == Fahrenheit,
        # i.e. 0 or 32
        return (self.to_canon(10) == other.to_canon(10) and
                self.canonical_suffix == other.canonical_suffix)

    def __ne__(self, other):
        return not self.__eq__(other)

"""
###############################################################################
###############################################################################
###############################################################################
"""


class MeasurementUnit():
    """
    A class that can handle mm^2, mm/s, C/(s*m), etc.

    Since there can be multiple prefixes and suffixes, the only attributes are:

    Attributes (public-facing)
    ------------
    unit_string: str
        The original string
    canonical_unit_string: str
        The canonical form for all units within the original string

    Methods (public-facing)
    ------------
    create
        The only public-facing factory method for this class
    to_canon
        transforms v from original units to canonical units
    from_canon
        the inverse of to_canon

    Operators overloaded
    ------------
    = != + - * / ** % -(unary) +(unary) repr

    """
    # supported operators
    operators = {ast.Add: op.add, ast.Sub: op.sub,
                 ast.Mult: op.mul, ast.Div: op.truediv,
                 ast.Pow: op.pow,
                 ast.USub: op.neg}

    inverse_operators = {op.add: op.sub, op.sub: op.add,
                         op.mul: op.truediv, op.truediv: op.mul,
                         op.pow: lambda a, b: (a ** (-b)),
                         op.pos: op.neg, op.neg: op.pos}

    operator_symbols = {op.add: '+', op.sub: '-',
                        op.mul: '*', op.truediv: '/',
                        op.pow: '**',
                        op.pos: '', op.neg: '-'}

    unit_types = {'m': 'spatial', 's': 'temporal', 'C': 'temperature',
                  '': 'dimensionless'}

    def __repr__(self):
        """
        Pretty-print a nice summary of this unit.

        """
        return ("||MeasurementUnit. original form '" + self.unit_string +
                "' canonical form '" + self.canonical_unit_string + "'||")

    @property
    def unit_string(self):
        # The convention is to use '^' for exponentiation, but in Python
        # that symbol is used for binary xor.  Here we make the substitution.
        return self._unit_string.replace('**', '^')

    @property
    def canonical_unit_string(self):
        return self._canonical_unit_string.replace('**', '^')

    @property
    def canonical_unit(self):
        """
        A new MeasurementUnit, just the canonical version of the unit.

        """
        return self.create(self.canonical_unit_string)

    def __eq__(self, other):
        # TODO: figure out if the units are measuring the same type of thing
        #       i.e. time vs distance.  Right now this doesn't care about that

        # Because the function might be nonlinear e.g. 'm**2', we should
        # sample the function at two points to validate that they are the same
        # this technically won't work for functions of odd degree but let's
        # call that a known bug and leave it for now since the robust way
        # of doing this check would require traversing the syntax tree again
        # via ast and that seems complicated
        return (
            self.to_canon(10) == other.to_canon(10) and
            self.to_canon(100) == other.to_canon(100)
        )

    def __ne__(self, other):
        return not self.__eq__(other)

    @classmethod
    def create(cls, unit_string):
        """
        The public-facing factory method for this class

        unit_string: str
            The unit expression, e.g. 'mm^2' or 'cm/s' or 'C'

        """
        # In Python 2, ensure that unit_string is unicode
        if six.PY2 and isinstance(unit_string, str):
            unit_string = unit_string.decode('unicode-escape')

        # Ensure that unit_string is str in Python 3 or unicode in Python 2.
        assert(isinstance(unit_string, six.text_type))

        # ast can't handle parsing '', so just create the end product
        # ourselves
        if unit_string == '':
            return cls._create_from_atomic('')

        # ast can't handle treating '%' as a leaf node to be sent to
        # MeasurementUnitAtom's initializer. So we make the substitution
        # to the friendly 'percent' instead.
        unit_string = unit_string.replace('%', 'percent')

        # The convention is to use '^' for exponentiation, but in Python
        # that symbol is used for binary xor.  Here we make the substitution.
        unit_string = unit_string.replace('^', '**')

        # ast can't handle parsing 'in' as a variable, since it's a
        # reserved word in Python.  So let's make a substitution
        # to the kludgey but friendly "_in"
        unit_string = unit_string.replace('in', '_in')
        # Fix the case where 'min' was changed to 'm_in' above
        unit_string = unit_string.replace('m_in', 'min')

        node = ast.parse(unit_string, mode='eval').body
        return cls._create_from_node(node)

    # =====================================================================
    # "private" methods

    @classmethod
    def _create_from_atomic(cls, unit_string):
        """
        Create a MeasurementUnit from an "atomic" or "irreducible" string

        e.g. 'mm' or 'kilocelsius' is irreducible, but 'mm^2' is not
             ('mm^2' can be decomposed into 'mm' ** 2)

        """
        u = cls()
        u_atom = MeasurementUnitAtom(unit_string)

        # Copy over the necessary attributes from our atomic (irreducible)
        # unit representation
        u.to_canon = u_atom.to_canon
        u.from_canon = u_atom.from_canon
        u._unit_string = u_atom.unit_string
        u._canonical_unit_string = u_atom.canonical_unit_string

        return u

    @classmethod
    def _create_from_node(cls, node):
        """
        node: ast.Num or ast.BinOp or ast.UnaryOp or ast.Str or ast.Name
            The expression to be transformed into a MeasurementUnit

        """
        if isinstance(node, ast.Num):  # <number>
            assert(node.n != 0)  # A unit cannot have zero in the expression

            u = cls()

            u._unit_string = str(node.n)
            u._canonical_unit_string = str(node.n)
            u.to_canon = lambda x: node.n
            u.from_canon = lambda x: node.n

            return u

        elif isinstance(node, ast.BinOp):  # <left> <operator> <right>
            u = cls()
            t = type(node.op)

            # Recursively calculate the left and right nodes
            l = cls._create_from_node(node.left)
            r = cls._create_from_node(node.right)

            return u.operators[t](l, r)

        elif isinstance(node, ast.UnaryOp):  # <operator> <operand> e.g., -1
            t = type(node.op)

            # Recursively calculate the operand
            u = cls._create_from_node(node.operand)

            # Apply the unary operator to the created object
            return u.operators[t](u)

        elif isinstance(node, ast.Name):
            # If we're down to the leaf node, we parse the actual
            # unit string
            return cls._create_from_atomic(node.id)

        else:
            raise TypeError(node)

    @classmethod
    def _create_with_binary_operator(cls, l, r, oper):
        """
        Factory method to create a new MeasurementUnit object from
        a binary operation on two existing MeasurementUnit objects, l and r.

        e.g.
        import operator
        MeasurementUnit.create_with_binary_operator(l,r,op.mul), is l * m.

        Parameters
        -----------
        l,r: MeasurementUnit objects
        oper: a binary operator, e.g. operator.mul

        Returns
        -----------
        A MeasurementUnit object
            The combination of the l and r by oper

        """
        u = MeasurementUnit()

        # For instance, MeasurementUnit.create('m') ** 2 is not allowed.
        # use MeasurementUnit.create('m**2')
        assert(isinstance(l, cls))
        assert(isinstance(r, cls))

        # NOTE: This won't work with affine functions.  We should probably
        # raise an Assertion if there is a temperature somewhere in the mix.
        new_scalar = oper(l.to_canon(1), r.to_canon(1))

        u.to_canon = lambda x: x * new_scalar
        u.from_canon = lambda x: x / new_scalar

        # Combine the left and right nodes together
        u._unit_string = l._unit_string + \
            u.operator_symbols[oper] + r._unit_string
        u._canonical_unit_string = l._canonical_unit_string + \
            u.operator_symbols[oper] + r._canonical_unit_string

        return u

    @classmethod
    def _create_with_unary_operator(cls, r, oper):
        """
        Similar to the create_with_binary_operator but for unary

        """
        assert(isinstance(r, cls))

        u = MeasurementUnit()

        u.to_canon = lambda x: u.operators[oper](u.to_canon(x))
        u.from_canon = lambda x: u.inverse_operators[oper](u.from_canon(x))

        u._unit_string = u.operator_symbols[oper] + u._unit_string
        u._canonical_unit_string = u.operator_symbols[
            oper] + u._canonical_unit_string

    # Overloaded operators
    def __mul__(self, other):
        return self.__class__._create_with_binary_operator(self, other, op.mul)

    def __add__(self, other):
        return self.__class__._create_with_binary_operator(self, other, op.add)

    def __sub__(self, other):
        return self.__class__._create_with_binary_operator(self, other, op.sub)

    def __truediv__(self, other):
        return self.__class__._create_with_binary_operator(
            self, other, op.truediv)

    def __pow__(self, other):
        return self.__class__._create_with_binary_operator(self, other, op.pow)

    def __mod__(self, other):
        return self.__class__._create_with_binary_operator(self, other, op.mod)

    def __pos__(self):
        return self.__class__._create_with_unary_operator(self, op.pos)

    def __neg__(self):
        return self.__class__._create_with_unary_operator(self, op.neg)
