pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
        // SQLCipher Android releases
        maven(url = "https://repo.sqlcipher.net/maven/") {
            content {
                includeGroup("net.zetetic")
            }
        }
    }
}

rootProject.name = "DraftPeek"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:domain")
include(":core:testing")
include(":feature:browser")
include(":feature:editor")
include(":feature:settings")
include(":feature:stats")
include(":feature:terminal")
include(":benchmark")
