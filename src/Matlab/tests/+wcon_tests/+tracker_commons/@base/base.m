classdef base
    %
    %   Class
    %   wcon_tests.tracker_commons.base
    %
    %   See Also
    %   --------
    %   wcon_tests.tracker_commons
    
    properties
        root_path
    end
    
    methods
        function obj = base(parent)
            obj.root_path = parent.root_path;
        end
        function run(obj)
           
            obj.intermediate_wcon();
            obj.intermediate_wcon_zip();
            
        end
        function intermediate_wcon(obj)
            %
            
            output = h_loadFile(obj,'intermediate.wcon');
        end
        function intermediate_wcon_zip(obj)
            output = h_loadFile(obj,'intermediate.wcon.zip');
            keyboard
        end
    end
    
end

function output = h_loadFile(obj,file_name)

%TODO: Verify filename exists
file_path = fullfile(obj.root_path,file_name);
output = wcon.load(file_path);
end