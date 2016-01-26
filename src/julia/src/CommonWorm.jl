type CommonWorm
    id :: AbstractString
    t  :: Array{Float64, 1}
    x  :: Array{Array{Float64, 1}, 1}
    y  :: Array{Array{Float64, 1}, 1}
    cx :: Array{Float64, 1}
    cy :: Array{Float64, 1}
    custom :: Dict{AbstractString, Any}
end

#function can_merge_worms(a :: CommonWorm, b :: CommonWorm)
#    if (a.id != b.id) false
#    elseif (length(a.t) == 0 || length(b.t) == 0 || a[end] < b[1] || b[end] < a[1]) true
#    else false
#    end
#end
#
# THIS FUNCTION IS BROKEN--merging custom fields by throwing away one is almost surely wrong!
#function merge_worms(a :: CommonWorm, b :: CommonWorm)
#    assert(a.id == b.id)
#    if (length(a.t) == 0) CommonWorm(a.id, b.t, b.x, b.y, b.cx, b.cy, merge(a.custom,b.custom))
#    elseif (length(b.t) == 0) CommonWorm(a.id, a.t, a.x, a.y, a.cx, a.cy, merge(b.custom,a.custom))
#    elseif (a[end] < b[1]) CommonWorm(a.id, [a.t;b.t], [a.x;b.x], [a.y;b.y], [a.cx;b.cx], [a.cy;b.cy], merge(a.custom,b.custom))
#    elseif (b[end] < a[1]) CommonWorm(a.id, [b.t;a.t], [b.x;a.x], [b.y;a.y], [b.cx;a.cx], [b.cy;a.cy], merge(b.custom;a.custom))
#    else error("Cannot merge overlapping data sets")
#end

function common_worm_as_dict(cw :: CommonWorm)
    d = Dict{AbstractString, Any}("id" => cw.id, "t" => cw.t, "x" => cw.x, "y" => cw.y)
    if (length(cx) > 0)
        d["cx"] = cw.cx
    end
    if (length(cy) > 0)
        d["cy"] = cw.cy
    end
    if (length(custom) > 0)
        d = merge(custom,d)   # In case of duplicate keys, 2nd arg's keys win
    end
    d
end
