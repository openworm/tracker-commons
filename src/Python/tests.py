# -*- coding: utf-8 -*-
"""
Unit tests for the Python WCON parser

"""
import unittest
import json
import jsonschema
from io import StringIO
import os

from measurement_unit import MeasurementUnit
from wcon_parser import WCONWorm

class TestMeasurementUnit(unittest.TestCase):
    def test_unit_equivalence(self):
        self.assertTrue(MeasurementUnit('mm') == MeasurementUnit('millimetre'))
        self.assertTrue(MeasurementUnit('Mm') == MeasurementUnit('megametre'))
        self.assertTrue(MeasurementUnit('mm') != MeasurementUnit('Mm'))
        self.assertTrue(MeasurementUnit('d') == MeasurementUnit('day'))
        self.assertTrue(MeasurementUnit('min') == MeasurementUnit('minutes'))
        self.assertTrue(MeasurementUnit('%') != MeasurementUnit('K'))


    def test_canon_conversion(self):
        self.assertTrue(MeasurementUnit('cm').to_canon(1) == 10)
        self.assertTrue(MeasurementUnit('m').to_canon(1) == 1000)
        self.assertTrue(MeasurementUnit('F').from_canon(0) == 32)
        self.assertTrue(MeasurementUnit('microns').to_canon(1800) == 1.8)
        self.assertTrue(MeasurementUnit('F').to_canon(32) == 0)

        for suf in MeasurementUnit('m').all_suffixes:
            # Taking the value 10 to the canonical value, then back again,
            # should come back to 10 for all units (up to floating point 
            # epsilon)
            self.assertTrue(abs(
                        MeasurementUnit(suf).from_canon(
                        MeasurementUnit(suf).to_canon(10)) - 10
                      ) < 1e-8)
            

    def test_bad_units(self):
        # Verify that combining the full name with an abbreviation causes
        # an error to be raised
        with self.assertRaises(AssertionError):
            MeasurementUnit('msecond')
            
        with self.assertRaises(AssertionError):
            MeasurementUnit('millis')
            
        # This form, on the other hand, should be fine, since both prefix
        # and suffix are long-form.
        MeasurementUnit('milliseconds')


    @unittest.skip("Unit combinations are currently not implemented")
    def test_unit_combos(self):
        MeasurementUnit('m^2') == MeasurementUnit('metres^2')
        MeasurementUnit('C^3') == MeasurementUnit('Celsius^3')
        MeasurementUnit('m/s') == MeasurementUnit('metres/sec')
        MeasurementUnit('mm/s^2') == MeasurementUnit('millimeter/sec^2')
        MeasurementUnit('mm/s^2') == MeasurementUnit('mm/(s^2)')
        MeasurementUnit('mm^2/s^2') == MeasurementUnit('(mm^2)/(s^2)')
        MeasurementUnit('mm^2/s^2') == MeasurementUnit('mm^2/(s^2)')


class TestWCONParser(unittest.TestCase):
    def _validate_from_schema(self, wcon_string):
        try:
            jsonschema.validate(json.load(StringIO(wcon_string)), 
                                self._wcon_schema)
        except AttributeError:
            # Only load _wcon_schema if this method gets called.  Once
            # it's loaded, though, persist it in memory and don't lose it
            with open("../../wcon_schema.json", "r") as wcon_schema_file:
                self._wcon_schema = json.loads(wcon_schema_file.read())

            # Now that the schema has been loaded, we can try again
            self._validate_from_schema(wcon_string)

    def test_schema(self):
        basic_wcon = '{"tracker-commons":true, "units":{}, "data":[]}'
        
        self._validate_from_schema(basic_wcon)
        

    def test_tracker_commons_and_units(self):
        with self.assertRaises(ValueError):
            # The JSON parser shouldn't accept this as valid JSON
            # "ValueError: Expecting ',' delimiter: line 1 column 25 (char 24)"
            WCONWorm.load(StringIO('{"tracker-commons":blahblah}'))
        
        # Errors because "tracker-commons":true is not present
        with self.assertWarns(UserWarning):
            WCONWorm.load(StringIO('{"tracker-blah":true, "units":{}, "data":[]}'))
        with self.assertWarns(UserWarning):
            WCONWorm.load(StringIO('{"units":{}, "data":[]}'))
        # If we're being explicitly told that this is NOT a WCON file,
        # the parser should raise an error.
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":false, "units":{}, "data":[]}'))

        # This should fail because "units" is required
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true, "data":[]}'))

        # The smallest valid WCON file (Empty data array should be fine)
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}, "data":[]}'))

        # Duplicate keys should cause the parser to fail
        with self.assertRaises(KeyError):
            WCONWorm.load(StringIO('{"tracker-commons":true, '
                                    '"units":{"t":"s", "t":"s"}'
                                    ', "data":[]}'))


    def test_data1(self):
        # Single-valued 't' subelement should be fine
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                 '"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))

        # Two values for 'x' but four values for 'y': error
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                     '"data":[{"id":3, "t":1.3, '
                                              '"x":[3,4], '
                                              '"y":[5.4,3,1,-3]}]}'))

        # Two values for 't' but 3 for 'x' and 'y': error
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                     '"data":[{"id":3, "t":[1.3,8], '
                                              '"x":[3,4,5], "y":[5.4,3,5]}]}'))

        # Duplicated segments are OK if there are no differences
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Duplicated time indices are OK if the earlier entry was for a 
        # different worm 
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":2, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Duplicated time indices are OK if the earlier entry had those entries
        # missing
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, "x":[null,null], "y":[null,null]},'
                    '{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]}]}'))

        # Error if data from a later segment conflicts with earlier segments
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                '"data":[{"id":1, "t":1.3, "x":[3,4], "y":[5.4,3]},'
                        '{"id":1, "t":1.3, "x":[3,4], "y":[5.5,3]}]}'))

    @unittest.skip("TODO: enable these once we have __eq__ implemented")
    def test_origin_offset(self):
        # ox, without optional bracket
        w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, "ox":5000, '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[5003,5004], "y":[5.4,3]}]}'))
        self.assertEqual(w1, w2)

        # oy, with optional bracket
        w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, "oy":[4000], '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[3,4], "y":[4005.4,4003]}]}'))
        self.assertEqual(w1, w2)

        # ox and oy, without optional brackets
        w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, "ox":500, "oy":4000, '
            '"x":[3,4], "y":[4005.4,4003]}]}'))
        w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":1.3, '
            '"x":[503,504], "y":[4005.4,4003]}]}'))
        self.assertEqual(w1, w2)

        # ox, with two time points
        w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":[1.3,1.4], "ox":5000, '
            '"x":[3,4], "y":[5.4,3]}]}'))
        w2 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
            '"data":[{"id":1, "t":[1.3,1.4], '
            '"x":[5003,5004], "y":[5.4,3]}]}'))
        self.assertEqual(w1, w2)



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
        WCONWorm.load(StringIO(WCON_string))

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
        WCONWorm.load(StringIO(WCON_string2))

        # Origin values
        WCON_string3 = \
            """
            {
                "tracker-commons":true,
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[{ "id":1, "t":[1.3,1.5], "x":[[1,1,1],[1215.11, 1216.14, 1217.12]], "y":[[2,2,2],[234.89, 265.23, 235.08]], "ox":5000 },
            		{ "id":2, "t":1.4, "x":[125.11, 126.14, 117.12], "y":[23.3, 22.23, 21135.08] },
            		{ "id":1, "t":1.4, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08]},
            		{ "id":2, "t":1.5, "x":[1215.11, 1216.14, 1217.12], "y":[234.89, 265.23, 235.08] }
            	]
            }
            """
        self._validate_from_schema(WCON_string3)
        WCONWorm.load(StringIO(WCON_string3))

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
        w1 = WCONWorm.load(StringIO(WCON_string1))

        self.assertEqual(w1.metadata["strain"], "CB4856")
        self.assertEqual(w1.metadata["dish"]["size"], 35)

    @unittest.skip("TODO: not yet written")
    def test_chuncks(self):
        # Define our chunks
        chunks = []
        chunks.append(('{"tracker-commons":true,'
                        '"files":{"this":"_0", "prev:["_1", "_0"], "next"}'
                        '"units":{},"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))
        chunks.append(('{"tracker-commons":true, "units":{},'
                                 '"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))
        chunks.append(('{"tracker-commons":true, "units":{},'
                                 '"data":[{"id":3, "t":1.3, '
                                          '"x":[3,4], "y":[5.4,3]}]}'))

        # Create filenames for our chunks
        chunk_filenames = []
        for (chunk_index, chunk) in enumerate(chunks):
            chunk_filenames[chunk_index] = \
                'test_chunk_' + str(chunk_index) + '.wcon'
        
        # Save these chunks as files
        for (chunk_index, chunk) in enumerate(chunks):
            with open(chunk_filenames[chunk_index], 'w') as outfile:
                outfile.write(chunk)

        # Load them
        WCONWorm.load(StringIO(chunks[0]))

        # Validate that the chunks together are __eq__ to a merged file

        # Delete the files created
        for chunk_filename in chunk_filenames:
            os.remove(chunk_filename)
            

    @unittest.skip("DEBUG: to see if tests pass if we skip these")
    def test_data3(self):
        pass


if __name__ == '__main__':
    unittest.main()