#!/bin/bash
BASE_DIR=`pwd`
if [ -z "$GRAAL_HOME" ]; then
  GRAAL_HOME='/home/smarr/Projects/SOM/graal'
  if [ ! -d "$GRAAL_HOME" ]
  then
    GRAAL_HOME='/Users/guidochari/Documents/Projects/graal-compiler'
  fi
fi

if [ -z "$GRAAL_FLAGS" ]; then
  
  GRAAL_FLAGS='-G:-TraceTruffleInlining -G:-TraceTruffleCompilation -G:+TruffleSplittingNew -G:+TruffleCompilationExceptionsAreFatal'
  if [ "$GRAAL_HOME" = "/Users/guidochari/Documents/Projects/graal-compiler" ]; then
    echo Using Graal Development Flags
    GRAAL_FLAGS='-ea -XX:+UnlockDiagnosticVMOptions 
      -XX:+TraceDeoptimization
      -G:+TraceTruffleExpansionSource -G:-TruffleBackgroundCompilation -G:+TraceTruffleCompilationDetails'
	#-XX:+LogCompilation
  fi
fi

if [ ! -z "$DBG" ]; then
  # GRAAL_DEBUG_SWITCH='-d'
  GRAAL_DEBUG_SWITCH="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
fi

# GRAAL="$GRAAL_HOME/mxtool/mx"
GRAAL="mx -p $GRAAL_HOME/jvmci vm -server -d64 "

#echo $GRAAL $GRAAL_DEBUG_SWITCH $GRAAL_FLAGS \
#   -Xbootclasspath/a:build/classes:libs/truffle/build/truffle-api.jar \
#   som.vm.Universe "$@"
   
exec $GRAAL $GRAAL_DEBUG_SWITCH $GRAAL_FLAGS \
   -Xbootclasspath/a:build/classes:../graal-compiler/truffle/build/truffle-api.jar \
   som.vm.Universe "$@"
