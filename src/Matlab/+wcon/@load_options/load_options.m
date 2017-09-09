classdef load_options
    %
    %   Class:
    %   wcon.load_options
    %
    %   See Also:
    %   wcon.load
    %
    %
    %   TODO: It would be nice to have a nice display of the options
    %   with links to their documentation. This could be made a generic
    %   function.
    
    properties
        merge_data = false %merge_data : (default false)
        %   If true, data.x and data.y are matrices when all frames
        %       have the same number of samples.
        %   If false, all frames will be returned as cells.
        %
        %   This can save time on loading, but may causes differences
        %   in data between worms that have consistent frames and worms 
        %   that do not.
    end
    
    methods
    end
    
end

