plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    namespace = "com.example.composeuikit"
    compileSdk = libs.versions.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.compileSdkMinor.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                // JitPack's coordinate for a module in a multi-module repo:
                // com.github.<user>.<repo>:<module>:<tag>
                groupId = "com.github.DrNitz.compose-uikit"
                artifactId = "lib"
                version = "v0.1.0"
            }
        }
    }
}

dependencies {
    // api(): consumers of this library get these transitively.
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // BackHandler, so the reader closes on the system back gesture.
    implementation(libs.androidx.activity.compose)

    debugImplementation(libs.compose.ui.tooling)
}
