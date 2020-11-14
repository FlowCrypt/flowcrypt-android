#!/bin/bash

./gradlew --console=verbose :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.flowcrypt.email.DoesNotNeedMailserver