/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  id("com.android.application") version "8.3.1" apply false
  id("org.jetbrains.kotlin.android") version "1.9.23" apply false
  id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
  id("com.starter.easylauncher") version "6.3.0" apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.23" apply false
  id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
  id("org.ajoberstar.grgit") version "5.2.2" apply false
}

subprojects {
  apply(from = "$rootDir/ext.gradle.kts")
}
