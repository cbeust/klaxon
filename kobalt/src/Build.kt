
import com.beust.kobalt.plugin.java.javaCompiler
import com.beust.kobalt.plugin.kotlin.kotlinCompiler
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project

val p = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "2.0.3"

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-reflect:1.1.51")
    }

    dependenciesTest {
        compile("org.testng:testng:6.13.1",
                "org.jetbrains.kotlin:kotlin-test:1.1.51")
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
