classdef zip_file
    %
    %   Class:
    %   wcon.utils.zip_file
    %There is a lot of room for improvement in this file
    %
    %1) Ideally we switch to c code that handles reading the zip file
    %2) Ideally we can pad the result directly without a copy
    properties
        n_files
    end
    
    properties (Hidden)
        temp_directory
        file_paths
    end
    
    methods
        function obj = zip_file(zip_file_path)
            %
            %   This algorithm is likely to change.
            
            temp_zip_dir_path = tempname();
            
            file_paths = unzip(zip_file_path,temp_zip_dir_path);
            
            obj.temp_directory = temp_zip_dir_path;
            obj.n_files = length(file_paths);
            obj.file_paths = file_paths;
            
        end
        function data = readFile(obj,index)
            %
            %   Our current implementation relies on putting files
            %   onto disk, so we just need a file read. Eventually
            %   we'll change this ...
            
            file_path = obj.file_paths{index};
            
            [fid, msg] = fopen(file_path,'rb');
            
            if fid == (-1)
                disp(msg)
                error('Can''t open specified file');
            end

            try
                % read file
                data = fread(fid,'*uint8')';
            catch exception
                % close file
                fclose(fid);
                throw(exception);
            end

            % close file
            fclose(fid);

        end
    end
    
end

%{

%
%   wcon.utils.readZipData(zip_file_path)
%



z = wcon.utils.zip_file();

temp_zip_dir_path = tempname();

file_names = unzip(zip_file_path,temp_zip_dir_path);

keyboard

z.temp_directory = temp_zip_dir_path;
z.n_files = length(file_names);

output = z;

%N_Files
%function to read binary data of each file





% import com.mathworks.mlwidgets.io.InterruptibleStreamCopier
% a=java.io.ByteArrayInputStream(Z);
% b=java.util.zip.InflaterInputStream(a);
% isc = InterruptibleStreamCopier.getInterruptibleStreamCopier;
% c = java.io.ByteArrayOutputStream;
% isc.copyStream(b,c);
% Q=typecast(c.toByteArray,'uint8');

%}