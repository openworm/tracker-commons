import sys
sys.path.append('../../Python')

import wcon
from wcon import WCONWorms
from wcon import MeasurementUnit

worm = WCONWorms.load_from_file('../../../tests/minimax.wcon')
worm.save_to_file('pythonNonWrapperTest.wcon', pretty_print=True)

mergeable = WCONWorms.load_from_file('extra-test-data/minimax-mergeable.wcon')

merged = worm + mergeable
merged.save_to_file('pythonMerged.wcon', pretty_print=True)

merged2 = mergeable + worm
merged2.save_to_file('pythonMerged2.wcon', pretty_print=True)

nometa = WCONWorms.load_from_file('../../../tests/minimal.wcon')
print(nometa.metadata)
