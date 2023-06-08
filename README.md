# FlowCrypt Android App (Encrypt email with PGP)

Download on your Android device from the [FlowCrypt Downloads](https://flowcrypt.com/download) page.

![Build Status](https://flowcrypt.semaphoreci.com/badges/flowcrypt-android.svg?key=3683eef1-6121-4c12-bcf7-031d0b4a36eb)

## Running tests

This guide follows Google's recommendations for [testing apps on Android](https://developer.android.com/training/testing). Every scenario described in this section has been tested on Ubuntu. There are JUnit and Instrumentation tests. To be able to run tests locally, you should set up your environment.

### Device setup instructions

Please follow these steps to setup your virtual or physical device:

- [Set up your test environment](https://developer.android.com/training/testing/espresso/setup#set-up-environment).
- Some of the tests use [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver). We run a local mock web server via [FlowCryptMockWebServerRule](https://github.com/FlowCrypt/flowcrypt-android/blob/master/FlowCrypt/src/androidTest/java/com/flowcrypt/email/rules/FlowCryptMockWebServerRule.kt). It uses [TestConstants.MOCK_WEB_SERVER_PORT](https://github.com/FlowCrypt/flowcrypt-android/blob/master/FlowCrypt/src/androidTest/java/com/flowcrypt/email/TestConstants.kt#L19) as a port. Unfortunately, the Android system doesn't allow us to use the `HTTPS 433` port by default
to run a web server on the `localhost (127.0.0.1)`. That's why we have to run a mock web server on another port (for example, `1212`) and route all traffic from `127.0.0.1:433` to `127.0.0.1:1212`. For that purpose, you can use the [script/ci-wait-for-emulator.sh](https://github.com/FlowCrypt/flowcrypt-android/blob/master/script/ci-wait-for-emulator.sh#L13) script.
- Additionally, the test environment should handle all requests to `*.flowcrypt.test`. All traffic to `*.flowcrypt.test` should be routed to `localhost(127.0.0.1)`. To do this, a DNS server can be used. For example:

```bash
1. sudo apt install -y dnsmasq resolvconf
2. echo "#added by flowcrypt" | sudo tee -a /etc/dnsmasq.conf
3. echo "listen-address=127.0.0.1" | sudo tee -a /etc/dnsmasq.conf
4. echo "address=/flowcrypt.test/127.0.0.1" | sudo tee -a /etc/dnsmasq.conf
5. echo "address=/localhost/127.0.0.1" | sudo tee -a /etc/dnsmasq.conf
6. sudo systemctl restart dnsmasq
```

### Test types

We have two types of tests:

- Independent tests which don't require any additional dependencies.
- Tests that depend on an email server. Such tests are marked with the `@DependsOnMailServer` annotation.

Additionally, we have separate tests for the **consumer** and **enterprise** versions. Enterprise tests are marked with the `@EnterpriseTest` annotation. These tests use the [Gmail API](https://developers.google.com/gmail/api/guides) and are independent of the mail server.

We run tests on [Semaphore CI](https://semaphoreci.com/) for every commit. To run tests that depend on an email server we use a custom [Docker image](https://hub.docker.com/r/flowcrypt/flowcrypt-email-server), which extends [docker-mailserver](https://github.com/tomav/docker-mailserver). This image has predefined settings for local testing. It has accounts and messages which we need for testing. You can investigate the [`docker-mailserver`](https://github.com/FlowCrypt/flowcrypt-android/tree/master/docker-mailserver) folder to see more details. To be able to run tests that depend on an email server, please install `docker-compose` with [these instructions](https://docs.docker.com/compose/install/).

### Run independent tests

Please use the following steps to run **the independent tests** locally:

1. Setup your device (virtual or physical) following the [device setup instructions](#device-setup-instructions).
2. Run `../script/ci-instrumentation-tests-without-mailserver.sh 1 0` (where `1` is a `numShards` and `0` is a `shardIndex`). You can find more details [here](https://developer.android.com/training/testing/junit-runner#sharding-tests). Sharding the tests helps us run them in pieces on a few emulators at the same time to reduce runtime.

Please use the following steps to run **tests that depend on an email** server locally:

1. Setup your device (virtual or physical) following the [device setup instructions](#device-setup-instructions).
2. Run `./docker-mailserver/run_email_server.sh` and wait while the email server will be started.
3. Run `../script/ci-instrumentation-tests-with-mailserver.sh`.
4. Run `./docker-mailserver/stop_email_server.sh` to stop the email server.

Please follow these steps to run **all tests** locally:

1. Setup your device (virtual or physical) following the [device setup instructions](#device-setup-instructions).
2. Run `./docker-mailserver/run_email_server.sh` and wait while the email server will be started.
3. Run `./script/run-all-tests.sh` to execute all tests.
4. Run `./docker-mailserver/stop_email_server.sh` to stop the email server.

Please follow these steps to run **enterprise tests** locally:

1. Setup your device (virtual or physical) following the [device setup instructions](#device-setup-instructions).
2. Run `./script/ci-instrumentation-tests-enterprise.sh` to execute enterprise tests.

## Running the app in the emulator for the first time

To be able to run the app on an emulator please follow these steps:

### Install and run an emulator

1\. Run AVD Manager from **Tools** &#10140; **AVD Manager** or click on the highlighted icon:
![image](https://user-images.githubusercontent.com/2863246/136424474-3de87e4d-ffac-49d6-82e3-ec9831399721.png)

2\. Click on **Create Virtual Device**:
![image](https://user-images.githubusercontent.com/2863246/136425173-78ee0834-242d-48a6-8ff0-ec40cc9f9d6a.png)

3\. Select Hardware. **Pixel 4** will be a good choice. Then click **Next**:
![image](https://user-images.githubusercontent.com/2863246/136425849-f3839002-cd17-48a6-9027-c7a6561dd588.png)

4\. Download one of the latest available system images:
![image](https://user-images.githubusercontent.com/2863246/136426398-ebdcf49d-3566-45ee-b06b-698908cd5c55.png)

5\. Click **Finish**:
![image](https://user-images.githubusercontent.com/2863246/136427125-6aa91bef-f052-432f-a314-369d4b6d4825.png)

6\. Then, select the downloaded image and click on **Next**:
![image](https://user-images.githubusercontent.com/2863246/136427526-0c0cfc0b-b622-4420-9ea0-14aabff22423.png)

7\. Then, we'll see the final screen where we should click on **Finish**:
![image](https://user-images.githubusercontent.com/2863246/136428104-4eba085d-eddf-46e2-b495-87be8d9a2237.png)

8\. After that, AVD Manager will show a new emulator. Click on a green triangle to run an emulator:
![image](https://user-images.githubusercontent.com/2863246/136429163-e74b4ccf-360d-49af-a57b-846dd6be23fe.png)

9\. Then, we'll see a home screen of the emulator:
![image](https://user-images.githubusercontent.com/2863246/136430123-1277b324-4910-4594-9f7c-167314d1ecef.png)

### Build and run the app

1\. First of all, we should select the correct version of the build variants. Please use `consumerDebug`:
![image](https://user-images.githubusercontent.com/2863246/136431329-2b850d9f-6dc3-4849-817c-86ffcc85ec17.png)

2\. Then, we should check that our emulator is selected:
![image](https://user-images.githubusercontent.com/2863246/136431790-bd6fd50d-db0f-4a95-bffb-d3bd92e574b3.png)

3\. Now, we can run the app via **Run** &#10140; **Run(Alt+Shift+F10)** or click on the **Run** button:
![image](https://user-images.githubusercontent.com/2863246/136432060-088641d6-2bc5-44c0-bc58-80c50a49602e.png)

4\. Then, we'll see the app on the emulator in a few minutes(it depends on the hardware):
![image](https://user-images.githubusercontent.com/2863246/136433066-d98cd03b-9db0-47a6-9ac8-7d21d347b6ea.png)
