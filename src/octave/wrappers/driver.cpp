#include "octaveWconPythonWrapper.h"

#include <iostream>
using namespace std;

int main(int argc, char **argv) {
  initOctaveWconPythonWrapper();
  static_WCONWorms_load_from_file("../../../tests/minimax.wcon");
}
