#!/bin/bash

./gradlew :FlowCrypt:connectedConsumerDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DoesNotNeedMailserver

