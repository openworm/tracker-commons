classdef metadata < json.objs.dict
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
            
            [props,key_locations] = m.parseExcept({'lab','software'});
            if key_locations(1)
                props.lab = wcon.meta.lab.fromFile(m.getToken('lab'));
            end
            if key_locations(2)
                props.software = wcon.meta.software.fromFile(m.getToken('software'));
            end
            
            obj.props = props;

        end
    end
    
end

