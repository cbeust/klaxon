import com.beust.kobalt.*
import com.beust.kobalt.plugin.kotlin.kotlinProject
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.java.*
import com.beust.kobalt.plugin.publish.*

val project = project {
    name = "klaxon"
    group = "com.beust"
    artifactId = name
    version = "0.23"

    dependenciesTest {
        compile("org.testng:testng:6.9.9")
        compile("org.jetbrains.kotlin:kotlin-test:1.0.0-rc-1036")
    }

    assemble {
        mavenJars {}
    }

    bintray {
        publish = true
    }
}
