# Retrofit / Gson
-keepattributes Signature, *Annotation*
-keep class com.cambio.converter.data.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
