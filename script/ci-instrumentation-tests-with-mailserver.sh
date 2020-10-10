#!/bin/bash

./gradlew :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.flowcrypt.email.DoesNotNeedMailserver