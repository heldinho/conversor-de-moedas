# Gson lê os DTOs por reflexão: nomes de campos precisam sobreviver ao R8.
-keep class com.cambio.converter.data.** { *; }

# Assinaturas genéricas e anotações exigidas por Gson e Retrofit.
# O próprio Retrofit já embute as regras dele dentro do JAR
# (META-INF/proguard/retrofit2.pro), então não guardamos retrofit2.** inteiro —
# manter só o que é necessário deixa o dex menor.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
