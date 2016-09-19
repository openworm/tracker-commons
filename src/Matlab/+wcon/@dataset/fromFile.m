function obj = fromFile(file_path,options)
%
%
%   wcon.dataset.fromFile(obj,file_path,options)
%
%   Inputs
%   ------
%   file_path :
%   options
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
[~,~,ext] = fileparts(file_path);
if strcmp(ext,'.zip')
    %1) Get zip data
    %2) 
    temp = wcon.utils.zip_file(file_path);
    if temp.n_files > 1
        error('Multiple files detected, this functionality is not yet supported')
    end
    %TODO: We should build in the string buffering into the zip reading ...
    %With the current zip implementation, we could pass in the file path
    %but eventually I want to avoid unzipping the files to disk
    data_in_zip_file = temp.readFile(1);
    
    %This is temporary, char conversion back and forth kills performance
    temp_char_data = char(data_in_zip_file);
    
    tic;
    tokens = json.stringToTokens(temp_char_data);
    toc;
    keyboard
end

tokens = json.fileToTokens(file_path);

%Populate the file from the tokens
%---------------------------------
root = tokens.getRootInfo();

%This will allow us to maintain order ...
custom_prop_names = setdiff(root.key_names,STANDARD_BASE_PROP_NAMES);

%units
%metadata
%data
if any(strcmp(root.key_names,'metadata'))
   obj.addProp('meta',wcon.metadata.fromFile(root.getToken('metadata'))); 
end

obj.addProp('units',wcon.units.fromFile(root.getToken('units'))); 
obj.addLazyField('data',@()wcon.data.fromFile(root.getToken('data'),options));
obj.addProp('files',{file_path});

%Custom fields
%-------------
%Need to use tokens.getParsedData

%@fields => should be parsed
%non-@fields => general parsing ...?
if ~isempty(custom_prop_names)
   error('Custom code not yet supported') 
end

end