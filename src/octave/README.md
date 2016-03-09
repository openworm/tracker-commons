## Matlab/Octave Prototype for WCON API

This is primarily an Octave prototype, but it is expected to be portable/ported
to Matlab without any major compatibility issues, very probably with no changes.

### Setup

This package relies on the jsonlab library for JSON parsing capabilities. The
current prototype expects the git repository to be installed in the "externals"
folder. When setting up this package for the first time:

cd externals;
git clone https://github.com/fangq/jsonlab

### Current Status 

This prototype is mostly incomplete. Some very
basic functionality works, but all assumed API functions exist and are
exercised by main.m in the "driver" folder.

### Intended API

WCONWorms class object
* to_canon() # convert data to canonical units
* load_from_file(file) # loads from json (wcon) file
* save_to_file(file) # writes to json (wcon) file. save_to_file will also invoke to_canon()
* load(string) # loads from a literal string
* is_equal(object1, object2) # probably need to invoke to_canon() on both first
* file_equal(file1, file2) # do we really need this?
* merge(object1, object2) # Exception is raised on conflict. What is a conflict - time overlap with incompatible movement?
