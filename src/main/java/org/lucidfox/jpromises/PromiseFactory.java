/*
 * Copyright 2014-2017 Maia Everett <maia@lucidfox.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lucidfox.jpromises;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.lucidfox.jpromises.annotation.GwtCompatible;
import org.lucidfox.jpromises.core.AsyncRunner;
import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseHandler;
import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Resolver;
import org.lucidfox.jpromises.core.Thenable;

/**
 * <p>
 * A factory for creating Promise objects. It encapsulates a {@link DeferredInvoker} to create {@link Promise} objects
 * using the specified deferred invocation policy.
 * </p>
 * 
 * @apiNote In the JavaScript Promises implementation, the methods returned by this class are present as
 * static methods of the {@code Promise} class. Here, they are collected in a separate factory class to allow
 * specifying different deferred invocation policies. This class is not made final, so that it can be subclassed
 * with more convenience methods, or bound to a specific {@code DeferredInvoker}.
 */
@GwtCompatible
public class PromiseFactory {
	private final DeferredInvoker deferredInvoker;
	/**
	 * Instantiates a new promise factory.
	 *
	 * <p>
	 * All promises created by the resulting {@code PromiseFactory} will have their {@code then} callbacks executed
	 * under the executor specified by the {@code deferredInvoker} parameter.
	 * </p>
	 *
	 * @apiNote The primary use case for the JPromises library is typical GUI applications with a privileged main thread
	 * (sometimes called the event dispatch thread) that starts all the background tasks. Requiring to specify the
	 * policy for executing {@code then} callbacks ensures that they are executed on the main thread, after the
	 * operation represented by the promise completes in background.
	 *
	 * @param deferredInvoker the deferred invoker used to run {@code then} callbacks
	 */
	public PromiseFactory(final DeferredInvoker deferredInvoker) {
		this.deferredInvoker = deferredInvoker;
	}
	
	/**
	 * Instantiates a new {@link Promise} with the given {@link PromiseHandler}. The execution of the promise handler
	 * starts immediately.
	 *
	 * @param <V> the value type of the promise
	 * @param handler the promise handler
	 * @return the new promise whose evaluation is specified by the handler
	 */
	public final <V> Promise<V> promise(final PromiseHandler<V> handler) {
		return new Promise<>(deferredInvoker, handler);
	}
	
	/**
	 * Instantiates a new {@link Promise} asynchronously with the given {@link PromiseHandler}. The execution of the
	 * promise handler is delegated to {@code asyncRunner}.
	 *
	 * @param <V> the value type of the promise
	 * @param handler the promise handler
	 * @param asyncRunner 
	 * @return the new promise whose evaluation is specified by the handler
	 */
	public final <V> Promise<V> promiseAsync(final PromiseHandler<V> handler, final AsyncRunner asyncRunner) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) throws Exception {
				asyncRunner.runAsync(new Runnable() {
					@Override
					public void run() {
						try {
							handler.handle(resolve);
						} catch (final Exception e) {
							resolve.reject(e);
						}
					}
				});
			}
		});
	}
	
	/**
	 * <p>
	 * Instantiates a {@link Promise} resolved to {@code null}.
	 * </p><p>
	 * This method will rarely be used. Returning {@code null} from resolved/rejected callbacks does the same
	 * thing, and is faster. It may be necessary if interacting with custom thenables that do not allow
	 * resolved/rejected callbacks to return null.
	 * </p>
	 *
	 * @param <V> the value type of the promise
	 * @return the promise resolved to null
	 */
	public final <V> Promise<V> ofNull() {
		return resolve(null);
	}
	
	/**
	 * Instantiates a {@link Promise} resolved to the specified value. This is useful for returning a static value
	 * from resolved/rejected callbacks in {@code then}, which is allowed by the Promises/A+ specification, but not
	 * expressible in Java's static type system.
	 *
	 * @param <V> the value type of the promise
	 * @param value the value
	 * @return the promise resolved to the value
	 */
	public final <V> Promise<V> resolve(final V value) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) {
				resolve.resolve(value);
			}
		});
	}
	
	/**
	 * <p>
	 * Returns a {@link Promise} wrapping the given {@link Thenable}. If {@code thenable} is a {@code Promise}, then it
	 * is returned unchanged. Otherwise, this method creates a new {@code Promise} whose resolution is chained to the
	 * thenable's {@code then} method.
	 * </p>
	 * <p>
	 * <strong>Tip:</strong> This method can be used for typesafe casting of {@code Promise<Derived>} to
	 * {@code Promise<Base>} if {@code Base} is a supertype (superclass or superinterface) of {@code Derived}.
	 * Such use does not create new objects.
	 * </p>
	 *
	 * @param <V> the value type of the promise
	 * @param thenable the thenable
	 * @return the promise
	 */
	public final <V> Promise<V> deferResolve(final Thenable<? extends V> thenable) {
		if (thenable instanceof Promise) {
			// Short-circuit
			@SuppressWarnings("unchecked")
			final Promise<V> promise = (Promise<V>) thenable;
			return promise;
		}
		
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) throws Exception {
				resolve.deferResolve(thenable);
			}
		});
	}
	
	/**
	 * Instantiates a {@link Promise} that starts rejected with the given exception as its rejection reason.
	 *
	 * @param <V> the value type of the promise
	 * @param exception the exception (rejection reason)
	 * @return the new promise rejected with the given exception
	 */
	public final <V> Promise<V> reject(final Throwable exception) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) {
				resolve.reject(exception);
			}
		});
	}
	
	/**
	 * Performs the equivalent of the monadic "join" operation on a thenable that resolves to a thenable,
	 * converting it into a promise that resolves to the value of the inner thenable.
	 * 
	 * @param <V> the value type of the inner and returned promise
	 * @param layeredThenable the thenable with two levels of wrapping
	 * @return the "flattened" promise with only one level of wrapping
	 */
	public final <V> Promise<V> flatten(final Thenable<? extends Thenable<? extends V>> layeredThenable) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) throws Exception {
				layeredThenable.then(new ResolveCallback<Thenable<? extends V>, V>() {
					@Override
					public Thenable<V> onResolve(final Thenable<? extends V> returnedThenable) throws Exception {
						resolve.deferResolve(returnedThenable);
						return null;
					}
				}, new RejectCallback<V>() {
					@Override
					public Thenable<V> onReject(final Throwable exception) throws Exception {
						resolve.reject(exception);
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * Returns a {@code Promise} that wraps multiple thenables or promises. The returned promise is resolved when all
	 * thenables are resolved, or rejected when at least one thenable is rejected. The returned promise's value is
	 * a list containing the results of the thenables passed to the method, in the order passed.
	 *
	 * @param <V> the lower bound for value types of the combined thenables
	 * @param thenables the thenables to combine
	 * @return the combined promise
	 */
	@SafeVarargs
	public final <V> Promise<List<V>> all(final Thenable<? extends V>... thenables) {
		return all(Arrays.asList(thenables));
	}
	
	/**
	 * Returns a {@code Promise} that wraps multiple thenables or promises. The returned promise is resolved when all
	 * thenables are resolved, or rejected when at least one thenable is rejected. The returned promise's value is
	 * a list containing the results of the thenables passed to the method, in the order passed if {@code thenables}
	 * is an ordered collection, or in an undefined order otherwise.
	 *
	 * @param <V> the lower bound for value types of the combined thenables
	 * @param thenables the thenables to combine
	 * @return the combined promise
	 */
	public final <V> Promise<List<V>> all(final Collection<? extends Thenable<? extends V>> thenables) {
		return promise(new PromiseHandler<List<V>>() {
			private int remaining;
			private Object lock = new Object();
			
			@Override
			public void handle(final Resolver<List<V>> resolve) throws Exception {
				try {
					remaining = thenables.size();
					
					final List<V> result = new ArrayList<>(remaining);
					
					// Need a short-circuit here; there will be no resolve() calls in the loop for 0 items
					if (remaining == 0) {
						resolve.resolve(result);
						return;
					}
					
					int nextIndex = 0;
					
					for (final Thenable<? extends V> thenable: thenables) {
						final int index = nextIndex;
						nextIndex++;
						result.add(null);
						
						thenable.then(new ResolveCallback<V, Void>() {
							@Override
							public Promise<Void> onResolve(final V value) {
								synchronized (lock) {
									if (remaining > 0) {
										remaining--;
										result.set(index, value);
										
										if (remaining == 0) {
											resolve.resolve(result);
										}
									}
									
									return null;
								}
							}
						}, new RejectCallback<Void>() {
							@Override
							public Promise<Void> onReject(final Throwable exception) {
								synchronized (lock) {
									remaining = 0;
									resolve.reject(exception);
									return null;
								}
							}
						});
					}
				} catch (final Exception e) {
					synchronized (lock) {
						remaining = 0;
						throw e;
					}
				}
			}
		});
	}
	
	/**
	 * Returns a {@code Promise} that wraps multiple thenables or promises. The returned promise is resolved when at
	 * least one thenable are resolved, or rejected when at least one thenable is rejected. It is set to the state of
	 * the first thenable to be resolved or rejected.
	 *
	 * @param <V> the lower bound for value types of the combined thenables
	 * @param thenables the thenables to combine
	 * @return the combined promise
	 */
	@SafeVarargs
	public final <V> Promise<V> race(final Thenable<? extends V>... thenables) {
		return race(Arrays.asList(thenables));
	}
	
	/**
	 * Returns a {@code Promise} that wraps multiple thenables or promises. The returned promise is resolved when at
	 * least one thenable are resolved, or rejected when at least one thenable is rejected. It is set to the state of
	 * the first thenable to be resolved or rejected.
	 *
	 * @param <V> the lower bound for value types of the combined thenables
	 * @param thenables the thenables to combine
	 * @return the combined promise
	 */
	public final <V> Promise<V> race(final Iterable<? extends Thenable<? extends V>> thenables) {
		return promise(new PromiseHandler<V>() {
			private volatile boolean anyFinished = false;
			
			@Override
			public void handle(final Resolver<V> resolve) throws Exception {
				try {
					for (final Thenable<? extends V> thenable: thenables) {
						thenable.then(new ResolveCallback<V, Void>() {
							@Override
							public Promise<Void> onResolve(final V value) {
								if (!anyFinished) {
									resolve.resolve(value);
									anyFinished = true;
								}
								
								return null;
							}
						}, new RejectCallback<Void>() {
							@Override
							public Promise<Void> onReject(final Throwable exception) {
								if (!anyFinished) {
									resolve.reject(exception);
									anyFinished = true;
								}
								
								return null;
							}
						});
					}
				} catch (final Exception e) {
					anyFinished = true;
					throw e;
				}
			}
		});
	}
	
	/**
	 * Returns the string representation of this promise factory, which includes the string representation of
	 * the deferred invoker. Subclasses are encouraged to override this method.
	 */
	@Override
	public String toString() {
		return "PromiseFactory (deferredInvoker = " + deferredInvoker + ")";
	}
}
