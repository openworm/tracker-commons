classdef lazy_dict < handle
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
        props
        lazy_fields
    end
    
    methods
        %These are internal functions, normally subsasgn will work
        function addProp(obj,name,value)
           %TODO: Support invalid names
           obj.props.(name) = value; 
        end
        function addLazyField(obj,name,value)
            lazy_fields_local = obj.lazy_fields;
            if isempty(lazy_fields_local)
                lazy_fields_local = struct;
            end
            lazy_fields_local.(name) = value;
            obj.lazy_fields = lazy_fields_local;
            obj.props.(name) = 'Not yet evaluated (Lazy Property)';
        end
    end
    
    methods (Hidden=true)
        function mask = isfield(obj,field_or_fieldnames)
            if ischar(field_or_fieldnames)
                field_or_fieldnames = {field_or_fieldnames};
                %TODO: Need to look if props is empty ...
                mask = ismember(field_or_fieldnames,obj.fieldnames);
            end
        end
        % Overload property names retrieval
        function names = properties(obj)
            names = fieldnames(obj);
        end
        % Overload fieldnames retrieval
        function names = fieldnames(obj)
            names = sort(fieldnames(obj.props));  % return in sorted order
        end
        % Overload property assignment
        function obj = subsasgn(obj, subStruct, value)
            if strcmp(subStruct.type,'.')
                name = subStruct.subs;
                
                %NOTE: As designed we don't really support having
                %properties in the class itself, we could try and change it
                %so that we do, although I'm not really sure what the value
                %would be of placing properties in the class itself ...
                
                
                try
                    %Did this change, I'm getting subs as a {'string'}
                    %instead of 'string'
                    %2016a - string
                    %other versions?
                    %Does it depend on the form of the call?
                    obj.props.(name) = value;
                catch
                    try
                        obj.props = sl.struct.setField(obj.props,name,value);
                    catch ME
                        error('Could not assign "%s" property value', subStruct.subs);
                    end
                end
                
            else  % '()' or '{}'
                error('not supported');
            end
        end
        % Overload property retrieval (referencing)
        function value = subsref(obj, subStruct)
            s1 = subStruct(1);
            if strcmp(s1.type,'.')
                name = s1.subs;
                lazy_fields_local = obj.lazy_fields;
                if isfield(lazy_fields_local,name)
                    
                    fh = lazy_fields_local.(name);
                    
                    value = fh(); %evaluate function
                    
                    obj.lazy_fields = rmfield(lazy_fields_local,name);
                    
                    try
                        obj.props.(name) = value;
                    catch
                        obj.props = wcon.sl.struct.setField(obj.props,name,value);
                    end
                    
                    %                     if any(strcmp(obj.predefined_fields,name))
                    %                         obj.(name) = value;
                    %                     else
                    %                         obj.prop(name) = value;
                    %                     end
                else
                    try
                        value = obj.props.(name);
                    catch
                        %TODO: Might want to look for s1.subs being a method
                        %see commented out code above
                        builtin('subsref', obj, subStruct)
                        return
                    end
                end
                %TODO: Can we avoid the check on prop_lookup_failed by
                %doing a return in the catch????
            else  % '()' or '{}'
                %f.data(1).x
                %
                %   data => sl.obj.dict
                %
                %   () .  <= 2 events, () followed by .
                %
                value = builtin('subsref', obj, subStruct(1));
            end
            
            if length(subStruct) > 1
                value = subsref(value,subStruct(2:end));
            end
            
        end
        function disp(objs)
            if length(objs) > 1
                fprintf('%s of size %dx%d\n',class(objs),size(objs,1),size(objs,2));
            else
                %TODO: we need to check for properties that have been
                %defined in the inherited class ...
                if isempty(objs.props) || isempty(fieldnames(objs.props))
                    fprintf('%s with no properties\n',class(objs));
                else
                    fprintf('%s with properties:\n\n',class(objs));
                    disp(objs.props)
                end
            end
        end
        function value = getAsStructure(obj)
            value = obj.props;
            %TODO: Compute any lazy fields
        end
    end
    
end


