#!/bin/bash

./gradlew clean :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DoesNotNeedMailserver

