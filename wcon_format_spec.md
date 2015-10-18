# Introduction
WCON (Wormtracker Commons object notation) is a text-based data interchange format that is a constrained subset of JSON (see http://www.json.org/ for details).  It is designed to be both human and machine readable and to facilitate data exchange and inter-operability for worm tracking data that is independent of platform and the language used for implementation.  Our hope is that working in a common format will also encourage code sharing and development within the community.

WCON has a minimal core description so that the basic data from a tracking experiment can be parsed by any software that is WCON compatible regardless of resolution or whether the file is from a single or multi-worm tracker.  At the same time, we recognise that most groups that develop and use worm trackers have particular features they are interested in that can depend on the specifics of their experiments.  We have included recommendations for how such custom features should be included that can accommodate a wide range of custom features while preserving interchange of the basic tracking data.  Therefore, like JSON itself, we expect other lab- or tracker-specific documentation to refer to this standard in introducing restricted formats.


# Basic implementation
WCON files with a tag on the first line `"tracker-commons":true` for quick identification when. In addition to this tag, a basic WCON file has two objects: `units` and `data`.  `units` defines the temporal and spatial dimensions of the experiment as strings.  `data` includes coordinates of each worm centreline over time `t` as pairs of `x` and `y` values.  Each worm is identified by a numerical `id` so that multiple worms can be distinguished in one data set. Values for `units` and `id` must be single-valued (JSON numbers or strings).  Values for `t`, `x`, and `y` can be arrayed (JSON arrays contained in square brackets), but there are restrictions on their dimensions.  If `t` is a 1 by n array, `x` and `y` must both be n by m arrays.  n is the number of time points for the current `id` and m is the number of points defining each worm midline. m can vary across `t` and/or `id`.

A WCON file with single-valued `t` and arrayed `x` and `y`:

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        { "id":1, "t":1.3, "x":[1215.11, ...], "y":[234.89, ...] },
        { "id":2, ... },
        ...
        { "id":1, "t":1.4, ...},
        ...
    ]
}
```

If tracking is done at low resolution and the worm is defined using a single xy-coordinate, this is accommodated by simply using this single point for `x` and `y` at each time.

A WCON file with arrayed t and corresponding nested arrays for `x` and `y`.

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        { "id":1, 
          "t": [
               1.3, 
               1.4,
               1.5
          ], 
          "x": [
               [1215.11, ...], 
               [1133.24, ...],
               [1039.12, ...]
          ],
          "y": [
               [234.89, ...],
               [350.72, ...],
               [430.10, ...]
          ]
        },
        { "id":2, ... },
        ...
        { "id":1, "t":1.6, ...},
        ...
    ]
}
```

Note that even if some xy-coordinate arrays are nested, it is still possible to repeat an `id` in the same file if the same worm has more tracks later.  Allowing repeats in `id` is convenient for multi-worm tracking data as tracks can be joined simply by updating `id` without having to copy track data.

**Any software that reads WCON should ignore any unrecognised key-value pair**. This is essential to make WCON flexible and allows the inclusion of custom features and additional information not required by the basic format. Below we specify a method for including custom features and other additions that are included in the accompanying software examples.

# Including custom features
Even seemingly primitive features such as speed can have many variants (centroid vs. midbody vs. head; calculated over a window vs. frame-by-frame; etc).  Therefore, having a standard feature called "speed" is not sufficiently flexible for wide use.  Instead, custom features are included in a WCON file using group-specific tags that are easily identifiable by `@` followed by a unique identifier.  For software developed in worm labs, use the lab's strain designation as a unique identifier.  A current list of lab strain designations (the first code in capital letters) is available from the CGC (http://cbs.umn.edu/cgc/lab-code) or WormBase (https://www.wormbase.org/resources/laboratory).  If a single group has multiple incompatible feature sets, the strain designation can be followed by a suffix separated from the lab code by a non-letter character (e.g. `"@CF Vikram"` or `"@OMG_1.5"`).  For groups without a strain designation, choose an identifier that is not already on the list of lab strain identifiers.

A WCON file with three custom features:

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        "@OMG": {
               "feature_names": ["speed", "curvature", "width"],
               "feature_units": ["mm/s", "1/mm", "mm"]
        }
        { "id":1,
          "t":1.3,
          "x":[1215.11, ...],
          "y":[234.89, ...],
          "@OMG": { "feature_values": [0.34, 1.5, 0.873] }
        }
        ...
    ]
}
```

For features that describe an entire plate rather than properties of individual worms (such as density or aggregate number), the features object can be added outside of `data`:

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        { "id":1,
          "t":1.3,
          "x":[1215.11, ...],
          "y":[234.89, ...],
          "@OMG": { "feature_values": [0.34, 1.5, 0.873] }
        }
        ...
    ]
    "@OMG": {
          "plate_feature_names": ["density", "aggregate number"],
          "plate_feature_units": ["1/mm^2", null],
          "plate_feature_values": [0.1, 8]
    }
}
```

# Common additions handled in accompanying code
It is expected that users will take advantage of the flexibility of the WCON specification to create derivatives.  The following examples are handled by the accompanying code examples.  We recommend that code written in other languages or derivative formats also handle these cases.


## Experiment and software metadata
Information about experimental conditions and software versions is critical for reproducibility across labs.  JSON's flexibility makes it straightforward to include experimental conditions directly in tracking files.  This has the advantage that the data and corresponding metadata will not be separated, making the usefulness of tracking data more durable.  If further tags are required, they can of course be added, but where appropriate we recommend using the following tags.

```JSON
{
    "tracker-commons":true,
    "metadata": {
           "lab": {"location":"CRB, room 5020", "name":"Behavioural Genomics" },
           "who":"Firstname Lastname",
           "timestamp":"2012-04-23T18:25:43.511Z",
           "temperature": { "experiment":22, "cultivation":20, "units":"C" },
           "humidity": { "value":40, "units":"%" },
           "data": { "type":"petri", "size":35, "units":"mm" },
           "food": " "none" or "OP50" or "HB101", ... ",
           "media": " "NGM" or "agarose", ... ",
           "sex": " "hermaphrodite" or "male" ",
           "stage": " "L1", "L2", "L3", "L4", "adult", or "dauer" ",
           "age":"18:25:43.511",
           "strain": " "N2", "CB4856", or "JU245", €... ",
           "image_orientation": " "imaged onto agar" or "imaged through agar" ",
           "protocol": "text description of protocol€",
           "software": {
                "tracker": { "name":"Software Name", "version": 1.3 },
                "featureID": "@OMG"
           },
           "settings": "Any valid JSON entry with hardware and software configuration can go here",
     }
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        { "id":1, "t":1.3, "x":[7.2, ...], "y":[0.5, ...] },
    	...
    ]
}
```

## Worm orientation
To completely define a worm's orientation, it is necessary to know which part of the skeleton corresponds to the head and which side of the worm is dorsal or ventral.  Use `"head":"L"` to indicate that the head is the first xy-coordinate, `"head":"R"` to indicate that the head is the last xy-coordinate, and `"head":"?"` to indicate that the orientation is uncertain.  `"ventral":"CW"` indicates that the ventral side is clockwise from the first xy-coordinate (whether or not the first coordinate is the head), `"ventral":"CCW"` that the ventral side is counterclockwise from the first point, and `"ventral":"?"` that the dorsoventral orientation is unknown.  One or both of head and ventral can be arrayed if orientation changes within an object, but if `t` is a 1 by n array, the arrayed orientation object must have n values.  Single-valued entries for either head or ventral indicate that the single value applies for all times in the current object.

Knowing the head position and the ventral side, it is possible to infer whether a worm is crawling with its left or right side touching the agar surface, but only if it is known whether the image is recorded looking onto the agar surface or looking through the agar so we recommend that this information is also included in the experiment metadata (see above).

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
         { "id":1, "t":1.3, "x":[7.2, ...], "y":[0.5, ...],
          "head":"L", 
          "ventral":"CCW"
        },
    	...
    ]
}
```

## Variable origin
The origin used to define the worm's xy-coordinates can change over time to reduce file size by reducing the magnitude of the numbers in the worm coordinates.  For example, the origin at each time could be the centroid of the worm skeleton or the position of its head in plate coordinates.  Using this convention, as worms move, the scale of their skeleton coordinates does not increase as they move away from an arbitrary plate frame of reference.  The origin coordinates are defined by `ox` and `oy`.  If `t` is a 1 by n array, `ox` and `oy` can be either 1 by n arrays or single valued.  If they are single valued, the same origin is used for all skeletons in the current object.

```JSON
{
    "tracker-commons":true,
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
 	{ "id":1, "t":1.3, "x":[7.2, ...], "y":[0.5, ...],
          "ox":320.4, "oy":920.1
        },
    	...
    ]
}
```

## Splitting large WCON files into chunks
For very long tracking experiments, it may be convenient to split a single experiment across multiple WCON files.  To make it easier to reconstruct tracks across files, we support a `files` object with the following rules:

1. The variable part of the file name (for example a numerical suffix) is listed under `this`
2. To find another file, take the current file name, delete the last instance of the variable part, and replace it with the string in `prev` or `next`
3. There must be at least one `next`, and they occur in temporal order.  If the current file is the last in a series, the single value of `next` can be `null`.
4. There must be at least one `prev`, and they occur in reverse temporal order.  If the current file is the first in a series, the single value of `prev` can be `null`.

To maintain identity across files, ensure `id` use is consistent.

For example, for a directory containing files labelled filename_1.wcon, filename_2.wcon, filename_3.wcon, etc., filename_2.wcon would look like this:

```JSON
{
    "tracker-commons":true,
    "files": {
         "this": "_2",
         "prev": ["_1", "_0"],
         "next": "_3",
     }
    "units": {"t":"s", "x":"mm", "y":"mm" },
    "data": [
        { "id":1, "t":1.3, "x":[7.2, ...], "y":[0.5, ...] },
    	...
    ]
}
```

