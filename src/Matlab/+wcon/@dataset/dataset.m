classdef dataset < json.objs.lazy_dict
    %
    %   Class:
    %   wcon.dataset
    %
    %   Currently missing functionality:
    %   --------------------------------
    %   1) Creating from memory
    %   2) Loading from multiple files
    %
    %   See Also
    %   -----------
    %   wcon.load()
    %
    %   WCON Properties
    %   ---------------
    %   units : wcon.units
    %   data : wcon.data 
    %   meta : wcon.metadata
    
    
    %{
    WCON Properties
    ----------

    
    Additional Properties
    ----------------------    
    files : cellstr
    %}
    
    methods (Static)
        %wcon.dataset.fromFile
        obj = fromFile(file_path,varargin)
    end
    
    methods
        function s = struct(obj)
            %
            %   This method is called when saving. We exclude the files
            %   property
            s = obj.props;
            s = wcon.utils.rmfield(s,{'files'});
        end
    end
    
end

