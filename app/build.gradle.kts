import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ambientalia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ambientalia"
        minSdk = 26
        targetSdk = 35
        // Un único esquema de versión para CI y local: el número de commits del repo.
        // Así el APK de Actions y el de Android Studio se actualizan entre sí
        // (GITHUB_RUN_NUMBER solo existía en CI y dejaba al build local en 1).
        versionCode = runCatching {
            val proceso = ProcessBuilder("git", "rev-list", "--count", "HEAD")
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
            val texto = proceso.inputStream.bufferedReader().readText().trim()
            if (proceso.waitFor() == 0) texto.toInt() else 1
        }.getOrDefault(1)
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Misma key para todos los builds:
        //  · en CI llega por variables de entorno (secretos del repo)
        //  · en local se lee keystore/keystore.env (gitignorado)
        val local = Properties().apply {
            val archivo = rootProject.file("keystore/keystore.env")
            if (archivo.exists()) archivo.inputStream().use { load(it) }
        }
        fun param(nombre: String): String? =
            System.getenv(nombre)?.takeIf { it.isNotBlank() }
                ?: local.getProperty(nombre)?.trim()?.takeIf { it.isNotBlank() }

        val keystorePath = param("ANDROID_KEYSTORE_FILE")
        val almacen = (if (keystorePath.isNullOrBlank()) null else file(keystorePath))
            ?.takeIf { it.exists() }
            ?: rootProject.file("keystore/ambientalia.jks").takeIf { it.exists() }
        if (almacen != null) {
            val claveAlmacen = param("KEYSTORE_PASSWORD")
            val clave = param("KEY_ALIAS")
            val claveClave = param("KEY_PASSWORD")

            create("release") {
                storeFile = almacen
                storePassword = claveAlmacen
                keyAlias = clave
                keyPassword = claveClave
            }
            // La misma key para debug: así cada build de CI actualiza al anterior
            val configDebug = findByName("debug") ?: create("debug")
            configDebug.storeFile = almacen
            configDebug.storePassword = claveAlmacen
            configDebug.keyAlias = clave
            configDebug.keyPassword = claveClave
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Misma key que release: así cada build (CI o Android Studio) actualiza al anterior.
            signingConfig = signingConfigs.findByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
}
