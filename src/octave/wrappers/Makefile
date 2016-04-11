CPP=g++
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

all: driver

driver: ${WRAPPER_OBJS} driver.o
	$(CPP) -o driver ${WRAPPER_OBJS} driver.o ${PYTHON_LDFLAGS}

driver.o: driver.cpp
	$(CPP) $(CFLAGS) -c driver.cpp

%.o: %.cpp %.h ${COMMON_HEADERS}
	$(CPP) $(CFLAGS) -c $< ${PYTHON_CFLAGS}

clean:
	rm -f *~ *.o driver
