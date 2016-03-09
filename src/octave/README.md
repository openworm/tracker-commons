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