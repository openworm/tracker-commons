function test_shared_files

%{
    %Testing code
    %-----------------------------------------
    FILE_USE = 1;
    file_root = 'C:\Users\RNEL\Google Drive\OpenWorm\OpenWorm Public\Movement Analysis\example_data\WCON'
    options = struct;
    if FILE_USE == 1
        file_name = 'testfile_new.wcon'

        %This is incorrect
        %options.merge_data = true;
    else
        file_name = 'XJ30_NaCl500mM4uL6h_10m45x10s40s_Ea.wcon'
    end
    
    file_path = fullfile(file_root,file_name);

    tic
for i = 1:10    
f = wcon.load(file_path,options);

end
    toc/10
    
    tic
    profile on
for i = 1:10
    f = wcon.load(file_path,options);
end
    profile off
    toc
%}
