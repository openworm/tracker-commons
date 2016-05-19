wconoct;
loaded_worm = wconoct.load_from_file('../../../tests/minimax.wcon');
wconoct.save_to_file(loaded_worm,'wrappertest.wcon');
disp(loaded_worm);
canonical_worm = wconoct.to_canon(loaded_worm);
disp(canonical_worm);
wconoct.save_to_file(canonical_worm,'wrapperCanonical.wcon');
conflict_worm = wconoct.load_from_file('extra-test-data/minimax-conflict.wcon');
mergeable_worm = wconoct.load_from_file('extra-test-data/minimax-mergeable.wcon');
if (wconoct.eq(loaded_worm,canonical_worm) == 1)
  disp('Equal');
 else
   disp('Not Equal');
endif
added_worm = wconoct.add(loaded_worm,canonical_worm);
if (wconoct.eq(loaded_worm,added_worm) == 1)
  disp('Equal');
 else
   disp('Not Equal');
endif
disp('Trying to merge data with conflict');
added_worm = wconoct.add(loaded_worm,conflict_worm);
disp('Trying to merge compatible data');
added_worm = wconoct.add(loaded_worm,mergeable_worm);
if (wconoct.eq(loaded_worm,added_worm) == 1)
  disp('Equal');
 else
   disp('Not Equal');
endif

units_data = wconoct.units(loaded_worm);
num_units = wconoct.unitsDict_numElements(units_data);
t_mu = wconoct.unitsDict_valueFromKey(units_data,"'t'");
t_unitStr = wconoct.MU_unit_string(t_mu);
disp("units for 't' is:");
disp(t_unitStr);
width_mu = wconoct.unitsDict_valueFromKey(units_data,"'width'");
width_unitStr = wconoct.MU_unit_string(width_mu);
disp("units for 'width' is:");
disp(width_unitStr);
width_canonUnitStr = wconoct.MU_canonical_unit_string(width_mu);
disp("Canonical units for 'width' is:");
disp(width_canonUnitStr);
age_mu = wconoct.unitsDict_valueFromKey(units_data,"'age'");
age_unitStr = wconoct.MU_unit_string(age_mu);
disp("units for 'age' is:");
disp(age_unitStr);

metadata_data = wconoct.metadata(loaded_worm);
disp(metadata_data);
if (wconoct.isNoneHandle(metadata_data) == 1)
  disp('metadata is empty');
 else
   disp('Non-empty metadata');
endif
minimal_worm = wconoct.load_from_file('../../../tests/minimal.wcon');
empty_metadata_data = wconoct.metadata(minimal_worm);
disp(empty_metadata_data);
if (wconoct.isNoneHandle(empty_metadata_data) == 1)
  disp('metadata is empty');
 else
   disp('Non-empty metadata');
endif
num_worms = wconoct.num_worms(loaded_worm);
disp(num_worms);
worm_ids = wconoct.worm_ids(loaded_worm);
data_odict = wconoct.data_as_odict(loaded_worm);

hours_unit = MU_create('h');
canon_val = MU_to_canon(hours_unit, 1.0);
disp(canon_val);
from_canon_val = MU_from_canon(hours_unit, 3600.0);
disp(from_canon_val);
disp(MU_unit_string(hours_unit));
disp(MU_canonical_unit_string(hours_unit));
