classdef software < handle
    %
    %   Class:
    %   wcon.meta.software
    
    properties
        name = wcon.NULL
        version = wcon.NULL
        featureID = wcon.NULL
        commit_hash = wcon.NULL
    end
    
    methods (Static)
        function objs = fromFile(t)
            %
            %   obj = wcon.meta.software.fromFile(t)
            
           if strcmp(t.type,'array')
               n_objs = t.n_elements;
               json_objs = t.getObjectArray;
           else
               n_objs = 1;
               json_objs = t;
           end
           
           objs(n_objs) = wcon.meta.software;
           
           for iObj = 1:n_objs
              cur_obj = objs(iObj);
              cur_json = json_objs(iObj);
              names = cur_json.key_names;
              for iName = 1:length(names)
                  switch names{iName}
                      case 'name'
                          cur_obj.name = cur_json.getTokenString('name');
                      case 'version'
                          cur_obj.version = cur_json.getTokenString('version');
                      case 'featureID'
                          cur_obj.featureID = cur_json.getStringOrCellstr('featureID');
                      case 'commit_hash'
                          cur_obj.commit_hash = cur_json.getTokenString('commit_hash');
                      otherwise
                          error('unrecognized feature in software: ''%s''',names{iName})
                  end
              end
           end
        end
    end
    
end

