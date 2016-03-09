## -*- texinfo -*-
## @deftypefn  {Function File} {} WCONWorms ()
## Create a WCONWorms object representing an instance of Worm Movement Data
##

function w = WCONWorms ()
  w.meta = [0];
  w = class (w, "WCONWorms");
endfunction
