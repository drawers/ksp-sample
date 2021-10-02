plugins {
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    implementation(project(":annotation"))
    implementation(kotlin("stdlib"))
    implementation("com.squareup:kotlinpoet:1.10.1")
    implementation("com.squareup:kotlinpoet-ksp:1.10.1")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.31-1.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.4.32")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.4")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.4")
}