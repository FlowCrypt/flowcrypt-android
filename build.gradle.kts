/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  id("com.android.application") version "8.7.3" apply false
  id("org.jetbrains.kotlin.android") version "2.1.0" apply false
  id("androidx.navigation.safeargs.kotlin") version "2.8.5" apply false
  id("com.starter.easylauncher") version "6.4.0" apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "2.1.0" apply false
  id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
  id("org.ajoberstar.grgit") version "5.3.0" apply false
  id("com.google.protobuf") version "0.9.4" apply false
}

subprojects {
  apply(from = "$rootDir/ext.gradle.kts")
}
