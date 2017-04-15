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

| Language  | Principal Author  |  Capabilities |  Test Status | Packaged? |
| ------------- | ------------- | ------------- | ----------------- |------|
| [Scala](src/scala)  | Rex Kerr  | Feature-complete, beta  | [![Build Status](https://semaphoreci.com/api/v1/ichoran/tracker-commons/branches/master/badge.svg)](https://semaphoreci.com/ichoran/tracker-commons) | no |
| [Python](src/Python)  | Michael Currie | Feature-complete, beta | [![Build Status](https://travis-ci.org/openworm/tracker-commons.svg?branch=master)](https://travis-ci.org/openworm/tracker-commons) | [![PyPI package](https://badge.fury.io/py/wcon.svg)](http://badge.fury.io/py/wcon)  |
| [Java](src/java) | Rex Kerr | Feature-complete, beta | none | no |
| [Matlab](src/Matlab)  | Jim Hokanson  | Runnable implementation (alpha) | n/a | no |
| [R](src/R)  | Rex Kerr  | Lightweight wrapper using rscala (alpha) | none | no |
| [Julia](src/julia)  | Rex Kerr  | Runnable implementation (alpha) | none | no |
| [Octave](src/octave)  | Chee Wai Lee  | Lightweight wrapper to Python version | none | no |
| [Rust](src/Rust)  | n/a  | Preliminary notes only | n/a | no |
