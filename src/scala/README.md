# Tracker Commons Scala Reference Implementation

This project contains source code in Scala that implements the WCON data
format for tracking data.  When the text specification and the behavior of
the Scala implementation differ, and it is not obvious which one is correct,
the behavior of the Scala implementation should be presumed to be authoritative.

## Requirements

This project requires Java 8 and [Scala 2.12](http://scala-lang.org).  You do
not need to install Scala explicitly, as the build process is managed by
[SBT](http://www.scala-sbt.org/).  SBT must be installed to build the project,
and it will take care of downloading Scala and any needed libraries.

## Building the project

Type `sbt package` in this directory.  SBT will tell you where it has
created the jar file.  You can include this jar in your classpath.

## Using the reference implementation to read worm data

Here is an example of reading WCON data with the Scala implementation.
This example is meant to run in the SBT console (`sbt console` from the
command prompt, when within the `src/scala` directory).  The `scala`
REPL works just as well if you put the TrackerCommons jar file on the
classpath.

```scala
import org.openworm.trackercommons._
val worms = ReadWrite.read("../../tests/intermediate.wcon") match {
  case Right(ws) => ws
  case Left(err) => println(err); throw new Exception
}
```

The `ReadWrite` object will attempt to read a data file, delivering an
error message if it cannot.  The code above extracts the correct data if
there (the `Right` case), and prints the error and throws an exception if
not.  (If you don't care about error-handling, replace the `match` and following
statements with `.right.get`.)

## Using the reference implementation to create data

The Scala implementation also contains routines to create data suitable for writing in WCON format.  In particular, it helps populate the standard data structures and ensure that minimal requirements are met.

To create a simple WCON data file in memory, one can do something like the following:

```scala
import org.openworm.trackercommons._
val one = Create.worm(1).add(
  0, Array(-0.6, -0.3, 0, 0.3, 0.6), Array(0, -0.2, 0, 0.2, 0),
  ox = 0.6, oy = 0
).add(
  1, Array(-0.6, -0.3, 0, 0.3, 0.6), Array(-0.16, 0.04, 0.24, 0.04, -0.16),
  ox = 0.9, oy = -0.04
)
val two = Create.worm(2).add(
  1, Array(0, 0.2, 0, -0.2, 0), Array(-0.5, -0.25, 0, 0.25, 0.5),
  ox = 0, oy = 1.5
)
val wcon = Create.wcon().
  addData(one.result, two.result).
  setUnits()
val inMemory = wcon.result
val onDisk = wcon.setFile("scala-create-example.wcon").write
```

Note that the creation methods are meant to be used fluently (i.e. `that(arg).other(arg2).more(stuff)`).  The reason is that it uses phantom types to make sure you've actually gone through the necessary steps to create a valid WCON file (i.e. you must add some data, and you must set the units; and if you want to write the file, you must set the file).

Worm creation starts with specification of the worm ID, followed by calls to one or more of the large number of `add` methods that allow you to specify different subsets of optional data.

Note that all coordinates are _global_ coordinates.  If you specify offsets they will be used in the WCON file but not in memory.

## Running project tests

Type `sbt test` in this directory.  The test suite will run, which mostly
involves reading basic and advanced sample files, writing them, and checking
that the output is as expected.

## What does it mean for the Scala implementation to be the "Reference Implementation?"

The Tracker Commons code repository will contain code for a variety of languages
that may be used to read and write WCON files.  The behavior of all of these
should be identical.  Common sense should be used when output disagrees as to
which implementation is correct, but if it is not obvious the Scala version
is considered to have the "correct behavior".

#### What if implementations and/or the spec disagree?

Submit a bug report, or make a pull request with a fix!  Unless the implmentation
is documented to be incomplete, any differences from specification are bugs.

#### Why Scala?

1. Scala's strong typing and functional approach makes it easier to ensure
that code is correct.

2. There are parsing libraries available that compactly represent the logic
of what you are trying to parse.  This makes the gap between text and code
especially small, which is an advantage when trying to keep the two
synchronized.

3. Rex wrote this code, and he knows Scala well and likes it.
