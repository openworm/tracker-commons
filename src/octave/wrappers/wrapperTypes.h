#ifndef __WRAPPER_TYPES_H_
#define __WRAPPER_TYPES_H_

typedef int WconOctHandle;
typedef enum WconOctErrCodes {
  SUCCESS,
  FAILED
} WconOctError;
typedef struct unitskeyValuePair {
  char *key;
  WconOctHandle value;
} WconOctUnitsKeyValue;
typedef struct unitsDictStruct {
  int numElements;
  WconOctUnitsKeyValue *unitsDict;
} WconOctUnitsDict;
#endif /* __WRAPPER_TYPES_H_ */
