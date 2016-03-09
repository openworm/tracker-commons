addpath("../library:../externals/jsonlab");

origworm = WCONWorms();
result = isobject(origworm);
disp(result);
result = isa(origworm,"WCONWorms");
disp(result);
result = isa(origworm,"InvalidClass");
disp(result);

displayData(origworm);
origworm = to_canon(origworm);

% Octave does not seem to like side-effects. If you do not assign the return object,
%   the original object would not see the change.
worm = load_from_file(origworm,"../../../tests/minimal.wcon");
displayData(worm);

save_to_file(worm,"testing.wcon");

worm2 = load(worm,'{"obj":{"string":"value","array":[1,2,3]}}');
displayData(worm);
displayData(worm2);

is_equal(worm,worm2);
is_equal(worm2,origworm);

worm3 = merge(worm,worm2);
