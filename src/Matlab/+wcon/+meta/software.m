classdef software < json.objs.dict
    %
    %   Class:
    %   wcon.meta.software
    
    properties
        name = wcon.NULL
        version = wcon.NULL
        featureID = wcon.NULL
        commit_hash = wcon.NULL
    end
    
    methods
        function obj = software()
            n = wcon.NULL;
            s.version = n;
            s.name = n;
            s.featureID = n;
            s.commit_hash = n;
            obj.props = s;
        end
    end
    
    methods (Static)
        function objs = fromFile(m)
            %
            %   obj = wcon.meta.software.fromFile(m)
            
            temp = m.getParsedData;
            n_objs = length(temp);
            objs(n_objs) = wcon.meta.software;
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
                endd
            end
        end
    end
    
end

