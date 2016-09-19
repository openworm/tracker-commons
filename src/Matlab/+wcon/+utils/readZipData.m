function output = readZipData(zip_file_path)
%
%   wcon.utils.readZipData(zip_file_path)
%
%There is a lot of room for improvement in this file
%
%1) Ideally we switch to c code that handles reading the zip file
%2) Ideally we can pad the result directly without a copy


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