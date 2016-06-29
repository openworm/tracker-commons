#include "octaveWconPythonWrapper.h"

#include <Python.h>

#include <iostream>
#include <string.h>
using namespace std;

#include "wrapperInternal.h"

extern PyObject *wrapperGlobalWCONWormsClassObj;

// *****************************************************************
// ********************** WCONWorms Class

extern "C" 
WconOctHandle wconOct_static_WCONWorms_load_from_file(WconOctError *err,
						     const char *wconpath) {
  PyObject *pErr, *pFunc;

  wconOct_initWrapper(err); // just hand off user error variable
  if (*err == FAILED) {
    cerr << "ERROR: Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  pFunc = 
    PyObject_GetAttrString(wrapperGlobalWCONWormsClassObj,"load_from_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE; // failure condition
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
      return WCONOCT_NULL_HANDLE;
    }

    if (pValue != NULL) {
      // do not DECREF pValue until it is no longer referenced in the
      //   wrapper sublayer.
      WconOctHandle result = wrapInternalStoreReference(pValue);
      if (wconOct_isNullHandle(result)) {
	cerr << "ERROR: Failed to store python object reference" 
	     << endl;
	Py_DECREF(pValue);
	*err = FAILED;
	return WCONOCT_NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: Null handle from load_from_file." << endl;
      // No need to DECREF a NULL pValue
      *err = FAILED;
      return WCONOCT_NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: load_from_file not a callable python function" 
	 << endl;
    Py_DECREF(pFunc);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

extern "C" 
void wconOct_WCONWorms_save_to_file(WconOctError *err,
				    const WconOctHandle selfHandle,
				    const char *output_path,
				    int pretty_print,
				    int compressed) {
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  wconOct_initWrapper(err);
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

extern "C" 
WconOctHandle wconOct_WCONWorms_to_canon(WconOctError *err,
					const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }

  // to_canon is implemented as an object property and not a function
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"to_canon");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE; // failure condition
  }

  if (pAttr != NULL) {
    // do not DECREF pAttr until it is no longer referenced in the
    //   wrapper sublayer.
    WconOctHandle result = wrapInternalStoreReference(pAttr);
    if (wconOct_isNullHandle(result)) {
      cerr << "ERROR: Failed to store python object reference" 
	   << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return WCONOCT_NULL_HANDLE;
    } else {
      *err = SUCCESS;
      return result;
    }
  } else {
    cerr << "ERROR: Null handle from to_canon" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

// NOTE: Current probable bug:
//   x = y + z results in x == y; and
//   x = z + y results in x == z
// 
// TODO: When implementing C++ version of interface, consider
//   operator overloading.
extern "C" 
WconOctHandle wconOct_WCONWorms_add(WconOctError *err,
				    const WconOctHandle selfHandle, 
				    const WconOctHandle handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << handle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__add__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE; // failure condition
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
      return WCONOCT_NULL_HANDLE;
    } else {
      if (pValue != NULL) {
	// Do not DECREF stored pValue
	WconOctHandle result = wrapInternalStoreReference(pValue);
	if (result == WCONOCT_NULL_HANDLE) {
	  cerr << "ERROR: failed to store object reference in wrapper." 
	       << endl;
	  Py_DECREF(pValue);
	  *err = FAILED;
	  return WCONOCT_NULL_HANDLE;
	} else {
	  *err = SUCCESS;
	  return result;
	}
      } else {
	cerr << "ERROR: add produced NULL result object"
	     << endl;
	// no need to DECREF a NULL pValue
	*err = FAILED;
	return WCONOCT_NULL_HANDLE;
      }
    }
  } else {
    cerr << "ERROR: __add__ not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

// NOTE: bool functions will always be set false under error conditions.
//   The onus is on the middleware dev to always check for err values.
extern "C" int wconOct_WCONWorms_eq(WconOctError *err,
				    const WconOctHandle selfHandle, 
				    const WconOctHandle handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  
  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return 0;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return 0;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No valid object instance using handle "
	 << handle << endl;
    *err = FAILED;
    return 0;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__eq__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    *err = FAILED;
    return 0; // failure condition
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
      return 0;
    } else {
      int retValue = PyObject_IsTrue(pValue);
      Py_DECREF(pValue);
      pErr = PyErr_Occurred();
      if (pErr != NULL) {
	PyErr_Print();
	*err = FAILED;
	return 0;
      } else {
	*err = SUCCESS;
	if (retValue == 0) {
	  return 0;
	} else if (retValue == 1) {
	  return 1;
	} else { // really -1 according to specs.
	  // This is the annoying thing when dealing with
	  //   the mapping from true/false and 1,0,-1
	  *err = FAILED;
	  return 0;
	}
      }
    }
  } else {
    cout << "ERROR: __eq__ not a callable python function" << endl;
    Py_XDECREF(pFunc);
    *err = FAILED;
    return 0;
  }
}

extern "C" 
WconOctUnitsDict *wconOct_WCONWorms_units(WconOctError *err,
					  const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return NULL;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return NULL;
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
    return NULL;
  }

  if (pAttr != NULL) {
    if (PyDict_Check(pAttr)) {
      PyObject *key, *value; /* borrowed references */
      Py_ssize_t pos = 0;
      int num = PyDict_Size(pAttr);
      int idx = 0;
      cout << "Number of elements = " << num << endl;
      WconOctUnitsKeyValue *retKeyValueArray = 
	new WconOctUnitsKeyValue[num];
      while (PyDict_Next(pAttr, &pos, &key, &value)) {
	WconOctHandle muHandle = wrapInternalStoreReference(value);
	/* How does one construct a C string from a Python string? */
	PyObject *keyAscii = PyObject_ASCII(key);
	/* don't deallocate this! */
	if (muHandle == WCONOCT_NULL_HANDLE) {
	  cerr << "ERROR: PyDict index " << pos 
	       << " :Failed to store object reference in wrapper."  
	       << endl;
	  Py_DECREF(pAttr);
	  Py_DECREF(keyAscii);
	  *err = FAILED;
	  return NULL;
	} else {
	  char *newKey = PyUnicode_AsUTF8(keyAscii);
	  // cout << "Pair at idx " << idx << " (pos " << pos << ")" << endl;
	  retKeyValueArray[idx].value = muHandle;
	  retKeyValueArray[idx].key = new char[strlen(newKey)+1];
	  strcpy(retKeyValueArray[idx].key,newKey);
	  /*
	  cout << "Found key-value pair [" << retKeyValueArray[idx].key
	       << "] x [" << retKeyValueArray[idx].value << "]" << endl;
	  */
	  idx++;
	  Py_DECREF(keyAscii);
	}
      }
      *err = SUCCESS;
      WconOctUnitsDict *result = new WconOctUnitsDict;
      result->numElements = num;
      result->unitsDict = retKeyValueArray;
      return result;
    } else {
      cerr << "ERROR: units is not a dict object." << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return NULL;
    }
  } else {
    cerr << "ERROR: Null handle from units" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return NULL;
  }
}

extern "C" 
WconOctHandle wconOct_WCONWorms_metadata(WconOctError *err,
					const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }

  // Attribute is a Python dict (Dictionary) object with complex members
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"metadata");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
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
      WconOctHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == WCONOCT_NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return WCONOCT_NULL_HANDLE;
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
      return WCONOCT_NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: metadata is NULL" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

extern "C" 
WconOctHandle wconOct_WCONWorms_data(WconOctError *err,
				    const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
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
    return WCONOCT_NULL_HANDLE;
  }

  if (pAttr != NULL) {
    WconOctHandle result = wrapInternalStoreReference(pAttr);
    Py_DECREF(pAttr);
    if (result == WCONOCT_NULL_HANDLE) {
      cerr << "ERROR: failed to store object reference in wrapper." 
	   << endl;
      *err = FAILED;
      return WCONOCT_NULL_HANDLE;
    } else {
      *err = SUCCESS;
      return result;
    }
  } else {
    cerr << "ERROR: Null handle from data" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

extern "C" 
long wconOct_WCONWorms_num_worms(WconOctError *err,
				 const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
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

extern "C" 
WconOctHandle wconOct_WCONWorms_worm_ids(WconOctError *err,
					const WconOctHandle selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
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
    return WCONOCT_NULL_HANDLE;
  }

  if (pAttr != NULL) {
    if (PyList_Check(pAttr)) {
      WconOctHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == WCONOCT_NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return WCONOCT_NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: worm_ids is not a list object." << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return WCONOCT_NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: Null handle from worm_ids" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}

extern "C" 
WconOctHandle wconOct_WCONWorms_data_as_odict(WconOctError *err,
					     const WconOctHandle selfHandle){
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
  if (*err == FAILED) {
    cerr << "Failed to initialize wrapper library." << endl;
    return WCONOCT_NULL_HANDLE;
  }

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
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
    return WCONOCT_NULL_HANDLE;
  }

  if (pAttr != NULL) {
    if (PyDict_Check(pAttr)) {
      WconOctHandle result = wrapInternalStoreReference(pAttr);
      Py_DECREF(pAttr);
      if (result == WCONOCT_NULL_HANDLE) {
	cerr << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	*err = FAILED;
	return WCONOCT_NULL_HANDLE;
      } else {
	*err = SUCCESS;
	return result;
      }
    } else {
      cerr << "ERROR: data_as_odict is not a dict object." 
	   << endl;
      Py_DECREF(pAttr);
      *err = FAILED;
      return WCONOCT_NULL_HANDLE;
    }
  } else {
    cerr << "ERROR: Null handle from data_as_odict" << endl;
    // No need to DECREF a NULL pAttr
    *err = FAILED;
    return WCONOCT_NULL_HANDLE;
  }
}


