import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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
    id("com.jfrog.bintray") version "1.2"
    `maven-publish`
}

val kotlinVersion = property("kotlin.version").toString()

allprojects {
    version = "3.0.8"
    group = "com.beust"

    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://oss.sonatype.org/content/repositories/snapshots")
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

        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STARTED)
            displayGranularity = 0
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
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


