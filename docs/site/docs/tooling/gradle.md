# Gradle Plugin

The OptIn Gradle plugin simplifies working with OptIn in Gradle projects by reducing the amount of required
configuration. It automatically sets up the [OptIn annotation processor](javac.md) for all applicable source-sets when
a JVM ecosystem plugin is detected.

Additionally, it provides type-safe APIs to configure the opt-in verification.

## Usage

```kotlin
plugins {
    id("com.osmerion.opt-in") version "<latest>"
}
```


## Project Configuration

The `optIn` project extension which can be used to configure the plugin's behavior.

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


## Source-set Configuration

The `OptInSourceSetExtension` is available on all applicable source sets and can be used to customize the opt-in
verification. This is especially useful when working with third-party libraries that provide similar markers for API
semantics but that have no adopted OptIn.

```kotlin
sourceSets {
    named("main") {
        optIn {
            /* Treats Guava's @Beta annotation as marker annotation. */
            requiresOptIn("com.google.common.annotations.Beta")

            /* Treats Gradle's @HasInternalProtocol as marker annotation for subtyping only. */
            subtypingRequiresOptIn("org.gradle.internal.HasInternalProtocol")
        }
    }
}
```
