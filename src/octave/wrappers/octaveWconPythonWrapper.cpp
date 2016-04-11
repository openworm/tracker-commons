#include "octaveWconPythonWrapper.h"

#include <Python.h>

#include <iostream>
#include <stdlib.h> // for srand and rand
using namespace std;

// Included here because we need declarations from Python.h
#include "wrapperInternal.h"

PyObject *wrapperGlobalModule=NULL;
PyObject *wrapperGlobalWCONWormsClassObj=NULL;
PyObject *wrapperGlobalMeasurementUnitClassObj=NULL;

// Am exposing this as a wrapper interface method
//   because it is conceivable a user or some 
//   middleware tool might want to explicitly
//   invoke the initializer.
extern "C" void initOctaveWconPythonWrapper(PyWrapError *err) {
  static bool isInitialized = false;

  PyObject *pErr;

  // Always check regardless of initialization.
  // NOTE: This works based on the requirement that every exposed API
  //   method MUST invoke an initialization check.
  //   The only exceptions are direct query methods that do not require
  //     an initialized runtime, like isNullHandle.
  wrapInternalCheckErrorVariable(err);
  if (!isInitialized) {
    cout << "Initializing Embedded Python Interpreter" << endl;
    // initializing random number generator
    srand(1337);
    Py_Initialize();
    PyRun_SimpleString("import sys; sys.path.append('../../Python')\n");
    
    wrapperGlobalModule = PyImport_Import(PyUnicode_FromString("wcon"));
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(wrapperGlobalModule);
      *err = FAILED;
      return;
    }

    wrapperGlobalWCONWormsClassObj = 
      PyObject_GetAttrString(wrapperGlobalModule,"WCONWorms");
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(wrapperGlobalModule);
      Py_XDECREF(wrapperGlobalWCONWormsClassObj);
      *err = FAILED;
      return;
    }

    wrapperGlobalMeasurementUnitClassObj = 
      PyObject_GetAttrString(wrapperGlobalModule,"MeasurementUnit");
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(wrapperGlobalModule);
      Py_XDECREF(wrapperGlobalWCONWormsClassObj);
      Py_XDECREF(wrapperGlobalMeasurementUnitClassObj);
      *err = FAILED;
      return;
    }
    isInitialized = true;
  }

  *err = SUCCESS;
  return;
}

extern "C" bool isNullHandle(PyWrapHandle handle) {
  if (handle == NULL_HANDLE) {
    return true;
  } else {
    return false;
  }
}

extern "C" PyWrapHandle makeNullHandle() {
  return NULL_HANDLE;
}

extern "C" bool isNoneHandle(PyWrapHandle handle) {
  if (handle == WCONOCT_NONE_HANDLE) {
    return true;
  } else {
    return false;
  }
}

// *****************************************************************
// ********************** WCONWorms Class

extern "C" PyWrapHandle static_WCONWorms_load_from_file(PyWrapError *err,
							const char *wconpath) {
  PyObject *pErr, *pFunc;

  initOctaveWconPythonWrapper(err); // just hand off user error variable
  if (*err == FAILED) {
    cerr << "ERROR: Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  pFunc = 
    PyObject_GetAttrString(wrapperGlobalWCONWormsClassObj,"load_from_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  PyUnicode_FromString(wconpath), 
					  NULL);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(pValue);
      *err = FAILED;
      return NULL_HANDLE;
    }

    if (pValue != NULL) {
      // do not DECREF pValue until it is no longer referenced in the
      //   wrapper sublayer.
      PyWrapHandle result = wrapInternalStoreReference(pValue);
      if (isNullHandle(result)) {
	cerr << "ERROR: Failed to store python object reference" 
	     << endl;
	Py_DECREF(pValue);
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: Null handle from load_from_file." << endl;
      // No need to DECREF a NULL pValue
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: load_from_file not a callable python function" 
	 << endl;
    Py_DECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" void WCONWorms_save_to_file(PyWrapError *err,
				       const PyWrapHandle selfHandle,
				       const char *output_path,
				       bool pretty_print,
				       bool compressed) {
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "ERROR: Failed to initialize wrapper library." << endl;
    return;
  }

  WCONWorms_instance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_instance,"save_to_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return; // failure condition
  }

  // http://stackoverflow.com/questions/28576775/python-c-api-boolean-objects
  //   explains why all Python "literals" still need to respect
  //   reference counts.
  if (PyCallable_Check(pFunc) == 1) {
    Py_INCREF(Py_False);
    Py_INCREF(Py_False);
    PyObject *toPP = Py_False;
    PyObject *outputCompressed = Py_False;
    if (pretty_print) {
      Py_DECREF(Py_False);
      Py_INCREF(Py_True);
      toPP = Py_True;
    }
    if (compressed) {
      Py_DECREF(Py_False);
      Py_INCREF(Py_True);
      outputCompressed = Py_True;
    }
    PyObject_CallFunctionObjArgs(pFunc, 
				 PyUnicode_FromString(output_path),
				 toPP,
				 outputCompressed,
				 NULL);
    Py_DECREF(toPP);
    Py_DECREF(outputCompressed);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      *err = FAILED;
      return;
    } else {
      *err = SUCCESS;
      return;
    }
  } else {
    cerr << "ERROR: save_to_file not a callable python function" 
	 << endl;
    Py_DECREF(pFunc);
    *err = FAILED;
    return;
  }
}

extern "C" PyWrapHandle WCONWorms_to_canon(PyWrapError *err,
					   const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // to_canon is implemented as an object property and not a function
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"to_canon");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE; // failure condition
  }

  if (pAttr != NULL) {
    // do not DECREF pAttr until it is no longer referenced in the
    //   wrapper sublayer.
    PyWrapHandle result = wrapInternalStoreReference(pAttr);
    if (isNullHandle(result)) {
      cerr << "ERROR: Failed to store python object reference" 
	   << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL_HANDLE;
    } else {
      *err = SUCCESS;
      return result;
    }
  } else {
    cerr << "ERROR: Null handle from to_canon" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}

// NOTE: Current probable bug:
//   x = y + z results in x == y; and
//   x = z + y results in x == z
// 
// TODO: When implementing C++ version of interface, consider
//   operator overloading.
PyWrapHandle WCONWorms_add(PyWrapError *err,
			   const PyWrapHandle selfHandle, 
			   const PyWrapHandle handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << handle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__add__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  WCONWorms_instance,
					  NULL);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      *err = FAILED;
      return NULL_HANDLE;
    } else {
      if (pValue != NULL) {
	// Do not DECREF stored pValue
	PyWrapHandle result = wrapInternalStoreReference(pValue);
	if (result == NULL_HANDLE) {
	  cerr << "ERROR: failed to store object reference in wrapper." 
	       << endl;
	  Py_DECREF(pValue);
	  *err = FAILED;
	  return NULL_HANDLE;
	} else {
	  *err = SUCCESS;
	  return result;
	}
      } else {
	cerr << "ERROR: add produced NULL result object"
	     << endl;
	// no need to DECREF a NULL pValue
	*err = FAILED;
	return NULL_HANDLE;
      }
    }
  } else {
    cerr << "ERROR: __add__ not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE;
  }
}

// NOTE: bool functions will always be set false under error conditions.
//   The onus is on the middleware dev to always check for err values.
bool WCONWorms_eq(PyWrapError *err,
		  const PyWrapHandle selfHandle, 
		  const PyWrapHandle handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return false;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return false;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << handle << endl;
    *err = FAILED;
    return false;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__eq__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return false; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  WCONWorms_instance,
					  NULL);
    pErr = PyErr_Occurred();
    Py_DECREF(pFunc);
    if (pErr != NULL) {
      Py_XDECREF(pValue);
      PyErr_Print();
      *err = FAILED;
      return false;
    } else {
      int retValue = PyObject_IsTrue(pValue);
      Py_DECREF(pValue);
      pErr = PyErr_Occurred();
      if (pErr != NULL) {
	PyErr_Print();
	*err = FAILED;
	return false;
      } else {
	*err = SUCCESS;
	if (retValue == 0) {
	  return false;
	} else if (retValue == 1) {
	  return true;
	} else { // really -1 according to specs.
	  // This is the annoying thing when dealing with
	  //   the mapping from true/false and 1,0,-1
	  *err = FAILED;
	  return false;
	}
      }
    }
  } else {
    cout << "ERROR: __eq__ not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return false;
  }
}

extern "C" PyWrapHandle WCONWorms_units(PyWrapError *err,
					const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // Attribute is a Python dict (Dictionary) object.
  //   We are not going to mess with its internal structure for now.
  //   Experience informs me that it is best to build an auxilary
  //   interface for Octave to issue Dictionary-modifying commands
  //   to the Python runtime, than to attempt to make its own copy
  //   or have a copy managed by C/C++ (loosely related to threading
  //   consistency issues.)
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"units");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE;
  }

  if (pAttr != NULL) {
    if (PyDict_Check(pAttr)) {
      PyWrapHandle result = wrapInternalStoreReference(pAttr);
      if (result == NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	Py_DECREF(pAttr);
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: units is not a dict object." << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: Null handle from units" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" PyWrapHandle WCONWorms_metadata(PyWrapError *err,
					   const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // Attribute is a Python dict (Dictionary) object with complex members
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"metadata");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE;
  }

  if (pAttr != NULL) {
    // checking for None at the top makes it possible to
    // cascade the conditionals because of the need to
    // handle reference counts.
    Py_INCREF(Py_None);
    // C/C++ equality is supposed to work
    if (pAttr == Py_None) {
      Py_DECREF(Py_None);
      *err = SUCCESS;
      return WCONOCT_NONE_HANDLE;
    } else if (PyDict_Check(pAttr)) {
      Py_DECREF(Py_None);
      PyWrapHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: metadata is neither a dict object nor None" 
	   << endl;
      Py_DECREF(Py_None);
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: metadata is NULL" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" PyWrapHandle WCONWorms_data(PyWrapError *err,
				       const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // Attribute is a pandas package DataFrame object.
  //   I currently have no clue what that is, so I'm leaving out
  //   any error checks until I figure it out.
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"data");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE;
  }

  if (pAttr != NULL) {
    PyWrapHandle result = wrapInternalStoreReference(pAttr);
    Py_DECREF(pAttr);
    if (result == NULL_HANDLE) {
      cerr << "ERROR: failed to store object reference in wrapper." 
	   << endl;
      *err = FAILED;
      return NULL_HANDLE;
    } else {
      *err = SUCCESS;
      return result;
    }
  } else {
    cerr << "ERROR: Null handle from data" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" long WCONWorms_num_worms(PyWrapError *err,
				    const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return -1;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return -1;
  }

  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"num_worms");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return -1;
  }

  if (pAttr != NULL) {
    long result;
    result = PyLong_AsLong(pAttr);
    Py_DECREF(pAttr);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      cerr << "ERROR: Could not convert num_worms to a long value" << endl;
      *err = FAILED;
      return -1;
    } else {
      *err = SUCCESS;
      return result;
    }
  } else {
    cerr << "ERROR: Null handle from num_worms" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return -1;
  }
}

extern "C" PyWrapHandle WCONWorms_worm_ids(PyWrapError *err,
					   const PyWrapHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // Attribute is a Python list object (of worm ids - of some type)
  //   I've seen integers as well as strings, so that needs to be
  //   sorted out as well.
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"worm_ids");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE;
  }

  if (pAttr != NULL) {
    if (PyList_Check(pAttr)) {
      PyWrapHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: worm_ids is not a list object." << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: Null handle from worm_ids" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" PyWrapHandle WCONWorms_data_as_odict(PyWrapError *err,
						const PyWrapHandle selfHandle){
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL_HANDLE;
  }

  // Attribute is an OrderedDict object which is a subclass of Python's
  //   dict object implemented in the collections package. There should
  //   be no harm checking and treating the object as a regular dict
  //   object.
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"data_as_odict");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL_HANDLE;
  }

  if (pAttr != NULL) {
    if (PyDict_Check(pAttr)) {
      PyWrapHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: data_as_odict is not a dict object." 
	   << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: Null handle from data_as_odict" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL_HANDLE;
  }
}


// *****************************************************************
// ********************** MeasurementUnit Class
extern "C" PyWrapHandle static_MeasurementUnit_create(PyWrapError *err,
						      const char *unitStr) {
  PyObject *pErr, *pFunc;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL_HANDLE;
  }

  pFunc = 
    PyObject_GetAttrString(wrapperGlobalMeasurementUnitClassObj,
			   "create");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  PyUnicode_FromString(unitStr), 
					  NULL);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(pValue);
      *err = FAILED;
      return NULL_HANDLE;
    }

    if (pValue != NULL) {
      // do not DECREF pValue until it is no longer referenced in the
      //   wrapper sublayer.
      PyWrapHandle result = wrapInternalStoreReference(pValue);
      if (result == NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	Py_DECREF(pValue);
	*err = FAILED;
	return NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: Null handle from create" << endl;
      // No need to DECREF a NULL pValue
      *err = FAILED;
      return NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: create not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return NULL_HANDLE;
  }
}

extern "C" double MeasurementUnit_to_canon(PyWrapError *err,
					   const PyWrapHandle selfHandle,
					   const double val) {
  PyObject *MeasurementUnit_instance=NULL;
  PyObject *pErr, *pFunc;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return -1.0;
  }

  MeasurementUnit_instance = wrapInternalGetReference(selfHandle);
  if (MeasurementUnit_instance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return -1.0;
  }

  pFunc = 
    PyObject_GetAttrString(MeasurementUnit_instance,"to_canon");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return -1.0; 
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					    PyFloat_FromDouble(val),
					    NULL);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      Py_XDECREF(pValue);
      PyErr_Print();
      *err = FAILED;
      return -1.0;
    } else {
      if (pValue != NULL) {
	double retValue;
	retValue = PyFloat_AsDouble(pValue);
	pErr = PyErr_Occurred();
	Py_DECREF(pValue);
	if (pErr != NULL) {
	  PyErr_Print();
	  *err = FAILED;
	  return -1.0;
	} else {
	  *err = SUCCESS;
	  return retValue;
	}
      } else {
	// Nothing to DECREF for NULL retValue
	*err = FAILED;
	return -1.0;
      }
    }
  } else {
    cerr << "ERROR: to_canon not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return -1.0;
  }
}

extern "C" double MeasurementUnit_from_canon(PyWrapError *err,
					     const PyWrapHandle selfHandle,
					     const double val) {
  PyObject *MeasurementUnit_instance=NULL;
  PyObject *pErr, *pFunc;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return -1.0;
  }

  MeasurementUnit_instance = wrapInternalGetReference(selfHandle);
  if (MeasurementUnit_instance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return -1.0;
  }

  pFunc = 
    PyObject_GetAttrString(MeasurementUnit_instance,"from_canon");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return -1.0; 
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  PyFloat_FromDouble(val),
					  NULL);
    Py_DECREF(pFunc);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      Py_XDECREF(pValue);
      PyErr_Print();
      *err = FAILED;
      return -1.0;
    } else {
      if (pValue != NULL) {
	double retValue;
	retValue = PyFloat_AsDouble(pValue);
	Py_DECREF(pValue);
	pErr = PyErr_Occurred();
	if (pErr != NULL) {
	  PyErr_Print();
	  *err = FAILED;
	  return -1.0;
	} else {
	  *err = SUCCESS;
	  return retValue;
	}
      } else {
	// Nothing to DECREF for NULL retValue
	*err = FAILED;
	return -1.0;
      }
    }
  } else {
    cerr << "ERROR: from_canon not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return -1.0;
  }
}

extern "C" 
const char *MeasurementUnit_unit_string(PyWrapError *err,
					const PyWrapHandle selfHandle) {
  PyObject *MeasurementUnit_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL;
  }

  MeasurementUnit_selfInstance = wrapInternalGetReference(selfHandle);
  if (MeasurementUnit_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL;
  }

  pAttr = 
    PyObject_GetAttrString(MeasurementUnit_selfInstance,"unit_string");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL;
  }

  if (pAttr != NULL) {
    // TODO: Strings are quite a pain in the butt that will have to
    //   be looked into further for robustness issues. It sure looks
    //   like we're hitting some Python 2 versus Python 3 issues as well.
    char *result;
    // Assuming Python allocates the memory for the result on the heap.
    result = PyBytes_AsString(PyUnicode_AsASCIIString(pAttr));
    *err = SUCCESS;
    return result;
  } else {
    cerr << "ERROR: Null handle from unit_string" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL;
  }
}

extern "C" 
const char *MeasurementUnit_canonical_unit_string(PyWrapError *err,
						  const PyWrapHandle selfHandle) {
  PyObject *MeasurementUnit_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  initOctaveWconPythonWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL;
  }

  MeasurementUnit_selfInstance = wrapInternalGetReference(selfHandle);
  if (MeasurementUnit_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL;
  }

  pAttr = 
    PyObject_GetAttrString(MeasurementUnit_selfInstance,
			   "canonical_unit_string");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return NULL;
  }

  if (pAttr != NULL) {
    char *result;
    result = PyBytes_AsString(PyUnicode_AsASCIIString(pAttr));
    *err = SUCCESS;
    return result;
  } else {
    cerr << "ERROR: Null handle from canonical_unit_string" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL;
  }
}
