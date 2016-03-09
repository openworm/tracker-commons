addpath("../library:../externals/jsonlab");

worm = WCONWorms();
result = isobject(worm);
disp(result);
result = isa(worm,"WCONWorms");
disp(result);
result = isa(worm,"InvalidClass");
disp(result);

