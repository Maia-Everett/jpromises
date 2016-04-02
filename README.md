# Java Library for Promises

The JPromises library is a *mostly* compliant implementation of the
[JavaScript Promises/A+ specification](http://promisesaplus.com/) in Java.
It is designed to be compatible with AWT and GWT, and potentially with any kind of event dispatch mechanism.
Its main goal is to eliminate "callback hell" that arises with code heavily involving Node/GWT style async callbacks.

* [Javadoc](http://jpromises.lucidfox.org/javadoc/)

## Why not [JDeferred](http://jdeferred.org)?

By all means, JDeferred is the more mature and feature complete library. However, this library was written with
the explicit purpose of having a portable promises library in a complex project with GWT and Swing frontends.
It is designed with the following goals:

1. Minimal required dependencies (only the JDK itself)
2. Compatibility with GWT
3. Making minimal assumptions about the underlying event dispatch system, or the application's threading model

## Quick Start

Since the Promises API was designed to be fluent, browsing Javadoc is unlikely to give a clear idea of its
intended usage patterns. Examples will be more helpful.

(TODO)