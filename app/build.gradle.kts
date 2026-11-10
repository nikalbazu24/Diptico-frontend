plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val numero = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val rutaFirma = System.getenv("DIPTICO_KEYSTORE")
val versionBase = if (rutaFirma.isNullOrBlank()) "1.0" else "1.1"
// Receptor de reportes de Nicolás (Apps Script, solo escribe en su carpeta privada de Drive).
// Una variable DIPTICO_REPORTES_URL en GitHub, si existe, lo reemplaza.
val reportesUrl = providers.environmentVariable("DIPTICO_REPORTES_URL").orNull.orEmpty().trim()
    .ifEmpty { "https://script.google.com/macros/s/AKfycbwGhiuCtOLOTclmEU8f3hukrAMOtHAFw4Y2MJnOdu5bKXPfEGegImctiuI-qpPhCCE/exec" }
// App de Discord de Díptico (Application ID del portal de Discord, con «Public Client» activado).
// Por defecto, la app «Díptico» de Nicolás. Si la variable está, manda ella.
val discordApp = providers.environmentVariable("DIPTICO_DISCORD_APP").orNull.orEmpty().trim()
    .ifEmpty { "1569677228269117580" }
require(discordApp.isEmpty() || Regex("""[0-9]{15,22}""").matches(discordApp)) { "DIPTICO_DISCORD_APP debe ser el Application ID (solo números)." }
// Kit oficial de Discord (Social SDK): su licencia no deja subirlo al repositorio público, así que GitHub Actions
// lo descifra en app/discord/ antes de compilar. Sin el kit, Díptico compila igual (sin amigos ni «Jugando a…»).
val kitDiscord = file("discord/discord_partner_sdk.aar")
val conKitDiscord = kitDiscord.exists()
val carpetaKitDiscord = layout.buildDirectory.dir("kit-discord").get().asFile
if (conKitDiscord) copy { from(zipTree(kitDiscord)) { include("prefab/**") }; into(carpetaKitDiscord) }
require(reportesUrl.isEmpty() || Regex("""https://script[.]google[.]com/macros/s/[A-Za-z0-9_-]+/exec""").matches(reportesUrl)) {
    "DIPTICO_REPORTES_URL debe ser el enlace /exec de la aplicación web de Apps Script."
}

android {
    namespace = "cl.nikalbazu.diptico"
    compileSdk = 34

    defaultConfig {
        applicationId = "cl.nikalbazu.diptico"
        minSdk = 29
        targetSdk = 34
        versionCode = numero
        versionName = "$versionBase-beta.$numero"
        buildConfigField("String", "REPORTES_URL", "\"$reportesUrl\"")
        buildConfigField("String", "DISCORD_APP", "\"$discordApp\"")
        buildConfigField("boolean", "DISCORD_KIT", "$conKitDiscord")
        // Para volver de «Autorizar» en Discord a Díptico (discord-<Application ID>:/authorize/callback).
        manifestPlaceholders["discordEsquema"] = "discord-$discordApp"
        if (conKitDiscord) externalNativeBuild { cmake { arguments += "-DKIT=${carpetaKitDiscord.absolutePath}" } }
        // La Thor (y casi todas las consolas Android) es arm64: así el APK no carga librerías que no usa.
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        if (!rutaFirma.isNullOrBlank()) create("publica") {
            storeFile = file(rutaFirma)
            storePassword = requireNotNull(System.getenv("DIPTICO_STORE_PASSWORD"))
            keyAlias = requireNotNull(System.getenv("DIPTICO_KEY_ALIAS"))
            keyPassword = requireNotNull(System.getenv("DIPTICO_KEY_PASSWORD"))
        }
    }

    buildTypes {
        getByName("release") {
            // Versión optimizada: Compose corre mucho más fluido que en la de depuración.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("publica") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    if (conKitDiscord) externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }

    buildFeatures {
        buildConfig = true
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
                it.testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
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
    // Kit de Discord: amigos, inicio de sesión y «Jugando a…». El kit usa androidx.browser para «Autorizar».
    if (conKitDiscord) implementation(files(kitDiscord))
    implementation("androidx.browser:browser:1.8.0")

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

