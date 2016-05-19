#include "octaveWconPythonWrapper.h"

#include <iostream>
using namespace std;

int main(int argc, char **argv) {
  WconOctHandle loadedWCONWormsObjHandle = wconOct_makeNullHandle();
  WconOctHandle canonicalWCONWormsObjHandle = wconOct_makeNullHandle();
  WconOctHandle conflictingWCONWormsObjHandle = wconOct_makeNullHandle();
  WconOctHandle mergeableWCONWormsObjHandle = wconOct_makeNullHandle();

  WconOctHandle mergedHandle=0;
  WconOctHandle handle=0;

  bool conflictLoadFailed = false;
  bool mergeableLoadFailed = false;

  WconOctError err;

  handle = 
    wconOct_static_WCONWorms_load_from_file(&err,
					    "../../../tests/minimax.wcon");
  if (err == FAILED) {
    cerr << "Failed to load" << endl;
    return -1;
  }
  loadedWCONWormsObjHandle = handle;

  wconOct_WCONWorms_save_to_file(&err, 
				 loadedWCONWormsObjHandle,
				 "wrappertest.wcon", 1, 0);
  if (err == FAILED) {
    cout << "Failed to save" << endl;
    // ok to continue with test
  }

  bool convertedToCanonical = true;
  handle = wconOct_WCONWorms_to_canon(&err,
				      loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cout << "Failed to load canonical form" << endl;
    convertedToCanonical = false;
  } else {
    canonicalWCONWormsObjHandle = handle;
  }

  // TODO: Not the best way to test to_canon worked since save_to_file
  //   automatically writes in canonical form. Access an internal data
  //   value instead (e.g. in the minimax file, non-canonical fields
  //   included hours instead of seconds and meters instead of millimeters.
  if (convertedToCanonical) {
    // don't test if canonical form failed to load
    wconOct_WCONWorms_save_to_file(&err,
				   canonicalWCONWormsObjHandle,
				   "wrapperCanonical.wcon", 1, 0);
    if (err == FAILED) {
      cout << "Failed to save" << endl;
      // ok to fail
    }
  }

  handle = 
    wconOct_static_WCONWorms_load_from_file(&err,
					    "extra-test-data/minimax-conflict.wcon");
  if (err == FAILED) {
    cerr << "Error: Bad conflict load." << endl;
    conflictLoadFailed = true;
  } else {
    conflictingWCONWormsObjHandle = handle;
    cout << "||| Conflicting WCON Data loaded as handle " << handle << endl;
  }
  
  handle =
    wconOct_static_WCONWorms_load_from_file(&err,
					    "extra-test-data/minimax-mergeable.wcon");
  if (err == FAILED) {
    cerr << "Error: Bad Mergeable load." << endl;
    mergeableLoadFailed = true;
  } else {
    mergeableWCONWormsObjHandle = handle;
    cout << "||| Mergeable WCON Data loaded as handle " << handle << endl;
  }

  // Test that the canonical versions are compatible with the loaded ones
  //
  // A use-case Note: I'm unhappy with the need to check for errors here.
  //   I'd prefer to directly apply the results of "eq" to a conditional,
  //   but I cannot think of any way to get around this in a C interface.
  //   In a C++ interface, the solution is exception catching.
  //
  // Perhaps the other approach is to semantically treat errors as false.
  //   The error is reported in the wrapper code, but a false value is
  //   always returned. I'm tentatively taking this approach just because
  //   the error check for __eq__ is annoying.
  cout << "||| Compare Equivalent Canonical and Loaded Data" << endl;
  cout << "***** Yes Please *****" << endl;
  if (wconOct_WCONWorms_eq(&err,
			   loadedWCONWormsObjHandle,
			   canonicalWCONWormsObjHandle) == 1) {
    cout << "Yes, handle " << loadedWCONWormsObjHandle
	 << " is equivalent to handle " << canonicalWCONWormsObjHandle << endl;
  } else {
    cout << "No, handle " << loadedWCONWormsObjHandle
	 << " is NOT equivalent to handle " 
	 << canonicalWCONWormsObjHandle << endl;
  }
  if (err == FAILED) {
    cerr << "Prior comparison operation failed. Please ignore." << endl;
  }

  // Merging two equivalent copies of the same data 
  //   should yield the same exact copy
  //
  // Use-case Note:
  //   Failures are expected ... should this be considered
  //   "success"? Currently they are treated as failures,
  //   which I think is the right way to go.
  bool mergeOpFailed = false;
  WconOctHandle result;
  result = wconOct_WCONWorms_add(&err,
				 loadedWCONWormsObjHandle,
				 canonicalWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Handle " << loadedWCONWormsObjHandle
	 << " could not be added with handle " 
	 << canonicalWCONWormsObjHandle << endl;
    mergeOpFailed = true;
  } else {
    mergedHandle = result;
  }

  // Test that they are still the same
  cout << "||| Compare Merged Results of Canonical and Loaded Data" << endl;
  if (!mergeOpFailed) {
    cout << "***** Yes Please *****" << endl;
    if (wconOct_WCONWorms_eq(&err,
			     loadedWCONWormsObjHandle,
			     mergedHandle) == 1) {
      cout << "Yes, handle " << loadedWCONWormsObjHandle
	   << " is equivalent to handle " 
	   << mergedHandle << endl;
    } else {
      cout << "No, handle " << loadedWCONWormsObjHandle
	   << " is NOT equivalent to handle " 
	   << mergedHandle << endl;
    }
    if (err == FAILED) {
      cerr << "Prior comparison operation failed. Please ignore." << endl;
    }
  } else {
    cout << "No Equivalence tests for failed merge operations" << endl;
  }

  // Subsequent merge should fail because of single modified element
  if (!conflictLoadFailed) {
    result = wconOct_WCONWorms_add(&err,
				   loadedWCONWormsObjHandle,
				   conflictingWCONWormsObjHandle);
    if (err == FAILED) {
      cerr << "Error: Handle " << loadedWCONWormsObjHandle
	   << " could not be added with handle " 
	   << conflictingWCONWormsObjHandle << endl;
    }
  } else {
    cout << "No attempt to merge conflicting data since we failed "
	 << "to load the conflict file data." << endl;
  }

  // Subsequent merge should succeed because offending line is removed
  if (!mergeableLoadFailed) {
    mergeOpFailed = false;
    result = wconOct_WCONWorms_add(&err,
				   loadedWCONWormsObjHandle,
				   mergeableWCONWormsObjHandle);
    if (err == FAILED) {
      cerr << "Error: Handle " << loadedWCONWormsObjHandle
	   << " could not be added with handle " 
	   << mergeableWCONWormsObjHandle << endl;
      mergeOpFailed = true;
    } else {
      mergedHandle = result;
      // This is the alternative to the convenience macro
      wconOct_WCONWorms_save_to_file(&err,
				     mergedHandle,"mergeResult.wcon",
				     1, 0);
      if (err == FAILED) {
	cerr << "Failed to save file mergedResult.wcon" << endl;
      }
    }

    // Now they should not be the same
    if (!mergeOpFailed) {
      cout << "***** No Please *****" << endl;
      if (wconOct_WCONWorms_eq(&err,
			       loadedWCONWormsObjHandle,
			       mergedHandle) == 1) {
	cout << "Yes, handle " << loadedWCONWormsObjHandle
	     << " is equivalent to handle " 
	     << mergedHandle << endl;
      } else {
	cout << "No, handle " << loadedWCONWormsObjHandle
	     << " is NOT equivalent to handle " 
	     << mergedHandle << endl;
      }
      if (err == FAILED) {
	cerr << "Prior comparison operation failed. Please ignore." << endl;
      }
    } else {
      cout << "No Equivalence comparison since merge operation failed." 
	   << endl;
    }
  } else {
    cout << "No merge attempt since Mergeable file failed to load"
	 << endl;
  }

  // For now we will just test whether we can get our hands on
  //   the WCONWorms attribute entities
  WconOctUnitsDict *dict = wconOct_WCONWorms_units(&err,
						   loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire units from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else {
    cout << "Got units dictionary pointer"
	 << " from object handle " << loadedWCONWormsObjHandle << endl;
  }

  handle = wconOct_WCONWorms_metadata(&err,
				      loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire metadata from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else if (wconOct_isNoneHandle(handle)) {
    cout << "Got None value for metadata from handle "
	 << loadedWCONWormsObjHandle << endl;
  } else {
    cout << "Got metadata dictionary handle " << handle
	 << " from object handle " << loadedWCONWormsObjHandle << endl;
  }

  int tempHandle;
  // deliberately load a file with NO metadata to test that the None
  //   tag is respected.
  handle = 
    wconOct_static_WCONWorms_load_from_file(&err,
					    "../../../tests/minimal.wcon");
  if (err == FAILED) {
    cerr << "Error: Bad handle value " << handle << endl;
    cerr << "Skipping test for None metadata value" << endl;
    // skip the test if we cannot load the file, it's ok
  } else {
    tempHandle = handle;
    handle = wconOct_WCONWorms_metadata(&err, tempHandle);
    if (err == FAILED) {
      cerr << "Error: Failed to acquire metadata from handle " 
	   << loadedWCONWormsObjHandle << endl;
    } else if (wconOct_isNoneHandle(handle)) {
      cout << "Got None value for metadata from handle "
	   << loadedWCONWormsObjHandle << endl;
    } else {
      cout << "Got metadata dictionary handle " << handle
	   << " from object handle " << loadedWCONWormsObjHandle << endl;
    }
  }

  handle = wconOct_WCONWorms_data(&err, loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire data from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else {
    cout << "Got data (pandas DataFrame) handle " << handle 
	 << " from object handle " << loadedWCONWormsObjHandle << endl;
  }

  long numValue;
  numValue = wconOct_WCONWorms_num_worms(&err,loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire num_worms from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else {
    // expecting 3
    cout << "num_worms = " << numValue
	 << " in handle " << loadedWCONWormsObjHandle << endl;
  }

  handle = wconOct_WCONWorms_worm_ids(&err, loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire worm_ids from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else {
    cout << "Got worm_ids list handle " << handle 
	 << " from object handle " << loadedWCONWormsObjHandle << endl;
  }

  handle = wconOct_WCONWorms_data_as_odict(&err, loadedWCONWormsObjHandle);
  if (err == FAILED) {
    cerr << "Error: Failed to acquire data_as_odict from handle " 
	 << loadedWCONWormsObjHandle << endl;
  } else {
    cout << "Got data_as_odict OrderedDict of pandas DataFrame handle " 
	 << handle 
	 << " from object handle " << loadedWCONWormsObjHandle << endl;
  }


  // Testing MeasurementUnits now
  int hoursUnitHandle = 0; // easy to check against canonical (s)

  hoursUnitHandle = wconOct_static_MeasurementUnit_create(&err, "h");
  if (err == SUCCESS) {
    cout << "Hours Unit object with handle " << hoursUnitHandle << endl;
  } else {
    cerr << "Error: Failed to create a MeasurementUnit instance" << endl;
  }
  double testHourVal = 1.0;
  double testSecondVal = 3600.0;
  cout << testHourVal << " hour(s) is " 
       << wconOct_MeasurementUnit_to_canon(&err, hoursUnitHandle,testHourVal)
       << " second(s)" << endl;
  if (err == FAILED) {
    cerr << "Error: Prior to_canon call failed. Ignore the result." << endl;
  }
  cout << testSecondVal << " second(s) is "
       << wconOct_MeasurementUnit_from_canon(&err, 
					     hoursUnitHandle,testSecondVal)
       << " hour(s)" << endl;
  if (err == FAILED) {
    cerr << "Error: Prior to_canon call failed. Ignore the result." << endl;
  }

  cout << "Hours Unit String [" 
       << wconOct_MeasurementUnit_unit_string(&err, hoursUnitHandle) 
       << "]" << endl;
  if (err == FAILED) {
    cerr << "Error: Prior to_canon call failed. Ignore the result." << endl;
  }

  cout << "Hours Canonical Unit String [" 
       << wconOct_MeasurementUnit_canonical_unit_string(&err, hoursUnitHandle) 
       << "]" << endl;
  if (err == FAILED) {
    cerr << "Error: Prior to_canon call failed. Ignore the result." << endl;
  }
}
