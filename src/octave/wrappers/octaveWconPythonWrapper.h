#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_

#include "wrapperTypes.h"

/* Support Methods */
extern "C" void wconOct_initWrapper(PyWrapError *err);
extern "C" bool wconOct_isNullHandle(PyWrapHandle handle);
extern "C" PyWrapHandle wconOct_makeNullHandle();
extern "C" bool wconOct_isNoneHandle(PyWrapHandle handle);

/* WCONWorms */
extern "C" 
PyWrapHandle wconOct_static_WCONWorms_load_from_file(PyWrapError *err,
						     const char *wconpath);
extern "C" 
void wconOct_WCONWorms_save_to_file(PyWrapError *err,
				    const PyWrapHandle selfHandle,
				    const char *output_path,
				    bool pretty_print=false,
				    bool compressed=false);
extern "C" 
PyWrapHandle wconOct_WCONWorms_to_canon(PyWrapError *err,
					const PyWrapHandle selfHandle);
extern "C" PyWrapHandle wconOct_WCONWorms_add(PyWrapError *err,
					      const PyWrapHandle selfHandle,
					      const PyWrapHandle handle);
extern "C" bool wconOct_WCONWorms_eq(PyWrapError *err,
				     const PyWrapHandle selfHandle,
				     const PyWrapHandle handle);
extern "C" PyWrapHandle wconOct_WCONWorms_units(PyWrapError *err,
						const PyWrapHandle selfHandle);
extern "C" 
PyWrapHandle wconOct_WCONWorms_metadata(PyWrapError *err,
					const PyWrapHandle selfHandle);
extern "C" PyWrapHandle wconOct_WCONWorms_data(PyWrapError *err,
					       const PyWrapHandle selfHandle);
extern "C" long wconOct_WCONWorms_num_worms(PyWrapError *err,
					    const PyWrapHandle selfHandle);
extern "C" 
PyWrapHandle wconOct_WCONWorms_worm_ids(PyWrapError *err,
					const PyWrapHandle selfHandle);
extern "C" 
PyWrapHandle wconOct_WCONWorms_data_as_odict(PyWrapError *err,
					     const PyWrapHandle selfHandle);

/* MeasurementUnit */
extern "C" 
PyWrapHandle wconOct_static_MeasurementUnit_create(PyWrapError *err,
						   const char *unitStr);
extern "C" 
double wconOct_MeasurementUnit_to_canon(PyWrapError *err,
					const PyWrapHandle selfHandle,
					const double val);
extern "C" 
double wconOct_MeasurementUnit_from_canon(PyWrapError *err,
					  const PyWrapHandle selfHandle,
					  const double val);
extern "C" 
const char *wconOct_MeasurementUnit_unit_string(PyWrapError *err,
						const PyWrapHandle selfHandle);
extern "C" 
const char *wconOct_MeasurementUnit_canonical_unit_string(PyWrapError *err,
							  const PyWrapHandle selfHandle);
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
