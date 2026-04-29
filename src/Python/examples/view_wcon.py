# -*- coding: utf-8 -*-
import sys

sys.path.append('..')
from wcon import WCONWorms, MeasurementUnit

file_name = '../../../tests/minimax.wcon'
w = WCONWorms.load_from_file(file_name)
