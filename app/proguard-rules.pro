# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.sppidy.janus.model.** { *; }
-keepclassmembers class com.sppidy.janus.model.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.serialization — required for Navigation type-safe routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static **$* *;
    *** Companion;
}
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sppidy.janus.navigation.**$$serializer { *; }
-keepclassmembers class com.sppidy.janus.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.sppidy.janus.navigation.*Route { *; }
-keep class com.sppidy.janus.navigation.*Route$* { *; }

# Markdown renderer (mikepenz/multiplatform-markdown-renderer) + intellij-markdown parser
-keep class com.mikepenz.markdown.** { *; }
-dontwarn com.mikepenz.markdown.**
-keep class org.intellij.markdown.** { *; }
-dontwarn org.intellij.markdown.**
