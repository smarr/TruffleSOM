TruffleSOM - The Simple Object Machine Smalltalk implemented using Oracle's Truffle Framework
=============================================================================================

Introduction
------------

SOM is a minimal Smalltalk dialect used to teach VM construction at the [Hasso
Plattner Institute][SOM]. It was originally built at the University of Århus
(Denmark) where it was used for teaching and as the foundation for [Resilient
Smalltalk][RS].

In addition to TruffleSOM, other implementations exist for Java (SOM), C (CSOM),
C++ (SOM++), and Squeak/Pharo Smalltalk (AweSOM).

A simple Hello World looks like:

```Smalltalk
Hello = (
  run = (
    'Hello World!' println.
  )
)
```

This repository contains the [Truffle][T]-based implementation of SOM, including
SOM's standard library and a number of examples. Please see the [main project
page][SOM] for links to other VM implementations.

Obtaining and Running TruffleSOM
--------------------------------

To checkout the code:

    git clone https://github.com/SOM-st/TruffleSOM.git

Then, TruffleSOM can be build with Ant:

    ant jar

Afterwards, the tests can be executed with:

    ./som -cp Smalltalk TestSuite/TestHarness.som
   
A simple Hello World program is executed with:

    ./som -cp Smalltalk Examples/Hello.som

When working on TruffleSOM, for instance in Eclipse, it is helpful to download
the source files for Truffle as well:

    ant develop

Information on previous authors are included in the AUTHORS file. This code is
distributed under the MIT License. Please see the LICENSE file for details.


Build Status
------------

Thanks to Travis CI, all commits of this repository are tested.
The current build status is: [![Build Status](https://travis-ci.org/SOM-st/TruffleSOM.png?branch=master)](https://travis-ci.org/SOM-st/TruffleSOM)

 [SOM]: http://www.hpi.uni-potsdam.de/hirschfeld/projects/som/
 [SOMst]: https://travis-ci.org/SOM-st/
 [RS]:  http://dx.doi.org/10.1016/j.cl.2005.02.003
 [T]:   http://www.christianwimmer.at/Publications/Wuerthinger12a/
