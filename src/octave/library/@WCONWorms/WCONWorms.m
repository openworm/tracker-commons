function w = WCONWorms ()
  w.meta = [0];
  w = class (w, "WCONWorms");
  dat=loadjson('{"obj":{"string":"value","array":[1,2,3]}}')
endfunction
