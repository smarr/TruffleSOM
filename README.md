TruffleSOM - The Simple Object Machine Smalltalk implemented using the Truffle Framework
=========================================================================================

Introduction
------------

This is the repository for TruffleSOM, an implementation of the Simple Object
Machine (SOM). SOM is a minimal Smalltalk dialect that was used to teach VM
construction at the [Hasso Plattner Institute][SOM]. It was originally built at
the University of Århus (Denmark) where it was used for teaching and as the
foundation for [Resilient Smalltalk][RS].

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

TruffleSOM is a [Truffle][T]-based implementation of SOM, including SOM's
standard library and a number of examples. Please see the [main project
page][SOM] for links to other VM implementations.

Obtaining and Running TruffleSOM
--------------------------------

To checkout the code, run:

    git clone --depth 1 https://github.com/graalvm/mx.git
    git clone https://github.com/SOM-st/TruffleSOM.git
    export PATH=$PATH:`pwd`/mx

TruffleSOM uses the [mx](https://github.com/graalvm/mx) build tool, which is
easiest to use when on the PATH.

After downloading the git repositories, TruffleSOM can be build with:

    cd TruffleSOM
    ./som --setup labsjdk  # downloads a compatible JDK
    mx build

And now we, can execute tests with:

    ./som -G -cp Smalltalk TestSuite/TestHarness.som
   
A simple Hello World program is executed with:

    ./som -G -cp Smalltalk Examples/Hello.som

To work on TruffleSOM, mx can generate project definitions for
Eclipse, IntelliJ, and NetBeans. Chose the one you prefer:

    mx eclipseinit
    mx intellijinit
    mx netbeansinit

TruffleSOM uses the Graal compiler to reach state-of-the-art performance.
To use it, we need to compile it together with TruffleSOM:

    mx --env libgraal build

Afterwards, we can run a benchmark, and observe that the initial iterations
take much longer, but after a while we reach magnitudes faster execution speeds:

    ./som -cp Smalltalk Examples/Benchmarks/BenchmarkHarness.som Mandelbrot 100 500

Information on previous authors are included in the AUTHORS file. This code is
distributed under the MIT License. Please see the LICENSE file for details.


Build Status
------------

Thanks to GitHub Actions, all commits of this repository are tested.
The current build status is: ![Build Status](https://github.com/SOM-st/TruffleSOM/actions/workflows/ci.yml/badge.svg)

 [SOM]: https://www.hpi.uni-potsdam.de/hirschfeld/projects/som/
 [SOMst]: https://github.com/SOM-st/TruffleSOM/actions
 [RS]:  https://dx.doi.org/10.1016/j.cl.2005.02.003
 [T]:   https://www.christianwimmer.at/Publications/Wuerthinger12a/
