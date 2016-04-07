#include "octaveWconPythonWrapper.h"

#include <iostream>
using namespace std;

int main(int argc, char **argv) {
  int loadedWCONWormsObjHandle = 0;
  int canonicalWCONWormsObjHandle = 0;
  int conflictingWCONWormsObjHandle = 0;
  int mergeableWCONWormsObjHandle = 0;

  int mergedHandle=0;
  int handle=0;
  int result=0;

  bool conflictLoadFailed = false;
  bool mergeableLoadFailed = false;

  handle = static_WCONWorms_load_from_file("../../../tests/minimax.wcon");
  if (handle < 0) {
    cerr << "Error: Bad handle value " << handle << endl;
    // if we cannot even load the most basic data, punt.
    return -1; 
  }
  loadedWCONWormsObjHandle = handle;

  // test with pretty_print
  result = WCONWorms_save_to_file((unsigned int)loadedWCONWormsObjHandle,
				  "wrappertest.wcon",true);
  if (result < 0) {
    // ok to fail
    cerr << "Error: save_to_file failed on object handle " << handle << endl;
  }

  // test to_canon
  handle = WCONWorms_to_canon((unsigned int)loadedWCONWormsObjHandle);
  if (handle < 0) {
    // Failure to load canonical data should result in a punt as well
    cerr << "Error: Bad handle value " << handle << endl;
    return -1;
  }
  canonicalWCONWormsObjHandle = handle;

  // TODO: Not the best way to test to_canon worked since save_to_file
  //   automatically writes in canonical form. Access an internal data
  //   value instead (e.g. in the minimax file, non-canonical fields
  //   included hours instead of seconds and meters instead of millimeters.
  result = WCONWorms_save_to_file((unsigned int)canonicalWCONWormsObjHandle,
				  "wrapperCanonical.wcon",true);
  if (result < 0) {
    // ok to fail
    cerr << "Error: save_to_file failed on object handle " << handle << endl;
  }

  // Load data for subsequent tests
  handle = 
    static_WCONWorms_load_from_file("extra-test-data/minimax-conflict.wcon");
  if (handle < 0) {
    // Failure is considered conditional and turns off subsequent
    //   dependent tests
    cerr << "Error: Bad handle value " << handle << endl;
    conflictLoadFailed = true;
  }
  conflictingWCONWormsObjHandle = handle;
  cout << "||| Conflicting WCON Data loaded as handle " << handle << endl;
  
  handle = 
    static_WCONWorms_load_from_file("extra-test-data/minimax-mergeable.wcon");
  if (handle < 0) {
    // Failure is considered conditional and turns off subsequent
    //   dependent tests
    cerr << "Error: Bad handle value " << handle << endl;
    mergeableLoadFailed = true;
  }
  mergeableWCONWormsObjHandle = handle;
  cout << "||| Mergeable WCON Data loaded as handle " << handle << endl;

  // Test that the canonical versions are compatible with the loaded ones
  cout << "||| Compare Equivalent Canonical and Loaded Data" << endl;
  cout << "***** Yes Please *****" << endl;
  if (WCONWorms_eq(loadedWCONWormsObjHandle,
		   canonicalWCONWormsObjHandle) == 1) {
    cout << "Yes, handle " << loadedWCONWormsObjHandle
	 << " is equivalent to handle " << canonicalWCONWormsObjHandle << endl;
  } else {
    cout << "No, handle " << loadedWCONWormsObjHandle
	 << " is NOT equivalent to handle " 
	 << canonicalWCONWormsObjHandle << endl;
  }

  // Merging two equivalent copies of the same data 
  //   should yield the same exact copy
  bool mergeOpFailed = false;
  result = WCONWorms_add(loadedWCONWormsObjHandle,
			 canonicalWCONWormsObjHandle);
  if (result == -1) {
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
    if (WCONWorms_eq(loadedWCONWormsObjHandle,
		     mergedHandle) == 1) {
      cout << "Yes, handle " << loadedWCONWormsObjHandle
	   << " is equivalent to handle " 
	   << mergedHandle << endl;
    } else {
      cout << "No, handle " << loadedWCONWormsObjHandle
	   << " is NOT equivalent to handle " 
	   << mergedHandle << endl;
    }
  } else {
    cout << "No Equivalence tests for failed merge operations" << endl;
  }

  // Subsequent merge should fail because of single modified element
  if (!conflictLoadFailed) {
    result = WCONWorms_add(loadedWCONWormsObjHandle,
			   conflictingWCONWormsObjHandle);
    if (result == -1) {
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
    result = WCONWorms_add(loadedWCONWormsObjHandle,
			   mergeableWCONWormsObjHandle);
    if (result == -1) {
      cerr << "Error: Handle " << loadedWCONWormsObjHandle
	   << " could not be added with handle " 
	   << mergeableWCONWormsObjHandle << endl;
      mergeOpFailed = true;
    } else {
      mergedHandle = result;
      WCONWorms_save_to_file(mergedHandle,"mergeResult.wcon",true);
    }

    // Now they should not be the same
    if (!mergeOpFailed) {
      cout << "***** No Please *****" << endl;
      if (WCONWorms_eq(loadedWCONWormsObjHandle,
		       mergedHandle) == 1) {
	cout << "Yes, handle " << loadedWCONWormsObjHandle
	     << " is equivalent to handle " 
	     << mergedHandle << endl;
      } else {
	cout << "No, handle " << loadedWCONWormsObjHandle
	     << " is NOT equivalent to handle " 
	     << mergedHandle << endl;
      }
    } else {
      cout << "No Equivalence comparison since merge operation failed." 
	   << endl;
    }
  } else {
    cout << "No merge attempt since Mergeable file failed to load"
	 << endl;
  }

  // Testing MeasurementUnits now
  int hoursUnitHandle = 0; // easy to check against canonical (s)

  hoursUnitHandle = static_MeasurementUnit_create("h");
  if (hoursUnitHandle != -1) {
    cout << "Hours Unit object with handle " << hoursUnitHandle << endl;
  } else {
    cerr << "Error: Failed to create a MeasurementUnit instance" << endl;
  }
  double testHourVal = 1.0;
  double testSecondVal = 3600.0;
  cout << testHourVal << " hour(s) is " 
       << MeasurementUnit_to_canon(hoursUnitHandle,testHourVal)
       << " second(s)" << endl;
  cout << testSecondVal << " second(s) is "
       << MeasurementUnit_from_canon(hoursUnitHandle,testSecondVal)
       << " hour(s)" << endl;

  cout << "Hours Unit String [" 
       << MeasurementUnit_unit_string(hoursUnitHandle) << "]" << endl;
  cout << "Hours Canonical Unit String [" 
       << MeasurementUnit_canonical_unit_string(hoursUnitHandle) 
       << "]" << endl;
}
