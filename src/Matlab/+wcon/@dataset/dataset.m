classdef dataset < handle
    %
    %   Class:
    %   wcon.dataset
    
    %{
    file_root = 'C:\Users\RNEL\Google Drive\OpenWorm\OpenWorm Public\Movement Analysis\example_data\WCON'
    file_name = 'testfile_new.wcon'
    
    file_path = fullfile(file_root,file_name);
    
    f = wcon.loadDataset(file_path);
    
    https://github.com/openworm/tracker-commons/blob/master/WCON_format.md
    %}
    
    properties
        units
        data
        meta = wcon.NULL;
        files
    end
    
    methods (Static)
       obj = fromFile(file_path,varargin)
    end
    
end

