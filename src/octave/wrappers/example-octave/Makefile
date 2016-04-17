SWIG_MODULENAME=swigexample

all: ${SWIG_MODULENAME}.oct

${SWIG_MODULENAME}.oct:	${SWIG_MODULENAME}.cpp example.c example.h
	mkoctfile -o ${SWIG_MODULENAME} ${SWIG_MODULENAME}.cpp example.c

${SWIG_MODULENAME}.cpp: ${SWIG_MODULENAME}.i
	swig -octave -o ${SWIG_MODULENAME}.cpp ${SWIG_MODULENAME}.i

${SWIG_MODULENAME}.i: swig-example.template
	sed -e "s/SWIG_MOD_NAME/${SWIG_MODULENAME}/g" swig-example.template > ${SWIG_MODULENAME}.i

clean:
	rm -f *~ *.o ${SWIG_MODULENAME}.oct ${SWIG_MODULENAME}.i ${SWIG_MODULENAME}.cpp
