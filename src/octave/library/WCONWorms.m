classdef WCONWorms
  properties
    wcondata;
  end

  methods
    function obj = WCONWorms() 
      % Pretend initial data for every object
      obj.wcondata=loadjson('{"obj":{"string":"value","array":[1,2,3]}}');
    end

    function obj = displayData(obj)
      disp(obj.wcondata);
    end

    function obj = to_canon(obj)
      disp("Conversion to canonical form - unimplemented.");
    end

    function obj = load_from_file(obj, filename)
%      disp(filename);
      dat = loadjson([filename]);
      obj.wcondata = dat;
%      disp(obj.wcondata);
    end

    function obj = save_to_file(obj, filename)
%      disp(filename);
      disp(["Output to [" filename "] - unimplemented."]);
    end

    function obj = load(obj, json_string)
      dat = loadjson(json_string);
      obj.wcondata = dat;
    end

    function ret = is_equal(obj, obj2)
      if (isequal(obj.wcondata,obj2.wcondata))
	ret = 1
      else
        ret = 0
      endif
    end

    function obj = merge(obj, obj2)
      disp("Merging two objects - unimplemented.");
    end
  end
end
