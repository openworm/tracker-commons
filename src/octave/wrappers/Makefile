CPP=g++
AR=ar
SWIG=swig
MKOCTFILE=mkoctfile

CFLAGS=-std=gnu++11

PYTHON_VER=3.5
PYTHON_CONFIG=python${PYTHON_VER}-config

# For some bizarre reasons, config --ldflags will work for
#  the Mac the first time, and then subsequently fail to
#  include the path to the libraries
PYTHON_PREFIX=$(shell ${PYTHON_CONFIG} --prefix)
PYTHON_CFLAGS=$(shell ${PYTHON_CONFIG} --cflags)
PYTHON_LDFLAGS=$(shell ${PYTHON_CONFIG} --ldflags)

WRAPPER_OBJS=octaveWconPythonWrapper.o wrapperInternal.o \
	wconOct_wrapperWCONWorms.o \
	wconOct_wrapperMeasurementUnit.o
COMMON_HEADERS=octaveWconPythonWrapper.h wrapperTypes.h

WRAPPER_LIB=libWconOct.a
WRAPPER_LIB_LDFLAGS=-L. -lWconOct

SWIG_MODULENAME=wconoct

all: driver ${WRAPPER_LIB} ${SWIG_MODULENAME}.oct

${SWIG_MODULENAME}.oct: ${SWIG_MODULENAME}.cpp swigWrapper.c swigWrapper.h
	$(MKOCTFILE) -o ${SWIG_MODULENAME} ${SWIG_MODULENAME}.cpp swigWrapper.c

${SWIG_MODULENAME}.cpp: ${SWIG_MODULENAME}.i
	$(SWIG) -octave -o ${SWIG_MODULENAME}.cpp ${SWIG_MODULENAME}.i

${SWIG_MODULENAME}.i: wcon-oct-swig.template
	sed -e "s/SWIG_MOD_NAME/${SWIG_MODULENAME}/g" wcon-oct-swig.template > ${SWIG_MODULENAME}.i

driver: driver.o ${WRAPPER_LIB}
	$(CPP) -o driver driver.o ${WRAPPER_LIB_LDFLAGS} ${PYTHON_LDFLAGS}

libWconOct.a: ${WRAPPER_OBJS}
	$(AR) rcs libWconOct.a ${WRAPPER_OBJS}

driver.o: driver.cpp
	$(CPP) $(CFLAGS) -c driver.cpp

%.o: %.cpp ${COMMON_HEADERS}
	$(CPP) $(CFLAGS) -c $< ${PYTHON_CFLAGS}

clean:
	rm -f *~ *.o *.a driver *.oct *.i ${SWIG_MODULENAME}.cpp
