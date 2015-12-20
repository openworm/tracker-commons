The `wcon` Python package works with both Python 2 and 3.

If you already have the scipy stack installed, installing `wcon` should be as simple as:

```
pip install wcon
```

Here are the complete installation instructions to get from a freshly provisioned Ubuntu Amazon Web Services (AWS) Machine Instance (AMI) to a machine with WCON installed:

```
# PYTHON 2 STEPS FROM CLEAN UBUNTU AMI:
sudo apt-get update
sudo apt-get -y install python-pip

sudo apt-get -y install default-jdk
sudo apt-get -y install unzip
# cython will fail if you don't install this:
# (see http://stackoverflow.com/questions/11094718/)
sudo apt-get -y install python-dev

# Because 0.22 the latest version is no good because of kivy/buildozer#150
# https://github.com/kivy/buildozer/issues/150
sudo -H pip install cython==0.21
sudo -H pip install numpy

# Install scipy sudo -H pip install scipy doesn't work ("no lapack/blas resources found"), so instead, from 
# http://www.scipy.org/install.html:
sudo apt-get install python-numpy python-scipy python-matplotlib ipython ipython-notebook python-pandas python-sympy python-nose
# The previous command also installed pandas, matplotlib, IPython, and nose!  Nice!

# Install wcon (for Python 2):
sudo pip install wcon
```

These steps will differ slightly (e.g. use `pip3` instead of `pip` for installing on Python 3.)