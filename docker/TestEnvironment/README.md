# Test Environment Docker Image

This directory contains a Docker context for a headless Android test environment based on
`ubuntu:24.04`.

The image installs:

- OpenJDK 21
- Android SDK command-line tools
- `platform-tools`
- Android Emulator
- Android platform `android-36`
- Build tools `36.0.0`
- System image `system-images;android-36;google_apis;x86_64`

## Build

Use the helper script:

```bash
./docker/TestEnvironment/rebuild.sh
```

Equivalent manual command:

```bash
docker build -t flowcrypt/android-test-env -f docker/TestEnvironment/Dockerfile .
```

## Create an AVD inside the container

```bash
docker run --rm -it flowcrypt/android-test-env /opt/flowcrypt/scripts/create-avd.sh
```

## Run the emulator inside the container

The container includes reusable helpers:

- `/opt/flowcrypt/scripts/create-avd.sh`
- `/opt/flowcrypt/scripts/run-emulator.sh`

The container includes the emulator binaries, but actual emulator launch usually needs:

- `/dev/kvm` passed through to the container
- additional Docker flags such as `--device /dev/kvm`

If you need emulator DNS resolution for `*.localhost` / `*.flowcrypt.test` from
`docker/HttpsTestWebServer`, run that container first so host `dnsmasq` is available on
`127.0.0.1:53`. `run.sh` starts this container with `--network host --dns 127.0.0.1`.

Because `--network host` is used, `adb` inside container can see host-side emulator devices
through host `adb` server. To avoid mixing devices, stop host emulator(s) before testing or run
container `adb` on another port (for example `export ADB_SERVER_PORT=5038`).

Use the helper script:

```bash
./docker/TestEnvironment/run.sh
```

You can override DNS forwarded to emulator:

```bash
EMULATOR_DNS_SERVER=127.0.0.1 ./docker/TestEnvironment/run.sh
```

Equivalent manual command:

```bash
docker run --rm -it \
  --network host \
  --dns 127.0.0.1 \
  -e EMULATOR_DNS_SERVER=127.0.0.1 \
  --device /dev/kvm \
  --name flowcrypt-android-test-env \
  flowcrypt/android-test-env \
  bash
```

Then inside the container:

```bash
/opt/flowcrypt/scripts/create-avd.sh
/opt/flowcrypt/scripts/run-emulator.sh
```

DNS checks:

```bash
dig @127.0.0.1 fel.flowcrypt.test +short
adb shell getprop net.dns1
adb shell ping -c 1 fel.flowcrypt.test
```
