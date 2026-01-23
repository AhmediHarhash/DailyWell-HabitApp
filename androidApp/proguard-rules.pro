# Keep Kotlin serialization
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
