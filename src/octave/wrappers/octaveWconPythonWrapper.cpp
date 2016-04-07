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
      return -1;
    }

    if (pValue != NULL) {
      // do not DECREF pValue until it is no longer referenced in the
      //   wrapper sublayer.
      int result = wrapInternalStoreReference(pValue);
      if (result == -1) {
	cout << "ERROR: failed to store object reference in wrapper." 
	     << endl;
      }
      return result;
    } else {
      cout << "ERROR: Null handle from load_from_file." << endl;
      return -1;
    }
    Py_XDECREF(pFunc);
  } else {
    cout << "ERROR: load_from_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return -1;
  }
}

extern "C" int WCONWorms_save_to_file(unsigned int handle,
				      const char *output_path,
				      bool pretty_print) {
  PyObject *WCONWorms_instance=NULL;
  PyObject *pErr, *pFunc;
  int retCode;
  
  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return -1;
  }
  cout << "Attempting to save WCON data to [" << output_path 
       << "] on object handle " << handle << endl;

  WCONWorms_instance = wrapInternalGetReference(handle);
  if (WCONWorms_instance == NULL) {
    cerr << "ERROR: Failed to acquire object instance using handle "
	 << handle << endl;
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
    PyObject *toPP = Py_False;
    if (pretty_print) {
      toPP = Py_True;
    }
    PyObject_CallFunctionObjArgs(pFunc, 
				 PyUnicode_FromString(output_path),
				 toPP,
				 NULL);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      return -1;
    }
  }

  return 0;
}
