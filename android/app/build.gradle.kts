plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    id("projectConfig")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.compose.screenshot")
}
apply(plugin = "dagger.hilt.android.plugin")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

val commitHashProvider = providers.of(CommitHashValueSource::class) {}

android {
    if (projectConfig.compileSdkPreview != null) {
        compileSdkPreview = projectConfig.compileSdkPreview
    } else {
        compileSdk = projectConfig.compileSdk
    }

    defaultConfig {
        namespace = projectConfig.packageName

        minSdk = projectConfig.minSdk
        if (projectConfig.targetSdkPreview != null) {
            targetSdkPreview = projectConfig.targetSdkPreview
        } else {
            targetSdk = projectConfig.targetSdk
        }

        versionCode = projectConfig.version.code.toInt()
        versionName = projectConfig.version.name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        buildConfigField("String", "PACKAGENAME", "\"${projectConfig.packageName}\"")
        buildConfigField("String", "GITSHA", "\"${commitHashProvider.get()}\"")
        buildConfigField("String", "VERSION_CODE", "\"${projectConfig.version.code}\"")
        buildConfigField("String", "VERSION_NAME", "\"${projectConfig.version.name}\"")
    }

    // Tokkip's key comes from the environment (CI secrets), never from the repo.
    signingConfigs {
        create("tokkip") {
            val store = System.getenv("TOKKIP_KEYSTORE")?.let { File(it) }
            if (store != null && store.exists()) {
                storeFile = store
                storePassword = System.getenv("TOKKIP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TOKKIP_KEY_ALIAS") ?: "tokkip"
                keyPassword = System.getenv("TOKKIP_KEY_PASSWORD") ?: System.getenv("TOKKIP_KEYSTORE_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        all {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("beta") {
            signingConfig = signingConfigs["tokkip"]
            proguardFiles("proguard-foss.pro")
            isMinifyEnabled = true
            isShrinkResources = true
        }
        release {
            signingConfig = signingConfigs["tokkip"]
            proguardFiles("proguard-foss.pro")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    lint {
        abortOnError = true
        // AGP 9.3: lint and lintVital skip library modules unless checkDependencies is set
        checkDependencies = true
        applySdmSeDefaults()
        fatal.add("StopShip")
        // AGP has no per-buildType lint config; beta builds tolerate translations ahead of source strings
        val isBetaBuild = gradle.startParameter.taskNames.any { it.contains("beta", ignoreCase = true) }
        if (isBetaBuild) warning.add("ExtraTranslation") else fatal.add("ExtraTranslation")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Enables the screenshotTest source set (com.android.compose.screenshot). The matching
    // apply-time gate lives in gradle.properties (android.experimental.enableScreenshotTest).
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
        }
        //noinspection WrongGradleMethod
        tasks.withType<Test> {
            useJUnitPlatform()
            setupTests()
        }
    }

    sourceSets {
        getByName("test") {
            resources.directories.add("src/main/assets")
        }
        getByName("androidTest") {
            assets.directories.add("$projectDir/schemas")
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes.add("attach_hotspot_windows.dll")
        }
    }

}

androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: return@onVariants
        if (buildType != "release" && buildType != "beta") return@onVariants
        variant.outputs.single().outputFileName.set("TokkipHealth-v${projectConfig.version.name}-$buildType.apk")
    }
}

setupKotlinOptions(compose = true)

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${Versions.Desugar.core}")

    implementation(project(":app-common"))
    implementation(project(":app-common-ui"))
    implementation(project(":app-common-tokkip"))
    implementation(project(":app-common-coil"))
    implementation(project(":app-common-picker"))
    implementation(project(":app-common-stats"))
    testImplementation(project(":app-common-test"))
    implementation(project(":app-common-root"))
    implementation(project(":app-common-adb"))
    implementation(project(":app-common-io"))
    implementation(project(":app-common-pkgs"))
    implementation(project(":app-common-shell"))
    implementation(project(":app-common-data"))
    implementation(project(":app-common-exclusion"))
    implementation(project(":app-common-automation"))
    implementation(project(":app-common-setup"))
    implementation(project(":app-tool-corpsefinder"))
    implementation(project(":app-tool-systemcleaner"))
    implementation(project(":app-tool-appcleaner"))
    implementation(project(":app-tool-deduplicator"))
    implementation(project(":app-tool-squeezer"))
    implementation(project(":app-tool-analyzer"))
    implementation(project(":app-tool-swiper"))
    implementation(project(":app-tool-appcontrol"))
    implementation(project(":app-tool-scheduler"))

    addDI()
    addCoroutines()
    addSerialization()
    addRetrofit()



    addAndroidCore()
    implementation("androidx.documentfile:documentfile:1.1.0")
    addAndroidUI()
    addCompose()
    addGlance()
    addNavigation3()
    addWorkerManager()
    // WorkManager exposes ListenableFuture in its API (TaskWorkerControl.kt).
    // Media3 Transformer (via app-tool-squeezer) transitively pulls in full Guava at runtime,
    // but `implementation` scoping means it doesn't reach this module's compile classpath.
    implementation("com.google.guava:guava:33.3.1-android")
    addRoomDb()

    addTesting()

    implementation("io.github.z4kn4fein:semver:3.0.0")

    addLottie()

    implementation("sh.calvin.reorderable:reorderable:2.5.1")

    implementation("androidx.exifinterface:exifinterface:1.4.2")

    androidTestImplementation("androidx.navigation:navigation-testing:${Versions.AndroidX.Navigation.core}")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    androidTestUtil("androidx.test:orchestrator:1.6.1")
    androidTestUtil("androidx.test.services:test-services:1.6.0")




    testImplementation("androidx.test.ext:junit:1.3.0")

    addScreenshotTest()
}
