
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project

val p = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.27"

    dependenciesTest {
        compile("org.testng:testng:6.9.9",
                "org.jetbrains.kotlin:kotlin-test:1.0.3")
    }

    assemble {
        mavenJars {}
    }

    bintray {
        publish = true
    }
}
