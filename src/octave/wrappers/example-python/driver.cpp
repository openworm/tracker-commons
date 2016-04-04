#include "octaveWconPythonWrapper.h"

#include <iostream>
using namespace std;

void myfoo(int n);
void mybar(int n);

int main(int argc, char **argv) {
  myfoo(2);
  mybar(2);
  foo(2);
  bar(2);
}

void myfoo(int n) {
  cout << "In Local foo " << n << endl;
}

void mybar(int n) {
  cout << "In Local bar " << n*2 << endl;
}
