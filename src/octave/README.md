## Octave interface to WCON

The Octave interface to WCON is implemented as a wrapper library
around the Python implementation for WCON.

### Current Status 

The prototype implements the WCON API as described in the [Python
implementation API](../Python/README.md). The details of the
differences between the Python API and the Octave API are described
below.

The prototype will currently create valid handles of complex datatype
objects (lists, pandas DataFrame objects, etc ...) from Python, but no
support exists for manipulating them as yet. The main WCON object
classes WCONWorms and MeasurementUnit are the only object instances
where the handles can be used.

The prototype is currently tested against the following software stack:

Linux - Ubuntu LTS 14.04 (running as an OS image under VirtualBox
5.0.16 thru 5.0.20 r106931 on Mac OS X 10.11.4)
Octave 3.8.1
SWIG 2.0.11

### Setup

####One-time software stack setup

Please see our [installation instructions](./INSTALL.md).

### Running the Prototype

As of this implementation, it is currently necessary to copy

tracker-commons/wcon_schema.json

to

tracker-commons/src/Python/wcon/wcon_schema.json

With a load invocation as an example

```bash
octave
octave:1> wconoct
octave:2> worm1 = wconoct.load_from_file('../../../tests/multiworm.wcon')
```

### Octave API

The Octave API is not object oriented as in the case of
Python. Instead object instances are handled in the form of integer
handles in Octave.

In the following example, worm1 is a handle to a WCONWorms object
instance acquired after successfully loading the data from the file
multiworm.wcon.

```bash
octave:2> worm1 = wconoct.load_from_file('../../../tests/multiworm.wcon')
```

In general, object instance methods in the Python API require a valid
object reference handle in the corresponding Octave API. For example:

```bash
octave:3> wconoct.save_to_file(worm1,'output.wcon')
```

writes the instance worm1 out to file.

The prototype currently does not add a prefix to methods for the
Python class WCONWorms. It adds the "MU" prefix to methods for the
Python class MeasurementUnit so there is no ambiguity for method names
common to both classes like to_canon.

The current API is as follows:

####Support Methods
* initWrapper() - initializes the wrapper library, instantiates Python interpreter.
* isNullHandle(int handle) - given handle, is it NULL?
* isNoneHandle(int handle) - given handle, is it a Python None object?

####WCONWorms Methods
* int load_from_file(string path)
* save_to_file(int self, string path)
* int to_canon(int self) - returns object instance that is a canonical version of self
* int add(int self, int handle2) - merges the contents of self and handle2 and returns new object instance
* boolean eq(int self, int handle2)
* int units(int self) - returns list instance of units used in self. Note: list instances are not currently implemented. The handle is valid, but unuseable.
* int metadata(int self) - returns metadata instance used in self. Note: metadata instances are not currently implemented. The handle is valid, but unuseable.
* long num_worms(int self)
* int worm_ids(int self) - returns list instance of worm ids. Note: list instances ar not currently implemented. The handle is valid, but unuseable.
* int data_as_odict(int self) - returns pandas DataFrame object instance. Note: The handle is valid, but unuseable.

####MeasurementUnit Methods
* int MU_create(string unit_string)
* double MU_to_canon(int self, double value)
* double MU_from_canon(int self, double value)
* string MU_unit_string(int self)
* string MU_canonical_unit_string(int self)
