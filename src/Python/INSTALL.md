The `wcon` Python package works with both Python 2 and 3.

If you already have the scipy stack installed, installing `wcon` should be as simple as:

```
pip install wcon
```

Here are the complete installation instructions to get from a freshly provisioned Ubuntu Amazon Web Services (AWS) Machine Instance (AMI) to a machine with WCON installed:

```
# PYTHON 3 STEPS FROM CLEAN UBUNTU AMI

# UPDATE SYSTEM
sudo apt-get -y update
sudo apt-get -y upgrade
sudo apt-get -y dist-upgrade
sudo reboot

# INSTALL PYTHON AND LIBRARIES NEEDED BY WCON
PYTHON_VERSION=3.5
MINICONDA_DIR=~/miniconda3
wget http://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O miniconda.sh
chmod +x miniconda.sh
./miniconda.sh -b
export PATH=$MINICONDA_DIR/bin:$PATH
conda install --yes python=$PYTHON_VERSION numpy scipy pandas jsonschema psutil

# GET WCON FROM SOURCE
cd ~
mkdir github
git clone git@github.com:OpenWorm/tracker-commons
cd github/tracker-commons/src/Python
python tests/tests.py
```

For Python 2, these steps will differ slightly (e.g. change to `PYTHON_VERSION=2.7` and `MINICONDA_DIR=~/miniconda`)
