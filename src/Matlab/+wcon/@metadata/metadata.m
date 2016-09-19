classdef metadata < json.dict
    %
    %   Class:
    %   wcon.metadata
    %
    %   https://github.com/openworm/tracker-commons/blob/master/WCON_format.md#metadata-fields-in-detail
    
    %All entries in this object are optional
    
    %{
    custom
    lab
    who
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
    settings
    
    
    %}
    
    methods
        function obj = metadata()
           null = wcon.NULL;
           props = obj.props;
           props.custom = null;
           props.lab = null;
           props.who = null;
           props.timestamp = null;
           props.temperature = null;
           props.humidity = null;
           props.arena = null;
           props.food = null;
           props.media = null;
           props.sex = null;
           props.stage = null;
           props.age = null;
           props.strain = null;
           props.protocol = null;
           props.software = null;
           props.settings = null;
           obj.props = props;
        end
    end
    
    methods (Static)
        function obj = fromFile(m)
            %
            %   Inputs
            %   ------
            %   m : json.token_info.object_token_info
                        
            obj = wcon.metadata;
            attribute_names = m.key_names;
            n_names = length(attribute_names);
            %TODO: Use getTokenString
            for iName = 1:n_names
                name = attribute_names{iName};
                switch name
                    case 'lab'
                        value = wcon.meta.lab.fromFile(m.getToken('lab'));
                    case 'who'
                        value = m.getStringOrCellstr('who');
                    case 'timestamp'
                        temp = m.getTokenString('timestamp');
                        value = temp;
                        
%                         temp = m.getToken('timestamp');
%                         error('Not Yet Implemented'); 
%                         keyboard
                    case 'temperature'
                        temp = m.getToken('temperature');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'humidity'
                     	temp = m.getToken('humidity');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'arena'
                        temp = m.getTokenString('arena');
                        value = temp;
                    case 'food'
                        value = m.getTokenString('food');
                    case 'media'
                      	temp = m.getToken('media');
                        error('Not Yet Implemented'); 
                        keyboard
                    case 'sex'
                        temp = m.getTokenString('sex');
                        value = temp;
%                         temp = m.getToken('sex');
%                         error('Not Yet Implemented'); 
%                         keyboard
                    case 'stage'
                       	value = m.getTokenString('stage');
                    case 'age'
                     	value = m.getNumericToken('age');
                    case 'strain'
                     	value = m.getTokenString('strain');
                    case 'protocol'
                        value = m.getStringOrCellstr('protocol');
                    case 'software'
                        temp = m.getToken('software');
                        value = wcon.meta.software.fromFile(temp);
                    otherwise
                        %TODO: Check and handle custom ...
                        value = m.getParsedToken(name);
                end
                obj.addProp(name,value);

            end
            
        end
    end
    
end

