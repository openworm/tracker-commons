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
    Any numeric quantity followed on a per-animal or per-timepoint basis
    must have its units defined in an object.
    
    Standard units:
    ---------------
    - seconds
    - millimeters
    
    Data properties that must have units
    ------------------------------------
    - t
    - x
    - y
    
    
    Standard Properties: t x y
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
        function obj = fromFile(t,parent,options)
            %
            %   obj = wcon.units.fromFile(t)
            %
            %   Input
            %   -----
            %   t: json.objs.token.object
            %   parent: wcon.dataset
            %   options: wcon.load_options
            
            obj = wcon.units;
            obj.props = t.getParsedData();
            obj.props.parent = parent;
        end
    end
    
    methods
      	function s = struct(obj)
            %disp('I ran')
            s = obj.props;
            s = wcon.utils.rmfield(s,'parent');
        end 
    end
end

