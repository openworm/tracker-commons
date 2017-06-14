#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Unit tests for the Python WCON parser

"""
import os
import sys
from six import StringIO  # StringIO.StringIO in 2.x, io.StringIO in 3.x
import json
import jsonschema
import unittest
import filecmp
import glob
import collections
import shutil

sys.path.append('..')
from wcon import WCONWorms, MeasurementUnit
from wcon.measurement_unit import MeasurementUnitAtom


def setUpModule():
    # If the wcon module is installed via pip, wcon_schema.json is included
    # in the proper place.  In the git repo it is not, however, so to test
    # we must copy it over temporarily, then remove it once tests are done.
    shutil.copyfile('../../../wcon_schema.json', '../wcon/wcon_schema.json')


def tearDownModule():
    os.remove('../wcon/wcon_schema.json')


def flatten(list_of_lists):
    """
    Recursively travel through a list of lists, flattening them.

    """
    # From http://stackoverflow.com/questions/2158395
    for element in list_of_lists:
        # If it's iterable but not a string or bytes, then recurse, otherwise
        # we are at a "leaf" node of our traversal
        if(isinstance(element, collections.Iterable) and
           not isinstance(element, (str, bytes))):
            for sub_element in flatten(element):
                yield sub_element
        else:
            yield element


class TestDocumentationExamples(unittest.TestCase):

    def test_pull_doc_examples(self):
        """
        Pull out WCON examples from all .MD files and validate them.

        """
        cur_path = os.path.abspath(os.path.join(os.path.dirname(__file__),
                                                '..', '..', '..'))
        md_paths = [glob.glob(os.path.join(x[0], '*.md'))
                    for x in os.walk(cur_path)]
        md_paths = [x for x in md_paths if x != []]

        # Flatten, since the lists might be nested
        md_paths = list(flatten(md_paths))

        for md_path in md_paths:
            print("Checking for JSON code in %s " % md_path)
            with open(md_path, 'r') as f:
                md_string = f.read()
                # Find all code examples
                code_snippets = md_string.split('```')[1::2]

                # Consider only JSON code examples
                JSON_snippets = [s[4:] for s in code_snippets
                                 if s[:4] == 'JSON']

                for i, JSON_snippet in enumerate(JSON_snippets):
                    print("Testing JSON code snippet %i from %s" %
                          (i + 1, md_path))

                    JSON_snippet = JSON_snippet.replace('\n', '')
                    JSON_snippet = JSON_snippet.replace('\r', '')

                    WCONWorms.load(StringIO(JSON_snippet))


class TestMeasurementUnit(unittest.TestCase):

    def test_units_with_numbers(self):
        MU = MeasurementUnit
        self.assertTrue(MU.create('0.04*s').to_canon(1) == 0.04)
        self.assertTrue(MU.create('0.04/C').to_canon(1) == 0.04)
        self.assertTrue(MU.create('0.04/us').to_canon(-34.5) == -1380000)
        self.assertTrue(MU.create('4').from_canon(4) == 1)
        self.assertTrue(MU.create('0.04*s').canonical_unit_string == 's')
        self.assertTrue(MU.create('1/234234').canonical_unit_string == '')

    def test_unit_equivalence(self):
        MU = MeasurementUnit
        self.assertTrue(MU.create('mm') == MU.create('millimetre'))
        self.assertTrue(MU.create('Mm') == MU.create('megametre'))
        self.assertTrue(MU.create('mm') != MU.create('Mm'))
        self.assertTrue(MU.create('d') == MU.create('day'))
        self.assertTrue(MU.create('min') == MU.create('minutes'))
        self.assertTrue(MU.create('%') != MU.create('K'))

    def test_canon_conversion(self):
        MU = MeasurementUnit
        self.assertTrue(MU.create('cm').to_canon(1) == 10)
        self.assertTrue(MU.create('m').to_canon(1) == 1000)
        self.assertTrue(MU.create('F').from_canon(0) == 32)
        self.assertTrue(MU.create('microns').to_canon(1800) == 1.8)
        self.assertTrue(MU.create('F').to_canon(32) == 0)

        for suf in MeasurementUnitAtom('').all_suffixes:
            # Taking the value 10 to the canonical value, then back again,
            # should come back to 10 for all units (up to floating point
            # epsilon)
            mu_there = MU.create(suf).to_canon(10)
            mu_there_and_back = MU.create(suf).from_canon(mu_there)
            self.assertTrue(abs(mu_there_and_back - 10) < 1e-8)

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
        MU = MeasurementUnit
        MU.create('m^2') == MU.create('metres^2')
        MU.create('C') == MU.create('celsius')
        MU.create('m/s') == MU.create('metres/sec')
        MU.create('mm/s^2') == MU.create('millimeter/sec^2')
        MU.create('mm/s^2') == MU.create('mm/(s^2)')
        MU.create('mm^2/s^2') == MU.create('(mm^2)/(s^2)')
        MU.create('mm^2/s^2') == MU.create('mm^2/(s^2)')

        self.assertTrue(MU.create('m/min').to_canon(3) == 50)
        self.assertTrue(MU.create('1/min').to_canon(60) == 1)


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

    def test_schema(self):
        basic_wcon = '{"units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'

        self._validate_from_schema(basic_wcon)

    def test_equality_operator(self):
        JSON_path = '../../../tests/minimax.wcon'
        w2 = WCONWorms.load_from_file(JSON_path)
        w2.units['y'] = MeasurementUnit.create('m')
        w2data = w2.data.copy()

        w2c = w2.to_canon
        # A change to_canon should change the data in one but equality
        # should remain
        self.assertFalse(WCONWorms.is_data_equal(w2, w2c, convert_units=False))
        self.assertEqual(w2, w2)
        self.assertEqual(w2c, w2c)
        self.assertEqual(w2, w2c)

        # Change the units for w2 (not really what we should do but just
        # force a difference now, with w2c)
        w2.units['y'] = MeasurementUnit.create('mm')
        self.assertNotEqual(w2, w2c)

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

                w_loaded = WCONWorms.load_from_file(
                    JSON_path, validate_against_schema=False)

                # Save these worm tracks to a file, then load that file
                test_path = 'test.wcon'
                w_loaded.save_to_file(test_path, pretty_print=pretty)
                w_from_saved = WCONWorms.load_from_file(
                    test_path, validate_against_schema=False)

                self.assertEqual(w_loaded, w_from_saved)

                # then load and save AGAIN and do a file comparison to make
                # sure it's the same
                # this will test that we order the keys (even though this
                # isn't in the WCON standard it's nice for human
                # readability, i.e. to have "units" before "data",
                # "id" first in a data segment, etc.)
                w_loaded_again = WCONWorms.load_from_file(
                    test_path, validate_against_schema=False)
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
            WCONWorms.load(StringIO('{"lalala":blahblah}'))

        WCONWorms.load(
            StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        # This should fail because "units" is required
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO('{"data":[]}'))

        # The smallest valid WCON file (Empty data array should be fine)
        WCONWorms.load(
            StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, "data":[]}'))

        # Duplicate keys should cause the parser to fail
        with self.assertRaises(KeyError):
            WCONWorms.load(
                StringIO(
                    '{"units":{"t":"s","t":"s","x":"mm","y":"mm"},'
                    '"data":[]}'))

    def test_arrayed_software(self):
        WCONWorms.load(StringIO('{ "units":{"t":"s", "x":"mm", "y":"mm"}, '
                                '"data":[], "metadata":{'
                                '"software":[{"name":"a"}, '
                                '{"name":"b"}]} }'))

    def test_empty_aspect_size(self):
        # Worms with a segment that is empty should still parse without issue.
        WCON_string = \
            """
            {
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[{ "id":"2", "t":[1.4], "x":[[125.11, 126.14, 117.12]],
                          "y":[[23.3, 22.23, 21135.08]] },
                        { "id":"1", "t":[1.4], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] },
                        { "id":"2", "t":[1.5], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[234.89, 265.23, 235.08] },
                        { "id":"1", "t":[1.3,1.5],
                          "x":[[],[1215.11, 1216.14, 1217.12]],
                          "y":[[],[234.89, 265.23, 235.08]] }
                ]
            }
            """
        w = WCONWorms.load(StringIO(WCON_string))

        test_path = 'test_empty_aspect.wcon'

        w.save_to_file(test_path, pretty_print=True)
        w_from_saved = WCONWorms.load_from_file(test_path)

        self.assertEqual(w, w_from_saved)

        os.remove(test_path)

    def test_data1(self):
        # Two values for 'x' but four values for 'y': error
        with self.assertRaises(AssertionError):
            WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"},'
                                    '"data":[{"id":"3", "t":[1.3], '
                                    '"x":[[3,4]], '
                                    '"y":[[5.4,3,1,-3]]}]}'))

        # Two values for 't' but 3 for 'x' and 'y': error
        with self.assertRaises(AssertionError):
            WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"},'
                                    '"data":[{"id":"3", "t":[1.3,8], '
                                    '"x":[3,4,5], "y":[5.4,3,5]}]}'))

        # Duplicated segments are OK if there are no differences
        WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm"},'
                '"data":[{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]},'
                '{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]}]}'))

        # Duplicated time indices are OK if the earlier entry was for a
        # different worm
        WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm"},'
                '"data":[{"id":"2", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]},'
                '{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]}]}'))

        # Duplicated time indices are OK if the earlier entry had those entries
        # missing
        WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm"},'
                '"data":[{"id":"1", "t":[1.3], "x":[[null,null]], "y":[[null,null]]},'
                '{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]}]}'))

        # Error if data from a later segment conflicts with earlier segments
        with self.assertRaises(AssertionError):
            WCONWorms.load(
                StringIO(
                    '{"units":{"t":"s","x":"mm","y":"mm"},'
                    '"data":[{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]},'
                    '{"id":"1", "t":[1.3], "x":[[3,4]], "y":[[5.5,3]]}]}'))

    def test_origin_offset(self):
        # ox and oy
        w1 = WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm","ox":"mm","oy":"mm"},'
                '"data":[{"id":"1", "t":[1.3], "ox":[-500], "oy":[4000], '
                '"x":[[3,4]], "y":[[5.4,3]]}]}'))
        w2 = WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"},'
                                     '"data":[{"id":"1", "t":[1.3], '
                                     '"x":[[-497,-496]], "y":[[4005.4,4003]]}]}'))
        self.assertEqual(w1, w2)

        # ox with two time points
        w1 = WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm","ox":"mm","oy":"mm"},'
                '"data":[{"id":"1", "t":[1.3,1.4], "ox":[5000, 5000], "oy":[0, 0],'
                '"x":[[3],[4]], "y":[[5.4],[3]]}]}'))
        w2 = WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm"},'
                '"data":[{"id":"1", "t":[1.3,1.4], '
                '"x":[[5003],[5004]], "y":[[5.4],[3]]}]}'))
        self.assertEqual(w1, w2)

    def test_centroid(self):
        # ox, with two time frames, with centroid
        w1 = WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm","ox":"mm","oy":"mm",'
                '"cx":"mm","cy":"mm"},'
                '"data":[{"id":"1", "t":[1.3,1.4], "ox":[0,0], "oy":[5000,4990], '
                '"cx":[10, 11], "cy":[13, 12], "x":[[3, 5],[4, 5]], "y":[[5.4, 4],[3, 2]]}]}'))
        w2 = WCONWorms.load(
            StringIO(
                '{"units":{"t":"s","x":"mm","y":"mm",'
                '"cx":"mm","cy":"mm"},'
                '"data":[{"id":"1", "t":[1.3,1.4], "cx":[10, 11], "cy":[5013, 5002], '
                '"x":[[3, 5],[4, 5]], "y":[[5005.4, 5004],[4993, 4992]]}]}'))
        self.assertEqual(w1, w2)

        # oy with no oy: assertionerror
        with self.assertRaises(AssertionError):
            w1 = WCONWorms.load(
                StringIO(
                    '{"units":{"t":"s","x":"mm","y":"mm","oy":"mm",'
                    '"cx":"mm","cy":"mm"},'
                    '"data":[{"id":"1", "t":[1.3,1.4], "oy":[5000, 5000],'
                    '"cx":[10, 10], "cy":[10, 10], "x":[[3, 3, 3],[4, 4, 4.2]], '
                    '"y":[[5.4, 5.4, 5.5],[3, 3, 7]]}]}'))

        # units missing for centroid
        with self.assertRaises(AssertionError):
            WCONWorms.load(
                StringIO(
                    '{"units":{"t":"s","x":"mm","y":"mm","ox":"mm","oy":"mm"},'
                    '"data":[{"id":"1", "t":[1.3,1.4], "ox":[5, 5], "oy":[0, 0],'
                    '         "cx":[10, 10], "cy":[10, 10], "x":[[3],[4]], '
                    '         "y":[[5.4],[3]]}]}'))

    def test_merge(self):
        JSON_path = '../../../tests/minimax.wcon'
        w2 = WCONWorms.load_from_file(JSON_path)
        w3 = WCONWorms.load_from_file(JSON_path)

        # This should work, since the data is the same
        w4 = w2 + w3

        # Modifying w2's data in just one spot is enough to make the data
        # clash and the merge should fail
        w2._data[1].loc[1.3, (1, 'x', 0)] = 4000
        with self.assertRaises(AssertionError):
            w4 = w2 + w3

        # But if we drop that entire row in w3, it should accomodate the new
        # figure
        w3._data[1].drop(1.3, axis=0, inplace=True)
        w4 = w2 + w3
        self.assertEqual(w4.data.loc[1.3, (1, 'x', 0)], 4000)

    def test_merge_commutativity(self):
        worm1 = WCONWorms.load(
            StringIO('{"units":{"t":"s","x":"mm","y":"mm"},'
                     '"data":[{"id":"3", "t":[1.3], "x":[[3,4]], "y":[[5.4,3]]}]}'))
        worm2 = WCONWorms.load(
            StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, '
                     '"data":[{"id":"4", "t":[1.5], "x":[[5,2]], "y":[[1.4,6]]}]}'))

        merged = worm1 + worm2
        # merged.save_to_file('pythonMerged.wcon', pretty_print=True)

        merged2 = worm2 + worm1
        # merged2.save_to_file('pythonMerged2.wcon', pretty_print=True)

        self.assertNotEqual(worm1, worm2)
        self.assertEqual(merged, merged2)

    def test_data2(self):
        WCON_string = \
            """
            {
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[{ "id":"2", "t":[1.4], "x":[[125.11, 126.14, 117.12]],
                          "y":[[23.3, 22.23, 21135.08]] },
                        { "id":"1", "t":[1.4], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] },
                        { "id":"2", "t":[1.5], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] },
                        { "id":"1", "t":[1.3,1.5],
                          "x":[[1,1,1],[1215.11, 1216.14, 1217.12]],
                          "y":[[2,2,2],[234.89, 265.23, 235.08]] }
                ]
            }
            """
        self._validate_from_schema(WCON_string)
        WCONWorms.load(StringIO(WCON_string))

        # Test that extra features are ignored
        WCON_string1 = \
            """
            {
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[{ "id":"2", "t":[1.4], "x":[[125.11, 126.14, 117.12]],
                          "y":[[23.3, 22.23, 21135.08]] },
                        { "id":"1", "t":[1.4], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] },
                        { "id":"2", "t":[1.5], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]], "ignorethat":"yes" },
                        { "id":"1", "t":[1.3,1.5], "ignorethis": 12,
                          "x":[[1,1,1],[1215.11, 1216.14, 1217.12]],
                          "y":[[2,2,2],[234.89, 265.23, 235.08]] }
                ]
            }
            """
        self._validate_from_schema(WCON_string1)
        w = WCONWorms.load(StringIO(WCON_string1))
        # TODO: test that "ignorethis" and "ignorethat" are not present in w

        # order permuted from previous example
        WCON_string2 = \
            """
            {
                "units":{"t":"s", "x":"mm", "y":"mm"},
                "data":[{ "id":"1", "t":[1.3,1.5],
                          "x":[[1,1,1],[1215.11, 1216.14, 1217.12]],
                          "y":[[2,2,2],[234.89, 265.23, 235.08]] },
                        { "id":"2", "t":[1.4], "x":[[125.11, 126.14, 117.12]],
                          "y":[[23.3, 22.23, 21135.08]] },
                         { "id":"1", "t":[1.4], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] },
                        { "id":"2", "t":[1.5], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] }
                ]
            }
            """
        self._validate_from_schema(WCON_string2)
        WCONWorms.load(StringIO(WCON_string2))

        # Origin values
        WCON_string3 = \
            """
            {
                "units":{"t":"s", "x":"mm", "y":"mm", "ox":"mm", "oy":"mm"},
                "data":[{ "id":"1", "t":[1.3,1.5],
                          "x":[[1,1,1],[1215.11, 1216.14, 1217.12]],
                          "y":[[2,2,2],[234.89, 265.23, 235.08]],
                          "ox":[5000, 5000], "oy":[0, 0] },
                        { "id":"2", "t":[1.4], "x":[[125.11, 126.14, 117.12]],
                          "y":[[23.3, 22.23, 21135.08]] },
                        { "id":"1", "t":[1.4], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]]},
                        { "id":"2", "t":[1.5], "x":[[1215.11, 1216.14, 1217.12]],
                          "y":[[234.89, 265.23, 235.08]] }
                ]
            }
            """
        self._validate_from_schema(WCON_string3)
        WCONWorms.load(StringIO(WCON_string3))

    def test_metadata(self):
        WCON_string1 = \
            """
            {
                "metadata":{
                       "lab":{"location":"CRB, room 5020",
                              "name":"Behavioural Genomics" },
                       "who":"Firstname Lastname",
                       "timestamp":"2012-04-23T18:25:43.511Z",
                       "temperature":23.8,
                       "humidity":40.3,
                       "dish":{ "style":"petri", "size":35, "units":"mm" },
                       "food":"none",
                       "media":"agarose",
                       "sex":"hermaphrodite",
                       "stage":"adult",
                       "age":66.8,
                       "strain":"CB4856",
                       "protocol":"text description of protocol",
                       "software":{
                            "tracker":{"name":"Software Name",
                                       "version":"1.3.0"},
                            "featureID":"@OMG"
                       },
                       "settings":
    "Any valid string with hardware and software configuration details"
                },
                "units":{"t":"s", "x":"mm", "y":"mm", "humidity":"%",
                         "temperature":"C", "age":"h"},
                "data":[
                    { "id":"1", "t":[1.3], "x":[[7.2, 5]], "y":[[0.5, 0.86]] }
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
        # "current":null should be disallowed by the schema
        worm_file_text1 = (
            ('{"files":{"current":null, "prev":null, "next":["_1", "_2"]},'
             '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":"3", "t":[1.3], '
             '"x":[[3,4]], "y":[[5.4,3]]}]}'))
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO(worm_file_text1))

        # missing "current" should be disallowed by the schema
        worm_file_text2 = (
            ('{"files":{"prev":null, "next":["_1", "_2"]},'
             '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":"3", "t":[1.3], '
             '"x":[[3,4]], "y":[[5.4,3]]}]}'))
        with self.assertRaises(jsonschema.exceptions.ValidationError):
            WCONWorms.load(StringIO(worm_file_text2))

    def test_load_from_file(self):
        """
        Test that .load_from_file works identically to .load

        """
        worm_file_text3 = (
            ('{"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":"3", "t":[1.3], '
             '"x":[[3,4]], "y":[[5.4,3]]}]}'))

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
        chunks.append(
            ('{"files":{"current":"0.wcon", "prev":null, "next":["1.wcon", "2.wcon"]},'
             '"units":{"t":"s","x":"mm","y":"mm"},"data":[{"id":"3", "t":[1.3], '
             '"x":[[3,4]], "y":[[5.4,3]]}]}'))
        chunks.append(('{"units":{"t":"s","x":"mm","y":"mm"},'
                       '"files":{"current":"1.wcon", "prev":["0.wcon"], "next":["2.wcon"]},'
                       '"data":[{"id":"3", "t":[1.4], '
                       '"x":[[5,1]], "y":[[15.4,3]]}]}'))
        chunks.append(
            ('{"units":{"t":"s","x":"mm","y":"mm"},'
             '"files":{"current":"2.wcon", "prev":["1.wcon", "0.wcon"], "next":null},'
             '"data":[{"id":"3", "t":[1.5], '
             '"x":[[8,4.2]], "y":[[35.4,3]]}]}'))

        # Create filenames for our chunks
        chunk_filenames = [''] * len(chunks)
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

    # @unittest.skip("DEBUG: to see if tests pass if we skip these")
    def test_data3(self):
        pass


if __name__ == '__main__':
    unittest.main()
