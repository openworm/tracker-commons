classdef lab < handle
    %
    %   Class:
    %   wcon.meta.lab
    %
    
    %location
    %name
    %PI
    
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
    
    methods (Static)
        function objs = fromFile(m)
            %TODO: Check if this is an array ...
            
            if strcmp(m.type,'object')
                n_objs = 1;
                all_m = m;
            else
                n_objs = m.n_elements;
                all_m = m.getObjectArray;
            end
            
            objs(n_objs) = wcon.meta.lab;
            
            for iObj = 1:n_objs
                obj = objs(iObj);
                cur_m = all_m(iObj);
                attribute_names = cur_m.key_names;
                n_names = length(attribute_names);
                for iName = 1:n_names
                    switch attribute_names{iName}
                        case 'location'
                            obj.location = cur_m.getTokenString('location');
                        case 'name'
                            obj.name = cur_m.getTokenString('name');
                        case 'PI'
                            obj.PI = cur_m.getTokenString('PI');
                        case 'address'
                            obj.address = cur_m.getTokenString('address');
                        otherwise
                            error('Unsupported attribute for lab: ''%s''',attribute_names{iName})
                    end
                end
            end
        end
    end
    
end

