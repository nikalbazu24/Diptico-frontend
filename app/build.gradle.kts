plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val numero = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "cl.nikalbazu.diptico"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.nikalbazu.diptico"
        minSdk = 29
        targetSdk = 34
        versionCode = numero
        versionName = "1.0-beta.$numero"
        // La Thor (y casi todas las consolas Android) es arm64: así el APK no carga librerías que no usa.
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("diptico-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            // Versión optimizada: Compose corre mucho más fluido que en la de depuración.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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

    // Dos versiones del mismo Díptico:
    // "completa" (consolas) y "movil" (teléfonos): sin el acceso a notificaciones, que Google Play Protect
    // bloquea en teléfonos cuando la app no viene de Play Store. En la móvil no hay widget de música ni notificaciones.
    flavorDimensions += "tipo"
    productFlavors {
        create("completa") { dimension = "tipo" }
        create("movil") { dimension = "tipo" }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.maxHeapSize = "3g"
            }
        }
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    // Aplica los perfiles de arranque (de Compose y de Díptico) aunque el APK no venga de Play Store: arranque y listas más fluidos.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // IA local (Bisa sin internet ni créditos): modelos .task de MediaPipe
    implementation("com.google.mediapipe:tasks-genai:0.10.27")
    // Códigos QR (para abrir en el teléfono las páginas de las claves)
    implementation("com.google.zxing:core:3.5.3")

    // Capturas automáticas de las pantallas (solo para revisar el diseño, no van en la app)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.26.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
