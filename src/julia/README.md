# Julia implementation of Tracker Commons

Julia is a high-level language targeted at scientific data analysis and statistics.
It shares similarities with both Matlab and Python in syntax, but can compile very
fast (C-like) code when it can figure out that variables always contain primitive
types (doubles, etc.).

Thus, a long-term goal is for the Julia implementation to be fast.  It is presently
a work in progress, however, and speed presently takes third priority behind existence
and correctness of capabilities.

## Installation

This code has been tested with Julia 0.4.2.  It is no longer being tested with Julia 0.3.

To run this project you need to have [JSON.jl](https://github.com/JuliaLang/JSON.jl) installed.

Just type `Pkg.add("JSON")` at the Julia prompt.

## Minimal reader / writer

The minimal WCON reader is in `Minimal.jl`.  Just `include("Minimal.jl")` and then use

```julia
TrackerCommonsMinimal.read_wcon("my/path/to/my_data.wcon")
```

to get a `WormDataSet` structure which roughly reflects the minimal WCON data features.

If you have a `WormDataSet`, you can write it out with

```julia
TrackerCommonsMinimal.write_wcon("my/path/to/new_file.wcon")
```
