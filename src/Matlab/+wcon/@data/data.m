classdef data < json.objs.dict
    %
    %   Class:
    %   wcon.data
    %
    %   Parent:
    %   wcon.dataset
    %
    %   https://github.com/openworm/tracker-commons/blob/master/WCON_format.md#data   
    %
    %
    %   Properties
    %   ----------
    %   id : string
    %   t
    %   x
    %   y
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
        function verify(obj)
            error('Not yet implemented')
            
            %1) all data has id, t, x, & y
            %2) units are specified for t, x, & y
            %3) time is ordered
            %4) x and y have matching sizes at each time point
            %5) length of t matches length of the array
        end
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
        function objs = fromFile(t,parent,options)
            %
            %
            %   Inputs
            %   ------
            %   t : json.objs.token.array OR? json.objs.token.object
            %   options : wcon.load_options
            %       See wcon.load
            
            COLUMN_MAJOR = true;
            
            if options.merge_data
                max_numeric_collapse_depth = 2;
            else
                max_numeric_collapse_depth = 1;
            end
            
            temp = t.getParsedData(...
                'collapse_objects',false,... %
                'column_major',true,...
                'max_numeric_collapse_depth',max_numeric_collapse_depth);
            
            n_objs = length(temp);
            
            objs(n_objs) = wcon.data();
            for iObj = 1:n_objs
                cur_obj = objs(iObj);
                cur_obj.props = temp{iObj};
                cur_obj.props.parent = parent;
                %TODO: Units conversion ...
            end 
        end
    end
    methods
        function s = struct(obj)
            if length(obj) > 1
                error('Not yet implemeneted')
            end
            s = obj.props;
            s = wcon.utils.rmfield(s,'parent');
        end
    end
    
end
