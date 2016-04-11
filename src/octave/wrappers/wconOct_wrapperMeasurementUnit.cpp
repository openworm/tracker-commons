#include "octaveWconPythonWrapper.h"

#include <Python.h>

#include <iostream>
using namespace std;

#include "wrapperInternal.h"

extern PyObject *wrapperGlobalMeasurementUnitClassObj;

// *****************************************************************
// ********************** MeasurementUnit Class
extern "C" 
PyWrapHandle wconOct_static_MeasurementUnit_create(PyWrapError *err,
						   const char *unitStr) {
  PyObject *pErr, *pFunc;

  wconOct_initWrapper(err);
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

extern "C" 
double wconOct_MeasurementUnit_to_canon(PyWrapError *err,
					const PyWrapHandle selfHandle,
					const double val) {
  PyObject *MeasurementUnit_instance=NULL;
  PyObject *pErr, *pFunc;

  wconOct_initWrapper(err);
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

extern "C" 
double wconOct_MeasurementUnit_from_canon(PyWrapError *err,
					  const PyWrapHandle selfHandle,
					  const double val) {
  PyObject *MeasurementUnit_instance=NULL;
  PyObject *pErr, *pFunc;

  wconOct_initWrapper(err);
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
const char *wconOct_MeasurementUnit_unit_string(PyWrapError *err,
						const PyWrapHandle selfHandle) {
  PyObject *MeasurementUnit_selfInstance=NULL;
  PyObject *pErr, *pAttr;

  wconOct_initWrapper(err);
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
const char *wconOct_MeasurementUnit_canonical_unit_string(PyWrapError *err,
							  const PyWrapHandle selfHandle) {
  PyObject *MeasurementUnit_selfInstance=NULL;
  PyObject *pErr, *pAttr;
  
  wconOct_initWrapper(err);
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
