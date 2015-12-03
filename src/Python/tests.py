# -*- coding: utf-8 -*-
"""
Unit tests for the Python WCON parser

"""
import unittest
from io import StringIO

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

    def test_tracker_commons_and_units(self):
        with self.assertRaises(ValueError):
            # The JSON parser shouldn't accept this as valid JSON
            # "ValueError: Expecting ',' delimiter: line 1 column 25 (char 24)"
            WCONWorm.load(StringIO('{"tracker-commons":blahblah}'))
        
        #import pdb
        #pdb.set_trace()
        # Errors because "tracker-commons":true is not present
        with self.assertWarns(UserWarning):
            WCONWorm.load(StringIO('{"tracker-blah":true, "units":{}}'))
        with self.assertWarns(UserWarning):
            WCONWorm.load(StringIO('{"units":{}}'))
        # If we're being explicitly told that this is NOT a WCON file,
        # the parser should raise an error.
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":false, "units":{}}'))

        # This should fail because "units" is required
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true}'))

        # The smallest valid WCON file
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}}'))

        # Empty data array should be fine
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},' 
                                '"data":[]}'))

        # Duplicate keys should cause the parser to fail
        with self.assertRaises(KeyError):
            WCONWorm.load(StringIO('{"tracker-commons":true, '
                                    '"units":{"t":"s", "t":"s"}}'))


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
        WCONWorm.load(StringIO(WCON_string3))

    @unittest.skip("DEBUG: to see if tests pass if we skip these")
    def test_data3(self):
        pass


if __name__ == '__main__':
    unittest.main()