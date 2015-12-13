import com.beust.kobalt.*
import com.beust.kobalt.plugin.kotlin.kotlinProject
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.java.*
import com.beust.kobalt.plugin.publish.*

val project = kotlinProject {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.20"

    dependenciesTest {
        compile("org.testng:testng:6.9.9")
    }

    assemble {
        jar {
        }
    }

    jcenter {
        publish = true
    }
}
