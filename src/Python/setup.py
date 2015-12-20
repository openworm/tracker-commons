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

here = path.abspath(path.dirname(__file__))

# Get the long description from the README file
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()
    
setup(
    name='wcon',
    # Versions should comply with PEP440.  For a discussion on single-sourcing
    # the version across setup.py and the project code, see
    # https://packaging.python.org/en/latest/single_source_version.html
    version='0.1.1',
    description='Worm tracker Commons Object Notation',
    long_description=long_description,
    url='https://github.com/openworm/tracker-commons',
    author='Kerr, R; Brown, A; Currie, M; OpenWorm',
    author_email='ichoran@gmail.com',
    license='MIT',
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Science/Research',
        'Topic :: Scientific/Engineering :: Bio-Informatics',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 3.3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
    ],
    keywords='C. elegans worm tracking',
    packages=['wcon'],
    install_requires=['scipy', 'numpy', 'pandas', 'jsonschema', 'json']
)