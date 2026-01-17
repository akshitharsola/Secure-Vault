# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line number information for debugging stack traces (ENABLED)
-keepattributes SourceFile,LineNumberTable

# Keep source file names for better crash reports
-renamesourcefileattribute SourceFile

# ============================================================================
# SecureVault-specific ProGuard Rules
# ============================================================================

# Room Database - Prevent obfuscation of entities and DAOs
-keep class com.securevault.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Security Classes - Critical for encryption/decryption
-keep class com.securevault.utils.SecurityManager { *; }
-keep class com.securevault.utils.BackupEncryption { *; }
-keep class com.securevault.utils.BiometricHelper { *; }

# Data Models - Used for Gson serialization
-keep class com.securevault.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Gson specific rules - CRITICAL for backup/restore
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# CRITICAL: Keep TypeToken and generic signatures for backup restore
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
   *;
}

# CRITICAL: Preserve all fields and generic signatures in data models
-keepclassmembers class com.securevault.data.model.** {
    <fields>;
    <init>(...);
}

# Keep generic signature of Password List for TypeToken
-keep,allowobfuscation,allowshrinking class com.securevault.data.model.Password
-keep,allowobfuscation,allowshrinking class * implements java.util.List

# Keep UpdateManager for version checking
-keep class com.securevault.utils.UpdateManager { *; }
-keep class com.securevault.utils.UpdateManager$UpdateInfo { *; }

# Jetpack Compose - Recommended rules
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# Bouncy Castle Post-Quantum Cryptography (PQC) Rules
# ============================================================================

# Keep all Bouncy Castle PQC classes - needed for quantum-resistant encryption
-keep class org.bouncycastle.pqc.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-keep class org.bouncycastle.jcajce.** { *; }

# Keep Bouncy Castle providers
-keep class org.bouncycastle.jce.provider.** { *; }

# Don't warn about Bouncy Castle optional dependencies
-dontwarn org.bouncycastle.**

# Keep quantum backup encryption class
-keep class com.securevault.utils.QuantumBackupEncryption { *; }