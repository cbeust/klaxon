@file:Suppress("MayBeConstant")

object This {
    val version = KLAXON_VERSION
    val groupId = "com.beust"
    val artifactId = "klaxon"
    val description = "A JSON parsing library"
    val url = "https://github.com/cbeust/klaxon"
    val scm = "github.com/cbeust/klaxon.git"

    // Should not need to change anything below
    val issueManagementUrl = "https://$scm/issues"
    val isSnapshot = version.contains("SNAPSHOT")
}

allprojects {
    group = This.groupId
    version = This.version
    apply<MavenPublishPlugin>()
}

plugins {
    `maven-publish`
    signing
    id("com.jfrog.bintray") version "1.8.3" // Don't use 1.8.4, crash when publishing
    kotlin("jvm")
}

dependencies {
    listOf("test").forEach {
        testImplementation(kotlin(it))
    }
    listOf("org.testng:testng:7.0.0", "org.assertj:assertj-core:3.10.0").forEach {
        testImplementation(it)
    }

    listOf("stdlib", "reflect").forEach {
        implementation(kotlin(it))
    }
}

//
// Releases:
// ./gradlew bintrayUpload (to JCenter)
// ./gradlew publish (to Sonatype, then go to https://oss.sonatype.org/index.html#stagingRepositories to publish)
//

bintray {
    user = project.findProperty("bintrayUser")?.toString() ?: System.getenv("BINTRAY_USER")
    key = project.findProperty("bintrayApiKey")?.toString() ?: System.getenv("BINTRAY_API_KEY")
    dryRun = false
    publish = false

    setPublications("custom")

    with(pkg) {
        repo = "maven"
        name = This.artifactId
        with(version) {
            name = This.version
            desc = This.description
            with(gpg) {
                sign = true
            }
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

with(publishing) {
    publications {
        create<MavenPublication>("custom") {
            groupId = This.groupId
            artifactId = This.artifactId
            version = project.version.toString()
            afterEvaluate {
                from(components["java"])
            }
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set(This.artifactId)
                description.set(This.description)
                url.set(This.url)
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                issueManagement {
                    system.set("Github")
                    url.set(This.issueManagementUrl)
                }
                developers {
                    developer {
                        id.set("cbeust")
                        name.set("Cedric Beust")
                        email.set("cedric@beust.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://${This.scm}.git")
                    url.set("https://${This.scm}")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "sonatype"
            url = if (This.isSnapshot)
                uri("https://oss.sonatype.org/content/repositories/snapshots/") else
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("sonatypeUser")?.toString() ?: System.getenv("SONATYPE_USER")
                password = project.findProperty("sonatypePassword")?.toString() ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven {
            name = "myRepo"
            url = uri("file://$buildDir/repo")
        }
    }
}

tasks.register("publishSnapshotOnly") {
    if (This.isSnapshot) {
        doFirst {
            println("Publishing snapshot ${This.version}")
        }
        dependsOn("publish")
    } else {
        println("Not a snapshot, not publishing anything")
    }
}

// Sign with ./gradlew signCustomPublication
with(signing) {
    sign(publishing.publications.getByName("custom"))
}

tasks.test {
    useTestNG() {
//        suites("src/test/resources/testng.xml")
    }
}

tasks["publish"].doLast {
    if (! This.isSnapshot) {
        println("Now go to https://oss.sonatype.org/index.html#stagingRepositories to close" +
                " and publish the distribution")
    }
}
