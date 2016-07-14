function obj = fromFile(file_path,options)
%
%
%   wcon.dataset.fromFile(obj,file_path,options)
%
%   Still To Do:
%   ------------
%   1) Multiple file support
%
%   See Also:
%   ---------
%   wcon.loadDatset

%REQUIRED_BASE_PROP_NAMES = {'units','data'};
STANDARD_BASE_PROP_NAMES = {'units','metadata','data'};

obj = wcon.dataset;

%Parse the JSON data
%-------------------
tokens = json.tokens(file_path);

%Populate the file from the tokens
%---------------------------------
root = tokens.getRootInfo();

%This will allow us to maintain order ...
obj.logFieldNames(STANDARD_BASE_PROP_NAMES);

custom_prop_names = setdiff(root.key_names,STANDARD_BASE_PROP_NAMES);

%units
%metadata
%data

if any(strcmp(root.key_names,'metadata'))
   obj.meta = wcon.metadata.fromFile(root.getToken('metadata')); 
end

obj.units = wcon.units.fromFile(root.getToken('units')); 

%obj.data = wcon.data.fromFile(root.getToken('data'),options);
obj.lazy_fields('data') = @()wcon.data.fromFile(root.getToken('data'),options);
obj.files = {file_path};

%@fields => should be parsed
%non-@fields => general parsing ...?
if ~isempty(custom_prop_names)
   error('Custom code not yet supported') 
end

end