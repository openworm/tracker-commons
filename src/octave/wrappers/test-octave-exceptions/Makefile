all: exception_order.oct minimal_exception.oct

minimal: minimal_exception.oct

c: c_exception.oct

exception_order.oct:	exception_order.cpp 
	mkoctfile -o exception_order exception_order.cpp

exception_order.cpp: test_exception.i
	swig -octave -c++ -o exception_order.cpp test_exception.i

minimal_exception.oct: minimal_exception.cpp
	mkoctfile -o minimal_exception minimal_exception.cpp

minimal_exception.cpp: minimal_cpp.i
	swig -octave -c++ -o minimal_exception.cpp minimal_cpp.i

c_exception.oct: c_exception.cpp
	mkoctfile -o c_exception c_exception.cpp

c_exception.cpp: minimal_c.i
	swig -octave -o c_exception.cpp minimal_c.i

clean:
	rm -f *~ *.o *.oct *.cpp

