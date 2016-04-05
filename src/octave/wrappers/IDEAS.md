Random thoughts on implementation and design decisions.

1. Consider a C++ static object to encapsulate wrapper so Python embedding initialization
and finalization are automatically handled by the class constructor and destructor.

2. Consider Octave operator overloading for 2 WCONWorms objects. (see
http://www.gnu.org/software/octave/doc/v4.0.1/Operator-Overloading.html)
Note this requires that WCONWorms be implemented as a shell class object on Octave,
which isn't necessarily a bad thing.

3. Ideally we'd want to keep global copies of references to key API Python objects
that were only initialized once at init time. For example, making the calls to import
the wrapper module, or creating the Python references to static class objects like
WCONWorms.

4. Keep an eye out for additional complexity issues for when threads are used with Python.

5. We will need developer documentation on the Python folders to inform people maintaining
code there, that there is some coupling to their code from over here.

