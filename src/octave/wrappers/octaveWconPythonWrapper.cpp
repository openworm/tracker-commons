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
extern "C" void wconOct_initWrapper(WconOctError *err) {
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

extern "C" int wconOct_isNullHandle(WconOctHandle handle) {
  if (handle == WCONOCT_NULL_HANDLE) {
    return 1;
  } else {
    return 0;
  }
}

extern "C" WconOctHandle wconOct_makeNullHandle() {
  return WCONOCT_NULL_HANDLE;
}

extern "C" int wconOct_isNoneHandle(WconOctHandle handle) {
  if (handle == WCONOCT_NONE_HANDLE) {
    return 1;
  } else {
    return 0;
  }
}

