%module c_exception

%include "exception.i"

/* 
   last resource, catch everything but don't override 
   user's throw declarations.
*/

#if defined(SWIGUTL)
%exception {
  try {
    $action
      } catch(...) {
    SWIG_exception_fail(SWIG_RuntimeError,"postcatch unknown");
  }
 }
#elif defined(SWIGGO) && defined(SWIGGO_GCCGO)
%exception %{
  try {
    $action
#ifdef __GNUC__
      } catch (__cxxabiv1::__foreign_exception&) {
    throw;
#endif
  } catch(...) {
    SWIG_exception(SWIG_RuntimeError,"postcatch unknown");
  }
  %}
#else
%exception {
  try {
    $action
      } catch(...) {
    SWIG_exception(SWIG_RuntimeError,"postcatch unknown");
  }
 }
#endif

%inline %{
  struct E1
  {
  };

  struct E2 
  {
  };

  /* caught by %postexception */
  int foo()
  {
    throw E1();
    return 0;     
  }
    
  /* caught by %postexception */
  int bar()
  {
    throw E2();
    return 0;     
  }
  %}
