#!/bin/bash
## Exit on first error
if [ "$1" != "update" ]
then
  # quit on first error
  set -e
fi

## Determine absolute path of script
pushd `dirname $0` > /dev/null
SCRIPT_PATH=`pwd`
popd > /dev/null

SOM_DIR=$SCRIPT_PATH/../../..

## create folder for new results
echo mkdir -p $SCRIPT_PATH/results/
mkdir -p $SCRIPT_PATH/results/

## extract expected results
tar --exclude='._*' -xf $SCRIPT_PATH/expected-results.tar.bz2 -C $SCRIPT_PATH/

NEEDS_UPDATE=false

function doDiff {
  EXPECTED=$1
  NEW=$2

  diff -r $EXPECTED $NEW
  if [ $? -ne 0 ]; then
    NEEDS_UPDATE=true
  fi
}

function runTest {
  TEST=$1 
  runAndDiff "test-$TEST.yml" "$SOM_DIR/Smalltalk" "$SOM_DIR/TestSuite/TestHarness.som $TEST"
}

function runBenchmark {
  BENCH=$1
  ARG=$2
  runAndDiff "bench-$BENCH.yml" \
    "$SOM_DIR/Smalltalk:$SOM_DIR/Examples/Benchmarks/LanguageFeatures:$SOM_DIR/Examples/Benchmarks/TestSuite" \
    "$SOM_DIR/Examples/Benchmarks/BenchmarkHarness.som --gc $BENCH $ARG"
}

function runAndDiff {
  OUT_FILE=$1
  CLASSPATH=$2
  ARGS=$3
  CMD="$SOM_DIR/som -A -n $SCRIPT_PATH/results/$OUT_FILE -Dpolyglot.nodestats.Height=3 \
    -G -cp $CLASSPATH $ARGS"
  echo $CMD
  $CMD

  doDiff $SCRIPT_PATH/expected-results/$OUT_FILE $SCRIPT_PATH/results/$OUT_FILE
}

runAndDiff "hello.yml" "$SOM_DIR/Smalltalk" "$SOM_DIR/Examples/Hello.som"

runBenchmark Sieve   "1 20"
runBenchmark Recurse "1 12"

runTest EmptyTest
runTest SpecialSelectorsTest
# runTest ArrayTest
# runTest BlockTest
runTest BooleanTest
runTest ClassLoadingTest
runTest ClassStructureTest
runTest ClosureTest
runTest CoercionTest
runTest CompilerReturnTest
runTest DictionaryTest
runTest DoesNotUnderstandTest
runTest DoubleTest
# runTest GlobalTest
# runTest HashTest
runTest IntegerTest
runTest PreliminaryTest
# runTest ReflectionTest
# runTest SelfBlockTest
# runTest SetTest
# runTest StringTest
# runTest SuperTest
# runTest SymbolTest
# runTest SystemTest
# runTest VectorTest


if [ "$1" = "update" ] && [ "$NEEDS_UPDATE" = true ]
then
  ## move old results out of the way, and new results to expected folder
  rm -Rf $SCRIPT_PATH/old-results
  mv $SCRIPT_PATH/expected-results $SCRIPT_PATH/old-results
  mv $SCRIPT_PATH/results $SCRIPT_PATH/expected-results
  ## update the archive
  tar --exclude='._*' -cjf $SCRIPT_PATH/expected-results.tar.bz2 -C $SCRIPT_PATH expected-results
fi
