# Reglas de R8 / ProGuard para AmbientalIA

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Modelos de la API de JuanAI (paquete com.ambientalia.ai)
-keep,includedescriptorclasses class com.ambientalia.ai.**$$serializer { *; }
-keepclassmembers class com.ambientalia.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.ambientalia.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.ambientalia.ai.** { *; }

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Kotlin ---
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
