#!usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Sun Dec 13 00:47:21 2015

@author: Michael
"""

import pandas as pd
import warnings
from io import StringIO

from wcon_parser import WCONWorm
from measurement_unit import MeasurementUnit

pd.set_option('display.expand_frame_repr', False)

# Suppress RuntimeWarning warnings in Spider because it's a known bug
# http://stackoverflow.com/questions/30519487/
warnings.simplefilter(action = "ignore", category = RuntimeWarning)


if __name__ == '__main__':

    JSON_path = '../../tests/minimax.wcon'

    print("Loading " + JSON_path)

    w2 = WCONWorm.load_from_file(JSON_path)

    with open(JSON_path, 'r') as infile:
        w1 = WCONWorm.load(infile)
    
    u = MeasurementUnit('cm')



    
if __name__ == '__main__3':
    w1 = WCONWorm.load(StringIO('{"tracker-commons":true, "units":{},'
                                '"data":[{"id":3, "t":1.3, '
                                         '"x":[3,4,4,3.2], '
                                         '"y":[5.4,3,1,-3]}]}'))


