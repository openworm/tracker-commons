## R implementation of the Tracker Commons WCON File Format

R can run a Scala interpreter via the `rscala` package.  This gives R access to the Scala implementation of the WCON file format.

This project just contains minimal utility routines needed to get up and running quickly.

### Requirements

You need Java 8 and R installed.

### Installation

First, make sure the Scala installation works properly (you can run `sbt package` successfully).

Then, with R installed, type `install.packages("rscala")`.  If a recent version of Scala is installed, you should be able to run `library(rscala)` and nothing will happen; otherwise it will throw errors.  If you don't have a recent version of Scala installed, either install one or use `rscala::scalaInstall()` to get one automatically.

Finally, enter

```bash
sbt assembly
```

to build a `.jar` for use with R.  (You'll want to rerun this any time the Scala version changes and you want the updates.)

### Example usage

From within R, start with the `source('wcon-ready.r')`.  This will load a Scala interpreter (called `si`) which you can call using the syntax `si %~% 'scala.code("here")'`.

Here is an example of reading WCON data from R:

```R
source('wcon-ready.r')
si %~% 'val i = ReadWrite.read("../../tests/intermediate.wcon").right.get'
si %~% 'val w = ct.Wcon from i'
numrecords = si %~% 'w.datas.length'
firstid = si %~% 'w.datas(0).id'
first_time_of_first = si %~% 'w.datas(0).t(0)'
first_y_points = si %~% 'w.datas(0).ys(0)'
```

### Documentation

You mostly want to use the Scala documentation since the R implementation just calls through to the Scala.  You can use either the standard `org.openworm.trackercommons` structures, or the wrapped `org.openworm.trackercommons.compatibility` ones that use simpler data structures.  Either way, the Scaladocs can be built with `sbt doc` in the Scala project directory.

To learn more about how to use rscala to interface between R and Scala, see the [rscala documentation](https://cran.r-project.org/web/packages/rscala/rscala.pdf).
