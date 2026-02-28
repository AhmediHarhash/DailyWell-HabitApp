# ============================================================
# DailyWell ProGuard/R8 Security Rules
# CVE-DW-007 FIX: Comprehensive obfuscation and protection
# OWASP MASVS-CODE-2: Reverse Engineering Protection
# ============================================================

# ==================== KOTLIN SERIALIZATION ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.dailywell.**$$serializer { *; }
-keepclassmembers class com.dailywell.** {
    *** Companion;
}
-keepclasseswithmembers class com.dailywell.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== THIRD PARTY LIBS ====================
# Sherpa-ONNX for Piper TTS neural voice synthesis
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** { *; }

# Ktor HTTP client
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }

# ==================== SECURITY HARDENING ====================

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove println statements
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
    public static void print(...);
}

# Remove Timber logging if used
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}

# ==================== DEBUG INFO REMOVAL ====================

# Remove source file and line numbers (makes stack traces harder to read but more secure)
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove debug info
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# ==================== OBFUSCATION ENHANCEMENTS ====================

# Use more aggressive obfuscation
-repackageclasses ''
-allowaccessmodification

# Optimize aggressively
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Obfuscate package names
-flattenpackagehierarchy 'a'

# ==================== ANTI-TAMPERING ====================

# Keep critical security classes but obfuscate internals
-keep class com.dailywell.app.billing.BillingManager {
    public <methods>;
}

# Protect API configuration (but still obfuscate internal implementation)
-keep class com.dailywell.app.api.ApiConfig {
    public static <methods>;
}

# ==================== NATIVE CODE PROTECTION ====================

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==================== ENUM PROTECTION ====================

# Keep enum values (needed for serialization)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== REFLECTION PROTECTION ====================

# Keep classes accessed via reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ==================== CRASH REPORTING ====================

# Keep crash info for debugging (optional - remove for maximum obfuscation)
-keepattributes Exceptions

# ==================== HEALTH CONNECT ====================

# Keep Health Connect SDK classes
-keep class androidx.health.connect.client.** { *; }
-keep class androidx.health.** { *; }
