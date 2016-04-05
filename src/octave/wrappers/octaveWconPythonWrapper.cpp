#include "octaveWconPythonWrapper.h"

#include <Python.h>
#include <iostream>
using namespace std;

PyObject *wrapperGlobalModule=NULL;
PyObject *wrapperGlobalWCONWormsClassObj=NULL;
PyObject *wrapperGlobalMeasurementUnitClassObj=NULL;

extern "C" int initOctaveWconPythonWrapper() {
  static bool isInitialized = false;

  PyObject *pErr;

  if (!isInitialized) {
    cout << "Initializing Embedded Python Interpreter" << endl;
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
extern "C" int static_WCONWorms_load_from_file(const char *wconpath) {
  PyObject *pErr, *pFunc;
  PyObject *pValue=NULL;
  int retCode;

  retCode = initOctaveWconPythonWrapper();
  if (retCode == 0) {
    cout << "Failed to initialize wrapper library." << endl;
    return 0;
  }
  cout << "Attempting to load WCON file [" << wconpath << "]" << endl;

  pFunc = 
    PyObject_GetAttrString(wrapperGlobalWCONWormsClassObj,"load_from_file");
  pErr = PyErr_Occurred();
  if (pErr != NULL) {
    PyErr_Print();
    Py_XDECREF(pFunc);
    return 0; // failure condition
  }

  if (PyCallable_Check(pFunc) == 1) {
    // pValue is expected to be an instance of WCONWorms but
    //   we'll ignore that for now.
    pValue = PyObject_CallFunctionObjArgs(pFunc, 
					  PyUnicode_FromString(wconpath), 
					  NULL);
    pErr = PyErr_Occurred();
    if (pErr != NULL) {
      PyErr_Print();
      Py_XDECREF(pValue);
      return 0;
    }
    Py_XDECREF(pFunc);
  } else {
    cout << "ERROR: load_from_file not a callable function" << endl;
    Py_XDECREF(pFunc);
    return 0;
  }

  // So I'll have to find an elegant way to hang on to the pValue
  // PyObject reference returned from a successful call, and return
  // through the interface to Octave a handler it can use to reference
  // the object returned.
  
  // For now, to test whether we can actually use the object returned,
  // we're gonna immediately attempt to write it back out in a new test
  // wcon file. Am gonna be lazy about control flow error checks for now.
  if (pValue != NULL) {
    cout << "For Testing Only - writing the loaded file back out." << endl;
    pFunc =
      PyObject_GetAttrString(pValue,"save_to_file");
    if (PyCallable_Check(pFunc) == 1) {
      PyObject_CallFunctionObjArgs(pFunc,
				   PyUnicode_FromString("./wrappertest.wcon"),
				   Py_True,
				   NULL);
      pErr = PyErr_Occurred();
      if (pErr != NULL) {
	PyErr_Print();
      }
    } else {
      cout << "ERROR: save_to_file cannot be invoked from object." << endl;
      return 0;
    }
  } else {
    cout << "ERROR: For some reason, return value from loading was NULL." << endl;
    return 0;
  }
  
  return 1;
}
