#!/bin/bash

./gradlew :FlowCrypt:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DoesNotNeedMailserver

