#!/bin/bash

./gradlew clean :FlowCrypt:connectedEnterpriseTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.DoesNotNeedMailserverEnterprise

