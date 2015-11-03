module TrackerCommons

import JSON

type CommonWorm
    id
    t::Array{Float64,1}
    x::Array{Array{Float32,1},1}
    y::Array{Array{Float32,1},1}
end

function makedbl(a)
    x::Float64 = isa(a, Float64) ? a : nan(Float64)
    x
end

function get_a_worm(data::Dict{String, Any})
    id = data["id"]
    t = data["t"]
    if (isa(t, Float64))
        t = [convert(Float64, t)]
    elseif (typeof(t) <: Array)
        t = [makedbl(ti) for ti in t]
    else
        t = [nan(Float64)]
    end
    tt::Array{Float64,1} = t
    tt
end

function read_wcon(filename::String)
    j = JSON.parsefile(filename)
    j = convert(Dict{String, Any}, j)
    if (!haskey(j, "tracker-commons"))
        println("Warning, file does not identify as tracker-commons")
    elseif (j["tracker-commons"] != true)
        println("Warning, file does not specify that tracker-commons is false")
    end
    u = convert(Dict{String, Any}, j["units"])
    if (!haskey(u, "t"))
        error("WCON file does not specify units for t")
    end
    if (!haskey(u, "x"))
        error("WCON file does not specify units for x")
    end
    if (!haskey(u, "y"))
        error("WCON file does not specify units for y")
    end
    if (!haskey(j, "data"))
        error("WCON file does not contain a data section")
    end
    d = j["data"]
    if (isa(d, Dict{String, Any}))
        d = [d]
    end
    if (!(typeof(d) <: Array))
        error("WCON data section is not an array")
    end
    dd = [convert(Dict{String, Any}, x) for x in d]
    [get_a_worm(x) for x in dd]
end

end
