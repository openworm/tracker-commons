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
import os
import shutil
import json
import jsonschema
import zipfile
import numpy as np
import pandas as pd
idx = pd.IndexSlice

from .wcon_data import parse_data, convert_origin
from .wcon_data import df_upsert, data_as_array
from .wcon_data import get_sorted_ordered_dict
from .wcon_data import reverse_backwards_worms, sort_odict
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
    _data: dictionary of Pandas DataFrames  [private]
    num_worms: int                              [property]
    data_as_dict: dict                          [property]
    data: DataFrame if num_worms == 1 else dict of DataFrames [property]

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
    def num_worms(self):
        try:
            return self._num_worms
        except AttributeError:
            self._num_worms = len(self.worm_ids)
            return self._num_worms

    @property
    def worm_ids(self):
        try:
            return self._worm_ids
        except AttributeError:
            self._worm_ids = list(self._data.keys())
            return self._worm_ids

    @property
    def data(self):
        """
        Return all worms as one giant DataFrame.  Since this can
        be inefficient for sparse multiworm data, it is only "lazily"
        calculated, i.e. once requested, not at object initialization

        """
        try:
            return self._data_df
        except AttributeError:
            if self.num_worms == 0:
                self._data_df = None
            else:
                # Get a list of all dfs
                dfs = list(self._data.values())
                l = dfs[0]
                # Merge all the worm dfs together into one
                for r in dfs[1:]:
                    l = pd.merge(l, r, left_index=True, right_index=True,
                                 how='outer')
                self._data_df = l

            return self._data_df

    @property
    def data_as_odict(self):
        """
        Return the native ordered-dict-of-DataFrames, the cheapest
        option for sparse multiworm data

        """
        return self._data

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

        canonical = self.to_canon
        if canonical._data == {}:
            data_arr = []
        else:
            src = canonical.data_as_odict
            data_arr = []
            for worm_id in src:
                data_arr.extend(data_as_array(src[worm_id]))

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
        # import pdb; pdb.set_trace()
        if w1.num_worms != w2.num_worms:
            return False

        if convert_units:
            d1 = w1.to_canon._data
            d2 = w2.to_canon._data
        else:
            d1 = w1._data
            d2 = w2._data

        for worm_id in w1.worm_ids:
            try:
                df1 = d1[worm_id]
            except KeyError:
                df1 = None
            try:
                df2 = d2[worm_id]
            except KeyError:
                df2 = None

            if (df1 is None) ^ (df2 is None):
                # If one is None but the other is not (XOR), data is not equal
                return False
            elif df1 is None and df2 is None:
                # If both None, they are equal
                continue

            if not pd_equals(df1, df2):
                return False

        return True

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
        if self._data == {}:
            w._data = OrderedDict({})
            return w

        w._data = OrderedDict()
        for worm_id in self.worm_ids:
            w._data[worm_id] = self._data[worm_id].copy()

        # Go through each "units" key
        for data_key in self.units:
            mu = self.units[data_key]

            # Don't bother to "convert" units that are already in their
            # canonical form.
            if mu.unit_string == mu.canonical_unit_string:
                continue

            tmu = self.units['t']
            for worm_id in w.worm_ids:

                try:
                    # Apply across all worm ids and all aspects
                    mu_slice = \
                        w._data[worm_id].loc[:, idx[:, data_key, :]].copy()

                    w._data[worm_id].loc[:, idx[:, data_key, :]] = \
                        mu_slice.applymap(mu.to_canon)
                except KeyError:
                    # Just ignore cases where there are "units" entries but no
                    # corresponding data
                    pass

                # Special case: change the dataframe index, i.e. the time units
                if tmu.unit_string != tmu.canonical_unit_string:
                    # Create a function that can be applied elementwise to the
                    # index values
                    t_converter = np.vectorize(tmu.to_canon)
                    new_index = t_converter(w._data[worm_id].index.values)

                    w._data[worm_id].set_index(new_index, inplace=True)

        return w

    @classmethod
    def merge(cls, w1, w2):
        """
        Merge two worm groups, in their standard forms.

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

        for worm_id in w2c.worm_ids:
            if worm_id in w1c.worm_ids:
                try:
                    # Try to upsert w2c's data into w1c.  If we cannot
                    # without an error being raised, the data clashes.
                    w1c._data[worm_id] = df_upsert(w1c._data[worm_id],
                                                   w2c._data[worm_id])
                except AssertionError as err:
                    raise AssertionError("Data conflicts between worms to "
                                         "be merged on worm {0}: {1}"
                                         .format(str(worm_id), err))
            else:
                # The worm isn't in the 1st group, so just add it
                w1c._data[worm_id] = w2c._data[worm_id]

        # Sort w1c's list of worms
        w1c._data = sort_odict(w1c._data)

        # Create a fresh WCONWorms object to reset all the lazily-evaluated
        # properties that may change, such as num_worms, in the merged worm
        merged_worm = WCONWorms()
        merged_worm._data = w1c._data
        merged_worm.metadata = w2c.metadata
        merged_worm.units = w1c.units

        return merged_worm

    """
    ================================================================
    Load / save methods
    ================================================================
    """

    @classmethod
    def validate_filename(cls, JSON_path, is_zipped):
        """
        Perform simple checks on the file path

        JSON_path: str
            The path to the file to be evaluated

        is_zipped: bool
            Whether or not the path is for a zip archive

        """
        assert(isinstance(JSON_path, six.string_types))
        assert(len(JSON_path) > 0)

        if is_zipped:
            if JSON_path[-4:].upper() != '.ZIP':
                raise Exception("A zip archive like %s must have an "
                                "extension ending in '.zip'" % JSON_path)
            else:
                # delete the '.zip' part so the rest can be validated
                JSON_path = JSON_path[:-4]

        warning_message = (' is either less than 5 characters,'
                           'consists of only the extension ".WCON", or '
                           'does not end in ".WCON", the recommended '
                           'file extension.')

        if len(JSON_path) <= 5 or JSON_path[-5:].upper() != '.WCON':
            if is_zipped:
                warnings.warn('Zip file ends properly in .zip, but the '
                              'prefix' + warning_message)
            else:
                warnings.warn('The file name ' + warning_message)

    def save_to_file(self, JSON_path, pretty_print=False,
                     compress_file=False, num_chunks=1):
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
        compress_file: bool
            If True, saves a compressed version of the WCON JSON text file
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

        self.validate_filename(JSON_path, compress_file)

        with open(JSON_path, 'w') as outfile:
            json.dump(self.as_ordered_dict, outfile,
                      indent=4 if pretty_print else None)

        if compress_file:
            # Zip the file to a TEMP file, then rename to the original,
            # overwriting it with the zipped archive.
            zf = zipfile.ZipFile(JSON_path + '.TEMP',
                                 'w', zipfile.ZIP_DEFLATED)
            zf.write(JSON_path)
            zf.close()
            os.rename(JSON_path + '.TEMP', JSON_path)

    @classmethod
    def load_from_file(cls, JSON_path,
                       load_prev_chunks=True,
                       load_next_chunks=True,
                       validate_against_schema=True):
        """
        Factory method returning a merged WCONWorms instance of the file
        located at JSON_path and all related "chunks" as specified in the
        "files" element of the file.

        Uses recursion if there are multiple chunks.

        Parameters
        -------------
        JSON_path: str
            A file path to a file that can be opened
        validate_against_schema: bool
            If True, validate before trying to load the file, otherwise don't.
            jsonschema.validate takes 99% of the compute time for large files
            so use with caution.
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

        is_zipped = zipfile.is_zipfile(JSON_path)

        cls.validate_filename(JSON_path, is_zipped)

        # Check if the specified file is compressed
        if is_zipped:
            zf = zipfile.ZipFile(JSON_path, 'r')

            zf_namelist = zf.namelist()
            if len(zf_namelist) <= 0:
                raise Exception("Filename %s is a zip archive, which is fine, "
                                "but the archive does not contain any files.")
            elif len(zf_namelist) == 1:
                # Just one file is in the archive.
                print("The file is a zip archive with one file.  Attempting "
                      "to uncompress and then load.")
                wcon_bytes = zf.read(zf.namelist()[0])
                wcon_string = wcon_bytes.decode("utf-8")
                infile = StringIO(wcon_string)
                w_current = cls.load(infile, validate_against_schema)
            else:
                print("The zip archive contains multiple files.  We will "
                      "extract to a temporary folder and then try to load "
                      "the first file in the archive, then delete the "
                      "temporary folder.")
                # Note: the first file is all we should need since we assume
                #       the files in the archive are linked together using
                #       their respective JSON "files" entries

                # Make a temporary archive folder
                cur_path = os.path.abspath(os.path.dirname(JSON_path))
                archive_path = os.path.join(cur_path, '_zip_archive')
                if os.path.exists(archive_path):
                    raise Exception("Archive path %s already exists!"
                                    % archive_path)
                else:
                    os.makedirs(archive_path)

                # Extract zip archive to temporary folder
                for name in zf_namelist:
                    zf.extract(name, archive_path)
                zf.close()

                # Call load_from_file on the first file
                first_path = os.path.join(archive_path, zf_namelist[0])
                w = cls.load_from_file(first_path)

                # Delete the temporary folder
                shutil.rmtree(archive_path, ignore_errors=True)

                return w
        else:
            # The file is not a zip file, so assume it's just plaintext JSON
            with open(JSON_path, 'r') as infile:
                w_current = cls.load(infile, validate_against_schema)

        # CASE 1: NO "files" OBJECT, hence no multiple files.  We are done.
        w_cur = w_current
        if w_current.files is None:
            return w_current
        elif (('next' not in w_cur.files) and ('prev' not in w.cur.files)):
            # CASE 2: "files" object exists but no prev/next, assume nothing is
            # there
            return w_current
        else:
            # The merge operations below will blast away the .files attribute
            # so we need to save a local copy
            current_files = w_current.files

        # OTHERWISE, CASE 2: MULTIPLE FILES

        # The schema guarantees that if "files" is present,
        # "current", will exist.  Also, that "current" is not
        # null and whose corresponding value is a string at least one
        # character in length.
        cur_ext = current_files['current']

        # e.g. cur_filename = 'filename_2.wcon'
        # cur_ext = '_2', prefix = 'filename', suffix = '.wcon'
        cur_filename = JSON_path
        name_offset = cur_filename.find(cur_ext)
        if name_offset == -1:
            raise AssertionError(
                'Mismatch between the filename given in the file "' +
                cur_ext +
                '" and the file we loaded from "' +
                cur_filename +
                '".')
        path_string = cur_filename[:name_offset]

        load_chunks = {'prev': load_prev_chunks,
                       'next': load_next_chunks}

        for direction in ['prev', 'next']:
            # If we are supposed to load the previous chunks, and one exists,
            # load it and merge it with the current chunk
            # Same with the "next" chunks
            if (load_chunks[direction] and
                    current_files is not None and
                    current_files[direction] is not None and
                    len(current_files[direction]) > 0):
                cur_load_prev_chunks = (direction == 'prev')
                cur_load_next_chunks = (direction == 'next')

                new_file_name = path_string + current_files[direction][0]

                w_new = cls.load_from_file(new_file_name,
                                           cur_load_prev_chunks,
                                           cur_load_next_chunks,
                                           validate_against_schema)
                w_current = w_current + w_new

        # If no merging took place, we'll still need to delete the "files"
        # attribute if it's present (i.e. if both "prev" and "next" were null):
        if hasattr(w_current, "files"):
            del(w_current.files)

        return w_current

    @classmethod
    def load(cls, JSON_stream, validate_against_schema=True):
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
        validate_against_schema: bool
            If True, validate before trying to load the file, otherwise don't.
            jsonschema.validate takes 99% of the compute time for large files
            so use with caution.

        """
        w = cls()

        serialized_data = JSON_stream.read()

        # Load the whole JSON file into a nested dict.  Any duplicate
        # keys raise an exception since we've hooked in reject_duplicates
        root = json.loads(serialized_data, object_pairs_hook=reject_duplicates)

        # ===================================================
        # BASIC TOP-LEVEL VALIDATION AGAINST THE SCHEMA

        # Validate the raw file against the WCON schema
        if validate_against_schema:
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
            w._data = parse_data(root['data'])

            # Shift the coordinates by the amount in the offsets 'ox' and 'oy'
            for worm_id in w.worm_ids:
                convert_origin(w._data[worm_id])

                # Any worms with head=='R' should have their
                # coordinates reversed and head reset to 'L'
                reverse_backwards_worms(w._data[worm_id])
        else:
            # "data": {}
            w._data = OrderedDict({})

        # Raise error if there are any data keys without units
        units_keys = set(w.units.keys())

        for worm_id in w._data:
            df = w._data[worm_id]
            if df is None:
                data_keys = set()
            else:
                data_keys = set(df.columns.get_level_values(1))

            # "head" and "ventral" don't require units.
            keys_missing_units = data_keys - \
                units_keys - set(['head', 'ventral'])
            if keys_missing_units != set():
                raise AssertionError('In worm ' + str(worm_id) + ', the '
                                     'following data keys are missing '
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


def pd_equals(df1, df2):
    """
    I don't use DataFrame.equals because it returned False for no
    apparent reason with one of the centroid unit tests

    """
    if not df1.columns.identical(df2.columns):
        return False

    if not df1.index.identical(df2.index):
        return False

    try:
        pd.util.testing.assert_frame_equal(df1, df2)
    except AssertionError:
        return False

    return True


def reject_duplicates(ordered_pairs):
    """Reject duplicate keys."""
    unique_dict = {}
    for key, val in ordered_pairs:
        if key in unique_dict:
            raise KeyError("Duplicate key: %r" % (key,))
        else:
            unique_dict[key] = val

    return unique_dict
