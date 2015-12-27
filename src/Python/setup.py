# -*- coding: utf-8 -*-
"""
setup.py for the Python packager

Based on https://github.com/pypa/sampleproject/blob/master/setup.py
"""

# Always prefer setuptools over distutils
from setuptools import setup
# To use a consistent encoding
from codecs import open
from os import path
import os
from .wcon.version import __version__

here = path.abspath(path.dirname(__file__))
readme_path = path.join(here, 'README.md')

long_description = 'See https://github.com/openworm/tracker-commons\n'

# Get the long description from the README file, and add it.
if path.exists(readme_path):
    with open(readme_path, encoding='utf-8') as f:
        long_description += f.read()

print(os.listdir('.')) # DEBUG

setup(
    name='wcon',
    # Versions should comply with PEP440.  For a discussion on single-sourcing
    # the version across setup.py and the project code, see
    # https://packaging.python.org/en/latest/single_source_version.html
    version=__version__,
    description='Worm tracker Commons Object Notation',
    long_description=long_description,
    url='https://github.com/openworm/tracker-commons',
    author='Kerr, R; Brown, A; Currie, M; OpenWorm',
    author_email='ichoran@gmail.com',
    license='MIT',
    classifiers=[
        'Development Status :: 4 - Beta',
        'Intended Audience :: Science/Research',
        'Topic :: Scientific/Engineering :: Bio-Informatics',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
    ],
    keywords='C. elegans worm tracking',
    packages=['wcon'],
    install_requires=['jsonschema']
    # Actually also requires numpy, scipy and numpy but I don't want to force
    # pip to install these since pip is bad at that for those packages.
)