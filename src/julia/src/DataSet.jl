## This file is part of Open Worm's Tracker Commons project and is distributed under the MIT license.
## Contents copyright (c) 2016 by Rex Kerr, Calico Life Sciences, and Open Worm.

############################################
# DataSet representing an entire WCON file #
############################################

type DataSet
    conversions :: KnownUnits
    meta :: MetaData
    data :: Array{CommonWorm, 1}
    my_filename :: AbstractString
    next_filename :: Nullable{AbstractString}
    prev_filename :: Nullable{AbstractString}
    files_custom :: Dict{AbstractString, Any}
    custom :: Dict{AbstractString, Any}
end

function empty_dataset()
    DataSet(
        minimal_known_units(),
        empty_metadata(),
        [],
        "",
        Nullable{AbstractString}(),
        Nullable{AbstractString}(),
        no_custom(),
        no_custom()
    )
end

function convert_for_json(ds :: DataSet)
    result :: Dict{AbstractString, Any} = Dict{AbstractString, Any}()
    result["units"] = convert_for_json(ds.conversions)
    md = convert_for_json(ds.meta)
    if length(md) > 0 result["metadata"] = md end
    result["data"] = map(x -> convert_for_json(x), ds.data)
    for (k,v) in ds.custom
        result[k] = v
    end
    externalize_jsonable!(result, ds.conversions, Nullable{UnitConverter}())
    return result
end

function parsed_json_to_dataset(j0 :: Dict{AbstractString, Any}, fullname :: AbstractString)
    result :: Union{AbstractString, DataSet} = ""
    someC :: Bool = false
    someO :: Bool = false
    j = copy(j0)
    u = begin
            if !haskey(j, "units")
                result = "Invalid WCON file: no units"
                return result
            elseif !isa(j["units"], Dict{AbstractString, Any})
                result = "Failed to parse WCON units: units must be a JSON object"
                return result
            end
            ux = parsed_json_to_units(j["units"])
            if isa(ux, AbstractString)
                result = string("Failed to parse WCON units.  ", convert(AbstractString, u))
                return result
            end
            convert(KnownUnits, ux)
        end
    internalize_parsed_json!(j, u, Nullable{UnitConverter}())
    m = if !haskey(j, "metadata") empty_metadata()
        else
            if !isa(j["metadata"], Dict{AbstractString, Any})
                result = string("Failed to parse WCON metadata: metadata must be a JSON object")
                return result
            end
            mx = parsed_json_to_metadata(j["metadata"])
            if isa(mx, AbstractString)
                result = string("Failed to parse WCON metadata.  ", convert(AbstractString, mx))
                return result
            end
            convert(MetaData, mx)
        end
    d = begin
            if !haskey(j, "data")
                result = "Invalid WCON file: no data"
                return result
            elseif !isa(j["data"], Array{}) && !isa(j["data"], Dict{AbstractString, Any})
                result = "Invalid WCON file: data must be an array of JSON objects"
                return result
            end
            ds :: Array = isa(j["data"], Array{}) ? j["data"] : [j["data"]]
            ans = fill(empty_worm(), length(ds))
            for i in 1:length(ds)
                di = ds[i]
                if !isa(di, Dict{AbstractString, Any})
                    result = string("Failed to parse WCON data.  Element ", i, " is not a JSON object.")
                    return result
                end
                ai = parsed_json_to_worm(di)
                if isa(ai, AbstractString)
                    result = string("Failed to parse WCON data, element ", i, ".  ", convert(AbstractString, ai))
                    return result
                end
                cw = convert(CommonWorm, ai)
                if (!someO && cw.had_origin)
                    someO = true
                end
                if (!someC && length(cw.cx > 0))
                    someC = true
                end
                ans[i] = convert(CommonWorm, ai)
            end
            ans
        end
    c = extract_custom(j)
    if (someO && !haskey(u.mapper, "ox"))
        result = string("Failed to parse WCON because ox/oy was used but had no units")
        return result
    end
    if (someC && !haskey(u.mapper, "cx"))
        result = string("Failed to parse WCON because cx/cy was used but had no units")
        return result
    end
    all = DataSet(u, m, d, fullname, Nullable{AbstractString}(), Nullable{AbstractString}(), no_custom(), c)
    if haskey(j, "files")
        if ~isa(j["files"], Dict{AbstractString, Any})
            result = "Failed to parse WCON files entry: it is not a JSON object"
            return result
        end
        f = convert(Dict{AbstractString, Any}, j["files"])
        if !haskey(f, "this")
            result = "Failed to parse WCON files entry.  No 'this' key present."
            return result
        end
        if ~isa(f["this"], AbstractString)
            result = "Failed to parse WCON files entry.  'this' filename is not a string."
            return result
        end
        myname = convert(AbstractString, f["this"])
        i = rsearchindex(fullname, myname)
        if i > 0 && i+5 > length(fullname) && (endswith(lowercase(fullname), ".wcon") || endswith(lowercase(fullname), ".json"))
            i = rsearchindex(fullname, myname, length(fullname)-5+length(myname))
        end
        if i == 0
            result = string("Failed to parse WCON files entry.  Could not find 'this' name fragment ", myname, " within ", fullname)
            return result
        end
        fc = extract_custom(f)
        if length(fc) > 0 all.files_custom = fc end
        if haskey(f, "next")
            n = f["next"]
            if !isa(n, AbstractString) && !(isa(n, Array{}) && isa(n[1], AbstractString))
                result = "Failed to parse WCON files entry: 'next' should be a string or array of strings"
                return result
            end
            nextname = convert(AbstractString, isa(n, AbstractString) ? n : n[1])
            all.next_filename = Nullable(string(fullname[1:(i-1)], nextname, fullname[(i+length(myname)):end]))
        end
        if haskey(f, "prev")
            p = f["prev"]
            if !isa(p, AbstractString) && !(isa(p, Array{}) && isa(p[1], AbstractString))
                result = "Failed to parse WCON files entry: 'prev' should be a string or array of strings"
                return result
            end
            prevname = convert(AbstractString, isa(p, AbstractString) ? p : p[1])
            all.prev_filename = Nullable(string(fullname[1:(i-1)], prevname, fullname[(i+length(myname)):end]))
        end
    end
    result = all
    return result
end
