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

import pandas as pd
idx = pd.IndexSlice
import numpy as np
import time

sys.path.append('..')
from wcon import WCONWorms, MeasurementUnit
from wcon.measurement_unit import MeasurementUnitAtom

if __name__ == '__main__':
    print("BIG TEST: Test load and save and load and save")
    files_to_test = [sys.argv[1]]

    for JSON_path in files_to_test:
        for pretty in [True, False]:
            print("LOADING FOR TEST: " + JSON_path +
                  " (PRETTY = " + str(pretty) + ")")

            start_time = time.monotonic()
            w1 = WCONWorms.load_from_file(JSON_path,
                                          validate_against_schema=False)
            print("Time to load w1: " + str(time.monotonic() - start_time))

            # Save these worm tracks to a file, then load that file
            test_path = 'test.wcon'
            start_time = time.monotonic()
            w1.save_to_file(test_path, pretty_print=pretty)
            print("Time to save w1: " + str(time.monotonic() - start_time))

            start_time = time.monotonic()
            w2 = WCONWorms.load_from_file(test_path,
                                          validate_against_schema=False)
            print("Time to load w2: " + str(time.monotonic() - start_time))

            x1 = w1.data.loc[:, idx[0, 'x', 0]].fillna(0)
            x2 = w2.data.loc[:, idx[0, 'x', 0]].fillna(0)
            cmm = np.flatnonzero(x1 != x2)
            xx = pd.concat([x1, x2], axis=1)
            xx = xx.loc[cmm]

            # Then load and save AGAIN and do a file comparison to make
            # sure it's the same
            # this will test that we order the keys (even though this
            # isn't in the WCON standard it's nice for human
            # readability, i.e. to have "units" before "data",
            # "id" first in a data segment, etc.)
            w3 = WCONWorms.load_from_file(test_path,
                                          validate_against_schema=False)
            assert(w2 == w3)
            assert(w1 == w2)
            assert(w1 == w3)

            test_path2 = 'test2.wcon'
            w3.save_to_file(test_path2, pretty_print=pretty)

            # As described in the above comment: check that running
            # load/save twice should generate an IDENTICAL file.
            assert(filecmp.cmp(test_path, test_path2))

            os.remove(test_path)
            os.remove(test_path2)

# test_big_file()
