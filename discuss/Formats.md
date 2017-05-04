# worm-tracker
Compilation of information and code bases related to open source trackers for *C. elegans* and related organisms.

Universal Formats
-----------------

One of the key issues for making people's trackers interoperable is a minimal set of agreed formats.  One we discussed was a standardised worm (e.g. an outline and/or skeleton) that would be the end result of various computer vision algorithms as well as the starting point for most feature extraction methods.  Are there other parts of the pipeline that should be standardised to make it easiest for people to contribute modular algorithms?

1. Grayscale image of a worm, clipped out with a sensibly small bounding box.
Probably needs time/position info in addition to the raw pixels.
Maybe some info about suggested intensity thresholds or magnification?
2. Outline of the worm.  (4-connected or 8-connected pixel walk?  Interpolated points?  With interpolation or cubic splining or what?)
3. Skeleton (how many points? add spline info or not?  what to do about head/tail ambiguity--flag to indicate head first vs unknown?)
4. Time series of very basic movement parameters on a per-worm basis
  1. Time (in seconds, presumably)
  2. Position (arbitrary origin?  units?)
  3. Area of segmented animal (units of mm^2?)
  4. Length of segmented animal (units mm?)
  5. Bearing of animal (vector to head?  least squares line fit?)
    
Also, a uniform heavily hand-annotated data set for each major condition would be a great resource for testing and validation.  For example: single tracked worm crawling for 10 minutes with all interesting features hand-anotated (eggs laid, backing, etc.); five minutes of a bunch of worms on a plate, maybe with some stimulus to get them to execute more omega turns, also with all important events annotated; five minutes of worms swimming (both single and multiple?), etc..

Candidate Metadata
------------------

* Date/Time
* Temperature
* Humidity
* Plate size
* Volume of media
* Recipe of media (% agar and salt concentrations can have big effects, especially if tracking chemotaxis etc.)
* Date poured (relative to experiment?)
* Storage method (tupperware in 4*C vs on bench @ 23*C)
* Lab
* Location
* Experimenter(s)
* Lid on/off
* Food/No Food
* Type of food (OP50/HB101/etc, volume, density, dry/growth time, etc).
* Age (from egglay?, synchronization method: sync egglay, bleach prep L1 arrest, manually pick L4s, etc.)
* Cultivation Temperature during development

If stimuli are given (information about stimuli: ie. concentration, method of application, intensity of stimulus). This sort of info will need to be created for each new stimuli, but our group could be a guiding force to identify what should be recorded when a new stimulus begins to be used.


JSON as a candidate format
-------------------------

These days, the problem is usually solved by writing JSON.  XML was popular for a while, but my sense is that the complexity of the format introduces too many of its own problems; it's too hard to write and parse and make sure that your schema are up to date, etc..

JSON is a fairly minimal format that lets you deliver sets of labeled data.  The data could be a number, an array, text, or another labeled set of data.  This covers pretty much everything pretty well, and aside from the inherent inefficiency of using text for floating-point numbers, can be made pretty compact.  For example, we could define a document that looks something like this:

```JSON
{
  "metadata": {
    "lab":{"location":"USS Enterprise", "name":"Biochemistry Lab" },
    "who":"Janet Wallace",
    "strain":"N2",
    "timestamp":"2267-04-27T15:15:32"
  },
  "units": {
  		"t":"s",
		"x":"mm",
		"y":"mm"
  },
  "data": [
    { "id":"1", "t":[0, 0.25, 0.5, 0.7, 0.9, 1.1], 
		"x":[[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13]],
		"y":[[112, 135], [112, 135], [112, 135], [112, 135], [112, 135], [112, 135]] },
    { "id":"2", "t":[0, 0.25, 0.5, 0.7, 0.9, 1.1], 
		"x":[[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13],[1215.11, 1216.13]],
		"y":[[112, 135], [112, 135], [112, 135], [112, 135], [112, 135], [112, 135]] }
  ]
}
```

We'd basically need to decide on a simple nesting structure and names for the data we have in common, and then all do our own thing when it came to the rest.

If we wanted to go one better, we could have some central place where the document structure would be specified, so we didn't end up with 15 different names and annotation styles for head position.

Another advantage of doing things this way is that format converters are easy to write: they don't need to know much about the details of experiments, just about JSON documents.  So if we did end up with 15 different head position tags, someone could write a converter that converts all 15 to one agreed-upon format (once we all agreed), and then everyone who wanted to read head position would only have to support that single format (and run the converter on any data that didn't obey that format).

Almost everything can read JSON these days, even Matlab.  The key to making this work is to be tolerant of missing tags.  Then everyone can put in what their tracker knows about and leave the rest out.

Image Formats
------------

AVI isn't even a format.  It's a more of a container specification that lets you embed a video format, almost all of which are optimized for streaming, not for easy and accurate recovery of a particular frame.  So if AVI is chosen, we'd also need to pick a particular codec or set of codecs that we agree are easy enough to read.  Most of the standard ones (MPEG4) are lossy.  Uncompressed AVIs are huge.  There doesn't seem to be any really solid standard lossless compressed format out there (esp. which can support 16 bit grayscale, which some of us may have; if we had to pick one FFV1 would probably be the best we could do), which mostly voids the advantage of having a standard format (i.e. you have a wide range of tools to choose from to view/edit/whatever your data).  So AVI would in practice probably mean uncompressed AVI (which is has an underlying BMP-like format IIRC).

HDF5 is a format but it's not (just) an _image_ format.  So you end up unable to trivially view your data--you have to do everything through relatively unusual tools that understand HDF5 (and the HDF5 image specification in particular).  HDF5 is probably a reasonable choice if you're not storing the whole frames anyway but rather clipping out regions of interest with some full-field key frames if you wish to keep track of what the background is doing, but it really raises the barrier to entry if you really do just have a video.  On the plus side, it's relatively easy to include per-frame metadata.  On the minus side, it's a complicated format so it's easy to mess up reading or writing if you don't have good support from your tools.

TIFF doesn't like file sizes larger than 4GB, and quite some number of readers don't even understand multi-plane TIFFs.  Compression is not supported by a number of readers/writers.  Embedding comment tags ought to be trivial, but in practice it's awkward.  If you _could_ use TIFFs the way they were intended, it would be easy to include metadata (e.g. just as JSON in a string in a comment field), but in practice we'd need to verify that it is easy enough to actually use the format.

PNG has decent default compression, but it is really only suited for single frames.  If you're saving millions of frames, your filesystem is probably not going to be happy.  You can then zip them, but random access inside a ZIP file is kind of awkward and not terribly fast.

There are also various microscopy formats which tend to have solved the access and sometimes compression problems reasonably well, but which tend to be poorly documented and have little support from tools--so you're in approximately as bad of shape with HDF5.  (Better, because there are already sensible places to put things like resolution and frame rate; worse because they are fragile.)  I wouldn't recommend these, but bring them up to point out that a lot of people have tried to solve problems like this before.

Another thing to consider is that the larger your data files are, the less likely you are to be able to load any particular file into memory.  If you, for instance, just want the contents of a single PNG file, you call a routine, and boom, you have the pixels (maybe with an extra call or two to get them in accessible order).  But you probably don't want to open and close your 20GB HDF5 file each time (I think file systems are better at random access than HDF5 readers), and so then you need to carry around some sort of HDF5 file object.  And there might be errors, an the errors might propagate to your next call if you forget to handle them.  (Same deal with AVI, or any other format where more goes into a file than you can reasonably fit in RAM.)

So HDF5 might be the best compromise, but I am loath to recommend it because it has substantial drawbacks.  In particular, I'm not convinced that it is superior to either uncompressed AVI or to a bunch of PNG or TIFF files in the filesystem with a JSON metadata file.

I should point out that when the Open Microscopy Environment (OME) had the problem of standard formats, they ended up using TIFF with a few custom tags (OME-TIFF), plus a metadata format that is in XML because XML was hot when they were writing the format.  They have a nice but space-wasting idea that the complete metadata goes into every TIFF file so that you never have image data without understanding the context of what it means.  (It is not really focused on video data.)

So, anyway, this is a very long-winded way of explaining why I don't have a particular recommendation.

What would yield a particular recommendation?

(1) Clear and easy tutorials for getting image data in and out of HDF5 in every likely language.  (At least: C, Java, Matlab, Python.)  Benchmarks of this vs. reading a TIFF would be even better.

(2) Anyone with OME experience with a good account of how it's super-awesome.  Taking a similar approach would look better.

(3) An agreement that we will store our own data as whatever we want and provide tools to output chunks of single-image files in a really common format (TIFF or PNG) for others to read easily.  It would be important for us to actually create and maintain the tools, though.

