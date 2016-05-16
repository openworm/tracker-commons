##Installation

###Tested Platforms
Ubuntu LTS 14.04 - run as a VirtualBox OS image on Mac OS X.

###Pre-requisites:

As this Octave implementation of WCON is developed as a wrapper
library around the Python implementation, it is first dependent on the
installation instructions for Python as described in the [Python
implementation](../Python/INSTALL.md).

As of the current Octave implementation please follow the instructions
for Python version 3.5, and using the source code from git.

###Octave-specific setup:

The following instructions are best run as a bash shell script. Paths
are relative, so the script should be safe to run from any folder.

```
#!/bin/bash
# Pre-requisite - run python-install.sh
PYTHON_VERSION=3.5
MINICONDA_DIR=~/miniconda3

sudo apt-get install -y octave
# liboctave-dev is required for mkoctfile
sudo apt-get install -y liboctave-dev
sudo apt-get install -y swig
sudo apt-get install -y g++
export PATH=$MINICONDA_DIR/bin:$PATH
# Library paths to /lib/x86_64-linux-gnu and /usr/lib/x86_64-linux-gnu
#   are because the libgfortran.so.3 and libreadline.so.6 files installed
#   by miniconda3 are incompatible with the ones expected by swig and
#   octave.
export LD_LIBRARY_PATH=/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:$MINICONDA_DIR/lib:$LD_LIBRARY_PATH
cd ~/github/tracker-commons/src/octave/wrappers
make
cat >> ~/.profile <<EOF
export PATH=$MINICONDA_DIR/bin:\$PATH
export LD_LIBRARY_PATH=/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:$MINICONDA_DIR/lib:\$LD_LIBRARY_PATH
EOF
. ~/.profile
```
