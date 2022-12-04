plugins {
    id("com.osmerion.java-library-conventions")
}

dependencies {
    compileOnlyApi(projects.library)

    annotationProcessor(projects.tools.apt)
}