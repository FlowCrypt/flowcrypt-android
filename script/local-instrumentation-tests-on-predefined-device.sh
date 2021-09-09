#!/bin/bash

if [[ -z "$1" ]];
 then
  echo "emulator port is unset or set to the empty string"
  exit 1
 else
  varEmulatorPort=$1
fi

if [[ -z "$2" ]];
 then
  echo "numShards is unset or set to the empty string"
  exit 1
 else
  varNumShards=$2
fi

if [[ -z "$3" ]];
 then
  echo "shardIndex is unset or set to the empty string"
  exit 1
 else
  varShardIndex=$3
fi

if [[ ${varShardIndex} -ge ${varNumShards} ]]
 then
  echo "shardIndex should be lower than numShards"
 else
  ANDROID_SERIAL=emulator-"${varEmulatorPort}" ./gradlew :FlowCrypt:connectedDevTestDebugAndroidTest \
   -Pandroid.testInstrumentationRunnerArguments.numShards="${varNumShards}" \
   -Pandroid.testInstrumentationRunnerArguments.shardIndex="${varShardIndex}" \
   -Pandroid.testInstrumentationRunnerArguments.clearPackageData=true
fi
