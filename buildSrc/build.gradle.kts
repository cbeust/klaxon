import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
    java
}

repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://plugins.gradle.org/m2") }
}

dependencies {
    listOf("test").forEach {
        testImplementation(kotlin(it))
    }
    listOf("org.testng:testng:7.0.0", "org.assertj:assertj-core:3.10.0").forEach {
        testImplementation(it)
    }
}

tasks.withType<Javadoc> {
    options {
        quiet()
//            outputLevel = JavadocOutputLevel.QUIET
//            jFlags = listOf("-Xdoclint:none", "foo")
//            "-quiet"
    }
}
