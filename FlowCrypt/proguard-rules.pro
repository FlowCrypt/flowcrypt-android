#/*
# * © 2016-2019 FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# * Contributors: DenBond7
# */

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the default proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

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

-optimizationpasses 1

############################################# Android #################################################################
# Remove Logging statements
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** e(...);
    public static *** i(...);
}

# See details here https://stackoverflow.com/questions/33047806/proguard-duplicate-definition-of-library-class/35742739#35742739
-dontnote org.apache.commons.**

####################################### Application config ############################################################
-dontobfuscate
# Keep classes with @Expose annotation.
-keepclasseswithmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

-dontnote com.flowcrypt.email.api.retrofit.**
-dontnote com.flowcrypt.email.test.ui.widget.**

########################################## GSON #######################################################################
# Details here https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Add by DenBond7
-dontnote sun.misc.Unsafe

########################################## okhttp3.pro ################################################################
# Details here https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# Add by DenBond7
# in the following class we have code which reffer to code which exists on the device. So we can tp turn off those notes
-dontnote okhttp3.internal.platform.**

########################################## retrofit2.pro ##############################################################
# Details here https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

########################################## okio.pro ###################################################################
# Details here https://github.com/square/okio/blob/master/okio/jvm/src/main/resources/META-INF/proguard/okio.pro
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

########################################## proguard_guava.pro #########################################################
# Details here https://github.com/google/guava/wiki/UsingProGuardWithGuava

-dontwarn javax.lang.model.element.Modifier

# Note: We intentionally don't add the flags we'd need to make Enums work.
# That's because the Proguard configuration required to make it work on
# optimized code would preclude lots of optimization, like converting enums
# into ints.

# Throwables uses internal APIs for lazy stack trace resolution
-dontnote sun.misc.SharedSecrets
-keep class sun.misc.SharedSecrets {
  *** getJavaLangAccess(...);
}
-dontnote sun.misc.JavaLangAccess
-keep class sun.misc.JavaLangAccess {
  *** getStackTraceElement(...);
  *** getStackTraceDepth(...);
}

# FinalizableReferenceQueue calls this reflectively
# Proguard is intelligent enough to spot the use of reflection onto this, so we
# only need to keep the names, and allow it to be stripped out if
# FinalizableReferenceQueue is unused.
-keepnames class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
# However, it cannot "spot" that this method needs to be kept IF the class is.
-keepclassmembers class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
-keepnames class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}
-keepclassmembers class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}

# Striped64, LittleEndianByteArray, UnsignedBytes, AbstractFuture
-dontwarn sun.misc.Unsafe

# Striped64 appears to make some assumptions about object layout that
# really might not be safe. This should be investigated.
-keepclassmembers class com.google.common.cache.Striped64 {
  *** base;
  *** busy;
}
-keepclassmembers class com.google.common.cache.Striped64$Cell {
  <fields>;
}

-dontwarn java.lang.SafeVarargs

# I've disabled it because Throwable is a library class and ProGuard always leaves underlying libraries unchanged
#-keep class java.lang.Throwable {
#  *** addSuppressed(...);
#}

# Futures.getChecked, in both of its variants, is incompatible with proguard.

# Used by AtomicReferenceFieldUpdater and sun.misc.Unsafe
-keepclassmembers class com.google.common.util.concurrent.AbstractFuture** {
  *** waiters;
  *** value;
  *** listeners;
  *** thread;
  *** next;
}
-keepclassmembers class com.google.common.util.concurrent.AtomicDouble {
  *** value;
}
-keepclassmembers class com.google.common.util.concurrent.AggregateFutureState {
  *** remaining;
  *** seenExceptions;
}

# Since Unsafe is using the field offsets of these inner classes, we don't want
# to have class merging or similar tricks applied to these classes and their
# fields. It's safe to allow obfuscation, since the by-name references are
# already preserved in the -keep statement above.
-keep,allowshrinking,allowobfuscation class com.google.common.util.concurrent.AbstractFuture** {
  <fields>;
}

# Futures.getChecked (which often won't work with Proguard anyway) uses this. It
# has a fallback, but again, don't use Futures.getChecked on Android regardless.
-dontwarn java.lang.ClassValue

# MoreExecutors references AppEngine
-dontnote com.google.appengine.api.ThreadManager
-keep class com.google.appengine.api.ThreadManager {
  static *** currentRequestThreadFactory(...);
}
-dontnote com.google.apphosting.api.ApiProxy
-keep class com.google.apphosting.api.ApiProxy {
  static *** getCurrentEnvironment (...);
}

########################################## JAVAMAIL ###################################################################
-keep class com.sun.** { *; }
-keep class javax.** { *; }
-keep interface com.sun.** { *; }
-keep interface javax.** { *; }
-dontwarn com.sun.**
-dontnote com.sun.**
-dontwarn javax.**
-dontnote javax.**

######################################### APACHE ######################################################################
-keep class org.apache.** { *; }
-keep interface org.apache.** { *; }
-dontwarn org.apache.**

######################################### ACRA ########################################################################
-keep class org.acra.** { *; }
-keep interface org.acra.** { *; }
-keep public class * implements org.acra.sender.ReportSenderFactory { public <methods>; }
-dontwarn org.acra.**

##################################### ECLIPSESOURCE ###################################################################
-keep class com.eclipsesource.** { *; }
-keep interface com.eclipsesource.** { *; }
-keep enum com.eclipsesource.** { *; }
-keepclassmembers class com.eclipsesource.** { *; }
-keepclassmembers interface com.eclipsesource.** { *; }
-keepclassmembers enum com.eclipsesource.** { *; }
-keepnames class com.eclipsesource.** { *; }
-keepnames interface com.eclipsesource.** { *; }
-keepnames enum com.eclipsesource.** { *; }
-keepclassmembernames class com.eclipsesource.** { *; }
-keepclassmembernames interface com.eclipsesource.** { *; }
-keepclassmembernames enum com.eclipsesource.** { *; }
-dontwarn com.eclipsesource.**

######################################### NACHOS ######################################################################
-keep class com.hootsuite.nachos.** { *; }
-keep interface com.hootsuite.nachos.** { *; }
-dontwarn com.hootsuite.nachos.**

########################################## GLIDE ######################################################################
-keep class com.bumptech.glide.** { *; }
-keep interface com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.**

########################################## JUNIT ######################################################################
-keep class org.junit.** { *; }
-keep interface org.junit.** { *; }
-dontwarn org.junit.**

########################################## org.w3c.dom ################################################################
-keep class org.w3c.dom.** { *; }
-keep interface org.w3c.dom.** { *; }
-dontwarn org.w3c.dom.**

########################################## BOUNCYCASTLE ###############################################################
-keep class org.bouncycastle.** { *; }
-keep interface org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

########################################## Play services ##############################################################
-dontnote com.google.android.gms.**

########################################## Material library ###########################################################
#https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md
-dontnote com.google.android.material.**

########################################## Google API client ##########################################################
-dontnote io.grpc.Context
# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection.
# See details here https://developers.google.com/api-client-library/java/google-http-java-client/setup#proguard

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
