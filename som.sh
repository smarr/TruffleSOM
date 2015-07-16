#!/bin/sh
# -G:TruffleCompilationThreshold=3 -Xbootclasspath/a:build/classes \
java -server -cp build/classes:libs/truffle/build/truffle-api.jar \
        som.vm.Universe \
		"$@"
