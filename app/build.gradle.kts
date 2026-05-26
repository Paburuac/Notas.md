plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)              // procesador de Room
}

android {
    namespace = "com.tuapp.notasmd"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tuapp.notasmd"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.compose.material:material-icons-extended")
    // Core de Android
    implementation(libs.androidx.core.ktx)

    // Lifecycle y ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose (el BOM alinea las versiones de todas las libs de Compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navegación entre pantallas
    implementation(libs.androidx.navigation.compose)

    // Room — base de datos local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)     // genera código en tiempo de compilación

    // DataStore — guardar preferencias (tema día/noche, etc.)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines — para operaciones de BD sin bloquear la UI
    implementation(libs.kotlinx.coroutines.android)

    // Markwon — renderizar Markdown en la vista previa
    implementation(libs.markwon.core)
    implementation(libs.markwon.html)    // necesario para los <span> de color
    implementation(libs.markwon.table)

    // Solo para desarrollo (previews en Android Studio)
    debugImplementation(libs.androidx.ui.tooling)
}