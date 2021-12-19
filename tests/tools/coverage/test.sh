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
  TEST=lcov.info
  HARNESS="$SOM_DIR/som -A -cov $SCRIPT_PATH/results/$TEST -ct lcov \
    -G -cp $SOM_DIR/Smalltalk $SOM_DIR/TestSuite/TestHarness.som"
  echo $HARNESS
  $HARNESS

  doDiff $SCRIPT_PATH/expected-results/$TEST $SCRIPT_PATH/results/$TEST
}

runTest

if [ "$1" = "update" ] && [ "$NEEDS_UPDATE" = true ]
then
  ## move old results out of the way, and new results to expected folder
  rm -Rf $SCRIPT_PATH/old-results
  mv $SCRIPT_PATH/expected-results $SCRIPT_PATH/old-results
  mv $SCRIPT_PATH/results $SCRIPT_PATH/expected-results
  ## update the archive
  tar --exclude='._*' -cjf $SCRIPT_PATH/expected-results.tar.bz2 -C $SCRIPT_PATH expected-results
fi
