import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.cambio.converter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cambio.converter"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"
        vectorDrawables.useSupportLibrary = true

        // Só ARM: o x86/x86_64 só serve para emulador (não usamos) e pesa no APK.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // O resources.arsc vinha com ~50 idiomas de bibliotecas AndroidX que o
        // app nunca mostra. Fica só inglês e português.
        resourceConfigurations += listOf("en", "pt")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Lixo que só serve à ferramenta de build / reflexão Kotlin (não usamos).
        resources.excludes += setOf(
            "/META-INF/*.version",
            "kotlin-tooling-metadata.json",
            "kotlin/**/*.kotlin_builtins",
            "kotlin/*.kotlin_builtins",
            "DebugProbesKt.bin",
            // Lista de domínios do OkHttp: só importa para cookies de navegador.
            "okhttp3/internal/publicsuffix/**"
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.composables:icons-lucide:1.1.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
