import sys
sys.path.append('../../Python')

import wcon
from wcon import WCONWorms
from wcon import MeasurementUnit

worm = WCONWorms.load_from_file('../../../tests/minimax.wcon')
worm.save_to_file('pythonNonWrapperTest.wcon',pretty_print=True)
