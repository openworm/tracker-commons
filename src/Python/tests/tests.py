#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Unit tests for the Python WCON parser

"""
import six
import os, sys
from six import StringIO # StringIO.StringIO in 2.x, io.StringIO in 3.x
import json
import jsonschema
import unittest
import filecmp
import glob

sys.path.append('..')
from wcon import WCONWorms, MeasurementUnit
from wcon.measurement_unit import MeasurementUnitAtom

class TestMeasurementUnit(unittest.TestCase):
    def test_unit_equivalence(self):
        self.assertTrue(MeasurementUnit.create('mm') == MeasurementUnit.create('millimetre'))
        self.assertTrue(MeasurementUnit.create('Mm') == MeasurementUnit.create('megametre'))
        self.assertTrue(MeasurementUnit.create('mm') != MeasurementUnit.create('Mm'))
        self.assertTrue(MeasurementUnit.create('d') == MeasurementUnit.create('day'))
        self.assertTrue(MeasurementUnit.create('min') == MeasurementUnit.create('minutes'))
        self.assertTrue(MeasurementUnit.create('%') != MeasurementUnit.create('K'))


    def test_canon_conversion(self):
        self.assertTrue(MeasurementUnit.create('cm').to_canon(1) == 10)
        self.assertTrue(MeasurementUnit.create('m').to_canon(1) == 1000)
        self.assertTrue(MeasurementUnit.create('F').from_canon(0) == 32)
        self.assertTrue(MeasurementUnit.create('microns').to_canon(1800) == 1.8)
        self.assertTrue(MeasurementUnit.create('F').to_canon(32) == 0)

        for suf in MeasurementUnitAtom('').all_suffixes:
            # Taking the value 10 to the canonical value, then back again,
            # should come back to 10 for all units (up to floating point 
            # epsilon)
            self.assertTrue(abs(
                        MeasurementUnit.create(suf).from_canon(
                        MeasurementUnit.create(suf).to_canon(10)) - 10
                      ) < 1e-8)
            
    def test_bad_units(self):
        # Verify that combining the full name with an abbreviation causes
        # an error to be raised
        with self.assertRaises(AssertionError):
            MeasurementUnit.create('msecond')
            
        with self.assertRaises(AssertionError):
            MeasurementUnit.create('millis')
            
        # This form, on the other hand, should be fine, since both prefix
        # and suffix are long-form.
        MeasurementUnit.create('milliseconds')

    def test_unit_combos(self):
        MeasurementUnit.create('m^2') == MeasurementUnit.create('metres^2')
        MeasurementUnit.create('C') == MeasurementUnit.create('celsius')
        MeasurementUnit.create('m/s') == MeasurementUnit.create('metres/sec')
        MeasurementUnit.create('mm/s^2') == MeasurementUnit.create('millimeter/sec^2')
        MeasurementUnit.create('mm/s^2') == MeasurementUnit.create('mm/(s^2)')
        MeasurementUnit.create('mm^2/s^2') == MeasurementUnit.create('(mm^2)/(s^2)')
        MeasurementUnit.create('mm^2/s^2') == MeasurementUnit.create('mm^2/(s^2)')
        
        self.assertTrue(MeasurementUnit.create('m/min').to_canon(3) == 50)
        self.assertTrue(MeasurementUnit.create('1/min').to_canon(60) == 1)

class TestWCONParser(unittest.TestCase):
    def _validate_from_schema(self, wcon_string):
        try:
            jsonschema.validate(json.load(StringIO(wcon_string)), 
                                self._wcon_schema)
        except AttributeError:
            # Only load _wcon_schema if this method gets called.  Once
            # it's loaded, though, persist it in memory and don't lose it
            with open("../../../wcon_schema.json", "r") as wcon_schema_file:
                self._wcon_schema = json.loads(wcon_schema_file.read())

            # Now that the schema has been loaded, we can try again
            self._validate_from_schema(wcon_string)

    def test_schemas_same(self):
        # Schema had to be saved in two places, since it's common to the repo
        # but also needs to be found by the PyPi packager for pip.
        # Here we confirm that the files are identical.
        self.assertTrue(filecmp.cmp('../wcon/wcon_schema.json', 
                                    '../../../wcon_schema.json'))

    def test_schema(self):
        basic_wcon = '{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'
        
        self._validate_from_schema(basic_wcon)

    def test_equality_operator(self):
        JSON_path = '../../../tests/minimax.wcon'
        w2 = WCONWorms.load_from_file(JSON_path)
        w2.units['y'] = MeasurementUnit.create('m')
        w2data = w2.data.copy()
        
        w2c = w2.to_canon
        # A change to_canon should change the data in one but equality 
        # should remain
        self.assertFalse((w2.data == w2c.data).all().all())
        # This has the same effect as above
        self.assertFalse(WCONWorms.is_data_equal(w2,w2c,convert_units=False))
        self.assertEqual(w2, w2)
        self.assertEqual(w2c, w2c)
        self.assertEqual(w2, w2c)

        # Change the units for w2 (not really what we should do but just 
        # force a difference now, with w2c)
        w2.units['y'] = MeasurementUnit.create('mm')
        self.assertNotEqual(w2, w2c)
        
        # Confirm that data was not altered in situ after performing 
        # the to_canon operation
        self.assertTrue((w2data == w2.data).any().any())

    def test_save_and_load(self):
        """
        All .wcon files in the tests folder are loaded, saved, then loaded 
        again, and compared with the original loaded file.
        
        This is one of the best and most comprehensive tests in this suite.
        
        """
        print("BIG TEST: Test load and save and load and save")
        files_to_test = glob.glob('../../../tests/*.wcon')
        
        for JSON_path in files_to_test:
            for pretty in [True, False]:
                print("LOADING FOR TEST: " + JSON_path + 
                      " (PRETTY = " + str(pretty) + ")")

                w_loaded = WCONWorms.load_from_file(JSON_path)
        
                # Save these worm tracks to a file, then load that file
                test_path = 'test.wcon'
                w_loaded.save_to_file(test_path, pretty_print=pretty)
                w_from_saved = WCONWorms.load_from_file(test_path)
                
                self.assertEqual(w_loaded, w_from_saved)
            
                # then load and save AGAIN and do a file comparison to make
                # sure it's the same        
                # this will test that we order the keys (even though this 
                # isn't in the WCON standard it's nice for human 
                # readability, i.e. to have "tracker-commons" first, 
                # "id" first in a data segment, etc.)
                w_loaded_again = WCONWorms.load_from_file(test_path)
                self.assertEqual(w_loaded, w_loaded_again)
                self.assertEqual(w_loaded, w_from_saved)
                self.assertEqual(w_loaded_again, w_from_saved)
    
                test_path2 = 'test2.wcon'
                w_loaded_again.save_to_file(test_path2, pretty_print=pretty)
    
                # As described in the above comment: check that running
                # load/save twice should generate an IDENTICAL file.
                self.assertTrue(filecmp.cmp(test_path, test_path2))
                
                os.remove(test_path)
                os.remove(test_path2)
        

        
    def test_tracker_commons_and_units(self):
        with self.assertRaises(ValueError):
            # The JSON parser shouldn't accept this as valid JSON
            # "ValueError: Expecting ',' delimiter: line 1 column 25 (char 24)"
            WCONWorms.load(StringIO('{"tracker-commons":blahblah}'))
        
        # Errors because "tracker-commons":true is not present
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO('{"tracker-blah":true, "units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        if six.PY3:  # since assertWarns is not available in Python 2
            with self.assertWarns(UserWarning):
                WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        # If we're being explicitly told that this is NOT a WCON file,
        # the parser should raise an error.
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO('{"tracker-commons":false, "units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        # This should fail because "units" is required
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO('{"tracker-commons":true, "data":[]}'))

        # The smallest valid WCON file (Empty data array should be fine)
        WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        # Duplicate keys should cause the parser to fail
        with self.assertRaises(KeyError):
            WCONWorms.load(StringIO('{"tracker-commons":true, '
                                    '"units":{"t":"s","t":"s","x":"mm","y":"mm"}'
                                    ', "data":[]}'))


    def test_data1(self):
        # Single-valued 't' subelement should be fine
        WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                                 '"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))

        # Two values for 'x' but four values for 'y': error
        with self.assertRaises(AssertionError):
            WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                                     '"data":[{"id":3, "t":1.3, '
                                              '"x":[3,4], '
                                              '"y":[5.4,3,1,-3]}]}'))

        # Two values for 't' but 3 for 'x' and 'y': error
        with self.assertRaises(AssertionError):
            WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                                     '"data":[{"id":3, "t":[1.3,8], '
                                              '"x":[3,4,5], "y":[5.4,3,5]}]}'))

        # Duplicated segments are OK if there are no differences
        WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Duplicated time indices are OK if the earlier entry was for a 
        # different worm 
        WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":2, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Duplicated time indices are OK if the earlier entry had those entries
        # missing
        WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":1.3, "x":[null,null], "y":[null,null]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Error if data from a later segment conflicts with earlier segments
        with self.assertRaises(AssertionError):
            WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                '"data":[{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                        '{"id":1, "t":1.3, "x":[3,4], "y":[5.5,3]}]}'))

    def test_origin_offset(self):
        # ox, without optional bracket
        w1 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm","ox":"mm"},'
            '"data":[{"id":1, "t":1.3, "ox":5000, '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[5003,5004], "y":[5.4,3]}]}'))
        self.assertEqual(w1, w2)

        # oy, with optional bracket
        w1 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm","oy":"mm"},'
            '"data":[{"id":1, "t":1.3, "oy":[4000], '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[3,4], "y":[4005.4,4003]}]}'))
        self.assertEqual(w1, w2)

        # ox and oy, without optional brackets
        w1 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm","ox":"mm","oy":"mm"},'
            '"data":[{"id":1, "t":1.3, "ox":-500, "oy":4000, '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[-497,-496], "y":[4005.4,4003]}]}'))
        self.assertEqual(w1, w2)

        # ox, with two time points
        w1 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm","ox":"mm"},'
            '"data":[{"id":1, "t":[1.3,1.4], "ox":5000, '
            '"x":[[3],[4]], "y":[[5.4],[3]]}]}'))
        w2 = WCONWorms.load(StringIO('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
            '"data":[{"id":1, "t":[1.3,1.4], '
            '"x":[[5003],[5004]], "y":[[5.4],[3]]}]}'))
        self.assertEqual(w1, w2)


    def test_merge(self):
        JSON_path = '../../../tests/minimax.wcon'
        w2 = WCONWorms.load_from_file(JSON_path)
        w3 = WCONWorms.load_from_file(JSON_path)
        
        # This should work, since the data is the same
        w4 = w2 + w3

        # Modifying w2's data in just one spot is enough to make the data
        # clash and the merge should fail        
        w2.data.loc[1.3,(1,'x',0)] = 4000        
        with self.assertRaises(AssertionError):
            w4 = w2 + w3
        
        # But if we drop that entire row in w2, it should accomodate the new 
        # figure        
        w3.data.drop(1.3, axis=0, inplace=True)
        w4 = w2 + w3
        self.assertEqual(w4.data.loc[1.3,(1,'x',0)], 4000)

                
    def test_data2(self):
        WCON_string = \
            """
            {
                "tracker-commons":true,
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[
                       { "id":2, "t":1.4, "x":[125.11, 126.14, 117.12], "y":[23.3, 22.23, 21135.08] },
            		{ "id":1, "t":1.4, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] },
            		{ "id":2, "t":1.5, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] },
            		{ "id":1, "t":[1.3,1.5], "x":[[1,1,1],[1215.11, 1216.14, 1217.12]], "y":[[2,2,2],[234.89, 265.23, 235.08]] }
            	]
            }
            """
        self._validate_from_schema(WCON_string)
        WCONWorms.load(StringIO(WCON_string))

        # order permuted from previous example
        WCON_string2 = \
            """
            {
                "tracker-commons":true,
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[
            		{ "id":1, "t":[1.3,1.5], "x":[[1,1,1],[1215.11, 1216.14, 1217.12]], "y":[[2,2,2],[234.89, 265.23, 235.08]] },
                       { "id":2, "t":1.4, "x":[125.11, 126.14, 117.12], "y":[23.3, 22.23, 21135.08] },
            		{ "id":1, "t":1.4, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] },
            		{ "id":2, "t":1.5, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] }
            	]
            }
            """
        self._validate_from_schema(WCON_string2)
        WCONWorms.load(StringIO(WCON_string2))

        # Origin values
        WCON_string3 = \
            """
            {
                "tracker-commons":true,
                "units":{"t":"s", "x":"mm", "y":"mm", "ox":"mm"},
                "data":[{ "id":1, "t":[1.3,1.5], "x":[[1,1,1],[1215.11, 1216.14, 1217.12]], "y":[[2,2,2],[234.89, 265.23, 235.08]], "ox":5000 },
            		{ "id":2, "t":1.4, "x":[125.11, 126.14, 117.12], "y":[23.3, 22.23, 21135.08] },
            		{ "id":1, "t":1.4, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08]},
            		{ "id":2, "t":1.5, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] }
            	]
            }
            """
        self._validate_from_schema(WCON_string3)
        WCONWorms.load(StringIO(WCON_string3))


    def test_metadata(self):
        WCON_string1 = \
            """
            {
                "tracker-commons":true,
                "metadata":{
                       "lab":{"location":"CRB, room 5020", "name":"Behavioural Genomics" },
                       "who":"Firstname Lastname",
                       "timestamp":"2012-04-23T18:25:43.511Z",
                       "temperature":{ "experiment":22, "cultivation":20, "units":"C" },
                       "humidity":{ "value":40, "units":"%" },
                       "dish":{ "type":"petri", "size":35, "units":"mm" },
                       "food":"none",
                       "media":"agarose",
                       "sex":"hermaphrodite",
                       "stage":"adult",
                       "age":"18:25:43.511",
                       "strain":"CB4856",
                       "image_orientation":"imaged onto agar",
                       "protocol":"text description of protocol",
                       "software":{
                            "tracker":{"name":"Software Name", "version":"1.3.0"},
                            "featureID":"@OMG"
                       },
                       "settings":"Any valid JSON entry with hardware and software configuration can go here"
                },
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[
                    { "id":1, "t":1.3, "x":[7.2, 5], "y":[0.5, 0.86] }
                ]                
            }        
            """
        w1 = WCONWorms.load(StringIO(WCON_string1))

        self.assertEqual(w1.metadata["strain"], "CB4856")
        self.assertEqual(w1.metadata["dish"]["size"], 35)

    def test_bad_files_object(self):
        """
        Test that a badly specified "files" object will fail
        
        """
        # "this":null should be disallowed by the schema
        worm_file_text1 = (('{"tracker-commons":true,'
                            '"files":{"this":null, "prev":null, "next":["_1", "_2"]},'
                            '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":3, "t":1.3, '
                                                '"x":[3,4], "y":[5.4,3]}]}'))
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO(worm_file_text1))

        # "this":"" should be disallowed by the schema
        worm_file_text2 = (('{"tracker-commons":true,'
                            '"files":{"this":"", "prev":null, "next":["_1", "_2"]},'
                            '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":3, "t":1.3, '
                                                '"x":[3,4], "y":[5.4,3]}]}'))
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO(worm_file_text2))

    def test_load_from_file(self):
        """
        Test that .load_from_file works identically to .load

        """
        worm_file_text3 = (('{"tracker-commons":true,'
                            '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":3, "t":1.3, '
                                                '"x":[3,4], "y":[5.4,3]}]}'))

        # STREAM
        worm_from_stream = WCONWorms.load(StringIO(worm_file_text3))

        # FILE
        with open("test.wcon", 'w') as outfile:
            outfile.write(worm_file_text3)        
        worm_from_file = WCONWorms.load_from_file("test.wcon")
        os.remove("test.wcon")
        
        # COMPARISON
        self.assertEqual(worm_from_file, worm_from_stream)

    def test_chunks(self):
        """
        Test load_from_file with two or more chunks
        
        """
        # Define our chunks
        chunks = []
        chunks.append(('{"tracker-commons":true,'
                        '"files":{"this":"_0", "prev":null, "next":["_1", "_2"]},'  #'"files":{"this":"_0", "prev":null, "next":["_1", "_2"]},'
                        '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))
        chunks.append(('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                       '"files":{"this":"_1", "prev":["_0"], "next":["_2"]},'
                                 '"data":[{"id":3, "t":1.4, '
                                          '"x":[5,1], "y":[15.4,3]}]}'))
        chunks.append(('{"tracker-commons":true, "units":{"t":"s","x":"mm","y":"mm"},'
                       '"files":{"this":"_2", "prev":["_1", "_0"], "next":null},'
                                 '"data":[{"id":3, "t":1.5, '
                                          '"x":[8,4.2], "y":[35.4,3]}]}'))
        
        # Create filenames for our chunks
        chunk_filenames = ['']*len(chunks)
        for (chunk_index, chunk) in enumerate(chunks):
            chunk_filenames[chunk_index] = \
                'test_chunk_' + str(chunk_index) + '.wcon'
        
        # Save these chunks as files
        for (chunk_index, chunk) in enumerate(chunks):
            with open(chunk_filenames[chunk_index], 'w') as outfile:
                outfile.write(chunk)

        # First load one of them
        worm_combined_manually = WCONWorms.load(StringIO(chunks[0]))

        # Then merge the others sequentially to the first one
        for chunk in chunks[1:]:
            worm_chunk = WCONWorms.load(StringIO(chunk))
            worm_combined_manually += worm_chunk

        # Validate that the chunks together are __eq__ to the files
        # that find each other through their "files" object
        for chunk_filename in chunk_filenames:
            # Worm from files that found each other through 
            # the "files" object
            worm_from_files = WCONWorms.load_from_file(chunk_filename)
            
            self.assertEqual(worm_from_files, worm_combined_manually)

        # Delete the files created
        for chunk_filename in chunk_filenames:
            os.remove(chunk_filename)
            

    #@unittest.skip("DEBUG: to see if tests pass if we skip these")
    def test_data3(self):
        pass


if __name__ == '__main__':
    unittest.main()
