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
%   merge_data




in = struct();
in.merge_data = false;
%TODO: move to local sl
in = wcon.sl.in.processVarargin(in,varargin);

obj = wcon.dataset.fromFile(file_path,in);


end