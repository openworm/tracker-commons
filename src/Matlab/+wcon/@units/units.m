classdef units < sl.obj.dict
    %
    %   Class:
    %   wcon.units
    %
    %   Canonical Units:
    %   ----------------
    %   s
    %   mm
    %   ? temp?
    
    %{
    properties
        t
        x
        y
    end
    %}
    
    %{
    Tracker Commons software will automatically convert units to the 
    standard internal representations (e.g. inches to mm) for all fields 
    specified in the units block. It will look inside anything with a 
    custom tag. It will not look inside the settings block in metadata, 
    nor will it look inside unknown fields.
    
    Jim note: this will not look inside fields inside custom data
    
    %}
    
    methods (Static)
        function obj = fromFile(t)
            %
            %   obj = wcon.units.fromFile(t)
            
            obj = wcon.units;
            props = obj.props;
            
            names = t.key_names;
            for iName = 1:length(names)
                cur_name = names{iName};
                props(cur_name) = t.getTokenString(cur_name);
            end
        end
    end
    
end

