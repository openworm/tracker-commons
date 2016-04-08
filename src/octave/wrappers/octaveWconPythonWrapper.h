#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_
extern "C" int initOctaveWconPythonWrapper();

extern "C" int static_WCONWorms_load_from_file(const char *wconpath);
extern "C" int WCONWorms_save_to_file(const unsigned int selfHandle,
				      const char *output_path,
				      bool pretty_print=false,
				      bool compressed=false);
extern "C" int WCONWorms_to_canon(const unsigned int selfHandle);
extern "C" int WCONWorms_add(const unsigned int selfHandle,
			     const unsigned int handle);
extern "C" int WCONWorms_eq(const unsigned int selfHandle,
			    const unsigned int handle);
extern "C" int WCONWorms_units(const unsigned int selfHandle);
extern "C" int WCONWorms_metadata(const unsigned int selfHandle);
extern "C" int WCONWorms_data(const unsigned int selfHandle);
extern "C" long WCONWorms_num_worms(const unsigned int selfHandle);
extern "C" int WCONWorms_worm_ids(const unsigned int selfHandle);
extern "C" int WCONWorms_data_as_odict(const unsigned int selfHandle);

extern "C" int static_MeasurementUnit_create(const char *unitStr);
extern "C" double MeasurementUnit_to_canon(const unsigned int selfHandle,
					const double val);
extern "C" double MeasurementUnit_from_canon(const unsigned int selfHandle,
					  const double val);
extern "C" const char *MeasurementUnit_unit_string(const unsigned int selfHandle);
extern "C" const char *MeasurementUnit_canonical_unit_string(const unsigned int selfHandle);
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
