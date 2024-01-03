/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  id("com.android.application") version "8.1.4" apply false
  id("org.jetbrains.kotlin.android") version "1.9.21" apply false
  id("androidx.navigation.safeargs.kotlin") version "2.6.0" apply false
  id("com.starter.easylauncher") version "6.2.0" apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.21" apply false
  id("com.google.devtools.ksp") version "1.9.22-1.0.16" apply false
  id("org.ajoberstar.grgit") version "5.2.1" apply false
}

subprojects {
  apply(from = "$rootDir/ext.gradle.kts")
}
