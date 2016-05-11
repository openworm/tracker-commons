## Java implementation of Tracker Commons WCON data format

This project contains a Java interface to the WCON data format from OpenWorm's Tracker Commons.

This is not a stand-alone implementation.  Rather, the Java implementation calls the Scala implementation, as that also runs on the Java Virtual Machine.  This project supplies a few high-level routines that ease usage from Java.

### Requirements

You need to have Java 8 running on your machine.  Additionally, you need to be able to compile the Scala implementation: follow the instructions there.  When you can type `sbt package` there and have it complete successfully, you're ready to begin here.

Since you already need to have SBT running on your machine to build the Scala version, SBT is also used to build the Java version (instead of more traditional tools like Ant or Maven).  Again, you already have it, so you shouldn't have to worry about it any more than you already did.

### Building the Java Implementation

First, make sure the Scala implementation is up to date.  In the directory for the Scala implementation (at `"../scala"` from the Java directory), enter `sbt package`.

Then, in the `src/java` directory, enter

```bash
sbt assembly
```

at the command-line.  This will produce a single `.jar` file that contains the Scala runtime, the class files from the Scala implementation, any additional class files needed, and the routines in this package.  You can then include this single jar on your classpath.

### Example usage

Here is an example of reading WCON data using the Java WCON reader.

```java
import org.trackercommons.compatibility.*;
import org.trackercommons.javawrapper.*;

public class ReadWconExample {
  public static main(String args[]) {
    java.io.File f = new java.io.File("../tests/intermediate.wcon");
    Wcon w = Wcons.read(f);
    System.out.println("There are " + w.datas().length + " records");
    System.out.println("The first record has ID " + w.datas()[0].id());
    System.out.println("The first timepoint for that ID is " + w.datas()[0].t(0));
    System.out.println("The first x coordinate for that timepoint is " + w.datas()[0].x(0, 0));    
  }
}
```

You can compile this with `javac -cp path/to/JavaWcon-0.1.jar ReadWconExample.java` and run it with `java -cp path/to/JavaWcon-0.1.jar ReadWconExample`.
