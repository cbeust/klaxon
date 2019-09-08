plugins {
    java
    maven
    `java-library`
}

allprojects {
    group = "org.gradle.kotlin.dsl.samples.multiproject"

    version = "1.0"

    repositories {
        jcenter()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://plugins.gradle.org/m2") }
}

//dependencies {
//    listOf("org.testng:testng:7.0.0").forEach { testCompile(it) }
//}

//
//dependencies {
//    // Make the root project archives configuration depend on every subproject
//    subprojects.forEach {
//        archives(it)
//    }
//}

subprojects {
    extra["signing.secretKeyRingFile"] = System.getProperty("user.home") + "/.gnupg/secring.gpg"
}