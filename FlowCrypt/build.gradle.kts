/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

import com.android.ddmlib.DdmPreferences
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

//Setting global timeout for apk installation to 10 minutes. We need it for CI
DdmPreferences.setTimeOut(10 * 60 * 1000)

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("androidx.navigation.safeargs.kotlin")
  id("com.starter.easylauncher")
  id("kotlin-parcelize")
}

val keystoreProperties = Properties()
val propertiesFile = project.file("keystore.properties")
if (propertiesFile.exists()) {
  keystoreProperties.load(FileInputStream(propertiesFile))
}

android {
  /*if (projects.hasProperty("devBuild")) {
    splits.density.enable = false
    aaptOptions.cruncherEnable = false
  }*/

  compileSdkVersion(33)
  buildToolsVersion("33")
  namespace = "com.flowcrypt.email"

  defaultConfig {
    applicationId = "com.flowcrypt.email"
    minSdkVersion(26)
    targetSdkVersion(33)
    versionCode = 12
    versionName = "1212"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // The following argument makes the Android Test Orchestrator run its
    // "pm clear" command after each test invocation. This command ensures
    // that the app"s state is completely cleared between tests.
    //testInstrumentationRunnerArguments clearPackageData: "true"
    multiDexEnabled = true

    // used by Room, to test migrations
    javaCompileOptions {
      annotationProcessorOptions {
        //arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
      }
    }
  }

  signingConfigs {
    create("release") {
      var keyStoreFile = keystoreProperties["storeFile"]
      var keyStorePass = keystoreProperties["storePassword"] ?: ""
      var keySignAlias = keystoreProperties["keyAlias"]
      var keyPass = keystoreProperties["keyPassword"]

      if (project.hasProperty("runtimeSign")) {
        if (project.hasProperty("storeFile")) {
          keyStoreFile = project.property("storeFile")
        }

        if (project.hasProperty("storePassword")) {
          keyStorePass = project.property("storePassword") as String
        }

        if (project.hasProperty("keyAlias")) {
          keySignAlias = project.property("keyAlias")
        }

        if (project.hasProperty("keyPassword")) {
          keyPass = project.property("keyPassword")
        }
      }

      storeFile = if (keyStoreFile != null) file(keyStoreFile) else file("fix me...")
      storePassword = ""//keyStorePass ?: ""
      keyAlias = ""//keySignAlias
      keyPassword = ""//keyPass
    }

    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "flowcryptdebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    getByName("release") {
      isShrinkResources = false
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")

      buildConfigField("boolean", "IS_ACRA_ENABLED", "true")
      buildConfigField("boolean", "IS_MAIL_DEBUG_ENABLED", "false")
      buildConfigField("boolean", "IS_HTTP_LOG_ENABLED", "false")
      buildConfigField("String", "HTTP_LOG_LEVEL", "\"NONE\"")
      buildConfigField("String", "ATTESTER_URL", "\"https://flowcrypt.com/attester/\"")
      buildConfigField(
        "String",
        "SHARED_TENANT_FES_URL",
        "\"https://flowcrypt.com/shared-tenant-fes/\""
      )
      buildConfigField("String", "BACKEND_URL", "\"https://flowcrypt.com/api/\"")
      resValue("string", "gradle_is_acra_enabled", "true")
      resValue("string", "gradle_is_mail_debug_enabled", "false")
      resValue("string", "gradle_is_http_log_enabled", "false")
      resValue("string", "gradle_http_log_level", "NONE")
    }

    getByName("debug") {
      initWith(getByName("release"))
      isDebuggable = true
      versionNameSuffix =
        "_" + defaultConfig.versionCode + "__" + SimpleDateFormat("yyyy_MM_dd").format(Date())
      applicationIdSuffix = ".debug"
      signingConfig = signingConfigs.getByName("debug")

      buildConfigField("boolean", "IS_ACRA_ENABLED", "false")
      buildConfigField("boolean", "IS_HTTP_LOG_ENABLED", "true")
      buildConfigField("String", "HTTP_LOG_LEVEL", "\"BODY\"")
      resValue("string", "gradle_is_acra_enabled", "false")
      resValue("string", "gradle_is_http_log_enabled", "true")
      resValue("string", "gradle_http_log_level", "BODY")
    }

    create("uiTests") {
      initWith(getByName("debug"))
      versionNameSuffix =
        "_test_" + defaultConfig.versionCode + "__" + SimpleDateFormat("yyyy_MM_dd").format(Date())
      buildConfigField("String", "ATTESTER_URL", "\"https://flowcrypt.test/attester/\"")
      buildConfigField(
        "String",
        "SHARED_TENANT_FES_URL",
        "\"https://flowcrypt.test/shared-tenant-fes/\""
      )
      buildConfigField("String", "BACKEND_URL", "\"https://flowcrypt.test/backend/\"")
    }
  }

  testBuildType = "uiTests"

  sourceSets {
    //androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
  }

  //flavorDimensions = "standard"

  productFlavors {
    //This flavor must be used only for a development.
    //It has settings for a fast building (some features are disabled or not included).
    create("dev") {
      dimension = "standard"
      versionNameSuffix = "_dev"
      //resourceConfigurations += ["en", "xxhdpi"]
      buildConfigField("boolean", "IS_MAIL_DEBUG_ENABLED", "true")
      resValue("string", "gradle_is_mail_debug_enabled", "true")
    }

    //This is a consumer flavor
    create("consumer") {
      dimension = "standard"
    }

    //This is an enterprise flavor
    create("enterprise") {
      dimension = "standard"
      applicationIdSuffix = ".enterprise"

      buildConfigField("boolean", "IS_ACRA_ENABLED", "false")
      resValue("string", "gradle_is_acra_enabled", "false")
    }

    /*applicationVariants.all { variant ->
      //variant.resValue("string", "application_id", variant.applicationId)
    }*/
  }

  /*variantFilter { variant ->
    if (variant?.name in ["devRelease", "devUiTests"]) {
      println("Excluded \"$variant.name\" from build variant list as unused")
      // Gradle ignores any variants that satisfy the conditions above.
      setIgnore(true)
    }
  }*/

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    buildConfig = true
    viewBinding = true
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    //freeCompilerArgs += ["-opt-in=kotlin.RequiresOptIn"]
  }

  packagingOptions {
    //resources {
    //excludes += [
    //  "META-INF/DEPENDENCIES",
    // "META-INF/LICENSE.md",
    //  "META-INF/NOTICE.md",
    // "META-INF/*.SF",
    //"META-INF/*.DSA",
    //"META-INF/*.RSA"
    // ]
  }
}

//lint {
// warningsAsErrors = true
//}


configurations {
  //devDebugImplementation {}
}

/*easylauncher {
  buildTypes {
    debug {
      filters = [
        chromeLike(
          ribbonColor: "#6600CC",
      labelColor: "#FFFFFF",
      position: "top",
      overlayHeight: 0.25,
      textSizeRatio: 0.2
      )
      ]
    }

    uiTests {
      filters = [
        chromeLike(
          label: "test",
      ribbonColor: "#E91E63",
      labelColor: "#FFFFFF",
      position: "top",
      overlayHeight: 0.25,
      textSizeRatio: 0.2
      )
      ]
    }
  }

  variants {
    devDebug {
      filters = [
        chromeLike(
          label: "dev",
      ribbonColor: "#CC5F00",
      labelColor: "#FFFFFF",
      position: "top",
      overlayHeight: 0.25,
      textSizeRatio: 0.2
      )
      ]
    }
  }
}*/

dependencies {
  kapt("com.github.bumptech.glide:compiler:4.15.1")
  kapt("androidx.annotation:annotation:1.6.0")
  kapt("androidx.room:room-compiler:2.5.2")
  //ACRA needs the following dependency to use a custom report sender
  kapt("com.google.auto.service:auto-service:1.1.1")

  //devDebugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
  //noinspection FragmentGradleConfiguration. uiTests is the build type for testing.
  //uiTestsImplementation("androidx.fragment:fragment-testing:1.6.1")

  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
  androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
  androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test:rules:1.5.0")
  androidTestImplementation("androidx.test:core-ktx:1.5.0")
  androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
  androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
  androidTestImplementation("androidx.room:room-testing:2.5.2")
  androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
  androidTestImplementation("androidx.work:work-testing:2.8.1")
  androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
  androidTestImplementation("com.squareup.okhttp3:okhttp-tls:4.11.0")
  androidTestImplementation("com.athaydes.rawhttp:rawhttp-core:2.5.2")
  androidTestUtil("androidx.test:orchestrator:1.4.2")

  testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("androidx.room:room-testing:2.5.2")
  testImplementation("org.robolectric:robolectric:4.10.3")
  testImplementation("io.github.classgraph:classgraph:4.8.161")
  testImplementation("com.flextrade.jfixture:jfixture:2.7.2")
  testImplementation("com.shazam:shazamcrest:0.11")
  //we need it to test Parcelable
  testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")

  //implementation fileTree (dir: "libs", include: ["*.jar"])

  //it fixed compilation issue https://github.com/FlowCrypt/flowcrypt-android/pull/2064.
  //Should be reviewed and removed when more dependencies will be updated
  implementation("androidx.test:monitor:1.6.1")

  implementation("androidx.legacy:legacy-support-v4:1.0.0")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.legacy:legacy-preference-v14:1.0.0")
  implementation("androidx.cardview:cardview:1.0.0")
  implementation("androidx.browser:browser:1.5.0")
  implementation("androidx.recyclerview:recyclerview:1.3.1")
  implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.test.espresso:espresso-idling-resource:3.5.1")
  implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
  implementation("androidx.lifecycle:lifecycle-process:2.6.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
  implementation("androidx.room:room-runtime:2.5.2")
  implementation("androidx.room:room-ktx:2.5.2")
  implementation("androidx.paging:paging-runtime-ktx:2.1.2")
  implementation("androidx.preference:preference-ktx:1.2.0")
  implementation("androidx.core:core-ktx:1.10.1")
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation("androidx.activity:activity-ktx:1.7.2")
  implementation("androidx.fragment:fragment-ktx:1.6.1")
  implementation("androidx.work:work-runtime-ktx:2.8.1")
  implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
  implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
  implementation("androidx.navigation:navigation-runtime-ktx:2.6.0")
  implementation("androidx.webkit:webkit:1.7.0")

  implementation("com.google.android.gms:play-services-base:18.2.0")
  implementation("com.google.android.gms:play-services-auth:20.6.0")
  implementation("com.google.android.material:material:1.9.0")
  implementation("com.google.android.flexbox:flexbox:3.0.0")

  //https://mvnrepository.com/artifact/com.google.code.gson/gson
  implementation("com.google.code.gson:gson:2.10.1")
  //https://mvnrepository.com/artifact/com.google.api-client/google-api-client-android
  implementation("com.google.api-client:google-api-client-android:2.2.0")
  //https://mvnrepository.com/artifact/com.google.apis/google-api-services-gmail
  implementation("com.google.apis:google-api-services-gmail:v1-rev20230612-2.0.0")

  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
  implementation("com.squareup.okio:okio:3.4.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

  implementation("com.sun.mail:jakarta.mail:2.0.1")
  implementation("com.sun.activation:jakarta.activation:2.0.1")
  implementation("com.sun.mail:gimap:2.0.1") {
    //exclude group: "com.sun.mail" to prevent compilation errors
    //exclude group ("com.sun.mail")
  }

  implementation("org.pgpainless:pgpainless-core:1.6.1")

  implementation("com.github.bumptech.glide:glide:4.15.1")
  implementation("com.nulab-inc:zxcvbn:1.8.0")
  implementation("commons-io:commons-io:2.13.0")
  implementation("com.burhanrashid52:photoeditor:3.0.1")
  implementation("net.openid:appauth:0.11.1")
  implementation("org.bitbucket.b_c:jose4j:0.9.3")
  implementation("io.github.everythingme:overscroll-decor-android:1.1.1")
  implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20220608.1")
  implementation("org.jsoup:jsoup:1.16.1")
  implementation("com.sandinh:zbase32-commons-codec_2.12:1.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("ch.acra:acra-http:5.11.0")
  //ACRA needs the following dependency to use a custom report sender
  implementation("com.google.auto.service:auto-service-annotations:1.1.1")

  constraints {
    //due to https://github.com/FlowCrypt/flowcrypt-security/issues/199
    implementation("commons-codec:commons-codec:1.16.0") {
      because("version 1.11 has VULNERABILITY DESCRIPTION CWE-200")
    }
  }
}
