---
sidebar_position: 2
title: User Guide
---

# OptIn User Guide

The OptIn library provides a mechanism declaring and working with APIs that require explicit opt-in in Java.


## Setup

It is strongly recommended to use the [Gradle](./tooling/gradle.md) or [Maven](./tooling/maven.md) plugin when working
with opt-in to benefit from full compile-time verification. Alternatively, the [annotation processor and javac plugin](./tooling/javac.md)
can be configured manually.

The [IntelliJ IDEA plugin](./tooling/intellij_idea.md) provides inspections and quickfixes to help integrate OptIn
seamlessly into development workflows.


## Opting into a requirement

The OptIn mechanism lets API authors mark select APIs as requiring explicit acknowledgment before use. This is useful
for experimental APIs, unstable contracts, or low-level APIs that carry additional risk. A verifier can use the
information to check opt-in requirements at compile-time.

For ease of understanding, it is useful to differentiate between two roles:

- **API authors** define requirement markers and use them to annotate their APIs with requirements, and
- **API users** consent to using APIs with requirements by opting into them or propagating the requirement.


## Defining a Requirement Marker

To create a new requirement, define an annotation interface and meta-annotate it with `@RequiresOptIn`:

```java
@RequiresOptIn(message = "This API is experimental and may change or be removed in any future release.")
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface ExperimentalApi {}
```

The `message` attribute is shown to users in diagnostics when they access your API without opting in. Keep it concise
and actionable - ideally explaining what the risk is and what users should be aware of.

The `level` attribute controls how violations are reported:

- `Level.ERROR` (default): Use-sites that don't satisfy the requirement are treated as errors.
- `Level.WARNING`: Use-sites receive a warning but are not rejected.

### Constraints

Your marker must:

- Have `@Retention(RetentionPolicy.RUNTIME)`.
- Have an explicit `@Target` declaration restricted to a subset of `ANNOTATION_TYPE`, `CONSTRUCTOR`, `FIELD`, `METHOD`,
  `MODULE`, `PACKAGE`, and `TYPE`.


## Marking APIs

Once you have a requirement marker, annotate your APIs with it to impose an opt-in requirement on their use-sites.

### Annotating individual members

```java
public class Connection {

    @ExperimentalApi
    public void setLowLevelOption(int option, long value) { ... }

}
```

### Annotating an entire type

Annotating a type propagates the requirement to all of its members, including constructors, fields, methods, and
nested types:

```java
@ExperimentalApi
public class ExperimentalFeature {

    public ExperimentalFeature() { ... }   // requires opt-in
    public void run() { ... }              // requires opt-in

}
```

### Annotating a package

Place the annotation in `package-info.java` to apply the requirement to all types in the package:

```java
@ExperimentalApi
package com.example.experimental;

import com.example.annotations.ExperimentalApi;
```

### Subtyping requirements

If your type is safe to use but risky to extend or implement, use `@SubtypingRequiresOptIn` instead of a marker
annotation directly. Callers can reference and invoke the type freely; only subtypes must satisfy the requirement:

```java
@SubtypingRequiresOptIn(ExperimentalApi.class)
public interface ExtensionPoint {
    void process(Event event);
}
```

This is appropriate for types that may gain new abstract methods in future releases, or that require a careful
implementation to be correct.

### Constraints

`@SubtypingRequiresOptIn` may not be applied to `sealed` types.


## Using Opt-In APIs

When you use an API that carries an opt-in requirement, the verifier will emit a diagnostic unless you explicitly
satisfy the requirement. There are two ways to do so.

### Option 1: Opt in locally with `@OptIn`

Use `@OptIn` to acknowledge a requirement at a specific scope without propagating it further. This is the right choice
when you are a leaf consumer - you want to use the API but do not want to impose the requirement on your own callers.

```java
@OptIn(ExperimentalApi.class)
public void doSomething() {
    new ExperimentalFeature().run();  // no diagnostic
}
```

The scope of the acknowledgment matches the annotated element:

| Annotated element         | Scope of acknowledgment                          |
|---------------------------|--------------------------------------------------|
| Method or constructor     | The method or constructor body                   |
| Type declaration          | All members of the type                          |
| Field                     | The field's initializer                          |
| Package (`package-info`)  | All compilation units in the package             |
| Module (`module-info`)    | All compilation units in the module              |

`@OptIn` is repeatable, so you can acknowledge multiple requirements on a single declaration:

```java
@OptIn(ExperimentalApi.class)
@OptIn(InternalApi.class)
public void doSomething() { ... }
```

### Option 2: Propagate the requirement

If your own API builds on top of an opted-in API and you want to communicate that dependency to your callers,
annotate your declaration with the same marker. This _carries_ the requirement forward:

```java
@ExperimentalApi
public void doSomethingExperimental() {
    new ExperimentalFeature().run();
}
```

Now callers of `doSomethingExperimental()` must themselves opt in or propagate the requirement. This is the preferred
approach for library authors who expose APIs that depend on experimental or unstable foundations.

### Satisfying subtyping requirements

When extending or implementing a type annotated with `@SubtypingRequiresOptIn`, you have three options:

1. **Carry the marker** - annotate your subtype with the same marker, propagating the requirement:
   ```java
   @ExperimentalApi
   public class MyProcessor extends AbstractProcessor { ... }
   ```

2. **Propagate via `@SubtypingRequiresOptIn`** - restrict the requirement to your own subtypes only:
   ```java
   @SubtypingRequiresOptIn(ExperimentalApi.class)
   public abstract class BaseProcessor extends AbstractProcessor { ... }
   ```

3. **Opt in locally** - acknowledge the requirement without propagating it:
   ```java
   @OptIn(ExperimentalApi.class)
   public class MyProcessor extends AbstractProcessor { ... }
   ```


## Choosing the Right Approach

API users may find the following table useful as reference to decide on how to deal with requirements under certain
conditions.

| Situation                                                                                             | Recommended approach               |
|-------------------------------------------------------------------------------------------------------|------------------------------------|
| You use an experimental API in a private helper method                                                | `@OptIn` on the method             |
| You use an experimental API throughout a class                                                        | `@OptIn` on the type               |
| Your public API exposes or depends on an experimental API                                             | Carry the marker                   |
| You implement an interface with a subtyping requirement, and your implementation is also experimental | Carry the marker                   |
| You implement an interface with a subtyping requirement, and your implementation is stable            | `@OptIn` on the implementing type  |
| You provide an abstract base class that others will subtype                                           | Consider `@SubtypingRequiresOptIn` |
