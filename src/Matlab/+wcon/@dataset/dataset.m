classdef dataset < wcon.utils.lazy_dict
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
    Properties
    ----------
    units : wcon.units
    
    data : wcon.data 
    Loaded lazily
    
    meta : wcon.metadata
    
    files : 
    
    
    %}
    
    
%     properties
%         units
%         data %wcon.data
%         meta = wcon.NULL;
%         files = {}
%     end
    
    methods (Static)
        %wcon.dataset.fromFile
        obj = fromFile(file_path,varargin)
    end
    
end

