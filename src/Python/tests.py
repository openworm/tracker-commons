# -*- coding: utf-8 -*-
"""
Unit tests for the Python WCON parser

"""
import unittest
from io import StringIO

from measurement_unit import MeasurementUnit
from wcon_parser import WCONWorm

class TestMeasurementUnit(unittest.TestCase):
    def test(self):
        self.assertTrue(MeasurementUnit('mm') == MeasurementUnit('millimetre'))
        self.assertTrue(MeasurementUnit('Mm') == MeasurementUnit('megametre'))
        self.assertTrue(MeasurementUnit('mm') != MeasurementUnit('Mm'))
        self.assertTrue(MeasurementUnit('d') == MeasurementUnit('day'))
        self.assertTrue(MeasurementUnit('min') == MeasurementUnit('minutes'))
        self.assertTrue(MeasurementUnit('%') != MeasurementUnit('K'))
    
        self.assertTrue(MeasurementUnit('cm').to_canon(1) == 10)
        
        self.assertTrue(MeasurementUnit('m').to_canon(1) == 1000)
        
        for suf in MeasurementUnit('m').all_suffixes:
            # Taking the value 10 to the canonical value, then back again,
            # should come back to 10 for all units (up to floating point 
            # epsilon)
            self.assertTrue(abs(
                        MeasurementUnit(suf).from_canon(
                        MeasurementUnit(suf).to_canon(10)) - 10
                      ) < 1e-8)
            
        self.assertTrue(MeasurementUnit('F').to_canon(32) == 0)


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


class TestWCONParser(unittest.TestCase):
    def test(self):
        with self.assertRaises(ValueError):
            # The JSON parser shouldn't accept this as valid JSON
            # "ValueError: Expecting ',' delimiter: line 1 column 25 (char 24)"
            WCONWorm.load(StringIO('{"tracker-commons":blahblah}'))
        
        # Errors because "tracker-commons":true is not present
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-blah":true}'))
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":false}'))

        # This should fail because "units" is required
        with self.assertRaises(AssertionError):
            WCONWorm.load(StringIO('{"tracker-commons":true}'))

        # The smallest valid WCON file
        WCONWorm.load(StringIO('{"tracker-commons":true, "units":{}}'))



if __name__ == '__main__':
    unittest.main()