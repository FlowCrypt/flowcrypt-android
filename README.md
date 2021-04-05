# FlowCrypt Android App (Encrypt email with PGP)

Download on your Android device from the [https://flowcrypt.com/download]

![Build Status](https://flowcrypt.semaphoreci.com/badges/flowcrypt-android.svg?key=3683eef1-6121-4c12-bcf7-031d0b4a36eb)

## Running tests

This guide follows the [Google recommendations for testing apps on Android](https://developer.android.com/training/testing).
There are JUnit and Instrumentation tests. To be able to run tests locally you should set up your environment
according to [these instructions](https://developer.android.com/training/testing/espresso/setup#set-up-environment).
Every scenario described in this section was tested on Ubuntu.

We have two types of tests which can be run:

- Independent tests which don't require any additional dependencies.
- Tests which depend on an email server.
  Such tests are marked with the `@DependsOnMailServer` annotation.

We run tests on [Semaphore CI](https://semaphoreci.com/) for every commit.
To run tests which depend on an email server we use a custom [Docker image](https://hub.docker.com/r/flowcrypt/flowcrypt-email-server),
which extends [docker-mailserver](https://github.com/tomav/docker-mailserver).
This image has predefined settings for local testing. It has accounts and messages which we need for testing.
You can investigate [`docker-mailserver`](https://github.com/FlowCrypt/flowcrypt-android/tree/master/docker-mailserver)
folder to see more details. To be able to run tests which depend on an email server please install `docker-compose` with [these instructions](https://docs.docker.com/compose/install/).

Please use following steps to run **the independent tests** locally:

- Setup your device (virtual or physical) via [these instructions](https://developer.android.com/training/testing/espresso/setup#set-up-environment).
- Run `../script/ci-instrumentation-tests-without-mailserver.sh 1 0` (where `1` is a `numShards` and `0` is a `shardIndex`).
  You can find more details [here](https://developer.android.com/training/testing/junit-runner#sharding-tests).
  Sharding the tests help us run them in pieces on a few emulators at the same time to reduce runtime.

Please use following steps to run **tests that depend on an email** server locally:

- Setup your device (virtual or physical) via [these instructions](https://developer.android.com/training/testing/espresso/setup#set-up-environment).
- Run `./docker-mailserver/run_email_server.sh` and wait while the email server will be started.
- Run `../script/ci-instrumentation-tests-with-mailserver.sh`
- Run `./docker-mailserver/stop_email_server.sh` to stop the email server.

Please follow these steps to run **all tests** locally:

- Setup your device (virtual or physical) via [these instructions](https://developer.android.com/training/testing/espresso/setup#set-up-environment).
- Run `./docker-mailserver/run_email_server.sh` and wait while the email server will be started.
- Run `./script/run-all-tests.sh` to execute all tests.
- Run `./docker-mailserver/stop_email_server.sh` to stop the email server.
