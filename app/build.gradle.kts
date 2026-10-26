import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// AGP 9 compiles Kotlin natively (built-in Kotlin); only the Compose compiler plugin is applied
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Release signing, first match wins: keystore.properties (git-ignored), CHANNELLOGOS_* environment
// variables, or the TV apps' shared keystore ~/.signing/babadogan-tv-release.p12, whose password
// is the macOS Keychain item "babadogan-tv-release-keystore".
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)
val sharedKeystore = File(System.getProperty("user.home"), ".signing/babadogan-tv-release.p12")
val releaseStoreFile = signingValue("storeFile", "CHANNELLOGOS_KEYSTORE_FILE") ?: sharedKeystore.takeIf { it.isFile }?.path
// macOS only; elsewhere the password comes from keystore.properties or the environment.
fun keychainPassword(): String? = runCatching {
    providers.exec {
        commandLine("security", "find-generic-password", "-s", "babadogan-tv-release-keystore", "-w")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrNull()?.ifEmpty { null }
val releaseStorePassword = releaseStoreFile?.let { signingValue("storePassword", "CHANNELLOGOS_KEYSTORE_PASSWORD") ?: keychainPassword() }

android {
    namespace = "com.berkbb.channellogos"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.berkbb.channellogos"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.4.5"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = signingValue("keyAlias", "CHANNELLOGOS_KEY_ALIAS") ?: "babadogan-tv"
                keyPassword = signingValue("keyPassword", "CHANNELLOGOS_KEY_PASSWORD") ?: releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            // TvLogoTool is started by name through app_process; minification would rename it.
            isMinifyEnabled = false
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                // Keeps local builds working; debug-signed APKs cannot update installed TVs
                logger.warn("Channel Logos: no release keystore (keystore.properties, CHANNELLOGOS_KEYSTORE_FILE or ~/.signing/babadogan-tv-release.p12); signing release with the debug key.")
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Pure Kotlin/Java dependencies; the only native code is Compose's ~10 KB androidx.graphics.path per ABI, so one universal APK serves every ABI
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    // ADB client: the app talks to the TV's own adbd to act with shell rights
    implementation(libs.dadb) {
        // dadb 1.2.9 publishes its JUnit test libraries as runtime dependencies
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.opentest4j")
        exclude(group = "org.apiguardian")
    }
    // Draws picons' SVG logos into bitmaps (Android has no SVG decoder)
    implementation(libs.androidsvg)

    testImplementation(libs.junit)
}
