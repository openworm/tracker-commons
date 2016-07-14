classdef jsonable_dict < handle
    %
    %   Class:
    %   wcon.utils.jsonable_dict
    
    properties (Hidden)
        jsonable_fields  = {} %This will keep track of what fields we want
        %to save to disk. This allows having additional properties that
        %are temporary and not saved to disk.
        lazy_fields
        props
        predefined_fields
    end
    
    methods
        function value = get.predefined_fields(obj)
           value = builtin('fieldnames',obj);
        end
        
%         function value = get.jsonable_fields(obj)
%            value = obj.jsonable_fields;
%             if isempty(value)
%                 %TODO: Set value type to double, we're only using the keys
%                 obj.jsonable_fields = containers.Map;
%                 value = obj.jsonable_fields;
%             end 
%         end
        function value = get.props(obj)
            value = obj.props;
            if isempty(value)
                obj.props = containers.Map;
                value = obj.props;
            end
        end
        function value = get.lazy_fields(obj)
            value = obj.lazy_fields;
            if isempty(value)
                obj.lazy_fields = containers.Map;
                value = obj.lazy_fields;
            end
        end
    end
    
    %TODO: Make Hidden after finishing code ...
    methods
        %TODO: Override setting method
        %i.e. when the user does obj.new_variable = new_value
        %we need to make sure to log new_variable as something to save
        function logFieldNames(obj,field_names)
            %
            %   Inputs
            %   ------
            %   field_names : cellstr
            %       Names of properties that should be saved when writing
            %       to JSON
            
            %Doesn't maintain order
%             new_fields = containers.Map(field_names,zeros(1,length(field_names)));
%             obj.jsonable_fields = [obj.jsonable_fields; new_fields];

              obj.jsonable_fields = [obj.jsonable_fields field_names];
        end
        function addFieldsToObject(obj,field_names,values)
            
        end
        function addFieldToObject(obj,field,value)
            
        end
    end
    
    methods (Hidden=true)
        function mask = isfield(obj,field_or_fieldnames)
            if ischar(field_or_fieldnames)
                field_or_fieldnames = {field_or_fieldnames};
                mask = ismember(field_or_fieldnames,obj.props.keys());
            end
        end
        %TODO: Which one is needed for tab support?
        %------------------------------------------
        % Overload property names retrieval
        function names = properties(obj)
            names = fieldnames(obj);
        end
        % Overload fieldnames retrieval
        function names = fieldnames(obj)
            names = sort(obj.props.keys);  % return in sorted order
        end
        % Overload property assignment
        function obj = subsasgn(obj, subStruct, value)
            
            %TODO: Support logging of fieldnames to json
            
            if strcmp(subStruct.type,'.')
                keyboard
                try
                    obj.props(subStruct.subs) = value;
                catch
                    error('Could not assign "%s" property value', subStruct.subs);
                end
            else  % '()' or '{}'
                error('not supported');
            end
        end
        % Overload property retrieval (referencing)
        function value = subsref(obj, subStruct)
            %             persistent class_method_name_map
            %             if isempty(class_method_name_map)
            %                class_method_names = sl.obj.meta.getAllClassMethodNames(obj);
            %                class_method_name_map = containers.Map;
            %                for iName = 1:length(class_method_names)
            %                    cur_name = class_method_names{iName};
            %                    class_method_name_map
            %             end
            s1 = subStruct(1);
            if strcmp(s1.type,'.')
                
                name = s1.subs;
                
                %Evaluate if necessary and remove lazy evaluation directive
                %----------------------------------------------------------
                lazy_fields_local = obj.lazy_fields;
                if lazy_fields_local.isKey(name)
                   prop_lookup_failed = false; 
                   fh = lazy_fields_local(name);
                   lazy_fields_local.remove(name);
                   value = fh(); %evaluate function
                   if any(strcmp(obj.predefined_fields,name))
                       obj.(name) = value;
                   else
                       obj.prop(name) = value;
                   end
                else                
                    try
                        value = obj.props(s1.subs);
                        prop_lookup_failed = false;
                    catch
                        prop_lookup_failed = true;
                        %TODO: Might want to look for s1.subs being a method
                        %see commented out code above
                        value = builtin('subsref', obj, subStruct)
                        %error('"%s" is not defined as a property', s1.subs);
                    end
                end
                %TODO: Can we avoid the check on prop_lookup_failed by
                %doing a return in the catch????
                if ~prop_lookup_failed && length(subStruct) > 1
                    value = subsref(value,subStruct(2:end));
                end
            else  % '()' or '{}'
                %f.data(1).x
                %
                %   data => sl.obj.dict
                %
                %   () .  <= 2 events, () followed by .
                %
                value = builtin('subsref', obj, subStruct(1));
                if length(subStruct) > 1
                    value = subsref(value,subStruct(2:end));
                end
                %builtin('subsref', obj, subStruct)
                %error('not supported');
            end
            
        end
        function disp(obj,amount_to_indent)
            
            %TODO: If multiple objects, only display the predefined key
            %names
            
            if nargin == 1
                amount_to_indent = 0;
            end
            
            if length(obj) > 1
                disp('Code not yet written to support display of multiple objects')
                return
            end
            
            
            %keyboard
            
            %TODO: This was written when inheriting from
            %containers.Map and could be simplified
            local_props = obj.props;
            
            predefined_keys = builtin('fieldnames',obj);
            n_pre = length(predefined_keys);
            predefined_values = cell(1,n_pre);
            
            for iPre = 1:n_pre
               %TODO: Add on lazy loaded check
               cur_key = predefined_keys{iPre};
               predefined_values{iPre} = obj.(cur_key);
            end
            
            keys = [predefined_keys local_props.keys];
            
            %TODO: Add on lazy loaded check for local_props.values
            values = [predefined_values local_props.values];
            
            key_length = cellfun(@length,keys);
            padding_length = max(key_length) - key_length;
            key_displays = ...
                cellfun(@(x,y) [blanks(amount_to_indent) blanks(x) y],...
                num2cell(padding_length),keys,'un',0);
            for iK = 1:length(keys)
                cur_key_display = key_displays{iK};
                
                cur_value = values{iK};
                
                %Ideally this code would go elsewhere
                
                %TODO: Add is logical
                if isnumeric(cur_value) && isscalar(cur_value)
                    fprintf('%s: %d\n',cur_key_display,cur_value);
                elseif ischar(cur_value)
                    fprintf('%s: ''%s''\n',cur_key_display,cur_value);
                else
                    temp_size = sprintf('%dx',size(cur_value));
                    %Need to drop the extra 'x' in temp_size
                    fprintf('%s: [%s %s]\n',cur_key_display,temp_size(1:end-1),class(cur_value));
                end
            end
        end
    end
end

