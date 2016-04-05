CPP=g++

PYTHON_VER=3.5
PYTHON_CONFIG=python${PYTHON_VER}-config

# For some bizarre reasons, config --ldflags will work for
#  the Mac the first time, and then subsequently fail to
#  include the path to the libraries
PYTHON_PREFIX=$(shell ${PYTHON_CONFIG} --prefix)
PYTHON_CFLAGS=$(shell ${PYTHON_CONFIG} --cflags)
PYTHON_LDFLAGS=$(shell ${PYTHON_CONFIG} --ldflags)

WRAPPER_OBJS=octaveWconPythonWrapper.o
DRIVER_OBJS=driver.o

all: driver

driver: ${WRAPPER_OBJS} ${DRIVER_OBJS}
	$(CPP) -o driver ${WRAPPER_OBJS} ${DRIVER_OBJS} ${PYTHON_LDFLAGS}

${WRAPPER_OBJS}: octaveWconPythonWrapper.cpp octaveWconPythonWrapper.h
	$(CPP) -c octaveWconPythonWrapper.cpp ${PYTHON_CFLAGS}

${DRIVER_OBJS}: driver.cpp
	$(CPP) -c driver.cpp

clean:
	rm -f *~ *.o driver
