## This file is part of Open Worm's Tracker Commons project and is distributed under the MIT license.
## Contents copyright (c) 2016 by Rex Kerr, Calico Life Sciences, and Open Worm.

###############################################################
# Get basic worm data into / out of a JSON-derived dictionary #
###############################################################

type CommonWorm
    id :: AbstractString
    t  :: Array{Float64, 1}
    x  :: Array{Array{Float64, 1}, 1}
    y  :: Array{Array{Float64, 1}, 1}
    cx :: Array{Float64, 1}
    cy :: Array{Float64, 1}
    had_origin :: Bool
    custom :: Dict{AbstractString, Any}
end

function empty_worm()
    CommonWorm("", [], [], [], [], [], false, Dict())
end

function convert_for_json(cw :: CommonWorm)
    d = Dict{AbstractString, Any}("id" => cw.id, "t" => cw.t, "x" => cw.x, "y" => cw.y)
    if (length(cw.cx) > 0)
        d["cx"] = cw.cx
        d["cy"] = cw.cy
    end
    if (length(cw.custom) > 0)
        d = merge(custom,d)   # In case of duplicate keys, 2nd arg's keys win
    end
    d
end

function parsed_json_to_worm(d :: Dict{AbstractString, Any})
    result :: Union{AbstractString, CommonWorm} = "Failed to parse worm with no ID"
    if !haskey(d, "id")
        return result
    end
    id = begin
        temp = d["id"]
        if isa(temp, Number)
            string(temp)
        elseif isa(temp, AbstractString)
            convert(AbstractString, temp)
        else
            result = "Worm ID should be a number or a string"
            return result
            ""
        end
    end
    if !haskey(d, "t")
        result = string("Failed to parse worm ", id, ": no t")
        return result
    end
    t = begin
        temp = d["t"]
        if isa(temp, Number)
            [convert(Float64, temp)]
        elseif isa(temp, Array{Float64, 1})
            convert(Array{Float64, 1}, temp)
        else
            result = string("Failed to parse worm ", id, ": t is not a number or array of numbers")
            return result
            [0.0]
        end
    end
    if !haskey(d, "x")
        result = string("Failed to parse worm ", id, " at ", t[1], ": missing x")
        return result
    end
    if !haskey(d, "y")
        result = string("Failed to parse worm ", id, " at ", t[1], ": missing y")
        return result
    end
    x = fill(Array{Float64, 1}(), length(t))
    y = fill(Array{Float64, 1}(), length(t))
    for (q, q0, qs) in [(x, d["x"], "x"), (y, d["y"], "y")]
        if isa(q0, Array{Float64, 1})
            if length(q) == 1 q[1] = q0
            elseif length(q) == length(q0)
                for i in 1:length(q0)
                    q[i] = [q0[i]]
                end
            else
                result = string("Failed to parse worm ", id, " at ", t[1], ": wrong size for ", qs)
                return result
            end
        elseif isa(q0, Number)
            if length(q) == 1 q[1]= [convert(Float64, q0)]
            else
                result = string("Failed to parse worm ", id, " at ", t[1], ": wrong size for ", qs)
                return result
            end
        elseif isa(q0, Array)
            if length(q0) != length(t)
                result = string("Failed to parse worm ", id, " at ", t[1], ": wrong size for ", qs)
                return result
            end
            for i in 1:length(q0)
                q0i = q0[i]
                if isa(q0i, Array{Float64, 1})
                    q[i] = convert(Array{Float64, 1}, q0i)
                elseif isa(q0i, Number)
                    q[i] = [convert(Float64, q0i)]
                else
                    result = string("Failed to parse worm ", id, " at ", t[1], ": ", qs, " index ", i, " should be a number or array of numbers")
                end
            end
        else
            result = string("Failed to parse worm ", id, " at ", t[1], ": ", qs, " should be an array of numbers or an array of arrays of numbers")
            return result
        end
    end
    for i in 1:length(x)
        if length(x[i]) != length(y[i])
            result = string("Failed to parse worm ", id, ": lengths of x and y spines do not agree at t = ", t[i])
            return result
        end
    end
    if (haskey(d, "cx") != haskey(d, "cy"))
        result = string("Failed to parse worm ", id, ": only one of cx, cy but should have both or neither")
        return result
    end
    if (haskey(d, "ox") != haskey(d, "oy"))
        result = string("Failed to parse worm ", id, ": only one of ox, oy but should have both or neither")
        return rsult
    end
    cx = haskey(d,"cx") ? fill(NaN, length(t)) : Array{Float64, 1}()
    cy = haskey(d,"cy") ? fill(NaN, length(t)) : Array{Float64, 1}()
    for (q, c, s) in [(x, cx, "x"), (y, cy, "y")]
        kc = string("c",s)
        ko = string("o",s)
        if haskey(d, kc)
            dc = d[kc]
            if isa(dc, Number)
                cc = convert(Float64, dc)
                for i in 1:length(c) c[i] = cc end
            else
                cc = convert(Array{Float64, 1}, dc)
                if length(cc) == length(c)
                    for i in 1:length(c) c[i] = cc[i] end
                else
                    result = string("Failed to parse worm ", id, " at ", t[1], ": wrong size for ",kc)
                    return result
                end
            end
        end
        if haskey(d, kc) || haskey(d, ko)
            o = haskey(d, ko) ? d[ko] : d[kc]
            if isa(o, Number)
                oo = convert(Float64, o)
                for i in 1:length(q)
                    for j in 1:length(q[i])
                        q[i][j] = q[i][j] + oo
                    end
                end
                if haskey(d, ko) && haskey(d, kc)
                    for i in 1:length(c)
                        c[i] = c[i] + oo
                    end
                end
            elseif isa(o, Array{Float64, 1})
                oo = convert(Array{Float64, 1}, o)
                if length(oo) != length(q)
                    result = string("Failed to parse worm ", id, " at ", t[1], ": wrong size for ", haskey(d, ko) ? ko : kc)
                    return result
                end
                for i in 1:length(q)
                    oi = oo[i]
                    for j in 1:length(q[i])
                        q[i][j] = q[i][j] + oi
                    end
                end
                if haskey(d, ko) && haskey(d, kc)
                    for i in 1:length(c)
                        c[i] = c[i] + oo[i]
                    end
                end
            else
                result = string("Failed to parse worm ", id, " at ", t[1], ": need number or array of numbers for ", haskey(d, ko) ? ko : kc)
                return result
            end
        end
    end
    result = CommonWorm(id, t, x, y, cx, cy, haskey(d, "ox"), extract_custom(d))
    return result
end
