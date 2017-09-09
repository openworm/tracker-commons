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

obj = wcon.dataset;

%Parse the JSON data
%-------------------
%This needs to be changed, see:
%https://github.com/openworm/tracker-commons/issues/128

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
    
    %tic;
    root = json.tokens.parse(temp_char_data);
    %toc;
else
    root = json.tokens.load(file_path);
end
%--------------------------------------------------------------------------

props = root.parseExcept({'units','data','metadata'});

if ~isfield(props,'units')
    error('WCON file must contain units')
end
units_token = root.getToken('units');
props.units = wcon.units.fromFile(units_token,obj,options);

if ~isfield(props,'data') 
    error('WCON file must contain data')
end

%TODO: We could make this lazy again ...
%This needs to go at the end ...
%obj.addLazyField('data',@()wcon.data.fromFile(root.getToken('data'),options));
data_token = root.getToken('data');
props.data = wcon.data.fromFile(data_token,obj,options);

%meta-data is optional
if isfield(props,'metadata')
    props.metadata = wcon.metadata.fromFile(root.getToken('metadata'));
end

props.files = {file_path};
obj.props = props;


end