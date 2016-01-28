
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
                result = v * si_prefix_map(pre)
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
                    result = v * si_prefix_map(pre)
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
