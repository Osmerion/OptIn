# Annotation Processor

The OptIn annotation processor and javac plugin provide compile-time checks for opt-in requirements. Using it is
strongly recommended when working with OptIn.


## Usage

The recommended approach to using the annotation processor is using the respective build tool plugin for [Gradle](./gradle.md)
or [Maven](./maven.md). These plugins automatically take care of managing the required dependencies and offer safety for
configuring OptIn verification.

When working with an unsupported build tool or using the command-line, the annotation processor and plugin can be used
as usual with `--processor-path` or `--processor-module-path` and `-Xplugin` respectively.
