function obj = fromFile(file_path,options)
%
%
%   wcon.dataset.fromFile(obj,file_path,options)
%
%   Still To Do:
%   ------------
%   1) Multiple file support


obj = wcon.dataset;




%{
    file_root = 'C:\Users\RNEL\Google Drive\OpenWorm\OpenWorm Public\Movement Analysis\example_data\WCON'
    file_name = 'testfile_new.wcon'
    file_name = 'XJ30_NaCl500mM4uL6h_10m45x10s40s_Ea.wcon'
    
    file_path = fullfile(file_root,file_name);
    
    f = wcon.loadDataset(file_path);
%}

REQUIRED_BASE_PROP_NAMES = {'units','data'};
STANDARD_BASE_PROP_NAMES = {'units','metadata','data'};

tokens = json.tokens(file_path);

root = tokens.getRootInfo();

custom_prop_names = setdiff(root.attribute_names,STANDARD_BASE_PROP_NAMES);

%units
%metadata
%data

if any(strcmp(root.attribute_names,'metadata'))
   obj.meta = wcon.metadata.fromFile(root.getToken('metadata')); 
end

obj.units = wcon.units.fromFile(root.getToken('units')); 

obj.data = wcon.data.fromFile(root.getToken('data'),options);

obj.files = {file_path};

%@fields => should be parsed
%non-@fields => general parsing ...?
if ~isempty(custom_prop_names)
   error('Custom code not yet supported') 
end

end