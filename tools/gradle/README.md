# OptIn Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v.svg?style=for-the-badge&label=Gradle%20Plugin%20Portal&logo=Gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fosmerion%2Fopt-in%2Fcom.osmerion.opt-in.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.osmerion.opt-in)
![Gradle](https://img.shields.io/badge/Gradle-9.0.0-green.svg?style=for-the-badge&color=1ba8cb&logo=Gradle)

The OptIn Gradle plugin simplifies working with OptIn by automatically registering of the [OptIn Annotation Processor](../apt)
for all applicable source sets.


## Usage

```kotlin
plugins {
    id("com.osmerion.opt-in") version "<latest>"
}
```


## Configuration

The plugin also registers the `optIn` project extension which can be used to customize the plugin's behavior.

```kotlin
optIn {
    /* The group name of the GAV coordinates for the OptIn artifacts. */
    artifactGroup = "com.osmerion.optin"
    
    /* The version of the GAV coordinates for the OptIn artifacts. */
    artifactVersion = "<latest>"
}
```
