plugins {
    `kotlin-dsl`
}

group = "com.draftpeek.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "draftpeek.android.application"
            implementationClass = "com.draftpeek.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "draftpeek.android.library"
            implementationClass = "com.draftpeek.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "draftpeek.android.compose"
            implementationClass = "com.draftpeek.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "draftpeek.android.hilt"
            implementationClass = "com.draftpeek.buildlogic.AndroidHiltConventionPlugin"
        }
    }
}
