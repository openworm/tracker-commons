classdef units < json.dict
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
            
            
            setField = @wcon.sl.struct.setField;
            
            names = t.key_names;
            for iName = 1:length(names)
                cur_name = names{iName};
                value = t.getTokenString(cur_name);
                try
                    props.(cur_name) = value;
                catch
                    props = setField(props,cur_name,value);  
                end
            end
            obj.props = props;
        end
    end
    
end

