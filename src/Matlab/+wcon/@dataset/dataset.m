classdef dataset < handle
    %
    %   Class:
    %   wcon.dataset
    %
    %   
    
    properties
        units
        data %wcon.data
        meta = wcon.NULL;
        files
    end
    
    methods (Static)
       obj = fromFile(file_path,varargin)
    end
    
end

