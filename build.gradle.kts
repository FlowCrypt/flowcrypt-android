/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  id("com.android.application") version "8.1.0" apply false
  id("org.jetbrains.kotlin.android") version "1.8.10" apply false
  kotlin("kapt") version "1.9.0" apply false
  id("androidx.navigation.safeargs.kotlin") version "2.6.0" apply false
  id("com.starter.easylauncher") version "6.2.0" apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.0" apply false
}

apply {
  from("./ext.gradle.kts")
}
