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

extern "C" int initOctaveWconPythonWrapper() {
  static bool isInitialized = false;

  PyObject *pErr;

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
      return 0; // failure condition
    }

    wrapperGlobalWCONWormsClassObj = 
      PyObject_GetAttrString(wrapperGlobalModule,"WCONWorms");
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(wrapperGlobalModule);
      Py_XDECREF(wrapperGlobalWCONWormsClassObj);
      return 0; // failure condition
    }

    wrapperGlobalMeasurementUnitClassObj = 
      PyObject_GetAttrString(wrapperGlobalModule,"MeasurementUnit");
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(wrapperGlobalModule);
      Py_XDECREF(wrapperGlobalWCONWormsClassObj);
      Py_XDECREF(wrapperGlobalMeasurementUnitClassObj);
      return 0; // failure condition
    }
    isInitialized = true;
  }

  return 1;
}

// A Tentative C-only interface
//
// static_WCONWorms_load_from_file returns -1 for error
//    else a handle to be used by Octave/C/C++.
extern "C" int static_WCONWorms_load_from_file(const char *wconpath) {
  PyObject *pErr, *pFunc;
  int retCode;

  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Attempting to load WCON file [" << wconpath << "]" << endl;

  pFunc = 
    PyObject_GetAttrString(wrapperGlobalWCONWormsClassObj,"load_from_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    return -1; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  PyUnicode_FromString(wconpath), 
					  NULL);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(pValue);
      Py_XDECREF(pFunc);
      return -1;
    }

    if (pValue != NULL) {
      // do not DECREF pValue until it is no longer referenced in the
      //   wrapper sublayer.
      int result = wrapInternalStoreReference(pValue);
      if (result == -1) {
	cout << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	Py_DECREF(pValue);
      }
      Py_XDECREF(pFunc);
      return result;
    } else {
      cout << "ERROR: Null handle from load_from_file." << endl;
      // No need to DECREF a NULL pValue
      Py_XDECREF(pFunc);
      return -1;
    }
    Py_XDECREF(pFunc);
  } else {
    cout << "ERROR: load_from_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return -1;
  }
}

extern "C" int WCONWorms_save_to_file(const unsigned int selfHandle,
				      const char *output_path,
				      bool pretty_print,
				      bool compressed) {
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  int retCode;
  
  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Attempting to save WCON data to [" << output_path 
       << "] on object handle " << selfHandle << endl;

  WCONWorms_instance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    return -1;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_instance,"save_to_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    return -1; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    // NOTE: Am confused over why boolean literals in Python
    //   must respect reference count rules. Am tentatively
    //   not respecting them because I am afraid if I fail
    //   to DECREF it the correct number of times, things will
    //   blow up in my face. Something to keep an eye on.
    //
    // Keeping above notes, but answer can be found here:
    // http://stackoverflow.com/questions/28576775/python-c-api-boolean-objects

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
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      Py_DECREF(pFunc);
      PyErr_Print();
      return -1;
    } else {
      Py_DECREF(pFunc);
    }
  } else {
    cout << "ERROR: save_to_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return -1;
  }

  return 0;
}

extern "C" int WCONWorms_to_canon(const unsigned int selfHandle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  int retCode;
  
  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Create canonical copy of instance " << selfHandle << endl;

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << selfHandle << endl;
    return -1;
  }

  // to_canon is implemented as an object property and not a function
  pAttr = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"to_canon");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pAttr);
    return -1; // failure condition
  }

  if (pAttr != NULL) {
    // do not DECREF pAttr until it is no longer referenced in the
    //   wrapper sublayer.
    int result = wrapInternalStoreReference(pAttr);
    if (result == -1) {
	cout << "ERROR: failed to store object reference in wrapper." 
	     << endl;
	Py_DECREF(pAttr);
    }
    return result;
  } else {
    cout << "ERROR: Null handle from to_canon" << endl;
    // No need to DECREF a NULL pAttr
    return -1;
  }
}


// NOTE: The add operator had been implemented in Python as instance
//   methods with self-modification (merge) side effects. It is thus
//   not possible to implement something like x = y + z;
//
//  Updated: Apparently I'm wrong. The example tests (in Python) as
//   well as my own tests say that it returns a new object. As such
//   the default behavior is indeed x = y + z with no change to y.

//       We could however cheat for non-modifying operators like eq
//   if it turns out for there to be a use-case where it is better
//   to execute a class method instead.

// TODO: The following wrapper APIs really suggest the wrapper
//   interface is better off in C++ with operator overloading.
int WCONWorms_add(const unsigned int selfHandle, const unsigned int handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  int retCode;
  
  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Add instance " << handle << " into instance "
       << selfHandle << endl;  

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No self object instance using handle "
	 << selfHandle << endl;
    return -1;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No self object instance using handle "
	 << handle << endl;
    return -1;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__add__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    return -1; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    PyObject *pValue;
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  WCONWorms_instance,
					  NULL);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      Py_DECREF(pFunc);
      PyErr_Print();
      return -1;
    } else {
      if (pValue != NULL) {
	// Do not DECREF stored pValue
	int result = wrapInternalStoreReference(pValue);
	if (result == -1) {
	  cout << "ERROR: failed to store object reference in wrapper." 
	       << endl;
	  Py_DECREF(pValue);
	}
	Py_XDECREF(pFunc);
	return result;
      } else {
	cerr << "ERROR: add failed to produce a valid resulting object"
	     << endl;
	// no need to DECREF a NULL pValue
	Py_DECREF(pFunc);
	return -1;
      }
    }
  } else {
    cout << "ERROR: save_to_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return -1;
  }
  return 1; // success
}

// TODO: The tentative need to return an int for error conditions
//   instead of a bool is annoying, but can be addressed by having
//   the error condition be a parameter like in the case of the
//   MPI API implementation.
int WCONWorms_eq(const unsigned int selfHandle, const unsigned int handle) {
  PyObject *WCONWorms_selfInstance=NULL;
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  int retCode;
  
  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Is instance " << selfHandle << " equal to instance "
       << handle << "?" << endl;

  WCONWorms_selfInstance = wrapInternalGetReference(selfHandle);
  if (WCONWorms_selfInstance == NULL) {
    cerr << "ERROR: No self object instance using handle "
	 << selfHandle << endl;
    return -1;
  }

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: No self object instance using handle "
	 << handle << endl;
    return -1;
  }

  pFunc = 
    PyObject_GetAttrString(WCONWorms_selfInstance,"__eq__");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    return -1; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    // NOTE: This may explain why Py_True and Py_False has reference
    //   counts. Because PyObject * is anonymous, and because the
    //   system automatically INCREFs return values. Users are then
    //   expected to DECREF objects acquired from embedding as a 
    //   default. See my notes on this in save_to_file. The way
    //   I see it, if I don't acquire those 2 literals via an interface
    //   call, I should not need to DECREF it. Keep an eye on this.
    //   
    //   Python reference count problems tend to manifest themselves as
    //   a segfault.
    PyObject *retValue;
    retValue = PyObject_CallFunctionObjArgs(pFunc, 
					    WCONWorms_instance,
					    NULL);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      Py_XDECREF(retValue);
      Py_DECREF(pFunc);
      PyErr_Print();
      return -1;
    } else {
      if (PyObject_IsTrue(retValue)) {
	Py_DECREF(retValue);
	return 1; // true
      } else {
	Py_DECREF(retValue);
	return 0; // false
      }
      Py_DECREF(pFunc);
    }
  } else {
    cout << "ERROR: save_to_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return -1;
  }
}
