###########################################
### Get numeric data out of parsed JSON ###
###########################################

function make_dbl(a)
    x::Float64 = isa(a, Number) ? convert(Float64, a) : NaN
    x
end

function make_dbl_array(q::Any)
    if (typeof(q) <: Array)
        # Do it this way because map of Any array might still be Any
        result = zeros(length(q))
        for i in 1:length(q)
            result[i] = make_dbl(q[i])
        end
        result
    else [make_dbl(q)]
    end
end

function make_dbl_array(q::Any, n::Int64)
    if (typeof(q) <: Array)
        # Do it this way because map of Any array might still be Any
        result = Array(Float64, n)
        for i in 1:length(q)
            result[i] = make_dbl(q[i])
        end
        result
    else fill(make_dbl(q), n)
    end
end

function make_dbl_array_array(q::Any, n::Int64)
    if (n == 1)
        if (length(q)==1 && typeof(q[1]) <: Array)
            Array[make_dbl_array(q[1])]
        else
            Array[make_dbl_array(q)]
        end
    else
        result = Array(Array{Float64, 1}, n)
        for i in 1:length(q)
            result[i] = make_dbl_array(q[i])
        end
        result
    end
end

function extract_custom(d :: Dict{AbstractString, Any})
    dd = Dict{AbstractString, Any}()
    for (k,v) in d
        if startswith(k, "@")
            dd[k] = v
        end
    end
    dd
end
