# Gradle Plugin

The OptIn Gradle plugin can be used so simplify working with OptIn in Gradle projects by reducing the amount of required
configuration.

The plugin automatically sets up the [OptIn annotation processor](./annotation-processor.md) for all applicable source-sets
when a JVM ecosystem plugin is detected.


## Usage

```kotlin
plugins {
    id("com.osmerion.opt-in") version "<latest>"
}
```


## Configuration

The `optIn` project extension which can be used to configure the plugin's behavior.


### Third-Party integration

The OptIn tooling supports specifying additional marker annotations to support third-party libraries that are not yet
using OptIn themselves:

```kotlin
optIn {
    /* Treats Guava's @Beta annotation as marker annotation. */
    requiresOptIn("com.google.common.annotations.Beta")
    
    /* Treats Gradle's @HasInternalProtocol as marker annotation for subtyping only. */
    subtypingRequiresOptIn("org.gradle.internal.HasInternalProtocol")
}
```


### Using Custom Artifacts

By default, the OptIn Gradle plugin will use artifacts from the `com.osmerion.optin` group with versions matching the
plugin's version. This can be changed via properties:

```kotlin
optIn {
    /* The group name of the GAV coordinates for the OptIn artifacts. */
    artifactGroup = "com.osmerion.optin"
    
    /* The version of the GAV coordinates for the OptIn artifacts. */
    artifactVersion = "<latest>"
}
```
