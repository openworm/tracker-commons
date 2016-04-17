#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_

#include "wrapperTypes.h"

/* Support Methods */
extern "C" void wconOct_initWrapper(WconOctError *err);
extern "C" bool wconOct_isNullHandle(WconOctHandle handle);
extern "C" WconOctHandle wconOct_makeNullHandle();
extern "C" bool wconOct_isNoneHandle(WconOctHandle handle);

/* WCONWorms */
extern "C" 
WconOctHandle wconOct_static_WCONWorms_load_from_file(WconOctError *err,
						     const char *wconpath);
extern "C" 
void wconOct_WCONWorms_save_to_file(WconOctError *err,
				    const WconOctHandle selfHandle,
				    const char *output_path,
				    bool pretty_print=false,
				    bool compressed=false);
extern "C" 
WconOctHandle wconOct_WCONWorms_to_canon(WconOctError *err,
					const WconOctHandle selfHandle);
extern "C" WconOctHandle wconOct_WCONWorms_add(WconOctError *err,
					      const WconOctHandle selfHandle,
					      const WconOctHandle handle);
extern "C" bool wconOct_WCONWorms_eq(WconOctError *err,
				     const WconOctHandle selfHandle,
				     const WconOctHandle handle);
extern "C" WconOctHandle wconOct_WCONWorms_units(WconOctError *err,
						const WconOctHandle selfHandle);
extern "C" 
WconOctHandle wconOct_WCONWorms_metadata(WconOctError *err,
					const WconOctHandle selfHandle);
extern "C" WconOctHandle wconOct_WCONWorms_data(WconOctError *err,
					       const WconOctHandle selfHandle);
extern "C" long wconOct_WCONWorms_num_worms(WconOctError *err,
					    const WconOctHandle selfHandle);
extern "C" 
WconOctHandle wconOct_WCONWorms_worm_ids(WconOctError *err,
					const WconOctHandle selfHandle);
extern "C" 
WconOctHandle wconOct_WCONWorms_data_as_odict(WconOctError *err,
					     const WconOctHandle selfHandle);

/* MeasurementUnit */
extern "C" 
WconOctHandle wconOct_static_MeasurementUnit_create(WconOctError *err,
						   const char *unitStr);
extern "C" 
double wconOct_MeasurementUnit_to_canon(WconOctError *err,
					const WconOctHandle selfHandle,
					const double val);
extern "C" 
double wconOct_MeasurementUnit_from_canon(WconOctError *err,
					  const WconOctHandle selfHandle,
					  const double val);
extern "C" 
const char *wconOct_MeasurementUnit_unit_string(WconOctError *err,
						const WconOctHandle selfHandle);
extern "C" 
const char *wconOct_MeasurementUnit_canonical_unit_string(WconOctError *err,
							  const WconOctHandle selfHandle);
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
