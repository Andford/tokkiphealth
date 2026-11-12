// Tokkip himself: his drawing, motion and sounds, copied from Tokkip
// (Andford/tokkip, android/shared character/). Change him there, not here.
plugins {
    id("com.android.library")
    id("projectConfig")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "mx.loperstack.tokkip.shared"

    setupLibraryDefaults(projectConfig)

    setupModuleBuildTypes()

    buildFeatures {
        compose = true
    }

    setupCompileOptions()
}

setupKotlinOptions(compose = true)

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${Versions.Desugar.core}")
    addAndroidCore()
    addCompose()
    addCoroutines()
    // Material 3 Expressive shapes (cookie, clover, sunny…) and morphs between them.
    api("androidx.graphics:graphics-shapes:1.0.1")
}
