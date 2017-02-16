function obj = load(file_path,varargin)
%
%   obj = wcon.load(file_path,varargin)
%
%   Inputs
%   ------
%   file_path : string
%       Path to the file.
%
%   Optional Inputs
%   ----------------
%   merge_data : (default false)
%       If true, data.x and data.y are matrices when all frames
%       have the same number of samples.

%{

root_path = 'G:\wcon_files\'
ff = @(x) fullfile(root_path,x);
file_path = ff('XJ30_NaCl500mM4uL6h_10m45x10s40s_Ea.wcon');
file_path = ff('wcon_testfile_new.wcon');
obj = wcon.load(file_path);

%}

in.merge_data = false;
in = wcon.sl.in.processVarargin(in,varargin);

obj = wcon.dataset.fromFile(file_path,in);

end