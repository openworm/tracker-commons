function obj = load(file_path,varargin)
%
%   obj = wcon.load(file_path,varargin)
%
%   Inputs
%   ------
%   file_path : string
%       Path to the file.
%

%{
    %Testing code
    %-----------------------------------------
    FILE_USE = 2;
    file_root = 'C:\Users\RNEL\Google Drive\OpenWorm\OpenWorm Public\Movement Analysis\example_data\WCON'
    options = struct;
    if FILE_USE == 1
        file_name = 'testfile_new.wcon'

        %This is incorrect
        %options.merge_data = true;
    else
        file_name = 'XJ30_NaCl500mM4uL6h_10m45x10s40s_Ea.wcon'
    end
    
    file_path = fullfile(file_root,file_name);



    
    tic
    profile on
for i = 1:10
    f = wcon.load(file_path,options);
end
    profile off
    toc
%}



in = struct();
in.merge_data = false;
%TODO: move to local sl
in = sl.in.processVarargin(in,varargin);

obj = wcon.dataset.fromFile(file_path,in);


end