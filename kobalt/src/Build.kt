import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.*

val p = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.25"

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
