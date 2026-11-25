# Running instrumentation tests like on CI

Instrumentation tests that pass on a fast local emulator can still fail on CI. The CI machines
(`f1-standard-2`, 2 vCPU) run the emulator with 1 core and 2 GB RAM, so taps, IME sessions,
dialogs and background work are much slower there. Most flaky tests are timing races that only
show up on a slow device. Reproduce them on a slow local emulator before fixing them, and verify
the fix there as well. A green run on a fast emulator proves very little.

## 1. Create a separate AVD

Use a separate AVD, so your usual emulators stay untouched. Note that the script deletes
an existing AVD with the same name.

```bash
AVD_NAME=ci-slow-emulator AVD_RAM_SIZE=2048M ./script/create-avd.sh
```

It uses the same system image and device as CI (`android-36;google_apis;x86_64`, `pixel_9`).

## 2. Start the emulator with the CI settings

CI starts the emulator via `script/ci-setup-and-run-emulator.sh` → `script/run-emulator.sh`
with 1 core, 2048 MB RAM, no window, no snapshots, `-read-only` and `-wipe-data`.

If your host resolves `*.flowcrypt.test` with dnsmasq on `127.0.0.1` (see "Device setup
instructions" in [README](../README.md)), you can use the same script:

```bash
AVD_NAME=ci-slow-emulator EMULATOR_PORT=5560 EMULATOR_GPU_MODE=auto \
EMULATOR_LOG_FILE=/tmp/ci-slow-emulator.log EMULATOR_PID_FILE=/tmp/ci-slow-emulator.pid \
taskset -c 2,3 ./script/run-emulator.sh
```

Without `-dns-server`, the emulator forwards DNS queries to the servers from the host's
`/etc/resolv.conf` (see [emulator networking](https://developer.android.com/studio/run/emulator-networking-dns)).
So entries in the host's `/etc/hosts` reach the emulator only if that DNS server serves them.
For example, the `systemd-resolved` stub (`nameserver 127.0.0.53`) does it by default
(`ReadEtcHosts=yes`). Check it before starting the emulator:

```bash
grep nameserver /etc/resolv.conf     # e.g. 127.0.0.53
resolvectl query fes.flowcrypt.test  # must return 127.0.0.1
```

If it works, start the emulator directly without `-dns-server`. Otherwise use dnsmasq and the
script above.

```bash
taskset -c 2,3 "$ANDROID_HOME/emulator/emulator" \
  -avd ci-slow-emulator -port 5560 \
  -no-window -no-boot-anim -no-audio \
  -no-snapshot -no-snapshot-load -no-snapshot-save \
  -cores 1 -memory 2048 -gpu swiftshader_indirect \
  -read-only -no-metrics -wipe-data &
```

- `taskset -c 2,3` pins the emulator to 2 host CPUs, like the 2 vCPU CI machine.
- A separate port (`5560`) keeps the emulator apart from other running emulators.
- `-gpu swiftshader_indirect` is software rendering. It is at least as slow as CI and reproduced
  the CI failures well. CI itself passes `-gpu auto`.
- Stop other emulators while measuring: they compete for the same CPUs.

## 3. Apply the CI device setup

```bash
ANDROID_SERIAL=emulator-5560 ./script/ci-wait-for-emulator.sh
```

This is the script CI runs. It waits for boot and then:

- disables animations and sets `long_press_timeout` to 1500 ms;
- hides system crash/ANR dialogs (`hide_error_dialogs`), so they don't take the window focus;
- disables the keyboard (IME), so `typeText()` doesn't lose characters, and fails if any IME is
  still enabled. Set `DISABLE_IME=0` to keep it;
- runs `adb root` and redirects `127.0.0.1:443` to the mock web server port `1212` (`iptables`);
- forwards port `1212` and checks the network, including `fes.flowcrypt.test`.

Without the `iptables` redirect, all requests to `*.flowcrypt.test:443` fail with
`ECONNREFUSED`, and the test results mean nothing.

## 4. Run tests

Always set `ANDROID_SERIAL`, otherwise Gradle runs the tests on every connected device.

```bash
# a class (use the Enterprise task for tests annotated with @EnterpriseTest)
ANDROID_SERIAL=emulator-5560 ./gradlew :FlowCrypt:connectedConsumerUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.flowcrypt.email.ui.ComposeScreenFlowTest

# one test
... -Pandroid.testInstrumentationRunnerArguments.class=com.flowcrypt.email.ui.ComposeScreenFlowTest#testEmptyEmailMsg

# a package
ANDROID_SERIAL=emulator-5560 ./gradlew :FlowCrypt:connectedEnterpriseUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.flowcrypt.email.ui.gmailapi.passwordprotected
```

## 5. Make flakiness visible

`RetryRule.DEFAULT` runs a failed test up to 3 times (6 times with `@FlakyTest`), so a green
run can hide failures. To see them:

- **Count hidden retries.** `RetryRule` prints `<test>: run N failed` to logcat:

  ```bash
  adb -s emulator-5560 logcat -d | grep -E "run [0-9]+ failed|giving up"
  ```

- **Repeat a test without retries** (temporarily, don't commit it): replace
  `@RunWith(AndroidJUnit4::class)` with
  `@RunWith(RepeatableAndroidJUnit4ClassRunner::class)` plus
  `@RepeatableAndroidJUnit4ClassRunner.RepeatTest(5)`, and `RetryRule.DEFAULT` with
  `RetryRule(1)`.
  It doesn't work for a class with a `@ClassRule` mock web server (e.g. `ComposeScreenFlowTest`):
  the rule starts twice and every test fails with `BindException: EADDRINUSE`. Also, the tests get
  names like `testEmptyEmailSubject_1`, so `class=Class#method` matches nothing. For such a class
  keep `@RunWith(AndroidJUnit4::class)`, set only `RetryRule(1)` and start the test in a loop:

  ```bash
  ANDROID_SERIAL=emulator-5560 ./gradlew :FlowCrypt:installConsumerUiTests \
    :FlowCrypt:installConsumerUiTestsAndroidTest
  for i in {1..10}; do
    adb -s emulator-5560 shell am instrument -w -r \
      -e class com.flowcrypt.email.ui.ComposeScreenFlowTest#testEmptyEmailSubject \
      com.flowcrypt.email.debug.test/androidx.test.runner.AndroidJUnitRunner | grep -E '^OK|^Tests run'
  done
  ```
- **Run a few times.** Some races show up once in 5–10 runs. Compare the code before and after
  the fix under the same conditions.

## 6. Find the cause

Look at logcat (`adb logcat -d`) and at the view hierarchy dumps in
`FlowCrypt/build/outputs/connected_android_test_additional_output/`.

| What you see | What it means |
|---|---|
| `Overslept and turned a tap into a long press` | The tap took longer than `long_press_timeout`, the view got a long click (e.g. a menu item shows its tooltip). |
| `RootViewWithoutFocusException`, `mCurrentFocus=... Application Not Responding: com.android.systemui` in `adb shell dumpsys window` | "System UI isn't responding" right after boot. `ci-wait-for-emulator.sh` sets `hide_error_dialogs`, so the system doesn't show such dialogs. `BaseTest.dismissANRSystemDialogIfExists()` also taps `android:id/aerr_wait`. |
| Many unrelated tests of one shard fail with `RootViewWithoutFocusException`, logcat shows `ANR in system` and `Application Not Responding: system` windows again and again | `system_server` reported an ANR right after boot, and the system shows "Process system isn't responding" for the rest of the job. A dialog dismissed in `@Before` comes back during the test. Check that `adb shell settings get global hide_error_dialogs` returns `1`. |
| `commitText on inactive InputConnection`, `Session id mismatch` during `typeText()` | The IME reconnects and key events are lost (`"message"` → `"essage"`). Use `replaceText()` or insert text without key events. |
| A toast instead of the expected dialog after clicking Send | The compose screen is still looking up a recipient's key (`please_wait_while_information_about_recipients_will_be_updated`). |
| A click opens another screen | The layout moved between computing the click coordinates and the click (a button appeared or the keyboard opened). |
| `Failed to take screenshot`, `Timed out waiting for 5000 ms` | Espresso takes a screenshot on every failed check. In a polling loop each failed check then costs ~5 s. |
| A request returns nothing (`null`, "nothing was thrown") only on CI, and the test takes much longer there | The mock web server does heavy work (e.g. generates a PGP key) while handling the request and exceeds the client's read timeout. Some clients (e.g. `WkdClient`) return `null` on a timeout. |

CI keeps `logcat.txt.gz` in the job artifacts (Semaphore → job → Artifacts). It shows the
real exception, while the job log often shows only the teardown error.

## 7. How to fix a race

- Wait for an observable state instead of `Thread.sleep()`: a view, a database row, a request
  received by the mock web server. `BaseTest.waitUntil {}` polls a condition.
- Keep `@FlowCryptTestSettings(useCommonIdling = false)`. The common idling resource is not
  reliable, so the tests wait for concrete states instead.
- Recipient lookups: `BaseTest.waitUntilRecipientChipIsSettled()` waits until the recipient chip
  keeps its final color. Call it right after adding a recipient and after returning to the compose
  screen (it starts a new lookup).
- Dialogs that close themselves (e.g. `ChoosePublicKeyDialogFragment`): wait until the compose
  screen is in focus again, see `BaseComposeGmailFlow.includeOwnPublicKey()`.
- Sending: `BaseComposeGmailFlow.doAfterSendingChecks()` waits until the mock web server receives
  the message, not only until the outbox is empty.
- Mock web server: prepare heavy responses (generated keys, large bodies) before the test sends
  requests, not inside `Dispatcher.dispatch()`, see `WkdClientTest`.
- `closeSoftKeyboard()` can time out on a screen where the keyboard is just appearing. Repeat the
  action and check the result instead.
- When polling with Espresso, use your own failure handler
  (`withFailureHandler { error, _ -> throw error }`) to avoid a screenshot per failed check.
