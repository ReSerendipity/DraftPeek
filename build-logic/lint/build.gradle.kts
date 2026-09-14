plugins {
    `java-library`
}

group = "com.draftpeek.buildlogic"

dependencies {
    compileOnly("com.android.tools.lint:lint-api:31.10.1")
    compileOnly("com.android.tools.lint:lint-checks:31.10.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    manifest {
        attributes("Lint-Registry-V2" to "com.draftpeek.buildlogic.lint.DraftPeekIssueRegistry")
    }
}
