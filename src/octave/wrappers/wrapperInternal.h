#ifndef __WRAPPER_INTERNAL_H_
#define __WRAPPER_INTERNAL_H_
// For internal (non-API) wrapper functionality
#include <Python.h>

#include "wrapperTypes.h"

// Special handle return values
#define NULL_HANDLE -1337
#define WCONOCT_NONE_HANDLE -42

// Internal functions
PyWrapHandle wrapInternalStoreReference(PyObject *pythonRef);
PyObject *wrapInternalGetReference(PyWrapHandle key);

// Internal Checks
void wrapInternalCheckErrorVariable(PyWrapError *err);
#endif /* __WRAPPER_INTERNAL_H_ */
