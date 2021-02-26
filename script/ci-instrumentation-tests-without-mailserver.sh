#!/bin/bash

if [[ -z "$1" ]];
 then
  echo "numShards is unset or set to the empty string"
  exit 1
 else
  varNumShards=$1
fi

if [[ -z "$2" ]];
 then
  echo "shardIndex is unset or set to the empty string"
  exit 1
 else
  varShardIndex=$2
fi

if [[ ${varShardIndex} -ge ${varNumShards} ]]
 then
  echo "shardIndex should be lower than numShards"
 else
  ./gradlew --console=verbose :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter -Pandroid.testInstrumentationRunnerArguments.numShards="${varNumShards}" -Pandroid.testInstrumentationRunnerArguments.shardIndex="${varShardIndex}" -Pandroid.testInstrumentationRunnerArguments.clearPackageData=true
fi