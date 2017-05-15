# -*- coding: utf-8 -*-
import sys

sys.path.append('..')
from wcon import WCONWorms

import tests

tests.setUpModule()

file_name = 'asic-1 (ok415) on food L_2010_07_08__11_46_40___7___5.wcon'
w = WCONWorms.load_from_file(file_name)

# Truncate to just the first 10 seconds
w.data_as_odict['1'] = w.data_as_odict['1'].ix[0:10]

# Grab the first 10 frames (doesn't work since the first 3885 frames are NaN)
#w.data_as_odict['1'] = w.data_as_odict['1'].iloc[:10]

# View the experiment data as a CSV (so it can be loaded in Excel, for instance)
#w.data_as_odict['1'].to_csv('bbb.csv')

# Save the smaller file
w.save_to_file('smaller.wcon', pretty_print=True)

tests.tearDownModule()