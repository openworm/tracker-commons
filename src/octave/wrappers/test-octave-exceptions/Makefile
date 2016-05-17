all: exception_order.oct

exception_order.oct:	exception_order.cpp 
	mkoctfile -o exception_order exception_order.cpp

exception_order.cpp: exception.i
	swig -octave -c++ -o exception_order.cpp exception.i

clean:
	rm -f *~ *.o exception_order.oct exception_order.cpp

