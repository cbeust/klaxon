description =
    "Holds tests that are relevant to or utilize the core and the plugins."

val pluginsProject = project(":plugins")
dependencies {
    testImplementation(project(":klaxon"))
    testImplementation(project(":testing:test-resources"))
    pluginsProject.subprojects.forEach {
        testImplementation(project(it.path))
    }
}