#include "wrapperInternal.h"

#include <iostream>
#include <unordered_map>
#include <utility>

#include <limits.h>
#include <stdlib.h> // for srand and rand
using namespace std;

// Note that while the theoretical range on the key
//   is of the order of unsigned int, the fact that
//   we need to indicate error conditions means
//   that the effective range of the keys are from
//   0..INT_MAX
unordered_map<unsigned int, PyObject *> refHandles;
unsigned int totalActiveRefs = 0;
// The more I think about it, the more I think
//   there has to be a better way than using a
//   random key or a sequential key, both of
//   which have their problems.

WconOctHandle wrapInternalStoreReference(PyObject *pythonRef) {

  if (pythonRef == NULL) {
    cerr << "ERROR: NULL reference object supplied" << endl;
    return WCONOCT_NULL_HANDLE;
  }

  if (totalActiveRefs >= INT_MAX) {
    // We're already maxed out. Immediately return error.
    cerr << "ERROR: Out of room for new Python references" << endl;
    return WCONOCT_NULL_HANDLE;
  }

  // get a random number
  unsigned int randkey;
  randkey = rand()%INT_MAX;
  // assume rand implementation is good
  unsigned int count = INT_MAX;
  while (count >= 0) {
    // try key against hash table
    unordered_map<unsigned int,PyObject *>::const_iterator result =
      refHandles.find(randkey);
    if (result == refHandles.end()) {
      pair<unsigned int,PyObject *> refKeyPair(randkey,pythonRef);
      refHandles.insert(refKeyPair);
      totalActiveRefs++;
      return randkey;
    }
    count--;
  }
  // Error condition
  return WCONOCT_NULL_HANDLE;
}

PyObject *wrapInternalGetReference(WconOctHandle handle) {
  unsigned int key = 0;

  if (handle == WCONOCT_NULL_HANDLE) {
    cerr << "ERROR: Trying to access a NULL handle." << endl;
    return NULL;
  } else if (handle == WCONOCT_NONE_HANDLE) {
    cerr << "ERROR: Py_None is not a valid wrapper access object." << endl;
    return NULL;
  } else {
    key = (unsigned int)handle;
  }

  unordered_map<unsigned int,PyObject *>::const_iterator result =
    refHandles.find(key);
  if (result != refHandles.end()) {
    return result->second;
  } else {
    return NULL;
  }
}

void wrapInternalCheckErrorVariable(WconOctError *err) {
  // passing a NULL value is strictly forbidden. Shut the entire
  // code down if this is detected.
  if (err == NULL) {
    cerr << "ERROR: Error return variable may not be NULL. Shutting Down!"
	 << endl;
  }
}
