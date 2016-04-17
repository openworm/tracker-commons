classdef metadata < handle
    %
    %   Class:
    %   wcon.metadata
    %
    %   https://github.com/openworm/tracker-commons/blob/master/WCON_format.md#metadata-fields-in-detail
    
    %All entries in this object are optional
    
    properties
        custom = wcon.NULL;
        lab = wcon.NULL;
        who = wcon.NULL;
        timestamp = wcon.NULL;
        temperature = wcon.NULL;
        humidity = wcon.NULL;
        arena = wcon.NULL;
        food = wcon.NULL;
        media = wcon.NULL;
        sex = wcon.NULL;
        stage = wcon.NULL;
        age = wcon.NULL;
        strain = wcon.NULL;
        protocol = wcon.NULL;
        software = wcon.NULL;
        settings %arbitrary JSON
    end
    
    methods
    end
    
    methods (Static)
        function obj = fromFile(m)
            obj = wcon.metadata;
            custom = struct;
            attribute_names = m.attribute_names;
            n_names = length(attribute_names);
            %TODO: Use getTokenString
            for iName = 1:n_names
                switch attribute_names{iName}
                    case 'lab'
                        obj.lab = wcon.meta.lab.fromFile(m.getToken('lab'));
                    case 'who'
                        temp = m.getToken('who');
                        if ischar(temp)
                            obj.who = temp;
                        else
                            obj.who = temp.getCellstr();
                        end
                    case 'timestamp'
                        temp = m.getToken('timestamp');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'temperature'
                        temp = m.getToken('temperature');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'humidity'
                     	temp = m.getToken('humidity');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'arena'
                        temp = m.getToken('arena');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'food'
                        obj.food = m.getTokenString('food');
                    case 'media'
                      	temp = m.getToken('media');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'sex'
                        temp = m.getToken('sex');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'stage'
                       	temp = m.getTokenString('stage');
                    case 'age'
                     	obj.age = m.getNumericToken('age');
                    case 'strain'
                     	obj.strain = m.getTokenString('strain');
                    case 'protocol'
                    	temp = m.getToken('protocol');
                        if ischar(temp)
                            obj.protocol = temp;
                        else
                            obj.protocol = temp.getCellstr();
                        end
                    case 'software'
                        temp = m.getToken('software');
                        obj.software = wcon.meta.software.fromFile(temp);
                    otherwise
                        %TODO: Check and handle custom ...
                        cur_name = attribute_names{iName};
                        custom.(cur_name(2:end)) = m.getParsedToken(cur_name);
                end
                if ~isempty(fieldnames(custom))
                   obj.custom = custom; 
                end
            end
            
        end
    end
    
end

