#include "octaveWconPythonWrapper.h"

#include <Python.h>
#include <iostream>
using namespace std;

extern "C" void initOctaveWconPython() {
  static bool isInitialized = false;
  if (!isInitialized) {
    cout << "Initializing Embedded Python Interpreter" << endl;
    Py_Initialize();
    PyRun_SimpleString("import sys; sys.path.append('pythonLib')\n");
    isInitialized = true;
  }
}

extern "C" void foo(int n) {
  PyObject *pName, *pModule, *pFunc;
  PyObject *pErr;

  initOctaveWconPython();
  cout << "In C++ Wrapper foo with input " << n << endl;
  pName = PyUnicode_FromString("octaveWconTest");
  pModule = PyImport_Import(pName);
  Py_DECREF(pName);
  if (pModule != NULL) {
    pFunc = PyObject_GetAttrString(pModule, "myfoo");
    if ((pFunc != NULL) && (PyCallable_Check(pFunc) == 1)) {
      PyObject_CallFunctionObjArgs(pFunc, PyLong_FromLong(long(n)), NULL);
      // pErr does not need its reference count decremented as we do not own it
      pErr = PyErr_Occurred();
      if (pErr != NULL) {
	PyErr_Print();
      }
    } else {
      Py_XDECREF(pFunc);
      cout << "ERROR: Function foo failed to load" << endl;
    }
  } else {
    Py_XDECREF(pModule);
    cout << "ERROR: Module octaveWconTest not found" << endl;
  }
}

extern "C" void bar(int n) {
  PyObject *pName, *pModule, *pFunc;
  PyObject *pErr;

  initOctaveWconPython();
  cout << "In C++ Wrapper foo with input " << n << endl;
  pName = PyUnicode_FromString("octaveWconTest");
  pModule = PyImport_Import(pName);
  Py_DECREF(pName);
  if (pModule != NULL) {
    pFunc = PyObject_GetAttrString(pModule, "mybar");
    if ((pFunc != NULL) && (PyCallable_Check(pFunc) == 1)) {
      PyObject_CallFunctionObjArgs(pFunc, PyLong_FromLong(long(n)), NULL);
      // pErr does not need its reference count decremented as we do not own it
      pErr = PyErr_Occurred();
      if (pErr != NULL) {
	PyErr_Print();
      }
    } else {
      Py_XDECREF(pFunc);
      cout << "ERROR: Function bar failed to load" << endl;
    }
  } else {
    Py_XDECREF(pModule);
    cout << "ERROR: Module octaveWconTest not found" << endl;
  }
}
