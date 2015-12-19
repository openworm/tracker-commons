#!usr/bin/env python
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
from io import StringIO
import json, jsonschema

from wcon_data import parse_data, convert_origin
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
        jsonschema.validate(json.load(StringIO(wcon_string)), cls().schema)

    @classmethod
    def is_data_equal(cls, w1, w2, convert_units=True):
        """
        Parameters
        -------------
        w1, w2: WCONWorm objects
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
            return w1.standard_form.data == w2.standard_form.data
        else:
            return w1.data == w2.data

    @property
    def standard_form(self):
        """
        Return a new WCONWorm object, with .units and .data changed so they
        are in standard form.
        
        """
        
        # TODO
        for data_key in self.units:
            mu = self.units[data_key]
            self.data.loc[:,(4,data_key)].apply(mu.to_canon)
            #DEBUG
            #TODO
            
        # Go through each "units" attribute
        return self

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

    def __eq__(self, other):
        """
        Comparison operator (overloaded)
        
        Equivalent to .is_data_equal and .is_metadata_equal
        
        Units are converted
        
        Special units are not considered
        
        """
        return (WCONWorm.is_data_equal(self, other) and
                WCONWorm.is_metadata_equal(self, other))
            
    @classmethod
    def does_data_clash(cls, w1, w2):
        """
        Return True if any shared data between w1 and w2 clashes.
        
        """
        pass
        # TODO: maybe use the upsert functionality
        return True
    
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

    def save_to_file(self, JSON_path, num_chunks=1):
        """
        Save this object to the path specified.  The object
        will be serialized as a WCON JSON text file.
        
        Parameters
        -----------
        JSON_path: str
            The path to save this object to.  A warning is raised if the path
            does not end in ".WCON"
        num_chunks: int
            The number of chunks to break this object into.  If
            num_chunks > 1 then num_chunks files will be created.
            Filenames will have "_1", "_2", etc., added
            to the end of the filename after the last path separator (e.g. "/")
            and then, before the last "." (if any)
            
        """
        # TODO
        pass


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

        assert(isinstance(JSON_path, str))
        assert(len(JSON_path)>0)
        

        if len(JSON_path) <= 5 or JSON_path[-5:].upper() != '.WCON':
            warnings.warn('The file name is either less than 5 characters,'
                          'consists of only the extension ".WCON", or '
                          'does not end in ".WCON", the recommended'
                          'file extension.')


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

        # We don't want 'files' to stay after loading
        del(w_current.files)

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
        root = json.loads(serialized_data, object_pairs_hook=reject_duplicates)
    
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
            w.units[key] = MeasurementUnit.create(w.units[key])

        
        if len(root['data']) > 0:
            w.data = parse_data(root['data'])
            
            # Shift the coordinates by the amount in the offsets 'ox' and 'oy'
            convert_origin(w.data)
        else:
            # "data": {}
            w.data = None

        # Raise error if there are any data keys without units
        # TODO
        # w1.data.index.name
        # w1.data.columns
        # w1.units.keys()

        # ===================================================
        # HANDLE THE OPTIONAL ELEMENTS: 'files', 'metadata'

        if 'files' in root:
            w.files = root['files']
        else:
            w.files = None

        if 'metadata' in root:
            w.metadata = root['metadata']
        else:
            w.metadata = None

        # DEBUG: temporary
        w.root = root

        return w


