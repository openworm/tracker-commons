#include "octaveWconPythonWrapper.h"

#include <iostream>
using namespace std;

int main(int argc, char **argv) {
  int handle=0;
  int result=0;
  initOctaveWconPythonWrapper();
  handle = static_WCONWorms_load_from_file("../../../tests/minimax.wcon");
  if (handle < 0) {
    cerr << "Error: Bad handle value " << handle << endl;
  }
  result = WCONWorms_save_to_file((unsigned int)handle,
				  "wrappertest.wcon",true);
  if (result < 0) {
    cerr << "Error: save_to_file failed on object handle " << handle << endl;
  }
}
