# Java Library for Promises

The JPromises library is a *mostly* compliant implementation of the
[JavaScript Promises/A+ specification](http://promisesaplus.com/) in Java. It is designed to be compatible with
AWT/Swing, JavaFX and GWT, and potentially with any kind of event dispatch mechanism.
Its main goal is to eliminate "callback hell" that arises with code heavily involving Node/GWT style async callbacks.

The GWT module also includes the `JsPromise` class, a thin wrapper around native JavaScript promises from ES6.

The library has no external dependencies and requires **Java 7 or later**, but is designed with Java 8 idioms in mind,
and Java 8 is strongly recommended because lambda expressions make typical use cases a lot more concise and readable.

## Latest release

The most recent release is version 0.2.2. [(Javadoc)](https://www.javadoc.io/doc/org.lucidfox.jpromises/jpromises/0.2.2)

### Maven

```xml
<dependency>
  <groupId>org.lucidfox.jpromises</groupId>
  <artifactId>jpromises</artifactId>
  <version>0.2.2</version>
</dependency>
```

And if you need the GWT module:

```xml
<dependency>
  <groupId>org.lucidfox.jpromises</groupId>
  <artifactId>jpromises-gwt</artifactId>
  <version>0.2.1</version>
</dependency>
```

Gradle:

```
'org.lucidfox.jpromises:jpromises:0.2.2'
```

And if you need the GWT module:

```
'org.lucidfox.jpromises:jpromises-gwt:0.2.2'
```

## Advantages

This library was written with the explicit purpose of having a portable promises library in a complex project with GWT
and Swing frontends. It is designed with the following goals:

1. Small and simple
2. Minimal required dependencies (only the JDK itself)
3. Compatibility with GWT
4. Making minimal assumptions about the underlying event dispatch system, or the application's threading model

## Quick Start

If you are familiar with promises in JavaScript, or Java 8's `CompletableFuture`, then JPromises should be simple conceptually.

If you want to know more about JavaScript promises, [this](https://davidwalsh.name/promises) and then [this](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises) are good places to start.

If you just want to see some code and then get digging in Javadoc yourself, here is a minimal working example:

```java
ExecutorService executor = ...;
PromiseFactory promiseFactory = new AndroidPromiseFactory();
// Or AwtPromiseFactory, or JavaFXPromiseFactory, etc.

promiseFactory.promiseAsync(resolve -> {
	String value = someLongComputation();
	resolve.resolve(sb.toString());
}, executor::submit).thenAccept(result -> {
	// Do something with the result
}, exception -> {
	// Handle the exception
));
```

## What is a promise?

In most non-trivial programs, you have to deal with asynchronous operations, which execute in background (in Java, this typically means a worker thread) and then need to make their result available to the thread that started them. For example, if a GUI application needs to access a network server, it will typically need to use a worker thread to send the request and wait for the server response, to avoid making the application unresponsive while the operation completes. The question then becomes how to make the result of the operation available to the main thread in a clear and convenient way.

A **promise**, simply put, is a wrapper, or container, for a value that will eventually be retrieved by an asynchronous operation. The asynchronous operation can either complete successfully (**resolving** the promise) or it may fail, **rejecting** the promise. A promise can be in one of three states:

* **Pending**: The asynchronous operation did not yet complete.
* **Resolved**: The operation completed successfully and its result has become the value of the promise.
* **Rejected**: The operation failed, and the promise stores the exception that 

In the JPromises library, promises are generic objects. For example, `Promise<String>` represents a promise that will contain a `String` value when (and if) its asynchronous operation completes successfully.

You can think of a promise as an initially-empty box for its eventual value, with an attached alarm that goes off when the value does arrive. This alarm comes in the form of the `then` family of methods. The beauty of promises is that a `then` method returns a new promise, so promises can be **chained**, with the next asynchronous operation starting after the previous one returns:

```java
Promise<UserInfo> userInfoPromise = doServerLogin();

userInfoPromise.then(userInfo -> {
	log.debug("User info: {}", userInfo);
	return getWallPosts(userInfo.getAuthToken());
}).thenAccept(wallPosts -> {
	drawPrettyPage(wallPosts);
}).onException(e -> {
	showErrorDialog(e);
});
```


## `Promise<T>` vs `Future<T>`

The Java 5 `Future` interface is also a kind of container for an asynchronously computed value. The difference between a promise and a future is that futures provide no notification mechanism like `then`, nor do they provide easy means to chain futures into a sequence. There are basically only three things you can do with a future: cancel it, periodically poll for a value, or block to wait until it completes.

Promises are more directly comparable with Java 8's `CompletableFuture`, which is really a promise by another name.


## Creating promises: the event loop and the `PromiseFactory`

Since `then` is a notification mechanism for the thread that started the asynchronous operation (in GUI applications this is normally the main thread), there needs to be some mechanism by which the notification code (the `then` callback) can run:

1. on the main thread, rather than on the worker thread; and
2. when it's safe to do so, i.e. when the main thread is not busy with anything else.

GUI applications and some other platforms that make use of background worker threads (such as event-driven servers: Node.js or, for a Java example, Vert.x) constantly run an **event loop** on the main thread. So, if you want to run some code on the main thread later, when it's convenient for the system to do so, you post it to the platform's event queue, and the event loop runs it later when it's convenient to do so (i.e. when the stack unwinds from user code all the way back to platform code).

To create promises, we need, therefore, to tell them how to post their `then` callbacks to the event loop. Usually there is a method for this, such as AWT/Swing's `EventQueue.invokeLater`, JavaFX's `Platform.runLater`, and Android's `Handler.post`. A **promise factory** is created with this knowledge, and you use the promise factory to create promises.

```java
PromiseFactory promiseFactory = new AndroidPromiseFactory();
```

Or, indeed, `AwtPromiseFactory`, `GwtPromiseFactory` or `JavaFXPromiseFactory`. If you're running your own custom event loop, you can create your own promise factory and tell it how to post code to the event loop:

```java
new PromiseFactory(myEventLoop::runLater);
```

or if you're stuck with Java 7:

```java
new PromiseFactory(new DeferredInvoker() {
	@Override
	public void invokeDeferred(Runnable task) {
		myEventLoop.runLater(task);
	}
});
```

## Example: retrieving a web page

Suppose you want to retrieve this very page from your Java program, and you want to do so in a background thread. You have a promise factory, and now you just create a promise and resolve it in the background thread when the operation completes:

```java
URL pageUrl = new URL("https://github.com/lucidfox/jpromises");

Promise<String> pageTextPromise = promiseFactory.promise(resolve -> {
	new Thread(() -> {
		StringBuilder sb = new StringBuilder();
	
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(pageUrl.openStream(), StandardCharsets.UTF_8))) {
			String str;
			
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			
			// operation completed successfully - resolve the promise
			resolve.resolve(sb.toString());
		} catch (IOException e) {
			// network failure - reject the promise
			resolve.reject(e);
		}
	}).start();
});
```

Java 7 syntax is slightly more verbose:

```java
Promise<String> pageTextPromise = promiseFactory.promise(new PromiseHandler<String>() {
	@Override
	public void handle(Resolver<String> resolve) {
		new Thread(new Runnable() {
			...
		}).start();
	}
});

```

So now the main thread can continue on its merry way (for example, drawing the application window and handling user input), and when the promise completes, the event loop will notify the main thread.

> If you're familiar with JavaScript promises, you will recognize the `PromiseFactory.promise` method as the equivalent of the `new Promise(resolve, reject)` constructor in JavaScript. JavaScript does not need this extra level of indirection because it only has one event loop to speak of, and thus no need to abstract away different event loops. Also, the promise handler takes only one parameter because of two; Java has no function objects, so there is no benefit of passing two objects instead of one that exposes all methods needed for control over the promise.

For the common case of running some block of code asynchronously, there is the `promiseAsync` convenience method, which simply delegates to the executor function passed as the second argument:

```java
promiseFactory.promiseAsync(resolve -> {
	// Do the asynchronous operation
}, task -> new Thread(task).start()); 

ExecutorService executor = ...;

promiseFactory.promiseAsync(resolve -> {
	// Do the asynchronous operation
}, executor::submit); 
```

Note that you do not need to explicitly call `reject` within the scope of the promise handler itself. You can just let any thrown exceptions bubble up; the promise will interpret catching such an exception as rejection. If you are inside explicitly asynchronous code, however (such as within a `Thread`'s `Runnable`), you will need to explicitly call `reject`.

## Using the result: `then`, `thenApply` and `thenAccept`

Once our promise completes, it will use the event loop to notify the main thread. If we simply need to use the value and not transform it into some other value, we can use `thenAccept`:

```java
pageTextPromise.thenAccept(pageText -> {
	pageDisplayWidget.setText(pageText);
});
```

> The `thenAccept` method returns a `Promise<Void>`. You can use that resulting promise as normal, calling any `then` methods on it in turn, and its resolved value will be `null`.

On the other hand, suppose we need to transform our value into something else, and wrap the result of the transformation into a new promise. Here we have two options. If we're going to transform **synchronously**, we use `thenApply`. For example, we can parse the HTML directly on the main thread:

```java
Promise<Document> docPromise = pageTextPromise.thenApply(pageText -> {
	return Jsoup.parse(pageText);
});
```
On the other hand, if we want to run some other operation **asynchronously** after the original promise completes, we can use the method that is named simply `then` (alias: `thenCompose`), and return a new promise:


```java
Promise<Document> pageTextPromise.then(pageText -> {
	Promise<Document> result = transformAsync(pageText);
	return result;
});
```

Note that the callback function passed to the `then` method returns a promise, and the `then` method itself **also** returns a promise — yet a third one. This combined promise first waits for the original promise to finish (the one on which we're calling `then`), after which it waits for the returned promise to finish, and only then itself returns. Whew! This is what is called promise chaining, and it is a very powerful and expressive mechanism for concisely writing sequences of asynchronous operations.

> For those familiar with the Java 8 Streams API, think of the relation between `thenAccept`, `thenApply` and `then`/`thenCompose` as similar to the relation between `forEach`, `map` and `flatMap`. This relation is not coincidental, as streams and promises are both examples of a specific functional programming concept: [monads](https://en.wikipedia.org/wiki/Monad_(functional_programming)).

> For those familiar with JavaScript promises, there are two subtleties here worth mentioning. First, JavaScript promises combine all three methods discussed here into one method named simply `then`, and uses dynamic typing to handle differently the situation when the result returned from the `then` callback is a promise or not, or when the callback did not return any value at all. Java is statically typed, and Java promises are additionally generically typed, so the JPromises library needs to make a clear, statically typed distinction between these three cases.
>
> The second difference from JavaScript promises is more subtle. In JavaScript, calling `promise.then(null)` will result in a promise resolved to the same value as the original promise, but in the JPromises library, it will result in a promise resolved to `null`! The reason is, again, static typing in Java. Since `then` can return a promise with a different parameter type (for example, you can call `then` on a `Promise<String>` and get a `Promise<Integer>`), simply passing the original promise's value to the new promise could result in a `ClassCastException` at runtime. If you really need to preserve the original value, write an explicit callback and do it yourself.

Like with promise handlers, any exception thrown inside a `then` callback will cause the returned promise to be rejected with that exception.

## Exception handling

If an exception is passed to `reject`, or thrown in a promise handler or a `then` callback, the resulting promise will be in the **rejected** state. You can have two ways of dealing with exceptions.

One, you can pass an optional second argument to any of the `then` methods that will serve as an exception handler:

```java
Promise<Document> docPromise = pageTextPromise.thenApply(pageText -> {
	return Jsoup.parse(pageText);
}, exception -> {
	showErrorDialog(exception);
	return errorDocument;
});
```

The second parameter returns the same kind of value as the first; so for `thenAccept` it returns no value, for `thenApply` it returns a plain value, and for `then`, a promise.

If the exception handler itself throws an exception, the resulting promise will, obviously, be rejected with that exception.

The second way to handle an exception is to let it trickle down through the entire promise chain until an exception handler is encountered (or not). If you don't pass an exception handler, or pass `null`, it will cause the next promise in the chain to be rejected with the same exception if the original promise is rejected; promise rejection is thus a "contagious" condition, affecting all subsequent promises in the chain unless the exception is handled. (This makes it similar to stack unwinding for unhandled exceptions in ordinary synchronous code.)

Three methods, `onExceptionAccept`, `onExceptionApply` and `onException`, accept only an exception handler. The first is equivalent to `thenAccept(null, handler)`. The other two are restricted to returning a promise of the same type as the original promise. They are convenient for specifying some fallback behavior if the original operation fails.

```java
getWebPageAsync()
	.thenApply(Jsoup::parse)
	.onExceptionApply(e -> {
		logException(e);
		return errorDocument;
	})
	.thenAccept(doc -> {
		workWithHtmlDocument(doc);
	});
```

And `onExceptionAccept` can be used at the end of a promise chain as a catch-all for any exception that occurs anywhere throughout the chain:

```java
loginAsync(username, password)
	.then(authToken -> {
		return getWallPostsAsync(authToken);
	})
	.thenApply(wallPosts -> {
		return parseHtml(wallPosts);
	})
	.thenAccept(documents -> {
		render(documents);
	})
	.onExceptionAccept(e -> {
		if (e instanceof ServiceUnavailableException) {
			showSorryDialog();
		} else if (e instanceof NoSuchUserException) {
			showInvalidLoginMessage();
		} else {
			logException(e);
			showErrorDialog(e);
		}
	});
```

