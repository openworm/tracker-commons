#include "swigWrapper.h"
#include "octaveWconPythonWrapper.h"

#include <stdlib.h>
#include <stdio.h>

void initWrapper() {
  WconOctError err;
  wconOct_initWrapper(&err);
  if (err == FAILED) {
    fprintf(stderr,"Err: initWrapper failed\n");
    exit(-1);
  }
}

int isNullHandle(int handle) {
  WconOctHandle testHandle = (WconOctHandle)handle;
  return (wconOct_isNullHandle(testHandle));
}

int isNoneHandle(int handle) {
  WconOctHandle testHandle = (WconOctHandle)handle;
  return (wconOct_isNoneHandle(testHandle));
}

/* WCONWorms */
int load_from_file(const char *path) {
  WconOctError err;
  WconOctHandle wormHandle;
  wormHandle = wconOct_static_WCONWorms_load_from_file(&err,path);
  if (err == FAILED) {
    fprintf(stderr,"Err: load_from_file failed\n");
    exit(-1);
  } else {
    return (int)wormHandle;
  }
}

void save_to_file(int selfHandle, const char *path) {
  WconOctError err;
  WconOctHandle octSelf = (WconOctHandle)selfHandle;
  wconOct_WCONWorms_save_to_file(&err,octSelf,path,
				 1, 0);
  if (err == FAILED) {
    fprintf(stderr,"Err: save_to_file failed\n");
    exit(-1);
  }
}

int to_canon(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  retHandle = wconOct_WCONWorms_to_canon(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: to_canon failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

int add(int selfHandle, int handle2) {
  WconOctError err;
  WconOctHandle octSelf, octHandle2, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  octHandle2 = (WconOctHandle)handle2;
  retHandle = wconOct_WCONWorms_add(&err,octSelf,octHandle2);
  if (err == FAILED) {
    fprintf(stderr,"Warning: add failed\n");
    /* exit(-1); */
  } else {
    return (int)retHandle;
  }
}

int eq(int selfHandle, int handle2) {
  WconOctError err;
  WconOctHandle octSelf, octHandle2;
  octSelf = (WconOctHandle)selfHandle;
  octHandle2 = (WconOctHandle)handle2;
  int retVal = wconOct_WCONWorms_eq(&err,octSelf,octHandle2);
  if (err == FAILED) {
    fprintf(stderr,"Err: eq failed\n");
    exit(-1);
  } else {
    return retVal;
  }
}

int units(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  retHandle = wconOct_WCONWorms_units(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: units failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

int metadata(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  retHandle = wconOct_WCONWorms_metadata(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: metadata failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

long num_worms(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf;
  octSelf = (WconOctHandle)selfHandle;
  long retValue = wconOct_WCONWorms_num_worms(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: num_worms failed\n");
    exit(-1);
  } else {
    return retValue;
  }
}

int worm_ids(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  retHandle = wconOct_WCONWorms_worm_ids(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: worm_ids failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

int data_as_odict(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf, retHandle;
  octSelf = (WconOctHandle)selfHandle;
  retHandle = wconOct_WCONWorms_data_as_odict(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: data_as_odict failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

/* MeasurementUnit */
int MU_create(const char *unitStr) {
  WconOctError err;
  WconOctHandle retHandle;
  retHandle = wconOct_static_MeasurementUnit_create(&err,unitStr);
  if (err == FAILED) {
    fprintf(stderr,"Err: MU_create failed\n");
    exit(-1);
  } else {
    return (int)retHandle;
  }
}

double MU_to_canon(int selfHandle, double value) {
  WconOctError err;
  WconOctHandle octSelf;
  octSelf = (WconOctHandle)selfHandle;
  double retValue = wconOct_MeasurementUnit_to_canon(&err,octSelf,value);
  if (err == FAILED) {
    fprintf(stderr,"Err: MU_to_canon failed\n");
    exit(-1);
  } else {
    return retValue;
  }
}

double MU_from_canon(int selfHandle, double value) {
  WconOctError err;
  WconOctHandle octSelf;
  octSelf = (WconOctHandle)selfHandle;
  double retValue = wconOct_MeasurementUnit_from_canon(&err,octSelf,value);
  if (err == FAILED) {
    fprintf(stderr,"Err: MU_from_canon failed\n");
    exit(-1);
  } else {
    return retValue;
  }
}

const char *MU_unit_string(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf;
  octSelf = (WconOctHandle)selfHandle;
  char *retValue;
  /* Note: Proper memory allocation expected from invoked method */
  retValue = wconOct_MeasurementUnit_unit_string(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: MU_unit_string failed\n");
    exit(-1);
  } else {
    return retValue;
  }
}

const char *MU_canonical_unit_string(int selfHandle) {
  WconOctError err;
  WconOctHandle octSelf;
  octSelf = (WconOctHandle)selfHandle;
  char *retValue;
  /* Note: Proper memory allocation expected from invoked method */
  retValue = wconOct_MeasurementUnit_canonical_unit_string(&err,octSelf);
  if (err == FAILED) {
    fprintf(stderr,"Err: MU_canonical_unit_string failed\n");
    exit(-1);
  } else {
    return retValue;
  }
}
