classdef tracker_commons
    %
    %   Class:
    %   wcon_tests.tracker_commons
    
    %{
        tester = wcon_tests.tracker_commons;
        tester.run
    %}
    
    properties
        root_path
    end
    
    methods
        function obj = tracker_commons()
            %
            %   obj = wcon_tests.tracker_commons
            %
            
            temp = which('wcon.load');
            tc_root = fileparts(fileparts(fileparts(fileparts(temp))));
            obj.root_path = fullfile(tc_root,'tests');
        end
        function run(obj)
            base = wcon_tests.tracker_commons.base(obj);
            base.run();
        end
    end
    
end

