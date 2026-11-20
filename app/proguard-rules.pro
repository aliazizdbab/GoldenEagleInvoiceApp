# Keep kotlinx.serialization classes for backup/restore
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.goldeneagle.invoice.**$$serializer { *; }
-keepclassmembers class com.goldeneagle.invoice.** {
    *** Companion;
}
-keepclasseswithmembers class com.goldeneagle.invoice.** {
    kotlinx.serialization.KSerializer serializer(...);
}
