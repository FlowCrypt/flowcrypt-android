/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
apply(from = "$rootDir/ext.gradle.kts")

plugins {
  id("com.android.application") version "9.5.1" apply false
  // Safe Args still emits casts suppressed as CAST_NEVER_SUCCEEDS; Kotlin reports them as
  // UNSAFE_CAST_RELYING_ON_NULL. Keep these warnings visible until the generator is fixed.
  id("androidx.navigation.safeargs.kotlin") version "2.10.2" apply false
  id("com.starter.easylauncher") version "6.4.1" apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.21" apply false
  // 2.3.13 removes deprecated Configuration.setVisible calls, but has a concurrency regression:
  // https://github.com/google/ksp/issues/3286
  id("com.google.devtools.ksp") version "2.3.12" apply false
}
