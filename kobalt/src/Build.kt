import com.beust.kobalt.*
import com.beust.kobalt.plugin.kotlin.kotlinProject
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.java.*
import com.beust.kobalt.plugin.publish.*

val project = kotlinProject {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.21"

    dependenciesTest {
        compile("org.testng:testng:6.9.9")
        compile("org.jetbrains.kotlin:kotlin-test:1.0.0-beta-4584")
    }

    assemble {
        jar {
        }
    }

    jcenter {
        publish = true
    }
}
