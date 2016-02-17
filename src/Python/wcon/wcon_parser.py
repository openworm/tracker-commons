#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Methods
------------
reject_duplicates

Classes
------------
WCONWorms

"""
import six
import warnings
from collections import OrderedDict
from six import StringIO
from os import path
import json
import jsonschema
import pandas as pd
idx = pd.IndexSlice

from .wcon_data import parse_data, convert_origin
from .wcon_data import df_upsert, data_as_array
from .wcon_data import get_sorted_ordered_dict
from .wcon_data import reverse_backwards_worms
from .measurement_unit import MeasurementUnit


class WCONWorms():
    """
    A set of worm tracker data for one or more worms, as specified by
    the WCON standard.

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

    [Note: the "files" key is not persisted unless the .load
           factory method is used.]

    Public-Facing Methods
    -------------
    load_from_file   (JSON_path)                [class method]
    save_to_file     (JSON_path, pretty_print)
    to_canon                                    [property]
    __add__                                     [use "+"]
    __eq__                                      [use "=="]

    Usage
    -------------
    # From a string literal:
    from io import StringIO
    w2 = WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, '
                                  '"data":[]}'))

    # WCONWorms.load_from_file accepts any valid WCON, but .save_to_file
    # output is always "canonical" WCON, which makes specific choices about
    # how to arrange and format the WCON file.  This way the functional
    # equality of any two WCON files can be tested by this:

        w1 = WCONWorms.load_from_file('file1.wcon')
        w2 = WCONWorms.load_from_file('file2.wcon')

        assert(w1 == w2)

        # or:

        w1.save_to_file('file1.wcon')
        w2.save_to_file('file2.wcon')

        import filecmp
        assert(filecmp.cmp('file1.wcon', file2.wcon'))

    Custom WCON versions
    --------------------

    Any top-level key other than the basic:

    - files
    - units
    - metadata
    - data

    ... is ignored.  Handling them requires subclassing WCONWorms.

    """
    """
    ================================================================
    Properties
    ================================================================
    """

    basic_keys = ['files', 'units', 'metadata', 'data']

    @property
    def schema(self):
        try:
            return self._schema

        except AttributeError:
            # Only load _schema if this method gets called.  Once
            # it's loaded, though, persist it in memory and don't lose it
            here = path.abspath(path.dirname(__file__))

            with open(path.join(here, "wcon_schema.json"), "r") as f:
                self._schema = json.loads(f.read())

            # Now that the schema has been loaded, we can try again
            return self._schema

    @classmethod
    def validate_from_schema(cls, wcon_string):
        jsonschema.validate(json.load(StringIO(wcon_string)), cls().schema)

    @property
    def canonical_units(self):
        """
        A dictionary of canonical versions of the unit for all quantities

        """
        return {k: self.units[k].canonical_unit for k in self.units.keys()}

    @property
    def as_ordered_dict(self):
        """
        Return a representation of the worm as an OrderedDict.  This is most
        useful when saving to a file.

        Returns the canonical version of the data, with units in
        canonical form, and the data converted to canonical form.

        The three keys are:

        - 'units'
        - 'metadata'
        - 'data'

        """
        # Not strictly required by JSON but nice to order the four top-level
        # keys so we use OrderedDict here instead of dict.
        ord_dict = OrderedDict()

        # A dictionary of the canonical unit strings for all quantities except
        # aspect_size, which is generated at runtime.
        units_obj = {k: self.units[k].canonical_unit_string
                     for k in self.units.keys() if k != 'aspect_size'}
        # Sort the units so that every time we save this file, it produces
        # exactly the same output.  Not required in the JSON standard, but
        # nice to have.
        units_obj = get_sorted_ordered_dict(units_obj)
        ord_dict.update({'units': units_obj})

        # The only optional object is "metadata" since "files" is not
        # necessary since we don't currently support saving to more than
        # one chunk.
        if self.metadata:
            # Again, sort the metadata (recursively) so that the same file
            # is produced each time that can stand up to diffing
            metadata_obj = get_sorted_ordered_dict(self.metadata)
            ord_dict.update({'metadata': metadata_obj})

        canonical_data = self.to_canon.data
        if canonical_data is None:
            data_arr = []
        else:
            data_arr = data_as_array(canonical_data)
        ord_dict.update({'data': data_arr})

        return ord_dict

    """
    ================================================================
    Comparison Methods
    ================================================================
    """
    @classmethod
    def are_units_equal(cls, w1, w2):
        """
        Returns
        ---------
        boolean
            True if w1.units == w2.units, with the only conversion being
            between units that mean the same thing
            (e.g. 'mm' and 'millimetres')
            False otherwise

        """
        if set(w1.units.keys()) != set(w2.units.keys()):
            return False

        for k in w1.units.keys():
            if w1.units[k] != w2.units[k]:
                return False

        return True

    @classmethod
    def is_metadata_equal(cls, w1, w2):
        """
        Returns
        ----------
        boolean
            True if w1.metadata == w2.metadata

        """
        return w1.metadata == w2.metadata

    @classmethod
    def is_data_equal(cls, w1, w2, convert_units=True):
        """
        Parameters
        -------------
        w1, w2: WCONWorms objects
            The objects whose .data attributes will be compared
        convert_units: bool
            If True, the data will first be converted to a standard form
            so that if one worm uses millimetres and the other metres, the
            data can still be properly compared

        TODO:
            Add a "threshold" parameter so that perfect equality is not
            the only option

        """

        if convert_units:
            d1 = w1.to_canon.data
            d2 = w2.to_canon.data

        else:
            d1 = w1.data
            d2 = w2.data

        if (d1 is None) ^ (d2 is None):
            # If one is None but the other is not (XOR), data is not equal
            return False
        elif d1 is None and d2 is None:
            # If both None, they are equal
            return True

        # I don't use DataFrame.equals because it returned False for no
        # apparent reason with one of the centroid unit tests
        def pd_equals(d1, d2):
            try:
                pd.util.testing.assert_frame_equal(d1, d2)
            except AssertionError:
                return False

            return True

        return (pd_equals(d1, d2) and
                d1.columns.identical(d2.columns) and
                d1.index.identical(d2.index))

    def __eq__(self, other):
        """
        Comparison operator (overloaded)

        Equivalent to .is_data_equal and .is_metadata_equal

        Units are converted

        Special units are not considered

        """
        return (WCONWorms.is_data_equal(self, other) and
                WCONWorms.is_metadata_equal(self, other))

    def __ne__(self, other):
        return not self.__eq__(other)

    def __add__(self, other):
        """
        Addition operator (overloaded)

        """
        return self.merge(self, other)

    @property
    def is_canon(self):
        """
        Returns whether all units are already in their canonical forms.

        """
        for data_key in self.units:
            mu = self.units[data_key]
            if mu.unit_string != mu.canonical_unit_string:
                return False

        return True

    @property
    def to_canon(self):
        """
        Return a new WCONWorms object, with the same .metadata, but with
        .units and .data changed so they are in standard form.

        """
        w = WCONWorms()
        w.metadata = self.metadata
        w.units = self.canonical_units

        # Corner case
        if self.data is None:
            w.data = None
            return w

        w.data = self.data.copy()

        for data_key in self.units:
            mu = self.units[data_key]

            # Don't bother to "convert" units that are already in their
            # canonical form.
            if mu.unit_string == mu.canonical_unit_string:
                continue

            try:
                # Apply across all worm ids and all aspects
                mu_slice = w.data.loc[:, idx[:, data_key, :]].copy()

                w.data.loc[:, idx[:, data_key, :]] = \
                    mu_slice.applymap(mu.to_canon)
            except KeyError:
                # Just ignore cases where there are "units" entries but no
                # corresponding data
                pass

        # Go through each "units" attribute
        return w

    @classmethod
    def merge(cls, w1, w2):
        """
        Merge two worms, in their standard forms.

        Units can differ, but not in their standard forms.

        Metadata must be identical.

        Data can overlap, as long as it does not clash.

        Clashes are checked at a low level of granularity:
        e.g. if two worms have different metadata but the individual metadata
        entries do not conflict, this method will still fail and raise an
        AssertionError.

        """
        if not cls.is_metadata_equal(w1, w2):
            raise AssertionError("Metadata conflicts between worms to be "
                                 "merged.")

        w1c = w1.to_canon
        w2c = w2.to_canon

        try:
            # Try to upsert w2c's data into w1c.  If we cannot
            # without an error being raised, the data clashes.
            w1c.data = df_upsert(w1c.data, w2c.data)
        except AssertionError as err:
            raise AssertionError("Data conflicts between worms to "
                                 "be merged: {0}".format(err))

        return w1c

    """
    ================================================================
    Load / save methods
    ================================================================
    """

    @classmethod
    def validate_filename(cls, JSON_path):
        """
        Perform simple checks on the file path

        """
        assert(isinstance(JSON_path, six.string_types))
        assert(len(JSON_path) > 0)

        if len(JSON_path) <= 5 or JSON_path[-5:].upper() != '.WCON':
            warnings.warn('The file name is either less than 5 characters,'
                          'consists of only the extension ".WCON", or '
                          'does not end in ".WCON", the recommended'
                          'file extension.')

    def save_to_file(self, JSON_path, pretty_print=False, num_chunks=1):
        """
        Save this object to the path specified.  The object
        will be serialized as a WCON JSON text file.

        Parameters
        -----------
        JSON_path: str
            The path to save this object to.  A warning is raised if the path
            does not end in ".WCON"
        pretty_print: bool
            If True, adds newlines and spaces to make the file more human-
            readable.  Otherwise, the JSON output will use as few characters
            as possible.
        num_chunks: int
            The number of chunks to break this object into.  If
            num_chunks > 1 then num_chunks files will be created.
            Filenames will have "_1", "_2", etc., added
            to the end of the filename after the last path separator
            (e.g. "/") and then, before the last "." (if any)

        """
        if num_chunks > 1:
            raise NotImplementedError("Saving a worm to more than one chunk "
                                      "has not yet been implemented")

        self.validate_filename(JSON_path)

        with open(JSON_path, 'w') as outfile:
            json.dump(self.as_ordered_dict, outfile,
                      indent=4 if pretty_print else None)

    @classmethod
    def load_from_file(cls, JSON_path,
                       load_prev_chunks=True,
                       load_next_chunks=True):
        """
        Factory method returning a merged WCONWorms instance of the file
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

        cls.validate_filename(JSON_path)

        with open(JSON_path, 'r') as infile:
            w_current = cls.load(infile)

        # CASE 1: NO "files" OBJECT, hence no multiple files.  We are done.
        if w_current.files is None:
            return w_current
        else:
            # The merge operations below will blast away the .files attribute
            # so we need to save a local copy
            current_files = w_current.files

        # OTHERWISE, CASE 2: MULTIPLE FILES

        # The schema guarantees that if "files" is present,
        # "this", "prev" and "next" will exist.  Also, that "this" is not
        # null and whose corresponding value is a string at least one
        # character in length.
        cur_ext = current_files['this']

        # e.g. cur_filename = 'filename_2.wcon'
        # cur_ext = '_2', prefix = 'filename', suffix = '.wcon'
        cur_filename = JSON_path
        if cur_filename.find(cur_ext) == -1:
            raise AssertionError('Cannot find the current extension "' +
                                 cur_ext + '" within the current filename "' +
                                 cur_filename + '".')
        prefix = cur_filename[:cur_filename.find(cur_ext)]
        suffix = cur_filename[cur_filename.find(cur_ext) + len(cur_ext):]

        load_chunks = {'prev': load_prev_chunks,
                       'next': load_next_chunks}

        for direction in ['prev', 'next']:
            # If we are supposed to load the previous chunks, and one exists,
            # load it and merge it with the current chunk
            # Same with the "next" chunks
            if (load_chunks[direction] and
                    current_files is not None and
                    current_files[direction] is not None):
                cur_load_prev_chunks = (direction == 'prev')
                cur_load_next_chunks = (direction == 'next')

                new_file_name = (prefix + current_files[direction][0] +
                                 suffix)
                w_new = cls.load_from_file(new_file_name,
                                           cur_load_prev_chunks,
                                           cur_load_next_chunks)
                w_current = w_current + w_new

        # If no merging took place, we'll still need to delete the "files"
        # attribute if it's present (i.e. if both "prev" and "next" were null):
        if hasattr(w_current, "files"):
            del(w_current.files)

        return w_current

    @classmethod
    def load(cls, JSON_stream):
        """
        Factory method to create a WCONWorms instance

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
        root = json.loads(serialized_data, object_pairs_hook=reject_duplicates)

        # ===================================================
        # BASIC TOP-LEVEL VALIDATION AGAINST THE SCHEMA

        # Validate the raw file against the WCON schema
        jsonschema.validate(root, w.schema)

        # ===================================================
        # HANDLE THE REQUIRED ELEMENTS: 'units', 'data'

        w.units = root['units']

        for key in w.units:
            w.units[key] = MeasurementUnit.create(w.units[key])

        # The only data key without units should be aspect_size, since it's
        # generated during the construction of the pandas dataframe
        # it is a dimensionless quantity
        w.units['aspect_size'] = MeasurementUnit.create('')

        if len(root['data']) > 0:
            w.data = parse_data(root['data'])

            # Shift the coordinates by the amount in the offsets 'ox' and 'oy'
            convert_origin(w.data)

            # Any worms with head=='R' should have their
            # coordinates reversed and head reset to 'L'
            reverse_backwards_worms(w.data)
        else:
            # "data": {}
            w.data = None

        # Raise error if there are any data keys without units
        if w.data is None:
            data_keys = set()
        else:
            data_keys = set(w.data.columns.get_level_values(1))
        units_keys = set(w.units.keys())
        # "head" and "ventral" don't require units.
        keys_missing_units = data_keys - units_keys - set(['head', 'ventral'])
        if keys_missing_units != set():
            raise AssertionError('The following data keys are missing '
                                 'entries in the "units" object: ' +
                                 str(keys_missing_units))

        # ===================================================
        # HANDLE THE OPTIONAL ELEMENTS: 'files', 'metadata'

        if 'files' in root:
            w.files = root['files']

            # Handle the case of a single 'next' or 'prev' entry, by
            # wrapping it in an array, so we can reliably assume that
            # entries are always wrapped in arrays.
            for direction in ['next', 'prev']:
                if hasattr(w.files, direction):
                    if isinstance(getattr(w.files, direction), str):
                        setattr(w.files, direction,
                                [getattr(w.files, direction)])
        else:
            w.files = None

        if 'metadata' in root:
            w.metadata = root['metadata']
        else:
            w.metadata = None

        return w


def reject_duplicates(ordered_pairs):
    """Reject duplicate keys."""
    unique_dict = {}
    for key, val in ordered_pairs:
        if key in unique_dict:
            raise KeyError("Duplicate key: %r" % (key,))
        else:
            unique_dict[key] = val

    return unique_dict
