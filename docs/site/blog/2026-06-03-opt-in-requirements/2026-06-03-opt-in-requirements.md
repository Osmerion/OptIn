---
slug: welcome
title: Introducing opt-in requirements for Java APIs
authors: [tmm]
---

Every library author has been there: you have a new API that works, but you're not ready to commit to it forever.
Maybe the design will change. Maybe it's a low-level escape hatch that most users shouldn't touch. You add a note to the
JavaDoc - "experimental, use at your own risk" - and hope for the best.

Some projects introduce custom annotations like `@Beta` or `@Incubating` which are - with a few exceptions - unsupported
by IDEs and have no compiler support.

OptIn aims to standardize declaring and working with APIs that require explicit opt-in in Java.

{/* truncate */}


## Kotlin's opt-in requirements

Kotlin's opt-in requirements solve this problem elegantly. Library authors declare a _requirement marker_ - a custom
annotation - and attach it to any API that requires explicit opt-in. Users can then opt in to the requirements locally
or propagate the requirements. The Kotlin compiler enforces that all requirements are satisfied at compile-time.

The result is opt-in as a contract, not a suggestion. Consumers can't stumble into an unstable API by accident. They 
have to make a deliberate, visible choice.


## Introducing OptIn for Java

This project brings the same mechanism to Java.

At its core, OptIn for Java introduces two annotations:

- `@RequiresOptIn`  - a meta-annotation used to define requirement markers. You attach it to your own annotation
                      interface, giving it a message and a severity level (`WARNING` or `ERROR`). That annotation then
                      becomes the marker your users must acknowledge.
- `@OptIn`          - the acknowledgment. Callers annotate their own code with `@OptIn(ExperimentalApi.class)` to say:
                      "I know what I'm doing, and I accept the risk."

Requirements are contagious by design. An `@ExperimentalApi` class passes its requirement to all its members and nested
types. A method whose signature mentions an experimental type inherits the requirement automatically. This means
opt-in can't be circumvented by routing through an intermediary - the verifier sees through the whole chain.

OptIn implements javac plugin and an annotation processor that together form the verifier that verifies opt-in usage at
compile-time.


## Try OptIn now!

While the core aspects of the API and specification are unlikely to change, there are still a few details to iron out
and - presumably - issues to discover. We need your feedback now to finalize the specification! Please consider trying
out opt-in.

[Learn more!](https://osmerion.github.io/OptIn/docs/start-here.md)
