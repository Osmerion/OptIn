rootProject.name = "sandbox"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("../build-logic")
}

includeBuild("../") {
    dependencySubstitution {
        substitute(module("com.osmerion.opt-in:opt-in")).using(project(":library"))
        substitute(module("com.osmerion.opt-in:tools-apt")).using(project(":library"))
        substitute(module("com.osmerion.optin:optin")).using(project(":library"))
    }
}

//file("modules").listFiles(File::isDirectory)!!.forEach { dir ->
//    fun hasBuildscript(it: File) = File(it, "build.gradle.kts").exists()
//
//    if (hasBuildscript(dir)) {
//        val projectName = dir.name
//
//        include(projectName)
//        project(":$projectName").projectDir = dir
//    }
//}