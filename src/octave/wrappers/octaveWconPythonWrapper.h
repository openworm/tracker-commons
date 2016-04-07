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
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
