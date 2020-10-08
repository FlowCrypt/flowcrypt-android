#!/bin/bash

./gradlew :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.ReadyForCIAnnotation -Pandroid.testInstrumentationRunnerArguments.numShards=2 -Pandroid.testInstrumentationRunnerArguments.shardIndex=1