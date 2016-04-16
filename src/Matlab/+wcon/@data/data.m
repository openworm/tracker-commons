classdef data < sl.obj.dict
    %
    %   Class:
    %   wcon.loaded_data
    %
    %   This may change ...
    %
    
    
    
    
    
    %{
    Each data element may contain
    id : string or number, required
    t : numeric or array?
    x : array 
    y : array
    ox :
    oy :
    cx :
    cy :
    head :
    ventral : 
    
    
    %}
    
    %   id
    %   
    
% % %     properties
% % %         id %required if data is present
% % %         x  
% % %         y
% % %         t
% % %     end
    
    methods (Static)
        function objs = fromFile(t,options)
            %
            %
            %   Inputs
            %   ------
            %   t
            %   options : struct
            %       See wcon.loadDataset
            %
            if strcmp(t.type,'array')
                n_objs = t.n_elements;
                data_json_objs = t.getObjectArray;
            else
                n_objs = 1;
                data_json_objs = t;
            end
            
            objs(n_objs) = wcon.data();
            for iObj = 1:n_objs
                cur_obj = objs(iObj);
                props = cur_obj.props;
                cur_json = data_json_objs(iObj);
                
                names = cur_json.attribute_names;
                for iName = 1:length(names)
                   cur_name = names{iName};
                   switch cur_name
                       case 'id'
                           props('id') = cur_json.getToken('id');
                       case 't'
                           props('t') = h__getNumericArray(cur_json,'t',options);
                       case 'x'
                           props('x') = h__getNumericArray(cur_json,'x',options);
                       case 'y'
                           props('y') = h__getNumericArray(cur_json,'y',options);
                       otherwise
                           %What do we want to do here ????
                           keyboard
                   end
                end
            end 
        end
    end
    
end

function output = h__getNumericArray(cur_json,field_name,options)
%
%   Inputs:
%   -------
%   options:
%       See wcon.loadDataset

    temp = cur_json.getArrayToken('t');
    switch temp.array_depth
        case 1
            output = temp.get1dNumericArray();
        case 2
            if in.merge_data
                output = temp.get2dNumericArray();
            else
                output = temp.getArrayOf1dNumericArrays();
            end
        otherwise
            error('Unhandled case')
    end
end

