classdef dict < handle
    %
    %   Class:
    %   sl.obj.dict
    %
    %   This class supports arbitrary property (attribute) names.
    %
    %   All attributes can be accessed via parentheses:
    %       obj.(<property>) e.g. obj.('my awesome property!)
    %
    %   Valid variable names can be accessed via just the dot operator:
    %       obj.<valid_property>  e.g. obj.valid_property
    %
    %   Issues:
    %   -------
    %   1) Providing methods for this class makes property attribute
    %   and method lookup ambiguous. 
    %   2) Tab complete does not work when accessing via parentheses,
    %       e.g.: 
    %           obj.('my_va   <= tab complete wouldn't work
    %           obj.my_va   <= tab complete would work
    %
    %
    %   http://undocumentedmatlab.com/blog/class-object-tab-completion-and-improper-field-names
    
    properties
        props %containers.Map
    end
    
    methods
        function value = get.props(obj)
           value = obj.props; 
           if isempty(value)
              obj.props = containers.Map;
              value = obj.props;
           end
        end
    end
    
    methods
% % % %         function obj = dict()
% % % %             %myMap = containers.Map(KEYS, VALUES)
% % % %             
% % % %             %TODO: This fails for large value initialization
% % % %             %obj.props = containers.Map;
% % % %         end

    end
    
    methods (Hidden=true)
        function mask = isfield(obj,field_or_fieldnames)
           if ischar(field_or_fieldnames)
               field_or_fieldnames = {field_or_fieldnames};
               mask = ismember(field_or_fieldnames,obj.props.keys());
           end
        end
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
            if strcmp(subStruct.type,'.')
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
                try
                    value = obj.props(s1.subs);
                    prop_lookup_failed = false;
                catch
                    prop_lookup_failed = true;
                    %TODO: Might want to look for s1.subs being a method
                    %see commented out code above
                    builtin('subsref', obj, subStruct)
                    %error('"%s" is not defined as a property', s1.subs);
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
            
            if nargin == 1
                amount_to_indent = 0;
            end
            
            %TODO: This was written when inheriting from
            %containers.Map and could be simplified 
            local_props = obj.props;
            keys = local_props.keys;
            values = local_props.values;
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

