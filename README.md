# Java Library for Promises

The JPromises library is a *mostly* compliant implementation of the
[JavaScript Promises/A+ specification](http://promisesaplus.com/) in Java. It is designed to be compatible with
AWT/Swing, JavaFX and GWT, and potentially with any kind of event dispatch mechanism.
Its main goal is to eliminate "callback hell" that arises with code heavily involving Node/GWT style async callbacks.

The GWT module also includes the `JsPromise` class, a thin wrapper around native JavaScript promises from ES6.

The library has no external dependencies and requires **Java 7 or later**, but is designed with Java 8 idioms in mind,
and Java 8 is strongly recommended because lambda expressions make typical use cases a lot more concise and readable.

## Latest release

The most recent release is version 0.1. [(Javadoc)](http://jpromises.lucidfox.org/javadoc/)

### Maven

```xml
<dependency>
  <groupId>org.lucidfox.jpromises</groupId>
  <artifactId>jpromises</artifactId>
  <version>0.1</version>
</dependency>
```

And if you need the GWT module:

```xml
<dependency>
  <groupId>org.lucidfox.jpromises</groupId>
  <artifactId>jpromises-gwt</artifactId>
  <version>0.1</version>
</dependency>
```

Gradle:

```
'org.lucidfox.jpromises:jpromises:0.1'
```

And if you need the GWT module:

```
'org.lucidfox.jpromises:jpromises-gwt:0.1'
```

## Advantages

This library was written with the explicit purpose of having a portable promises library in a complex project with GWT
and Swing frontends. It is designed with the following goals:

1. Minimal required dependencies (only the JDK itself)
2. Compatibility with GWT
3. Making minimal assumptions about the underlying event dispatch system, or the application's threading model

## Quick Start

Since the Promises API was designed to be fluent, browsing Javadoc is unlikely to give a clear idea of its
intended usage patterns. Examples will be more helpful.

(TODO)