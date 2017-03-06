
import com.beust.kobalt.plugin.java.javaCompiler
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project

val p = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.29"

    dependenciesTest {
        compile("org.testng:testng:6.11",
                "org.jetbrains.kotlin:kotlin-test:1.0.3")
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
}
