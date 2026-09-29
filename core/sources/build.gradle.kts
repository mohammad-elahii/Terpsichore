plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.Mohammad.Elahi.terpsichore.core.sources"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
