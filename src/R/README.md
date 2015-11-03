## R implementation of Tracker Commons

This tool will require at least one of three separate JSON packages, "rjson", "RJSONIO", and "jsonlite".  You can install them by typing

```R
install.packages("RJSONIO")
install.packages("rjson")
install.packages("jsonlite")
```

## Impediments

R's JSON support is idiosyncratic.  This is somewhat understandable as R itself is highly idiosyncratic, but no JSON package allows one to import a JSON file and reproduce it again.  This is particularly problematic for parts of the WCON specification that allow arbitrary JSON (e.g. metadata settings).  Specifically:

* rjson and RJSONIO cannot tell the difference between `1` and `[1]`.  It's always `1`.
* jsonlite cannot tell the difference between `1` and `[1]`.  It's always `[1]` (worse).
* RJSONIO sticks the word Infinity into JSON if numeric data is infinite.
* Only jsonlite imports numeric lists as numeric (otherwise they're regular lists).
* Only jsonline can export non-finite numbers (and NA) as `null`.
* jsonlite cannot tell the difference between `"fish"` and `["fish"]`.  It's always `["fish"]`.  (This is really bad.)

#### Possible resolutions

* Write another JSON parser that actually stores JSON faithfully
* Splice together the least problematic parts of jsonlite and RJSONIO
* Have only a reader not a writer for R

#### Current situation

It is possible to overcome these issues, but it is a nontrivial amount of work.  Therefore, the R implementation is on hold at this time.
