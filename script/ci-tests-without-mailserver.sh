#!/bin/bash

./gradlew :FlowCrypt:testDevTestDebugUnitTest :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DoesNotNeedMailserver

