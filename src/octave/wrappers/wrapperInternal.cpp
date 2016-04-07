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

int wrapInternalStoreReference(PyObject *pythonRef) {
  if (totalActiveRefs >= INT_MAX) {
    // We're already maxed out. Immediately return error.
    cout << "ERROR: Out of room for new Python references" << endl;
    return -1;
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
  return -1;
}

PyObject *wrapInternalGetReference(unsigned int key) {
  unordered_map<unsigned int,PyObject *>::const_iterator result =
    refHandles.find(key);
  if (result != refHandles.end()) {
    return result->second;
  } else {
    return NULL;
  }
}
