import com.beust.kobalt.*
import com.beust.kobalt.plugin.java.javaCompiler
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray

val p = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.26"

    javaCompiler {
        args("-source", "1.7", "-target", "1.7")
    }

    dependenciesTest {
        compile("org.testng:testng:6.9.9",
                "org.jetbrains.kotlin:kotlin-test:1.0.0")
    }

    assemble {
        mavenJars {}
    }

    bintray {
        publish = true
    }
}
