#ifndef __WRAPPER_INTERNAL_H_
#define __WRAPPER_INTERNAL_H_
// For internal (non-API) wrapper functionality
#include <Python.h>

// Internal functions
int wrapInternalStoreReference(PyObject *pythonRef);
PyObject *wrapInternalGetReference(unsigned int key);
#endif /* __WRAPPER_INTERNAL_H_ */
