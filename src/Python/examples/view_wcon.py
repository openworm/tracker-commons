# -*- coding: utf-8 -*-
import sys

sys.path.append('..')
from wcon import WCONWorms, MeasurementUnit

file_name = 'asic-1 (ok415) on food L_2010_07_08__11_46_40___7___5.wcon'
w = WCONWorms.load_from_file(file_name)
