#!/bin/bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

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
   #print test names
   #adb shell am instrument -w \
   # -e filter com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter \
   # -e numShards 3 \
   # -e shardIndex 1 \
   # -e log true \
   # com.flowcrypt.email.debug.test/androidx.test.runner.AndroidJUnitRunner

  ./gradlew --console=plain --no-daemon --build-cache --max-workers=2 :FlowCrypt:connectedConsumerUiTestsAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter \
    -Pandroid.testInstrumentationRunnerArguments.numShards="${varNumShards}" \
    -Pandroid.testInstrumentationRunnerArguments.shardIndex="${varShardIndex}"
fi
