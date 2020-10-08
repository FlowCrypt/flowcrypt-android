#!/bin/bash

./gradlew :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DebugTestAnnotation