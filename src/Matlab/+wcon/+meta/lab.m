classdef lab < json.objs.dict
    %
    %   Class:
    %   wcon.meta.lab
    %
    
    %{
    properties
        location = wcon.NULL
        %to specify physical location or address
        
        name = wcon.NULL
        %to indicate the name of the laboratory as necessary
        
        PI = wcon.NULL
        %to indicate the principal investigator of that lab
        
        address = wcon.NULL
        %temp
    end
    %}
    
    methods
        function obj = lab()
           n = wcon.NULL;
           s.location = n;
           s.name = n;
           s.PI = n;
           s.address = n;
           obj.props = s;
        end
    end
    
    methods (Static)
        function objs = fromFile(m)
            %
            %   Inputs
            %   ------
            %   m : json
            %
            
            temp = m.getParsedData;
            n_objs = length(temp);
        	objs(n_objs) = wcon.meta.lab;
            if iscell(temp)
                for iObj = 1:n_objs
                   cur_obj = objs(iObj);
                   cur_obj.props = temp{iObj};
                end
            elseif length(temp) > 1
               	for iObj = 1:n_objs
                   cur_obj = objs(iObj);
                   cur_obj.props = temp(iObj);
                end
            else
                objs.props = temp;
            end
        end
    end
    
end

