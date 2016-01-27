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

The minimal WCON reader is in `Minimal.jl`.  If you want to build a lightweight WCON reader/writer
for your own data, this may be a good place to start.  To use it, just `include("Minimal.jl")` and then
enter

```julia
TrackerCommonsMinimal.read_wcon("my/path/to/my_data.wcon")
```

to get a `WormDataSet` structure which roughly reflects the minimal WCON data features.

If you have a `WormDataSet`, you can write it out with

```julia
TrackerCommonsMinimal.write_wcon("my/path/to/new_file.wcon")
```

## Full-featured reader / writer

### Data I/O

TODO: write examples.

### Data Types

Worm data, including a single ID, plus a time series and x- and y-coordinate values, is
represented in the `CommonWorm` type.

TODO: finish section

#### A note about Data Frames

Julia contains a _Data Frame_ type, motivated by a similar type in R, that consists of
tabular data with named column headers.  Data frames are commonly used in data analysis
and statistics, so it would seem that they would be a natural fit for worm tracking data.

However, tracking data is not necessarily a convenient rectangular shape: worms may be
tracked for different periods of time, and within one worm, some data may be static (which side is ventral),
some may be a scalar for each time point (centroid), and other data may be vector valued (x coordinates of
spine).  Forcing the data into tabular form could thus be inefficient and require the user to
deal with frequent missing data.

Thus, the Tracker Commons implementation imports the data in custom structures.  Users are
encouraged to convert this to Data Frames in those cases where it suits their analysis.

### Unit Conversions

TODO: finish section

