
import com.beust.kobalt.plugin.java.javaCompiler
import com.beust.kobalt.plugin.kotlin.kotlinCompiler
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project

object Version {
    val klaxon = "4.0.1"
    val kotlin = "1.2.60"
}

val klaxon = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = Version.klaxon
    directory = "klaxon"

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}",
                "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}")
    }

    dependenciesTest {
        compile("org.testng:testng:6.13.1",
                "org.assertj:assertj-core:3.5.2",
                "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}")
    }

    assemble {
        mavenJars {}
    }

    bintray {
        publish = true
    }

    javaCompiler {
        args("-source", "1.7", "-target", "1.7")
    }

    kotlinCompiler {
        args("-no-stdlib")
    }
}

val jackson = project(klaxon) {
    name = "klaxon-jackson"
    directory = "plugins/$name"
    group = "com.beust"
    artifactId = name
    version = "1.0.0"

    dependencies {
        compile("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    }

    bintray {
        publish = true
    }

    assemble {
        mavenJars {}
    }
}
