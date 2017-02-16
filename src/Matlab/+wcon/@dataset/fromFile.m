function obj = fromFile(file_path,options)
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
%   wcon.load

%REQUIRED_BASE_PROP_NAMES = {'units','data'};
%STANDARD_BASE_PROP_NAMES = {'units','metadata','data'};

obj = wcon.dataset;

%Parse the JSON data
%-------------------
%This needs to be changed ...
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
    root = json.tokens.parse(temp_char_data);
    toc;
else
    root = json.tokens.load(file_path);
end
%--------------------------------------------------------------------------

props = root.parseExcept({'units','data','metadata'});

if ~isfield(props,'units')
    error('WCON file must contain units')
end
props.units = wcon.units.fromFile(root.getToken('units'));

if ~isfield(props,'data') 
    error('WCON file must contain data')
end
%TODO: We could make this lazy again ...
%This needs to go at the end ...
%obj.addLazyField('data',@()wcon.data.fromFile(root.getToken('data'),options));
props.data = wcon.data.fromFile(root.getToken('data'));

if isfield(props,'metadata')
    props.metadata = wcon.metadata.fromFile(root.getToken('metadata'));
end

props.files = {file_path};
obj.props = props;


end