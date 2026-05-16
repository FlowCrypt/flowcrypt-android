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

```bash
docker build -t flowcrypt-android-test-env -f docker/TestEnvironment/Dockerfile .
```

## Create an AVD inside the container

```bash
docker run --rm -it flowcrypt-android-test-env create-avd.sh
```

## Run the emulator inside the container

The container includes the emulator binaries, but actual emulator launch usually needs:

- `/dev/kvm` passed through to the container
- additional Docker flags such as `--device /dev/kvm`

Example:

```bash
docker run --rm -it \
  --device /dev/kvm \
  flowcrypt-android-test-env \
  bash
```

Then inside the container:

```bash
create-avd.sh
emulator -avd ci-emulator -no-window -no-boot-anim -no-audio -gpu swiftshader_indirect
```
