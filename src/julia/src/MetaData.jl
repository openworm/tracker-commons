## This file is part of Open Worm's Tracker Commons project and is distributed under the MIT license.
## Contents copyright (c) 2016 by Rex Kerr, Calico Life Sciences, and Open Worm.

###########################################################
# Structures for recommended metadata in WCON file format #
###########################################################

import Base.==
import Base.isequal

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
    who :: Array{AbstractString, 1}
    timestamp :: Nullable{DateTime}
    temperature :: Float64
    humidity :: Float64
    arena :: Nullable{Arena}
    food :: AbstractString
    media :: AbstractString
    sex :: AbstractString
    stage :: AbstractString
    age :: Float64
    strain :: AbstractString
    protocol :: Array{AbstractString, 1}
    software :: Array{Software, 1}
    settings :: Nullable{Any}
    custom :: Dict{AbstractString, Any}
end

==(a :: Laboratory, b :: Laboratory) =
    a.pi == b.pi && a.name == b.name && a.location == b.location && a.custom == b.custom

==(a :: Arena, b :: Arena) =
    a.kind == b.kind &&
    isequal(a.diameterA, b.diameterA) &&
    isequal(a.diameterB, b.diameterB) &&
    a.orientation == b.orientation &&
    isequal(a.custom, b.custom)

==(a :: Software, b :: Software) =
    a.name == b.name && a.version == b.version && a.featureID == b.featureID && isequal(a.custom, b.custom)

==(a :: MetaData, b :: MetaData) =
    a.lab.isnull == b.lab.isnull && (a.lab.isnull ? true : (a.lab.value == b.lab.value)) &&
    a.who == b.who &&
    a.timestamp.isnull == b.timestamp.isnull && (a.timestamp.isnull ? true : isequal(a.timestamp.value, b.timestamp.value)) &&
    isequal(a.temperature, b.temperature) &&
    isequal(a.humidity, b.humidity) &&
    a.arena.isnull == b.arena.isnull && (a.arena.isnull ? true : isequal(a.arena.value, b.arena.value)) &&
    a.food == b.food &&
    a.media == b.media &&
    a.sex == b.sex &&
    isequal(a.age, b.age) &&
    a.protocol == b.protocol &&
    a.software == b.software &&
    a.settings.isnull == b.settings.isnull && (a.settings.isnull ? true : isequal(a.settings.value, b.settings.value)) &&
    a.custom == b.custom

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
    return MetaData(Nullable{Laboratory}(), [], Nullable{DateTime}(), NaN, NaN, Nullable{Arena}(), "", "", "", "", NaN, "", [], [], Nullable{Any}(), no_custom())
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
        m["size"] = !isnan(a.diameterB) ? [a.diameterA; a.diameterB] : a.diameterA
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
    if !isnan(m.age) d["age"] = m.age end
    if length(m.protocol) > 0 d["protocol"] = m.protocol end
    nzsw = filter(x -> length(x) > 0, map(x -> convert_for_json(x), m.software))
    if length(nzsw) > 0
        d["software"] = length(nzsw) == 1 ? nzsw[1] : nzsw
    end
    if !m.settings.isnull d["settings"] = get(m.settings) end
    if length(m.custom) > 0 d["custom"] = m.custom end
    return d
end

function error_accum(err :: AbstractString, msg :: AbstractString)
    result :: AbstractString = length(err) > 0 ? string(err, "; ", msg) : msg
    return result
end

function error_if_not_string(a :: Any, err :: AbstractString, msg :: AbstractString)
    result :: AbstractString = err
    if !isa(a, AbstractString) result = error_accum(err, msg) end
    return result
end

function empty_if_not_string(a :: Any)
    isa(a, AbstractString) ? convert(AbstractString, a) : ""
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
        else Laboratory(values[1], values[2], values[3], extract_custom(m))
        end
    return result
end

function parsed_json_to_arena(m :: Dict{AbstractString, Any})
    result :: Union{Arena, AbstractString} = ""
    err :: AbstractString = ""
    keys = ["type", "size", "orientation"]
    kind = get(m, keys[1], "")
    err = error_if_not_string(kind, err, string("Arena ", keys[1], " should be a string"))
    diam = make_dbl_array(get(m, keys[2], Array{Float64,1}()))
    if length(diam) == 1
        if !isa(diam[1],Number) err = error_accum(err, string("Arena ", keys[2], " should be numeric")) end
    elseif length(diam) == 2
        if !(isa(diam[1], Number) && isa(diam[2], Number)) err = error_accum(err, string("Arena ", keys[2], " should have only numeric entries")) end
    elseif length(diam) > 2
        err = error_accum(err, string("Arena ", keys[2], " size should have at most two dimensions"))
    end
    orient = get(m,keys[3], "")
    err = error_if_not_string(orient, err, string("Arena ", keys[3], " should be a string"))
    result =
        if length(err) > 0 err
        else Arena(
            kind,
            length(diam) > 0 ? diam[1] : NaN,
            length(diam) > 1 ? diam[2] : NaN,
            orient,
            extract_custom(m)
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
    if isa(fid, AbstractString)
        str = convert(AbstractString, fid)
        if length(str) > 0 featureID = Set{AbstractString}([str]) end
    elseif isa(fid, Array)
        sid = convert(Array, fid)
        allstring = true
        for s in sid
            if !isa(s, AbstractString) allstring = false end
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
        else Software(name, version, featureID, extract_custom(m))
        end
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
        false, false, true, false, false,
        false, true, true, true, true,
        false, true, false, false, false
    ]
    vecstring = [
        false, true, false, false, false,
        false, false, false, false, false,
        false, false, true, false, false
    ]
    onenumber = [
        false, false, false, true, true,
        false, false, false, false, false,
        true, false, false, false, false
    ]
    strings = fill("", length(keys))
    for i in 1:length(keys)
        if onestring[i]
            s = get(d, keys[i], "")
            strings[i] = empty_if_not_string(s)
            if !isa(s, AbstractString)
                err = error_accum(err, string("MetaData ", keys[i], " should be a string"))
            end
        end
    end
    vstrings = fill(Array{AbstractString,1}(), length(keys))
    for i in 1:length(keys)
        if vecstring[i]
            s = get(d, keys[i], Array{AbstractString,1}())
            if isa(s, AbstractString)
                vstrings[i] = [convert(AbstractString, s)]
            elseif isa(s, Array{AbstractString,1})
                vstrings[i] = convert(Array{AbstractString,1}, s)
            else
                err = error_accum(err, string("MetaData ", keys[i], " should be a string or array of strings"))
            end
        end
    end
    numbers = fill(NaN, length(keys))
    for i in 1:length(keys)
        if onenumber[i]
            n = get(d, keys[i], NaN)
            if isa(n, Number)
                numbers[i] = convert(Float64, n)
            else
                err = error_accum(err, string("MetaData", keys[i], " should be a numeric value"))
            end
        end
    end
    laboratory =
        if haskey(d, "lab")
            l = d["lab"]
            if isa(l, Dict{AbstractString, Any})
                ld = convert(Dict{AbstractString, Any}, l)
                lab = parsed_json_to_laboratory(ld)
                if isa(lab, AbstractString)
                    err = error_accum(err, convert(AbstractString, lab))
                    Nullable{Laboratory}()
                else
                    Nullable(convert(Laboratory, lab))
                end
            else
                err = error_accum(err, "lab metadata should be a JSON object")
                Nullable{Laboratory}()
            end
        else Nullable{Laboratory}()
        end
    arena =
        if haskey(d, "arena")
            a = d["arena"]
            if isa(a, Dict{AbstractString, Any})
                ad = convert(Dict{AbstractString, Any}, a)
                arn = parsed_json_to_arena(ad)
                if isa(arn, AbstractString)
                    err = error_accum(err, convert(AbstractString, arn))
                    Nullable{Arena}()
                else
                    Nullable(convert(Arena, arn))
                end
            else
                err = error_accum(err, "arena metadata should be a JSON object")
            end
        else Nullable{Arena}()
        end
    softwares =
        if haskey(d, "software")
            s = d["software"]
            if isa(s, Dict{AbstractString, Any})
                s = Array(s)
            end
            if isa(s, Array)
                sa = convert(Array, s)
                softs = fill(empty_software(), length(sa))
                for i in 1:length(sa)
                    si = sa[i]
                    if isa(si, Dict{AbstractString, Any})
                        sid = convert(Dict{AbstractString, Any}, si)
                        soft = parsed_json_to_software(sid)
                        if isa(soft, AbstractString)
                            err = error_accum(err, convert(AbstractString, soft))
                        else
                            softs[i] = convert(Software, soft)
                        end
                    else
                        err = error_accum(err, "MetaData software should be a JSON object or an array of JSON objects")
                    end
                end
                softs
            else
                err = error_accum(err, "MetaData software should be a JSON object or an array of JSON objects")
                Array{Software,1}()
            end
        else Array{Software,1}()
        end
    stamp =
        if haskey(d, "timestamp")
            try
                ts = onestring[3]
                i = findlast(x -> x == 'Z' || x == '+' || x == '-')
                if i > 0 ts = ts[1:(i-1)] end
                Nullable(DateTime(ts))
            catch e
                err = error_accum(err, string("MetaData time stamp parsing error: ", e))
                Nullable{DateTime}()
            end
        else Nullable{DateTime}()
        end
    settings =
        if haskey(d, "settings")
            Nullable(d["settings"])
        else
            Nullable{Any}()
        end
    if length(err) > 0 result = err
    else result = MetaData(
        laboratory, vstrings[2], stamp, numbers[4], numbers[5],
        arena, strings[7], strings[8], strings[9], strings[10],
        numbers[11], strings[12], vstrings[13], softwares, settings,
        extract_custom(d)
    )
    end
    return result
end
