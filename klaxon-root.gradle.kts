buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://oss.sonatype.org/content/repositories/snapshots")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin.version")}")
    }
}

plugins {
    // https://github.com/diffplug/spotless/tree/master/plugin-gradle
    id("com.diffplug.gradle.spotless") version "3.14.0"
    id("com.jfrog.bintray") version "1.2"
    `maven-publish`
}

val kotlinVersion = property("kotlin.version").toString()
val ktlintVersion = "0.28.0"

allprojects {
    apply {
        plugin("com.diffplug.gradle.spotless")
    }

    version = "3.0.8"
    group = "com.beust"

    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://oss.sonatype.org/content/repositories/snapshots")
        }
    }

    spotless {
        /*
         * We use spotless to lint the Gradle Kotlin DSL files that make up the build.
         * These checks are dependencies of the `check` task.
         */
        kotlinGradle {
            ktlint(ktlintVersion)
            trimTrailingWhitespace()
        }
    }
}

// Projects that will be published.
val publishedProjects = setOf(
    project(":klaxon")
) + project(":plugins").subprojects

// Projects that contain source code.
val sourceProjects =
    publishedProjects + project(":testing").subprojects

configure(sourceProjects) {
    apply {
        plugin("java-library")
        plugin("kotlin")
    }

    dependencies {
        "api"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        "api"("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

        "testImplementation"("org.testng:testng:6.13.1")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        "testImplementation"("org.assertj:assertj-core:3.5.2")
    }

    tasks.withType<Test>().configureEach {
        useTestNG()
    }

    spotless {
        kotlin {
            ktlint(ktlintVersion)
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}

configure(publishedProjects) {
    apply(from = rootProject.file("gradle/publishing.gradle"))

    tasks.register<Jar>("sourceJar") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "An archive of the source code"
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    tasks.withType<Test>().configureEach {
        useTestNG()
    }
}

tasks.withType<Wrapper>().configureEach {
    gradleVersion = "4.10"
    distributionType = Wrapper.DistributionType.ALL
}

/**
 * Retrieves the [java][org.gradle.api.plugins.JavaPluginConvention] project convention.
 */
val Project.java: org.gradle.api.plugins.JavaPluginConvention
    get() = convention.getPluginByName("java")
