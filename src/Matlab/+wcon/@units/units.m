classdef units < sl.obj.dict
    %
    %   Class:
    %   wcon.units
    
    %{
    properties
        t
        x
        y
    end
    %}
    
    methods (Static)
        function obj = fromFile(t)
            %
            %   obj = wcon.units.fromFile(t)
            
            obj = wcon.units;
            props = obj.props;
            
            names = t.attribute_names;
            for iName = 1:length(names)
                cur_name = names{iName};
                props(cur_name) = t.getTokenString(cur_name);
            end
        end
    end
    
end

