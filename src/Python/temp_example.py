#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Created on Sun Dec 13 00:47:21 2015

@author: Michael
"""
import numpy as np
import pandas as pd
import warnings
from six import StringIO
import json

from wcon import WCONWorms, MeasurementUnit

pd.set_option('display.expand_frame_repr', False)

# Suppress RuntimeWarning warnings in Spider because it's a known bug
# http://stackoverflow.com/questions/30519487/
# warnings.simplefilter(action = "ignore", category = RuntimeWarning)


if __name__ == '__main__':

    JSON_path = '../../tests/minimax.wcon'

    w2 = WCONWorms.load_from_file(JSON_path)

    w2.save_to_file('fwefwef.WCON', pretty_print=True)
    w3 = WCONWorms.load_from_file('fwefwef.WCON')

    u = MeasurementUnit.create('cm')

    # io=StringIO()
    # json.dump([None], io)
    # io.getvalue()

if __name__ == '__main__2':
    worm_file_text2 = (('{"units":{"t":"s","x":"m","y":"m"},'
                        '"data":[{"id":3, "t":1.3, '
                        '"x":[3,4], "y":[5.4,3]}]}'))

    w1 = WCONWorms.load(StringIO(worm_file_text2))


if __name__ == '__main__3':
    w1 = WCONWorms.load(StringIO('{"units":{},'
                                 '"data":[{"id":3, "t":1.3, '
                                 '"x":[3,4,4,3.2], '
                                 '"y":[5.4,3,1,-3]}]}'))
