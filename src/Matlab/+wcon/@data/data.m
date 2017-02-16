classdef data < json.objs.dict
    %
    %   Class:
    %   wcon.loaded_data
    %
    %   Improvements:
    %   -------------
    %   1) Fix print in sl.obj.dict
    %   2) Link to parent object (and thus to units)
    %   3) Work on converting to data from saved mat file
    %   4) create extraction function for origin and centroid fixed worm
    %   5) get example of 2d matrix data and finish support for it
    
    
    
    
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
    
    methods
        function plotWorm(obj,varargin)
            
           in.step_size = 1;
           in = sl.in.processVarargin(in,varargin);
           
           %TODO: We could build in id support for object selection
           if length(obj) > 1
               error('Method only supports a singular object')
           end
           props = obj.props;
           x = props('x');
           y = props('y');
           
           %TODO: Need to get standard representation ...
           %May be:
           %1d
           %2d - not yet handled ...
           %{1d}
           
           if isfield(obj,'ox')
              error('Ox not yet implemented') 
           end
           
           if ~iscell(x)
               x = {x};
               y = {y};
           end
           
           %ksadkasdfkajsdkfjasdfkajsdf
           %This is awful, perhaps we need to write extraction routines ...
           %get x and y data with origin built in and with the option
           %of converting to a particular unit
           
           n_frames = length(x);
           
           if isfield(obj,'cx')
              cx = props('cx');
              cy = props('cy');
           else
              cx = zeros(1,n_frames);
              cy = zeros(1,n_frames);
           end
           
%            plot(cellfun(@(x,y) max(x + y),x,num2cell(cx)))
%            hold all
%            plot(cellfun(@(x,y) max(x + y),y,num2cell(cy)))
%            hold off
  
%            keyboard
%            
           temp_min_x = min(cellfun(@(x,y) min(x + y),x,num2cell(cx)));
           temp_max_x = max(cellfun(@(x,y) max(x + y),x,num2cell(cx)));
           temp_min_y = min(cellfun(@(x,y) min(x + y),y,num2cell(cy)));
           temp_max_y = max(cellfun(@(x,y) max(x + y),y,num2cell(cy)));
           
           figure
           set(gca,'xlim',[temp_min_x temp_max_x],'ylim',[temp_min_y temp_max_y],'xlimmode','manual','ylimmode','manual')
           h = line(1,1);
           
           for iFrame = 1:in.step_size:length(x)
              cur_x = x{iFrame} + cx(iFrame);
              cur_y = y{iFrame} + cy(iFrame);
              try
                 set(h,'xdata',cur_x,'ydata',cur_y);
              catch ME
                 %TODO: look for invalid or deleted object error
                 %i.e. that the user closed the function
              end
%               keyboard
%               cla
%               plot(gca,cur_x,cur_y)
              pause(0.001)
           end
           
        end
    end

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
            
            %
            temp = t.getParsedData();
            
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
                
                names = cur_json.key_names;
                                
                for iName = 1:length(names)
                   cur_name = names{iName};
                   switch cur_name
                       case 'id'
                           props.('id') = cur_json.getToken('id');
                       case 't'
                           props.('t') = h__getNumericArray(cur_json,'t',options);
                       case 'x'
                           props.('x') = h__getNumericArray(cur_json,'x',options);
                       case 'y'
                           props.('y') = h__getNumericArray(cur_json,'y',options);
                       otherwise
                           %TODO: We need to get a static method to the
                           %generic parser ...

                           value = cur_json.getParsedData('index',iName);
                           
                           %Ideally we would be able to use the addProp
                           %method, but I'm holding onto the props value
                           %here so I would need:
                           %
                           %    cur_obj.props = props;
                           %    cur_obj.addProp()
                           %    props = cur_obj.props
                           %
                           %    :/ This is not great
                           %
                           %    TODO: Will we support arbitrary field names
                           %    ...
                           
                           
                           props = json.setField(props,cur_name,value);
% % % % %                            addProp(obj,name,value)
% % % % %                            %What do we want to do here ????
% % % % %                            %We might need to change this ....
% % % % %                            props.(cur_name) = h__getNumericArray(cur_json,cur_name,options);
                   end
                   cur_obj.props = props;
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
%
%   See Also:
%   ---------
%   json.token_info.array_token_info

    temp = cur_json.getArrayToken(field_name);
    switch temp.array_depth
        case 1
            output = temp.get1dNumericArray();
%             if ~options.merge_data
%                output = {output}; 
%             end
        case 2
            if options.merge_data
                output = temp.get2dNumericArray();
            else
                output = temp.getArrayOf1dNumericArrays();
            end
        otherwise
            error('Unhandled case')
    end
end

