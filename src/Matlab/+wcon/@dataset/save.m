function save(obj,file_path)
%
%   Save is currently implemented by converting all objects to structs
%   and then calling libjson. Eventually this should be changed for better
%   performance ...
%

%Round trip issues:
%------------------
%1) Array of objects or a singular object
%
%   "prop":{} or "prop":[{}] => both yield .prop = object


temp = h__process_object(obj);

wcon.utils.savejson('',temp,'FileName',file_path);

end

function output = h__process_object(objs)

n_objs = length(objs);

%TODO: Add check, not allowing multi-dimensional object array as an input
output = cell(1,n_objs);
for iObj = 1:n_objs
    input_object = objs(iObj);
    
    if isa(input_object,'wcon.utils.lazy_dict') || isa(input_object,'wcon.sl.obj.dict')
        s = input_object.getPropertiesStruct();
    else
        orig_state = warning('off','MATLAB:structOnObject');
        s = struct(input_object);
        warning(orig_state);
    end
    
    output{iObj} = h__process_struct(s);
    
end

if n_objs == 1
    output = output{1};
end

end

function output = h__process_cell(input)


output = cell(size(input));
for iValue = 1:numel(output)
    cur_input_value = input{iValue};
    if isobject(cur_input_value)
        %Should never have nulls in a cell array
        %         if isa(temp,'wcon.NULL')
        %             continue
        %         end
        value = h__process_object(cur_input_value);
    elseif isstruct(cur_input_value)
        value = h__process_struct(cur_input_value);
    elseif iscell(cur_input_value)
        value = h__process_cell(cur_input_value);
    elseif ischar(cur_input_value)
        value = cur_input_value;
    elseif isnumeric(cur_input_value);
        value = cur_input_value;
    else
        error('Unhandled data type')
    end
    
    output{iValue} = value;
end
end

function s_out = h__process_struct(s_in)

s_out = struct;
fn = fieldnames(s_in);
for iField = 1:length(fn)
    cur_field_name = fn{iField};
    temp = s_in.(cur_field_name);
    if isobject(temp)
        if isa(temp,'wcon.NULL')
            continue
        end
        value = h__process_object(temp);
    elseif isstruct(temp)
        value = h__process_struct(temp);
    elseif iscell(temp)
        value = h__process_cell(temp);
    elseif ischar(temp)
        value = temp;
    elseif isnumeric(temp);
        value = temp;
    else
        error('Unhandled data type')
    end
    
    try
        s_out.(cur_field_name) = value;
    catch
        s_out = wcon.sl.struct.setField(s_out,cur_field_name,value);
    end
    %TODO
end

end
