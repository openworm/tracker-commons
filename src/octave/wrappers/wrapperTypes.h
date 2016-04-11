#ifndef __WRAPPER_TYPES_H_
#define __WRAPPER_TYPES_H_

/* Because C is a little clunky with bool */
#ifndef __cplusplus
#ifdef __STDC__
#if (defined(__STDC_VERSION__) && (__STDC_VERSION__ >= 199901L))
#include <stdbool.h>
#else
typedef enum __bool {
  false,
  true
} bool;
#endif /* C99 Check */
#endif /* __STDC__ */
#endif /* __cplusplus */

typedef int WconOctHandle;
typedef enum WconOctErrCodes {
  SUCCESS,
  FAILED
} WconOctError;

#endif /* __WRAPPER_TYPES_H_ */
