---
sidebar_position: 1
---

# Start Here

Quick links: [Maven Central] | [GitHub] | [User Guide] | [JavaDoc] | [Spec] | [Issues] | [Discuss]

## What is this?

The idea behind OptIn is to create a standard for declaring and working with APIs that require explicit opt-in in Java.
Common use-cases include marking experimental features that are not subject to a libraries' usual stability guarantees
yet, or requiring explicit consent for low-level APIs that are complex to use correctly.

```java
// Create a requirement marker
@RequiresOptIn(message = "This API is subject to change and may change without prior notice.")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ExperimentalNotifications {}

interface MyNotificationProcessor {
    
    void sendOldNotification();
    
    @ExperimentalNotifications // Declares an opt-in requirement
    void sendFancyNewNotification();
    
}

// Opts-in into the requirement marker
@OptIn(ExperimentalNotifications.class)
void onMessage(MyNotificationProcessor processor) {
    processor.sendFancyNewNotification();
}
```

OptIn is heavily inspired by Kotlin's opt-in requirements and aims to bring the same capabilities to the Java ecosystem.
As such, it aims to integrate seamlessly with Kotlin's opt-in requirements.


## How does it work?

To learn more and start using OptIn, refer to:

- the [User Guide], or
- the [OptIn Specification](./spec.md).


## Get involved!

OptIn needs your feedback to finalize the specification and improve tooling support. While OptIn is still in an early
state, the API surface is small and large semantic changes are unlikely. Now is the time to try out OptIn and report any
feedback (even if everything is working fine for you)!

- [Report issues](https://github.com/Osmerion/OptIn/issues)
- [Leave general feedback](https://github.com/Osmerion/OptIn/discussions)


[Discuss]: https://github.com/Osmerion/OptIn/discussions
[GitHub]: https://github.com/Osmerion/OptIn
[JavaDoc]: https://osmerion.github.io/OptIn/docs/api/org/jspecify/annotations/package-summary.html
[Issues]: https://github.com/Osmerion/OptIn/issues
[Maven Central]: https://central.sonatype.com/artifact/com.osmerion.optin/opt-in
[Spec]: ./spec.md
[User Guide]: ./user-guide.md
