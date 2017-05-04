classdef units < json.objs.dict
    %
    %   Class:
    %   wcon.units
    %
    %   https://github.com/openworm/tracker-commons/blob/master/WCON_format.md#units
    %
    %   Canonical Units:
    %   ----------------
    %   s
    %   mm
    
    %{
    Standard Properties:
        t
        x
        y
    %}
    
    %{
    Specification:
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
            %
            %   Input
            %   -----
            %   t : json.objs.token.object
            
            obj = wcon.units;
            obj.props = t.getParsedData();
            
        end
    end
    
end

