## This file is part of Open Worm's Tracker Commons project and is distributed under the MIT license.
## Contents copyright (c) 2016 by Rex Kerr, Calico Life Sciences, and Open Worm.

#################################################
# Implementation of Tracker Commons recommended #
# unit conversions for WCON format              #
#################################################

si_prefix_map = Dict(
    "c" => 1e-2,
    "centi" => 1e-2,
    "m" => 1e-3,
    "milli" => 1e-3,
    "u" => 1e-6,
    "\u00B5" => 1e-6,
    "\u03BC" => 1e-6,
    "micro" => 1e-6,
    "n" => 1e-9,
    "nano" => 1e-9,
    "k" => 1e3,
    "kilo" => 1e3,
    "M" => 1e6,
    "mega" => 1e6,
    "G" => 1e9,
    "giga" => 1e9
)

nonscaled_units_to_internal = Dict(
    "C" => x :: Float64 -> x,
    "K" => x :: Float64 -> x - 273.15,
    "F" => x :: Float64 -> ((x - 32)*5.0)/9.0
)

nonscaled_units_to_external = Dict(
    "C" => x :: Float64 -> x,
    "K" => x :: Float64 -> x + 273.15,
    "F" => x :: Float64 -> (x*9.0)/5.0 + 32
)

long_units_list = collect(Dict(
    "meter" => 1e3,
    "metre" => 1e3,
    "meters" => 1e3,
    "metres" => 1e3,
    "micron" => 1e-3,
    "microns" => 1e-3,
    "inch" => 25.4,
    "inches"=> 25.4,
    "second" => 1.0,
    "seconds" => 1.0,
    "minute" => 60.0,
    "minutes" => 60.0,
    "hour" => 3600.0,
    "hours" => 3600.0,
    "day" => 86400.0,
    "days" => 86400.0
))

abbrev_units_list = collect(Dict(
    "1" => 1.0,
    "%" => 0.01,
    "m" => 1e3,
    "in" => 25.4,
    "s" => 1.0,
    "min" => 60.0,
    "h" => 3600.0,
    "hr" => 3600.0,
    "d" => 86400.0
))

function make_nonscaled_converters(input :: AbstractString)
    result :: Nullable{Tuple{Function, Function}} = Nullable{Tuple{Function, Function}}()
    if haskey(nonscaled_units_to_internal, input) && haskey(nonscaled_units_to_external)
        result = Nullable(nonscaled_units_to_internal(input), nonscaled_units_to_external(input))
    end
    return result
end

function make_simple_scale(input :: AbstractString)
    result :: Float64 = NaN
    tidy = strip(input)
    longi = findfirst(x -> endswith(tidy, first(x)), long_units_list)
    if longi > 0
        k, v = long_units_list[longi]
        if tidy == k result = v
        else
            pre = tidy[1:(length(tidy) - length(k))]
            if haskey(si_prefix_map, pre) && length(pre) > 1
                result = v * si_prefix_map[pre]
            end
        end
    else
        shorti = findfirst(x -> endswith(input,first(x)), abbrev_units_list)
        if shorti > 0
            k, v = abbrev_units_list[shorti]
            if tidy == k result = v
            elseif k != "1" && k != "%"
                pre = tidy[1:(length(tidy)-length(k))]
                if haskey(si_prefix_map, pre) && length(pre) == 1
                    result = v * si_prefix_map[pre]
                end
            end
        end
    end
end

function make_complex_scale(input :: AbstractString)
    result :: Float64 = NaN
    div = findfirst(input, '/')
    if (div > 0)
        result = make_complex_scale(input[1:(div-1)]) / make_complex_scale(input[(div+1):end])
    else
        mul = findfirst(input, '*')
        if (mul > 0)
            result = make_complex_scale(input[1:(mul-1)]) * make_complex_scale(input[(div+1):end])
        else
            pwr = findfirst(input, '^')
            if (pwr > 0)
                result = make_simple_scale(input[1:(pwr-1)]) ^ (try parse(Int8, strip(input[(pwr+1):end])) catch; NaN end)
            else
                result = make_simple_scale(input)
            end
        end
    end
    return result
end

function make_complex_converters(input :: AbstractString)
    result :: Nullable{Tuple{Function, Function}} = Nullable{Tuple{Function, Function}}()
    sks = make_complex_scale(input)
    if !isnan(sks)
        result = Nullable((x :: Float64 -> x * sks, x :: Float64 -> x / sks))
    end
    return result
end

function make_any_converters(input :: AbstractString)
    result :: Nullable{Tuple{Function, Function}} = Nullable{Tuple{Function, Function}}()
    nsc = make_nonscaled_converters(strip(input))
    result = nsc.isnull ? make_complex_converters(input) : Nullable(nsc.value)
    return result
end

type UnitConverter
    description :: AbstractString
    to_internal :: Function
    to_external :: Function
end

type KnownUnits
    mapper :: Dict{AbstractString, UnitConverter}
    custom :: Dict{AbstractString, Any}
end

function minimal_known_units()
    convert(
      KnownUnits,
      parsed_json_to_units(Dict{AbstractString, Any}("t" => "s", "x" => "mm", "y" => "mm"))
    )
end

function convert_for_json(ku :: KnownUnits)
    result = Dict{AbstractString, Any}()
    for (k,v) in ku.mapper
        if k == "cx"
            if ku.mapper["x"].description != v.description result[k] = v.description end
        elseif k == "cy"
            if ku.mapper["y"].description != v.description result[k] = v.description end
        elseif k == "ox"
            if ku.mapper["x"].description != v.description result[k] = v.description end
        elseif k == "oy"
            if ku.mapper["y"].description != v.description result[k] = v.description end
        else
            result[k] = v.description
        end
    end
    if length(ku.custom) > 0
        for (k,v) in ku.custom 
            result[k] = v
        end
    end
    return result
end

function parsed_json_to_units(x :: Dict{AbstractString, Any})
    result :: Union{AbstractString, KnownUnits} = "Units block parsing failed"
    m = Dict{AbstractString, UnitConverter}()
    c = Dict{AbstractString, Any}()
    for (k,v) in x
        if startswith(k, "@")
            c[k] = v
        else
            if isa(v, AbstractString)
                s = convert(AbstractString, v)
                u = make_any_converters(s)
                if u.isnull
                    result = string("Units of ",k," cannot be parsed: ", s)
                    return result
                elseif haskey(m, k)
                    result = string("Units of ",k," should not be given more than once")
                    return result
                else
                    (fwd,bkw) = u.value
                    m[k] = UnitConverter(s, fwd, bkw)
                end
            else
                result = string("Units of ",k," should be given as a string")
                return result
            end
        end
    end
    if !haskey(m,"t") || !haskey(m,"x") || !haskey(m, "y")
        result = string("Units must be specified for all of t, x, and y")
    else
        result = KnownUnits(m,c)
    end
    return result
end

function floatize_any_array(a :: Array{})
    if isa(a, Array{Float64, 1})
        return a
    end
    result = fill(NaN, length(a))
    for i in 1:length(a)
        x = a[i]
        if isa(x, Number)
            result[i] = convert(Float64, x)
        elseif isa(x, Void)
            result[i] = NaN
        else
            return a
        end
    end
    return result
end

function internalize_parsed_json!(j :: Any, ku :: KnownUnits, uc :: Nullable{UnitConverter})
    if isa(j, Dict)
        d = convert(Dict,j)
        for (k,v) in d
            if k != "settings"
                uk = haskey(ku.mapper, k) ? Nullable(ku.mapper[k]) : uc
                if isa(v, Number) && !uk.isnull
                    d[k] = uk.value.to_internal(convert(Float64, v))
                else
                    x =
                        if isa(v, Array{})
                            if isa(v, Array{Float64, 1}) convert(Array{Float64, 1}, j)
                            else
                                y = floatize_any_array(convert(Array{}, v))
                                d[k] = y
                                y
                            end
                        else
                            v
                        end
                    internalize_parsed_json!(x, ku, uk)
                end
            end
        end
    elseif isa(j, Array{})
        ja = convert(Array{}, j)
        for i in 1:length(ja)
            ji = ja[i]
            if isa(ji, Number) && !uc.isnull
                ja[i] = uc.value.to_internal(convert(Float64, ji))
            else
                if isa(ji, Array{})
                    x = floatize_any_array(ji)
                    if isa(x, Array{Float64, 1})
                        ja[i] = x
                        if !uc.isnull
                            ys = convert(Array{Float64, 1}, x)
                            fn = uc.value.to_internal
                            for i in 1:length(ys)
                                ys[i] = fn(ys[i])
                            end
                        end
                    else
                        internalize_parsed_json!(ji, ku, uc)
                    end
                else
                    internalize_parsed_json!(ji, ku, uc)
                end
            end
        end
    end
end

function externalize_jsonable!(j :: Any, ku :: KnownUnits, uc :: Nullable{UnitConverter})
    if isa(j, Dict)
        d = convert(Dict, j)
        for (k,v) in d
            if k != "settings"
                uk = haskey(ku.mapper, k) ? Nullable(ku.mapper[k]) : uc
                if isa(v, Float64) && ! uk.isnull
                    d[k] = uk.value.to_external(convert(Float64, v))
                else
                    externalize_jsonable!(v, ku, uk)
                end
            end
        end
    elseif isa(j, Array{})
        if isa(j, Array{Float64, 1}) && !uc.isnull
            fn = uc.value.to_external
            jf = convert(Array{Float64, 1}, j)
            for i in 1:length(jf)
                jf[i] = fn(jf[i])
            end
        else
            ja = convert(Array{}, j)
            if uc.isnull
                for v in ja externalize_jsonable!(v, ku, uc) end
            else
                for i in 1:length(ja)
                    if isa(ja[i], Float64) ja[i] = fn(convert(Float64,ja[i]))
                    else
                        externalize_jsonable!(ja[i], ku, uc)
                    end
                end
            end
        end
    end
end
