#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_

#include "wrapperTypes.h"

/* Convenience Macros */
#define RunRetCheckAndRespond(errvar,ret,failResponse,func,...)	\
  ret = func(&errvar,__VA_ARGS__); \
  if (err == FAILED) { \
    failResponse; \
  }

#define RunVoidCheckAndRespond(errvar,failResponse,func,...)	\
  func(&errvar,__VA_ARGS__); \
  if (errvar == FAILED) { \
    failResponse; \
  }

/* Support Methods */
extern "C" void initOctaveWconPythonWrapper(PyWrapError *err);
extern "C" bool isNullHandle(PyWrapHandle handle);
extern "C" PyWrapHandle makeNullHandle();
extern "C" bool isNoneHandle(PyWrapHandle handle);

/* WCONWorms */
extern "C" PyWrapHandle static_WCONWorms_load_from_file(PyWrapError *err,
							const char *wconpath);
extern "C" void WCONWorms_save_to_file(PyWrapError *err,
				       const PyWrapHandle selfHandle,
				       const char *output_path,
				       bool pretty_print=false,
				       bool compressed=false);
extern "C" PyWrapHandle WCONWorms_to_canon(PyWrapError *err,
					   const PyWrapHandle selfHandle);
extern "C" PyWrapHandle WCONWorms_add(PyWrapError *err,
				      const PyWrapHandle selfHandle,
				      const PyWrapHandle handle);
extern "C" bool WCONWorms_eq(PyWrapError *err,
			     const PyWrapHandle selfHandle,
			     const PyWrapHandle handle);
extern "C" PyWrapHandle WCONWorms_units(PyWrapError *err,
					const PyWrapHandle selfHandle);
extern "C" PyWrapHandle WCONWorms_metadata(PyWrapError *err,
					   const PyWrapHandle selfHandle);
extern "C" PyWrapHandle WCONWorms_data(PyWrapError *err,
				       const PyWrapHandle selfHandle);
extern "C" long WCONWorms_num_worms(PyWrapError *err,
				    const PyWrapHandle selfHandle);
extern "C" PyWrapHandle WCONWorms_worm_ids(PyWrapError *err,
					   const PyWrapHandle selfHandle);
extern "C" PyWrapHandle WCONWorms_data_as_odict(PyWrapError *err,
						const PyWrapHandle selfHandle);

/* MeasurementUnit */
extern "C" PyWrapHandle static_MeasurementUnit_create(PyWrapError *err,
						      const char *unitStr);
extern "C" double MeasurementUnit_to_canon(PyWrapError *err,
					   const PyWrapHandle selfHandle,
					   const double val);
extern "C" double MeasurementUnit_from_canon(PyWrapError *err,
					     const PyWrapHandle selfHandle,
					     const double val);
extern "C" const char *MeasurementUnit_unit_string(PyWrapError *err,
						   const PyWrapHandle selfHandle);
extern "C" const char *MeasurementUnit_canonical_unit_string(PyWrapError *err,
							     const PyWrapHandle selfHandle);
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
