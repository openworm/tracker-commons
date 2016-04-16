function obj = loadDataset(file_path,varargin)
%
%   obj = wcon.loadDataset(file_path,varargin)

in.merge_data = false;
in = sl.in.processVarargin(in,varargin);

obj = wcon.dataset.fromFile(file_path,in);


end