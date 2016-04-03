This folder hosts experimental sandbox examples for interfacing
Octave with C/C++ using the swig tool.

It is extremely simple, but it serves to exercise important
functionality for:

1. The ability to invoke a C/C++ function from Octave.
2. The ability to maintain and manipulate persistent global state.
3. The ability to invoke C/C++ functions that manipulate #2.

The idea behind the above functionality for an Octave-WCON wrapper
around the Python (or any other language with C/C++ bindings) is
the ability of the wrapper to not only invoke code from the host
library, but also to handle the necessary book-keeping for any
translation issues. As an example, the current design decision on
mapping Python objects to Octave is to make use of integer handles,
so there has to be some book keeping done when crossing the
language boundaries.