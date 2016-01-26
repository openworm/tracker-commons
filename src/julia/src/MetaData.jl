
type Laboratory
    pi :: AbstractString
    name :: AbstractString
    location :: AbstractString
    custom :: Dict{AbstractString, Any}
end

type Arena
    kind :: AbstractString
    diameterA :: Float64
    diameterB :: Float64
    orientation :: AbstractString
    custom :: Dict{AbstractString, Any}
end

type Software
    name :: AbstractString
    version :: AbstractString
    featureID :: Set{AbstractString}
    custom :: Dict{AbstractString, Any}
end

type MetaData
    lab :: Nullable{Laboratory}
    who :: AbstractString
    timestamp :: Nullable{DateTime}
    temperature :: Float64
    humidity :: Float64
    arena :: Nullable{Arena}
    food :: AbstractString
    media :: AbstractString
    sex :: AbstractString
    stage :: AbstractString
    age :: Nullable{Dates.Millisecond}
    strain :: AbstractString
    protocol :: Array{AbstractString, 1}
    software :: Array{Software, 1}
    settings :: Nullable{Any}
    custom :: Dict{AbstractString, Any}
end

function no_custom()
    return Dict{AbstractString, Any}()
end

function empty_laboratory()
    return Laboratory("", "", "", no_custom())
end

function empty_arena()
    return Arena("", NaN, NaN, "", no_custom())
end

function empty_software()
    return Software("", "", Set{AbstractString}(), no_custom())
end

function empty_metadata()
    return MetaData(Nullable{Laboratory}(), "", Nullable{DateTime}(), NaN, NaN, Nullable{Arena}(), "", "", "", "", Nullable{Dates.Millisecond}(), "", [], [], Nullable{Any}(), no_custom())
end

function convert_for_json(l :: Laboratory)
    m = Dict{AbstractString, Any}()
    if length(l.pi) > 0 m["PI"] = l.pi end
    if length(l.name) > 0 m["name"]= l.name end
    if length(l.location) > 0 m["location"] = l.location end
    if length(l.custom) > 0 m["custom"] = l.custom end
    return m
end

function convert_for_json(a :: Arena)
    m = Dict{AbstractString, Any}()
    if length(a.kind) > 0 m["kind"] = a.kind end
    if (!isnan(a.diameterA))
        if (!isnan(a.diameterB)) m["size"] = [a.diameterA; a.diameterB]
        else m["size"] = a.diameterA
        end
    end
    if length(a.orientation) > 0 m["orientation"] = a.orientation end
    if length(a.custom) > 0 m["custom"] = l.custom end
    return m
end

function convert_for_json(s :: Software)
    m = Dict{AbstractString, Any}()
    if length(s.name) > 0 m["name"] = s.name end
    if length(s.version) > 0 m["version"] = s.version end
    if length(s.featureID) > 0 m["featureID"] = collect(s.featureID) end
    if length(s.custom) > 0 m["custom"] = s.custom end
    return m
end

function convert_for_json(m :: MetaData)
    d = Dict{AbstractString, Any}()
    if !m.lab.isnull
        ld = convert_for_json(get(m.lab))
        if length(ld) > 0 d["lab"] = ld end
    end
    if length(m.who) > 0 d["who"] = m.who end
    if !m.timestamp.isnull d["timestamp"] = string(get(m.timestamp)) end
    if !isnan(m.temperature) d["temperature"] = m.temperature end
    if !isnan(m.humidity) d["humidity"] = m.humidity end
    if !m.arena.isnull
        la = convert_for_json(get(m.arena))
        if length(la) > 0 d["arena"] = la end
    end
    if length(m.food) > 0 d["food"] = m.food end
    if length(m.media) > 0 d["media"] = m.media end
    if length(m.sex) > 0 d["sex"] = m.sex end
    if length(m.stage) > 0 d["stage"] = m.stage end
    if length(m.protocol) > 0 d["protocol"] = m.protocol end
    nzsw = filter(x -> length(x) > 0, map(x -> convert_for_json(x), m.software))
    if length(nzsw) > 0
        if length(nzsw) == 1 d["software"] = nzsw[1]
        else d["software"] = nzsw
        end
    end
    if !m.settings.isnull d["settings"] = get(m.settings) end
    if length(m.custom) > 0 d["custom"] = m.custom end
    return d
end

function error_accum(err :: AbstractString, msg :: AbstractString)
    result :: AbstractString =
        if length(err) > 0 string(err, "; ", msg)
        else msg
        end
    return result
end

function error_if_not_string(a :: Any, err :: AbstractString, msg :: AbstractString)
    result :: AbstractString = err
    if !(typeof(a) <: AbstractString) result = error_accum(err, msg) end
    return result
end

function empty_if_not_string(a :: Any)
    if typeof(a) <: AbstractString convert(AbstractString, a) else "" end
end

function parsed_json_to_laboratory(m :: Dict{AbstractString, Any})
    result :: Union{Laboratory, AbstractString} = ""
    err :: AbstractString = ""
    keys = ["PI", "name", "location"]  # REALLY important to keep these in the same order as Laboratory struct!
    values = ["", "", ""]
    for i in 1:length(keys)
        values[i] = get(m, keys[i], "")
        err = error_if_not_string(values[i], err, string("Laboratory ", keys[i], " should be a string"))
    end
    result =
        if length(err) > 0 err
        else Laboratory(
                values[1], values[2], values[3],
                Dict(filter(kv -> !(first(kv) in keys), collect(m)))
            )
        end
    return result
end

function parsed_json_to_arena(m :: Dict{AbstractString, Any})
    result :: Union{Arena, AbstractString} = ""
    err :: AbstractString = ""
    keys = ["type", "size", "orientation"]
    kind = get(m, keys[1], "")
    err = error_if_not_string(kind, err, string("Arena ", keys[1], " should be a string"))
    diam = make_dbl_array(get(m, keys[2], Array{Float64}()))
    if length(diam) == 1
        if !(typeof(diam[1]) <: Number) err = error_accum(err, string("Arena ", keys[2], " should be numeric")) end
    elseif length(diam) == 2
        if (!diam[1] <: Number && diam[2] <: Number) err = error_accum(err, string("Arena ", keys[2], " should have only numeric entries")) end
    elseif length(diam) > 2
        err = error_accum(err, string("Arena ", keys[2], " size should have at most two dimensions"))
    end
    orient = get(m,keys[3], "")
    err = error_if_not_string(orient, err, string("Arena ", keys[3], " should be a string"))
    result =
        if length(err) > 0 err
        else Arena(
            kind,
            if length(diam) > 0 diam[1] else NaN end,
            if length(diam) > 1 diam[2] else NaN end,
            orient,
            Dict(filter(kv -> !(first(kv) in keys), collect(m)))
        )
        end
    return result
end

function parsed_json_to_software(m :: Dict{AbstractString, Any})
    result :: Union{Software, AbstractString} = ""
    err :: AbstractString = ""
    keys = ["name", "version", "featureID"]
    name = get(m, "name", "")
    err = error_if_not_string(name, err, string("Software ", keys[1], " should be a string"))
    version = get(m, "version", "")
    err = error_if_not_string(name, err, string("Software ", keys[2], " should be a string"))
    fid = get(m, "featureID", "")
    featureID :: Set{AbstractString} = Set{AbstractString}()
    if (typeof(fid) <: AbstractString)
        str = convert(AbstractString, fid)
        if length(str) > 0 featureID = Set(str) end
    elseif typeof(fid) <: Array
        sid = convert(Array, fid)
        allstring = true
        for s in sid
            if !(typeof(s) <: AbstractString) allstring = false end
        end
        if !allstring err = error_accum(err, "Software featureIDs should all be strings")
        else
            ids = Set{AbstractString}(map(x -> convert(AbstractString,x), sid))
            if length(ids) < length(sid) err = error_accum(err, "Sofware should not have duplicate featureIDs")
            else featureID = ids
            end
        end
    else err = error_accum(err, "Software featureID should be a string or array of strings")
    end
    result =
        if length(err) > 0 err
        else Software(
            name, version, featureID,
            Dict(filter(kv -> !(first(kv) in keys), collect(m)))
        )
        end
    return result
end

function error_if_not_type{T}(a :: Union{T, AbstractString}, err :: AbstractString)
    result :: AbstractString =
        if typeof(a) <: T err
        elseif length(err) > 0 string(err, "; ", convert(AbstractString, a))
        else convert(AbstractString, a)
        end
    return result
end

function null_if_not_type{T}(tpe :: DataType{T}, a :: Union{T, AbstractString})
    result :: Nullable{T} =
        if typeof(a) <: tpe Nullable(convert(tpe, a)) else Nullable{T}() end
    return result
end

function parsed_json_to_metadata(d :: Dict{AbstractString, Any})
    result :: Union{MetaData, AbstractString} = ""
    err :: AbstractString = ""
    keys = [
        "lab", "who", "timestamp", "temperature", "humidity",
        "arena", "food", "media", "sex", "stage",
        "age", "strain", "protocol", "software", "settings"
    ]
    onestring = [
        false, false, false, false, false,
        false, true, true, true, true,
        false, true, false, false, false
    ]
    strings = fill("", length(keys))
    for i in 1:length(keys)
        if onestring[i]
            s = get(d, keys[i], "")
            strings[i] = empty_if_not_string(s)
            err = error_if_not_string(err, string("MetaData ", keys[i], " should be a string"))
        end
    end
    return result
end
