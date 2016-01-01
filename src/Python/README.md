# Python implementation of Tracker Commons

This project contains source code in Python that implements the WCON data format for tracking data.

*When the Python specification and the behavior of the Scala implementation differ, and it is not obvious which one is correct, the behavior of the Scala implementation should be presumed to be authoritative.*

### Installation

`wcon` is [registered with the Python Package Index](https://pypi.python.org/pypi/wcon/), so just enter this command from any shell:

```
pip install wcon
```

Any problems?  Visit the more detailed [installation guide](INSTALL.md).

### Usage

```
import wcon

# Open a worm file, convert it to canonical form, and save it
# (actually it's automatically converted before saving, but 
#  here we do it explicitly)
w = wcon.WCONWorms.load_from_file('tests/minimax.wcon')
canon_w = w.to_canon
w.save_to_file('test.wcon', pretty_print=True)

# From a string literal:
from io import StringIO
w2 = WCONWorms.load(StringIO('{"units":{"t":"s","x":"mm","y":"mm"}, '
                              '"data":[]}'))

# WCONWorms.load_from_file accepts any valid WCON, but .save_to_file 
# output is always "canonical" WCON, which makes specific choices about 
# how to arrange and format the WCON file.  This way the functional 
# equality of any two WCON files can be tested by this:

w1 = WCONWorms.load_from_file('file1.wcon')
w2 = WCONWorms.load_from_file('file2.wcon')

assert(w1 == w2)

# or:

w1.save_to_file('file1.wcon')
w2.save_to_file('file2.wcon')

import filecmp
assert(filecmp.cmp('file1.wcon', file2.wcon'))

w3 = w1 + w2  # merge the two.  An exception is raised if the data clashes
```


### `WCONWorms` class: Attributes

- `units`: dict
    - May be empty, but is never None since 'units' is required 
    to be specified.
- `metadata`: dict
    - If 'metadata' was not specified, metadata is None.
    - The values in this dict might be nested into further dicts or other
    data types.
- `data`: Pandas DataFrame or None
    - If 'data' was not specified, data is None.
- [Note: `files`, if present in the input, is not persisted unless the .load
       factory method is used.]

### `WCONWorms` class: Public-Facing Methods

- `load_from_file`   (JSON_path)                [class method]
- `save_to_file`     (JSON_path, pretty_print)
- `to_canon`                                    [property]
- `__add__`                                     [use `+`]
- `__eq__`                                      [use `==`]

### Custom WCON features

Any top-level key other than the basic:

- files
- units
- metadata
- data

... is ignored.  Handling them requires subclassing WCONWorms.


### WCON parser: proof of concept

Thanks to the Python libraries `json` and `jsonschema`, it is relatively trivial to parse and validate a WCON file.  Here's an example of how one might accomplish this, without even using the `wcon` package:

    import json, jsonschema
    
    # The WCON schema
    with open("wcon/wcon_schema.json", "r") as wcon_schema_file:
    	schema = json.loads(wcon_schema_file.read())
    
    # Our example WCON file
    JSON_path = '../../tests/minimax.wcon'
    with open(JSON_path, 'r') as infile:
    	serialized_data = infile.read()
    
    # Load the whole JSON file into a nested dict.
    w = json.loads(serialized_data)
    
    # Validate the raw file against the WCON schema
    jsonschema.validate(w, schema)

With the above code we end up with a nested dictionary `w` containing everything that was serialized in the `minimax.wcon` file.

### The `wcon` Python package

Using this `wcon` Python package, something similar can be accomplished:

    import wcon

    w = wcon.WCONWorms.load_from_file('../../tests/minimax.wcon')

Here, instead of being a nested dictionary, `w` is a `WCONWorms` object that is more powerful.  Here are some of the additional things that can be accomplished with the `WCONWorms` object:

- The WCON file is validated not just against the WCON schema, but also to ensure units are valid, that every data key has a corresponding unit, and that every data segment has "aspects" of the same length.  (e.g. if a skeleton at time `1.3` has 45 `x`-coordinates, it should also have 45 `y`-coordinates.  This condition is not expressible in a JSON schema but it is validated programatically by the WCONWorms initializer.
- Units are expressed as `MeasurementUnit` objects, which can be compared with other such objects, to verify that "mm" and "millimetres" refer to the same units, for instance.  (see the below section for more details)
- WCONWorms objects can have their data be converted into canonical units, and then saved again.
- WCONWorms objects can be loaded from multiple files and combined together, via the `"files"` object.
- Worm data recorded in multiple "tracks", or elements, in the `"data"` object, can have such tracks merged.
- Worm data can be extracted in a Pandas DataFrame format for easier downstream processing, since the dimensions of the data have been placed into one two-dimensional array, rather than in a nested arrays.
- WCONWorms can be subclassed by labs implementing "special features", i.e. top-level objects starting with "@" or objects within individual data array objects starting with "@".


### MeasurementUnit

The WCON format requires a `"units"` object, where you specify in what units your quantities are being measured.  `WCONParser` represents these units internally as `MeasurementUnit` objects. With `MeasurementUnit`, you can convert from any supported unit expression to the canonical one: 

    >>> MeasurementUnit.create('m')
    MeasurementUnit, original form: 'm' canonical form: 'mm'
    >>> u = MeasurementUnit.create('m')
    >>> u.to_canon(1)
    1000.0
    >>> u.from_canon(100)
    0.1
    >>> u = MeasurementUnit.create('F')
    >>> u.to_canon(72)
    22.222222222222221
    >>> u = MeasurementUnit.create('m/min')
    >>> u.canonical_unit_string
    'mm/s'
    >>> u.to_canon(5)
    83.33333333333334
    >>> u = MeasurementUnit.create('m^2')
    >>> u.to_canon(1)
    1000000.0
    
You can also check the equality of various unit expressions.  For example, all of these expressions will evaluate to `True`:

    MeasurementUnit.create('mm') == MeasurementUnit.create('millimetre')
    MeasurementUnit.create('Mm') == MeasurementUnit.create('megametre')
    MeasurementUnit.create('mm') != MeasurementUnit.create('Mm')
  
