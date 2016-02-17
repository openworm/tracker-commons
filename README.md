# Tracker Commons

The Tracker Commons is a repository of documentation, software, and discussions that enable labs to share their worm tracking data and analysis routines.

## The WCON format

The Worm tracker Commons Object Notation (WCON) file format provides a simple way to share _C. elegans_ tracking data between labs and analysis packages.

WCON uses the widely-supported [JSON](http://json.org) file format, so almost any language can easily import the data.
WCON specifies a few entries that you should include in a JSON "Object" (a.k.a. a map or dictionary) that will let
readers easily extract basic information about worms.

Here's an example!

```JSON
{
    "metadata":{
        "who":"Open Worm",
        "timestamp":"2016-01-22T17:44:48",
        "protocol":"Numbers made up by hand for an example!"
    },
    "units":{"t":"s", "x":"mm", "y":"mm"},
    "data":[
        {"id":"worm1", "t":0.1, "x":[0.33, 0.65, 0.8, 1.1, 1.2], "y":[2.31, 2.25, 2.0, 1.87, 1.66]},
        {"id":"worm1", "t":0.3, "x":[0.27, 0.6, 0.75, 1.0, 1.1], "y":[2.4, 2.3, 2.07, 1.78, 1.75]}
    ]
}
```

This file contains data for a single worm over two timepoints.  The worm's body is represented as
five points along the worm's spine.

## Tracker Commons software for reading and writing WCON files

The Tracker Commons repository contains implementations for reading and writing WCON data for a variety of languages.  Browse the `src` directory to learn more!

| Language  | Principal Author  |  Status |
| ------------- | ------------- | ------------- |
| [Scala](src/scala)  | Rex Kerr  | Available |
| [Julia](src/julia)  | Rex Kerr  | Available |
| [Python](src/Python)  | Michael Currie | Available [![Build Status](https://travis-ci.org/openworm/tracker-commons.svg?branch=master)](https://travis-ci.org/openworm/tracker-commons) [![PyPI package](https://badge.fury.io/py/wcon.svg)](http://badge.fury.io/py/wcon)  |
| [R](src/R)  | n/a  | Preliminary notes only |
| [Rust](src/Rust)  | n/a  | Preliminary notes only |
| Matlab  | n/a  | [Currently seeking contributors](https://github.com/openworm/tracker-commons/issues/45) |
| Octave  | n/a  | [Currently seeking contributors](https://github.com/openworm/tracker-commons/issues/45) |
