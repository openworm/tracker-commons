#ifndef __OCTAVE_WCON_PYTHON_WRAPPER_H_
#define __OCTAVE_WCON_PYTHON_WRAPPER_H_
extern "C" int initOctaveWconPythonWrapper();

extern "C" int static_WCONWorms_load_from_file(const char *wconpath);
extern "C" int WCONWorms_save_to_file(unsigned int handle,
				      const char *output_path,
				      bool pretty_print=false);
#endif /* __OCTAVE_WCON_PYTHON_WRAPPER_H_ */
