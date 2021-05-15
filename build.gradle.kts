plugins {
    kotlin("jvm") version "1.3.72"
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "io.flassie.imlab"
version = "0.1.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

pluginBundle {
    website = "https://imlab.by"
    vcsUrl = "https://github.com/Flassie/imlab-gradle-plugin"
    tags = listOf("imlab", "flassie")
}

gradlePlugin {
    plugins {
        create("imlab-plugin") {
            id = "io.flassie.imlab"
            displayName = "Imlab plugin"
            description = "Helper plugin to manage imlab dependencies"
            implementationClass = "io.flassie.imlab.ImlabPlugin"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        javaParameters = true
    }
}
