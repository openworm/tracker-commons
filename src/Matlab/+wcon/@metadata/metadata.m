classdef metadata < dynamicprops
    %
    %   Class:
    %   wcon.metadata
    %
    %   https://github.com/openworm/tracker-commons/blob/master/WCON_format.md#metadata-fields-in-detail
    
    %All entries in this object are optional
    
    properties
        lab = wcon.NULL;
        who = wcon.NULL;
        timestamp
        temperature
        humidity
        arena
        food
        media
        sex
        stage
        age
        strain
        protocol
        software
    end
    
    methods
    end
    
    methods (Static)
        function obj = fromFile(m)
            obj = wcon.metadata;
            attribute_names = m.attribute_names;
            n_names = length(attribute_names);
            %TODO: Use getTokenString
            for iName = 1:n_names
                switch attribute_names{iName}
                    case 'lab'
                        obj.lab = wcon.meta.lab.fromFile(m.getToken('lab'));
                    case 'who'
                        temp = m.getToken('who');
                        keyboard
                    case 'timestamp'
                        temp = m.getToken('timestamp');
                        keyboard
                    case 'temperature'
                        temp = m.getToken('temperature');
                        keyboard
                    case 'humidity'
                     	temp = m.getToken('humidity');
                        keyboard
                    case 'arena'
                        temp = m.getToken('arena');
                        keyboard
                    case 'food'
                    	temp = m.getToken('food');
                        keyboard
                    case 'media'
                      	temp = m.getToken('media');
                        keyboard
                    case 'sex'
                        temp = m.getToken('sex');
                        keyboard
                    case 'stage'
                       	temp = m.getToken('stage');
                        keyboard
                    case 'age'
                     	temp = m.getToken('age');
                        keyboard
                    case 'strain'
                     	temp = m.getToken('strain');
                        keyboard
                    case 'protocol'
                    	temp = m.getToken('protocol');
                        keyboard
                    case 'software'
                        temp = m.getToken('software');
                        keyboard
                    otherwise
                        %TODO: Check and handle custom ...
                        cur_name = attribute_names{iName};
                        keyboard
                        
                end
            end
        end
    end
    
end

