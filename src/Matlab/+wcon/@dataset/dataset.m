classdef dataset < json.lazy_dict
    %
    %   Class:
    %   wcon.dataset
    %
    %   Currently missing functionality:
    %   --------------------------------
    %   1) Creating from memory
    %   2) Loading from multiple files
    %
    %   See Also
    %   wcon.load()
    
    %{
    WCON Properties
    ----------
    units : wcon.units
    
    data : wcon.data 
    Loaded lazily
    
    meta : wcon.metadata
    
    Additional Properties
    ----------------------    
    files : cellstr
    %}
    
    methods (Static)
        %wcon.dataset.fromFile
        obj = fromFile(file_path,varargin)
    end
    
    methods
        function s = getPropertiesStruct(obj)
           s = getPropertiesStruct@wcon.utils.lazy_dict(obj);
           if isfield(s,'files')
              s = rmfield(s,'files');
           end
        end
    end
    
end

