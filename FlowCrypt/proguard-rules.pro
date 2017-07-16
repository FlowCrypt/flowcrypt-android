#/*
# * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
# * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
# * Contributors: DenBond7
# */

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\AndroidSdkTools/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

####################################### Application config #########################################
-keep class com.flowcrypt.email.api.email.** { *; }
-keep class com.flowcrypt.email.api.retrofit.request.model.** { *; }
-keep class com.flowcrypt.email.api.retrofit.response.base.** { *; }
-keep class com.flowcrypt.email.database.dao.** { *; }
-keep class com.flowcrypt.email.model.** { *; }
-keep class com.flowcrypt.email.js.** { *; }
-keepnames class hotels.car.rentals.flights.ukasoft.util.GlideConfiguration

# Remove Logging statements
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** e(...);
    public static *** i(...);
}

########################################## GSON ####################################################
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

########################################## JAVA ####################################################
-keep class com.sun.** { *; }
-keep class javax.** { *; }
-keep interface com.sun.** { *; }
-keep interface javax.** { *; }
-dontwarn com.sun.**
-dontwarn javax.**

######################################### GOOGLE ###################################################
-keep class com.google.** { *; }
-keep interface com.google.** { *; }
-dontwarn com.google.**

######################################### SQUARE ###################################################
-keep class com.squareup.** { *; }
-keep class retrofit2.** { *; }
-keep class okio.** { *; }
-keep class rx.** { *; }
-keep class okhttp3.** { *; }

-keep interface com.squareup.** { *; }
-keep interface retrofit2.** { *; }
-keep interface okio.** { *; }
-keep interface rx.** { *; }
-keep interface okhttp3.** { *; }

-dontwarn com.squareup.**
-dontwarn okio.**
-dontwarn rx.**
-dontwarn okhttp3.**

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

######################################### APACHE ###################################################
-keep class org.apache.** { *; }
-keep interface org.apache.** { *; }
-dontwarn org.apache.**

######################################### ACRA #####################################################
-keep class org.acra.** { *; }
-keep interface org.acra.** { *; }
-keep public class * implements org.acra.sender.ReportSenderFactory { public <methods>; }
-dontwarn org.acra.**

##################################### ECLIPSESOURCE ################################################
-keep class com.eclipsesource.** { *; }
-keep interface com.eclipsesource.** { *; }
-dontwarn com.eclipsesource.**

######################################### NACHOS ###################################################
-keep class com.hootsuite.nachos.** { *; }
-keep interface com.hootsuite.nachos.** { *; }
-dontwarn com.hootsuite.nachos.**
