classdef dataset < wcon.utils.jsonable_dict
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
    
    properties
        units
        data %wcon.data
        meta = wcon.NULL;
        files = {}
    end
    
    methods (Static)
        %wcon.dataset.fromFile
        obj = fromFile(file_path,varargin)
    end
    
end

