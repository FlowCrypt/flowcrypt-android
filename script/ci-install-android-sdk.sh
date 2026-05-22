#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euxo pipefail

# -----------------------------
# Ensure ~/.android/repositories.cfg exists
# -----------------------------
if [[ -d "$HOME/.android" ]]; then
  echo "$HOME/.android already exists"
else
  mkdir -p "$HOME/.android"
  touch "$HOME/.android/repositories.cfg"
fi

# -----------------------------
# Android SDK location defaults
# -----------------------------
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
INSTALL_KVM_DEPS="${INSTALL_KVM_DEPS:-1}"
RUN_KVM_CHECK="${RUN_KVM_CHECK:-1}"
ANDROID_PLATFORM="${ANDROID_PLATFORM:-android-36}"
ANDROID_SYSTEM_IMAGE="${ANDROID_SYSTEM_IMAGE:-system-images;android-36;google_apis;x86_64}"
ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-}"

# -----------------------------
# Pin cmdline-tools archive here
# -----------------------------
SDK_ARCHIVE="${SDK_ARCHIVE:-commandlinetools-linux-14742923_latest.zip}"

# ------------------------------------------------------------
# Check that SDK_ARCHIVE is the latest Android cmdline-tools
# Logic:
#  1) If check fails (network / parsing) -> skip silently
#  2) If check succeeds and version is outdated -> error + exit
# ------------------------------------------------------------
check_cmdline_tools_latest_or_fail() {
  local STUDIO_URL="https://developer.android.com/studio#command-tools"

  [[ -n "${SDK_ARCHIVE:-}" ]] || return 0
  [[ "$SDK_ARCHIVE" =~ ^commandlinetools-linux-([0-9]+)_latest\.zip$ ]] || return 0
  local CURRENT_VER="${BASH_REMATCH[1]}"

  # If required tools are missing -> skip (do NOT fail CI)
  command -v curl >/dev/null 2>&1 || return 0
  command -v grep >/dev/null 2>&1 || return 0
  command -v sed  >/dev/null 2>&1 || return 0
  command -v sort >/dev/null 2>&1 || return 0
  command -v tail >/dev/null 2>&1 || return 0

  # Disable xtrace only for the fetch/parse section (avoid dumping HTML)
  local xtrace_was_on=0
  case "$-" in *x*) xtrace_was_on=1; set +x ;; esac

  local HTML
  if ! HTML="$(curl -fsSL --connect-timeout 3 --max-time 8 "$STUDIO_URL" 2>/dev/null)"; then
    [[ "$xtrace_was_on" -eq 1 ]] && set -x
    return 0
  fi

  local LATEST_VER
  LATEST_VER="$(
    printf '%s' "$HTML" \
      | grep -oE 'commandlinetools-linux-[0-9]+_latest\.zip' \
      | sed -E 's/.*-([0-9]+)_latest\.zip/\1/' \
      | sort -n \
      | tail -n 1
  )"

  [[ "$xtrace_was_on" -eq 1 ]] && set -x
  [[ -n "$LATEST_VER" ]] || return 0

  if (( CURRENT_VER < LATEST_VER )); then  # download, unpack and remove sdk archive
    echo "ERROR: Outdated Android SDK Command-line Tools detected."
    echo "ERROR: Current pinned version: $CURRENT_VER ($SDK_ARCHIVE)"
    echo "ERROR: Latest available version: $LATEST_VER (commandlinetools-linux-${LATEST_VER}_latest.zip)"
    echo "ERROR: Reason: The official Android SDK repository metadata was updated; older pinned cmdline-tools may break or warn."
    echo "ERROR: Fix: Update SDK_ARCHIVE to the latest version and re-run CI."
    exit 1
  fi
}

# Run the check early
check_cmdline_tools_latest_or_fail

# -----------------------------
# KVM deps (as in your script)
# -----------------------------
if [[ "$INSTALL_KVM_DEPS" == "1" ]]; then
  sudo apt-get -qq install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils > /dev/null
fi
if [[ "$RUN_KVM_CHECK" == "1" ]]; then
  sudo kvm-ok
fi

# -----------------------------
# Install SDK if ~/Android doesn't exist (as in your script)
# -----------------------------
if [[ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "$ANDROID_HOME already exists, skipping installation"
else
  echo "$ANDROID_HOME does not exist, installing"
  mkdir -p "$ANDROID_HOME"

  # download, unpack and remove sdk archive
  wget "https://dl.google.com/android/repository/$SDK_ARCHIVE"
  unzip -qq "$SDK_ARCHIVE" -d "$ANDROID_HOME"

  # Ensure cmdline-tools are under: $ANDROID_HOME/cmdline-tools/latest
  mkdir -p "$ANDROID_HOME/cmdline-tools/latest"

  # The zip typically extracts into "$ANDROID_HOME/cmdline-tools/"
  # Move extracted content into "latest" in a safe way
  if [[ -d "$ANDROID_HOME/cmdline-tools" ]]; then
    # Move everything except "latest" into latest/
    shopt -s dotglob nullglob
    for p in "$ANDROID_HOME/cmdline-tools"/*; do
      [[ "$(basename "$p")" == "latest" ]] && continue
      mv "$p" "$ANDROID_HOME/cmdline-tools/latest/"
    done
    shopt -u dotglob nullglob
  fi

  cd "${SEMAPHORE_GIT_DIR:-$PWD}"
  rm -f "$SDK_ARCHIVE"

  # Install Android SDK
  (echo "yes" | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null | grep -v = || true)
  ( sleep 5; echo "y" ) | ("${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" "platforms;${ANDROID_PLATFORM}" > /dev/null | grep -v = || true)
  ("${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" "platform-tools" | grep -v = || true)
  ("${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" "emulator" | grep -v = || true)
  if [[ -n "$ANDROID_BUILD_TOOLS" ]]; then
    ("${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" "build-tools;${ANDROID_BUILD_TOOLS}" | grep -v = || true)
  fi
  (echo "y" | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" "${ANDROID_SYSTEM_IMAGE}" > /dev/null | grep -v = || true)
fi

# Uncomment this for debug
# "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --list
