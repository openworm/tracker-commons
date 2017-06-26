# Multi-Worm Tracker Converter

The Multi-Worm Tracker is software by Rex Kerr and colleagues that tracks
multiple animals in real time and outputs only animal contours.  The
original version of the software is described in Swierczek et al., Nature
Methods v. 8, pp. 592-598 (2011).

The original version of the software produces a custom text-based output
format which is largely compatible with the WCON format.

Here, a command-line converter is provided that is based upon the Scala
version of WCON.

## Compiling

Make sure you have `sbt` installed (see Scala WCON converter for more
details).  Then run `sbt assembly` to produce a single jar that contains the
converter and all dependencies.

## Running

The converter can be run with

```
java -jar target/scala-2.12/old-mwt-to-wcon-assembly-0.1.0.jar to/ 20150131_114112
```

where `to/` is a path for where to place the output and where the numeric
code is a directory for the Multi-Worm Tracker using its old (v. 1.3 and
before) format.  It can also be a zip file.

If multiple input files or directories are specified, they'll all be
converted separately and the results will be placed in the `to/` directory.

By default, the files will be chunked in time and will contain at most
approximately 100,000 data points, and all output files will be placed in a
single `.zip` file.

A pixel size of 0.026 mm per pixel is assumed.  If this is not the correct
value, set it with the `--pixel-size` option.  For instance, for 0.0372 mm
per pixel, use `--pixel-size=0.0327`.

If the output should have some other maximum number of points, use
`--max-data=1234` where `1234` is the maximum number of data points to
be included.  `0` is equivalent to infinity (all data in one file).

By default, the summary image will be copied.  To avoid this, use
`--no-image`.

Note that the input data must all fit in memory simultaneously.  You should
use flags like `-Xmx3G` to the java command to increase the memory as needed
(shown here, 3 gigabytes maximum allowed).

Note: you can also run directly from `sbt` using `sbt -J-Xmx3G 'run to/
20150131_1141'` or the like.

## What is preserved and what is lost

Event times are preserved in a structure that is TBD (i.e. they are not yet
but will be when this is complete).

Centroids, skeletons, and outlines are preserved as written.

Areas are preserved under the `"@XJ"` tag for every data point in the
format, `"@XJ": { "area": [0.01715, 0.01694, ...] }`, i.e. one point per
timepoint.

Everything else is discarded.
