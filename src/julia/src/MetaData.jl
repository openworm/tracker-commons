package TrackerCommons

type Laboratory
    pi :: AbstractString
    name :: AbstractString
    location :: AbstractString
    custom :: Dict{AbstractString, Any}
end

type Arena
    kind :: AbstractString
    diameter :: Float64
    other_diameter :: Nullable{Float64}
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
    who :: Nullable{AbstractString}
    timestamp :: Union{AbstractString, ???}
    temperature :: Nullable{Float64}
    humidity :: Nullable{Float64}
    arena :: Nullable{Arena}
    food :: AbstractString
    media :: AbstractString
    sex :: AbstractString
    stage :: Nullable{???}
    strain :: AbstractString
    protocol :: Array{AbstractString, 1}
    software :: Nullable{Software}
    settings :: Nullable{Any}
    custom :: Dict{AbstractString, Any}
end
