import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

group = "com.example.todo"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
    }
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.example.todo.MainKt"
        // Skiko loads native libraries; granting access up front avoids JDK 24+ restricted-method warnings.
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            // jpackage can only build installers for the host OS:
            // Deb/Rpm/AppImage on Linux, Msi/Exe on Windows.
            targetFormats(
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage,
                TargetFormat.Msi,
                TargetFormat.Exe,
            )
            packageName = "TodoDesktop"
            packageVersion = "1.0.0"
            description = "A simple to-do list built with Compose Multiplatform"
            vendor = "Example"

            linux {
                shortcut = true
                menuGroup = "Office"
            }
            windows {
                shortcut = true
                menu = true
                // Keep this UUID stable across releases so MSI upgrades replace the old version.
                upgradeUuid = "8c5a9f0e-3c1b-4d7e-9b2a-6f4e2d1c0a11"
            }
        }
    }
}
