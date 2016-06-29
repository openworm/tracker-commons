#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_

#include "wrapperTypes.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/* Support Methods */
void wconOct_initWrapper(WconOctError *err);
int wconOct_isNullHandle(WconOctHandle handle);
WconOctHandle wconOct_makeNullHandle();
int wconOct_isNoneHandle(WconOctHandle handle);

/* WCONWorms */
WconOctHandle wconOct_static_WCONWorms_load_from_file(WconOctError *err,
						     const char *wconpath);
void wconOct_WCONWorms_save_to_file(WconOctError *err,
				    const WconOctHandle selfHandle,
				    const char *output_path,
				    int pretty_print,
				    int compressed);

WconOctHandle wconOct_WCONWorms_to_canon(WconOctError *err,
					const WconOctHandle selfHandle);
WconOctHandle wconOct_WCONWorms_add(WconOctError *err,
					      const WconOctHandle selfHandle,
					      const WconOctHandle handle);
int wconOct_WCONWorms_eq(WconOctError *err,
				     const WconOctHandle selfHandle,
				     const WconOctHandle handle);
WconOctUnitsDict *wconOct_WCONWorms_units(WconOctError *err,
					  const WconOctHandle selfHandle);
WconOctHandle wconOct_WCONWorms_metadata(WconOctError *err,
					const WconOctHandle selfHandle);
WconOctHandle wconOct_WCONWorms_data(WconOctError *err,
					       const WconOctHandle selfHandle);
long wconOct_WCONWorms_num_worms(WconOctError *err,
					    const WconOctHandle selfHandle);
WconOctHandle wconOct_WCONWorms_worm_ids(WconOctError *err,
					const WconOctHandle selfHandle);
WconOctHandle wconOct_WCONWorms_data_as_odict(WconOctError *err,
					     const WconOctHandle selfHandle);

/* MeasurementUnit */
WconOctHandle wconOct_static_MeasurementUnit_create(WconOctError *err,
						   const char *unitStr);
double wconOct_MeasurementUnit_to_canon(WconOctError *err,
					const WconOctHandle selfHandle,
					const double val);
double wconOct_MeasurementUnit_from_canon(WconOctError *err,
					  const WconOctHandle selfHandle,
					  const double val);
const char *wconOct_MeasurementUnit_unit_string(WconOctError *err,
						const WconOctHandle selfHandle);
const char *wconOct_MeasurementUnit_canonical_unit_string(WconOctError *err,
							  const WconOctHandle selfHandle);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
