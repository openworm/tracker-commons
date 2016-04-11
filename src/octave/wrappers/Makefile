CPP=g++
AR=ar
CFLAGS=-std=gnu++11

PYTHON_VER=3.5
PYTHON_CONFIG=python${PYTHON_VER}-config

# For some bizarre reasons, config --ldflags will work for
#  the Mac the first time, and then subsequently fail to
#  include the path to the libraries
PYTHON_PREFIX=$(shell ${PYTHON_CONFIG} --prefix)
PYTHON_CFLAGS=$(shell ${PYTHON_CONFIG} --cflags)
PYTHON_LDFLAGS=$(shell ${PYTHON_CONFIG} --ldflags)

WRAPPER_OBJS=octaveWconPythonWrapper.o wrapperInternal.o
COMMON_HEADERS=wrapperTypes.h

WRAPPER_LIB=libWconOct.a
WRAPPER_LIB_LDFLAGS=-L. -lWconOct

all: driver ${WRAPPER_LIB}

driver: driver.o ${WRAPPER_LIB}
	$(CPP) -o driver driver.o ${WRAPPER_LIB_LDFLAGS} ${PYTHON_LDFLAGS}

libWconOct.a: ${WRAPPER_OBJS}
	$(AR) rcs libWconOct.a ${WRAPPER_OBJS}

driver.o: driver.cpp
	$(CPP) $(CFLAGS) -c driver.cpp

%.o: %.cpp %.h ${COMMON_HEADERS}
	$(CPP) $(CFLAGS) -c $< ${PYTHON_CFLAGS}

clean:
	rm -f *~ *.o driver
