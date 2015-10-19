# Tracker Commons Scala Reference Implementation

This project contains source code in Scala that implements the WCON data
format for tracking data.  When the text specification and the behavior of
the Scala implementation differ, and it is not obvious which one is correct,
the behavior of the Scala implementation should be presumed to be authoritative.

#### Other reference implementations

The Tracker Commons code repository will contain code for a variety of languages
that may be used to read and write WCON files.  The behavior of all of these
should be identical.  Common sense should be used when output disagrees as to
which implementation is correct, but if it is not obvious the Scala version
is considered authoritative.

#### Why Scala?

1. Scala's strong typing and functional approach makes it easier to ensure
that code is correct.

2. There are parsing libraries available that compactly represent the logic
of what you are trying to parse.  This makes the gap between text and code
especially small, which is an advantage when trying to keep the two
synchronized.

3. Rex wrote this code, and he knows Scala well and likes it.

## Requirements

This project requires Java 8 and [Scala 2.11](http://scala-lang.org).  You do
not need to install Scala explicitly, as the build process is managed by
[SBT](http://www.scala-sbt.org/).  SBT must be installed to build the project,
and it will take care of downloading Scala and any needed libraries.

## Building the project

Type `sbt package` in this directory.  SBT will tell you where it has
created the jar file.

## Running project tests

Type `sbt test` in this directory.  The test suite will run, which mostly
involves reading basic and advanced sample files, writing them, and checking
that the output is as expected.

## Using the reference implementation to write your own worm data

At this time, the reference implementation is not designed for widespread use.
We hope this will change.
