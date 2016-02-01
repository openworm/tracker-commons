import JSON

function read_file(fullname :: AbstractString)
    result :: Union{AbstractString, DataSet} = ""
    j = JSON.parsefile(fullname)
    result = parsed_json_to_dataset(j, fullname)
    return result
end

function read_all_files(fullname :: AbstractString)
    result :: Union{AbstractString, Array{DataSet, 1}} = ""
    fwd = Array{DataSet, 1}()
    bkw = Array{DataSet, 1}()
    first = read_file(fullname)
    if isa(first, AbstractString)
        result = convert(AbstractString, first)
        return result
    end
    working :: DataSet = first
    while !working.prev_filename.isnull
        more = read_file(working.prev_filename.value)
        if isa(more, AbstractString)
            result = string("Read ", 1 + length(bkw), " files successfully but failed on ", working.prev_filename.value, ".  ", convert(AbstractString, more))
            return result
        end
        push!(bkw, more)
        working = more
    end
    working = first
    while !working.next_filename.isnull
        more = read_file(working.next_filename.value)
        if isa(more, AbstractString)
            result = string("Read ", 1 + length(bkw) + length(fwd), " files successfully but failed on ", working.next_filename.value, ".  ", convert(AbstractString, more))
            return result
        end
        push!(fwd, more)
        working = more
    end
    result = [reverse(bkw); [first]; fwd]
    return result
end

function write_file(ds :: DataSet, fullname :: Nullable{AbstractString})
    j = convert_for_json(ds)
    fn = (fullname.isnull) ? ds.this_name : fullname.value
    fh = open(fn, "w")
    JSON.print(fh, j)
    close(fh)
end
