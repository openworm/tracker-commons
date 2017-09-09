function obj = load(file_path,varargin)
%
%   w = wcon.load(file_path,varargin)
%
%   Inputs
%   ------
%   file_path : string
%       Path to the file.
%
%   Optional Inputs
%   ----------------
%   Optional inputs are specified in wcon.load_options
%
%   Examples
%   --------
%   1) Standard Usage
%   w = wcon.load('C:\wcon_files\test.wcon')
%
%   2) Options
%   options = wcon.load_options;


%{

root_path = 'G:\wcon_files\'
ff = @(x) fullfile(root_path,x);
file_path = ff('XJ30_NaCl500mM4uL6h_10m45x10s40s_Ea.wcon');
file_path = ff('wcon_testfile_new.wcon');
w = wcon.load(file_path);

%}

in = wcon.load_options;
in = wcon.sl.in.processVarargin(in,varargin);

obj = wcon.dataset.fromFile(file_path,in);

end