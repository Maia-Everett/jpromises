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

import java.util.LinkedList;
import java.util.Queue;

import org.lucidfox.jpromises.annotation.GwtCompatible;
import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseHandler;
import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Resolver;
import org.lucidfox.jpromises.core.Thenable;
import org.lucidfox.jpromises.core.ValueRejectCallback;
import org.lucidfox.jpromises.core.ValueResolveCallback;
import org.lucidfox.jpromises.core.VoidRejectCallback;
import org.lucidfox.jpromises.core.VoidResolveCallback;

/**
 * <p>
 * The core class of the JPromises library.
 * </p><p>
 * {@code Promise} is a concrete implementation of {@link Thenable} that represents a value to be computed later.
 * </p><p>
 * A {@code Promise} is instantiated with a {@link PromiseHandler}, which computes the value and sets the promise
 * into either the resolved or rejected state. Further attempts to change the promise's state will throw
 * {@link IllegalStateException}.
 * </p><p>
 * Once a promise is either resolved or rejected, it will chain to all resolve or reject callbacks added by
 * {@link then}, in the same order in which {@code then} is called. For each call to {@code then}, either
 * the resolved callback is called if the promise was resolved (with the promise's resolved value), or
 * the rejected callback is called if the promise was rejected (with the promise's rejection reason).
 * </p><p>
 * If the callback returns a promise, then the promise previously returned by {@code then} is chained after
 * the promise returned by the callback, using its {@code then} method, and resolved/rejected with that promise's
 * result or reject reason, respectively. Otherwise, the promise previously returned by {@code then} is either resolved
 * with {@code null}, or rejected with the current promise's reject reason, depending on the current promise's state.
 * </p><p>
 * (Note that resolving the promise returned by {@code then} with {@code null} goes against the JavaScript Promises/A+
 * specification. In Java's typesafe world, it is necessary because, generally speaking, the returned promise may
 * have a different value type compared to the current promise. If you want to work with a meaningful value in the
 * chained promise, pass it a meaningful value through the resolved callback, perhaps using
 * {@link PromiseFactory#resolve}.)
 * </p><p>
 * Callbacks passed via {@code then} are guaranteed to be invoked asynchronously, as defined by the promise factory's
 * specified deferred invoker. In practice, this usually means they are invoked after all pending events in the
 * application's event loop are processed.
 * </p><p>
 * This class is thread-safe. Objects passed to the {@link PromiseHandler} or {@link #then} can be called from
 * any thread without corrupting the promise's state.
 * </p>
 *
 * @param <V> the value type
 */
@GwtCompatible
public final class Promise<V> implements Thenable<V> {
	private enum State { PENDING, RESOLVED, REJECTED }
	
	private final DeferredInvoker deferredInvoker;
	private final Queue<Deferred<V, ?>> deferreds = new LinkedList<>();
	private final Object lock = new Object();
	
	private State state = State.PENDING;
	private V resolvedValue;
	private Throwable rejectedException;
	
	/* package */ Promise(final DeferredInvoker deferredInvoker, final PromiseHandler<V> handler) {
		this.deferredInvoker = deferredInvoker;
		
		try {
			handler.handle(new Resolver<V>() {
				@Override
				public void resolve(final V value) {
					Promise.this.resolve(value);
				}

				@Override
				public void deferResolve(final Thenable<? extends V> thenable) {
					Promise.this.deferResolve(thenable);
				}

				@Override
				public void reject(final Throwable exception) {
					Promise.this.reject(exception);
				}
				
				@Override
				public Promise<V> getPromise() {
					return Promise.this;
				}
			});
		} catch (final Exception e) {
			synchronized (lock) {
				reject(e);
			}
		}
	}
	
	/* package */ Promise(final DeferredInvoker deferredInvoker, final V value) {
		this(deferredInvoker, new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve) {
				resolve.resolve(value);
			}
		});
	}
	
	private void resolve(final V value) {
		synchronized (lock) {
			if (state != State.PENDING) {
				throw new IllegalStateException("Promise state already defined.");
			}
			
			if (value == this) {
				throw new IllegalStateException("A promise cannot be resolved with itself.");
			}
			
			state = State.RESOLVED;
			resolvedValue = value;
			scheduleProcessDeferred();
		}
	}
	
	private void deferResolve(final Thenable<? extends V> thenable) {
		synchronized (lock) {
			if (state != State.PENDING) {
				throw new IllegalStateException("Promise state already defined.");
			}
			
			if (thenable == this) {
				throw new IllegalStateException("A promise cannot be resolved with itself.");
			}
			
			// Promise Resolution Procedure:
			// https://github.com/promises-aplus/promises-spec#the-promise-resolution-procedure
			try {
				thenable.then(new ResolveCallback<V, Object>() {
					@Override
					public Promise<Object> onResolve(final V value) {
						resolve(value);
						return null;
					}
				}, new RejectCallback<Object>() {
					@Override
					public Promise<Object> onReject(final Throwable exception) {
						reject(exception);
						return null;
					}
				});
			} catch (final Exception e) {
				reject(e);
			}
		}
	}
	
	private void reject(final Throwable exception) {
		synchronized (lock) {
			if (state != State.PENDING) {
				throw new IllegalStateException("Promise state already defined.");
			}
			
			state = State.REJECTED;
			rejectedException = exception;
			scheduleProcessDeferred();
		}
	}

	/**
	 * <p>
	 * Returns a new {@code Promise} that, after this promise is resolved or rejected,
	 * is in turn resolved/rejected with the value of the promise returned by {@code onResolve} if this promise was
	 * resolved, or the value of the promise returned by {@code onReject} if this promise was rejected.
	 * </p><p>
	 * After the callbacks are registered using {@code then}, if the promise's computation completes successfully,
	 * it calls {@code onResolve} with the computed value. If it fails, it calls {@code onReject} with
	 * the rejection reason (exception). Only one of these callbacks is called, and only once.
	 * </p><p>
	 * If either callback is null, or returns a null promise, the thenable returned by the method is in turn
	 * resolved or rejected, depending on whether this promise was resolved or rejected. If a valid promise was
	 * returned, the promise returned by this method is chained after the thenable returned by the callback.
	 * </p><p>
	 * This implementation throws no exceptions. Any exceptions are passed by rejecting the returned promise.
	 * Additionally, any exception thrown during execution of {@code onResolve} or {@code onReject} causes the resulting
	 * promise to be rejected with that exception.
	 * </p>
	 *
	 * @param <R> the value type of the result promise
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	@Override
	public <R> Promise<R> then(final ResolveCallback<? super V, R> onResolve, final RejectCallback<R> onReject) {
		final Deferred<V, R> deferred = new Deferred<>();
		deferred.resolveCallback = onResolve;
		deferred.rejectCallback = onReject;
		
		final Promise<R> result = new Promise<>(deferredInvoker, new PromiseHandler<R>() {
			@Override
			public void handle(final Resolver<R> resolve) {
				deferred.thenResolver = resolve;
			}
		});
		
		synchronized (lock) {
			deferreds.add(deferred);
			
			if (state != State.PENDING) {
				scheduleProcessDeferred();
			}
		}
		
		return result;
	}
	
	private void scheduleProcessDeferred() {
		deferredInvoker.invokeDeferred(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					processDeferred();
				}
			}
		});
	}
	
	private <R> void processDeferred() {
		assert state == State.RESOLVED || state == State.REJECTED;
		
		while (!deferreds.isEmpty()) {
			@SuppressWarnings("unchecked")
			final Deferred<V, R> deferred = (Deferred<V, R>) deferreds.remove();
			Thenable<R> next = null;
			Throwable exceptionInCallback = null;
			
			if (state == State.RESOLVED) {
				if (deferred.resolveCallback == null) {
					next = null;
				} else {
					try {
						next = deferred.resolveCallback.onResolve(resolvedValue);
					} catch (final Exception e) {
						exceptionInCallback = e;
					}
				}
			} else if (state == State.REJECTED) {
				if (deferred.rejectCallback == null) {
					next = null;
				} else {
					try {
						next = deferred.rejectCallback.onReject(rejectedException);
					} catch (final Exception e) {
						exceptionInCallback = e;
					}
				}
			} else {
				throw new AssertionError(); // Cannot be called from a PENDING state
			}
			
			if (next == null) {
				if (exceptionInCallback != null) {
					deferred.thenResolver.reject(exceptionInCallback);
				} else if (state == State.RESOLVED) {
					deferred.thenResolver.resolve(null);
				} else {
					deferred.thenResolver.reject(rejectedException);
				}
			} else {
				try {
					next.then(new ResolveCallback<R, Void>() {
						@Override
						public Promise<Void> onResolve(final R value) {
							deferred.thenResolver.resolve(value);
							return null;
						}
					}, new RejectCallback<Void>() {
						@Override
						public Promise<Void> onReject(final Throwable exception) {
							deferred.thenResolver.reject(exception);
							return null;
						}
					});
				} catch (final Exception e) {
					deferred.thenResolver.reject(e);
				}
			}
		}
	}
	
	/**
	 * Calls {@code then(onResolve, null)}.
	 * 
	 * @param <R> the value type of the result promise
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> then(final ResolveCallback<? super V, R> onResolve) {
		return then(onResolve, null);
	}
	
	/**
	 * Same as {@link #then(ResolveCallback, RejectCallback)}. This method is provided for users used to the
	 * {@code CompletableFuture} API.
	 *
	 * @param <R> the value type of the result promise
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> thenCompose(final ResolveCallback<? super V, R> onResolve, final RejectCallback<R> onReject) {
		return then(onResolve, onReject);
	}
	
	/**
	 * Same as {@link #then(ResolveCallback)}. This method is provided for users used to the
	 * {@code CompletableFuture} API.
	 * 
	 * @param <R> the value type of the result promise
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> thenCompose(final ResolveCallback<? super V, R> onResolve) {
		return then(onResolve, null);
	}
	
	/**
	 * Returns a new {@code Promise} that, after this promise is rejected, is resolved with the promise returned by
	 * {@code onReject}, or after this promise is resolved, is resolved with the value of this promise.
	 * 
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<V> onException(final RejectCallback<V> onReject) {
		return then(new ResolveCallback<V, V>() {
			@Override
			public Thenable<V> onResolve(final V value) throws Exception {
				return Promise.this;
			}
		}, onReject);
	}
	
	/**
	 * Returns a new {@code Promise} that, after this promise is resolved or rejected, is immediately resolved to the
	 * value returned by {@code onResolve} if this promise was resolved or by {@code onReject} if this promise was
	 * rejected.
	 * <p>
	 * This method works like the regular {@code then} method, except the callbacks passed to it return a flat value
	 * instead of a promise.
	 * </p>
	 * <p>
	 * This implementation throws no exceptions. Any exception thrown during execution of {@code onResolve} or
	 * {@code onReject} causes the resulting promise to be rejected with that exception.
	 * </p>
	 *
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param <R> the value type of the result promise
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> thenApply(final ValueResolveCallback<? super V, R> onResolve,
			final ValueRejectCallback<R> onReject) {
		return then(onResolve == null ? null : new ResolveCallback<V, R>() {
			@Override
			public Thenable<R> onResolve(final V value) throws Exception {
				return new Promise<R>(deferredInvoker, onResolve.onResolve(value));
			}
		}, onReject == null ? null : new RejectCallback<R>() {
			@Override
			public Thenable<R> onReject(final Throwable exception) throws Exception {
				return new Promise<R>(deferredInvoker, onReject.onReject(exception));
			}
		});
	}
	
	/**
	 * Calls {@code thenApply(onResolve, null)}.
	 * 
	 * @see #thenApply(ValueResolveCallback, ValueRejectCallback)
	 * @param <R> the value type of the result promise
	 * @param onResolve the resolve callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> thenApply(final ValueResolveCallback<? super V, R> onResolve) {
		return thenApply(onResolve, null);
	}
	
	/**
	 * Returns a promise that, after this promise is rejected, is resolved with the value returned by {@code onReject},
	 * or after this promise is resolved, is resolved with the value of this promise.
	 * 
	 * @see #thenApply(ValueResolveCallback, ValueRejectCallback)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<V> onExceptionApply(final ValueRejectCallback<V> onReject) {
		return thenApply(new ValueResolveCallback<V, V>() {
			@Override
			public V onResolve(final V value) throws Exception {
				return value;
			}
		}, onReject);
	}
	
	/**
	 * Returns a new {@code Promise} that, after this promise is resolved or rejected, calls {@code onResolve} and
	 * becomes resolved to {@code null} if this promise was resolved, or calls {@code onReject} and becomes resolved
	 * to {@code null} if this promise was rejected.
	 * <p>
	 * This implementation throws no exceptions. Any exception thrown during execution of {@code onResolve} or
	 * {@code onReject} causes the resulting promise to be rejected with that exception.
	 * </p>
	 * 
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> thenAccept(final VoidResolveCallback<? super V> onResolve, final VoidRejectCallback onReject) {
		return then(onResolve == null ? null : new ResolveCallback<V, Void>() {
			@Override
			public Thenable<Void> onResolve(final V value) throws Exception {
				onResolve.onResolve(value);
				return null;
			}
		}, onReject == null ? null : new RejectCallback<Void>() {
			@Override
			public Thenable<Void> onReject(final Throwable exception) throws Exception {
				onReject.onReject(exception);
				return null;
			}
		});
	}
	
	/**
	 * Calls {@code thenAccept(onResolve, null)}.
	 * 
	 * @see #thenAccept(VoidResolveCallback,VoidRejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> thenAccept(final VoidResolveCallback<? super V> onResolve) {
		return thenAccept(onResolve, null);
	}
	
	/**
	 * Returns a promise that, after this promise is rejected, calls {@code onReject} and becomes resolved with
	 * {@code null}, or after this promise is resolved, is resolved with {@code null}.
	 * 
	 * @see #thenAccept(VoidResolveCallback,VoidRejectCallback)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> onExceptionAccept(final VoidRejectCallback onReject) {
		return thenAccept(null, onReject);
	}
	
	/**
	 * Same as {@link #thenAccept(VoidResolveCallback,VoidRejectCallback)}, but does not return a value, preventing
	 * any further additions to the then-chain after this promise.
	 * 
	 * @see #thenAccept(VoidResolveCallback,VoidRejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 */
	public void done(final VoidResolveCallback<? super V> onResolve, final VoidRejectCallback onReject) {
		thenAccept(onResolve, onReject);
	}
	
	/**
	 * Calls {@code done(onResolve, null)}.
	 * 
	 * @see #done(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 */
	public void done(final VoidResolveCallback<? super V> onResolve) {
		done(onResolve, null);
	}
	
	/**
	 * <p>Returns the string representation of this promise.</p>
	 * 
	 * <p>It is only intended for debugging purposes, so the format is not fixed. However, the following may be
	 * regarded as typical:</p>
	 * 
	 * <ul>
	 * <li>{@code Promise (pending)}</li>
	 * <li>{@code Promise (resolved, value = 1970-01-01T00:00:00Z)}</li>
	 * <li>{@code Promise (rejected, exception = java.lang.ArithmeticException: / by zero)}</li>
	 * </ul>
	 * 
	 * <p>The state of the promise is, at this time, intentionally not exposed via the public API, though it may be
	 * in the future. Do not parse the output of this method to find the promise state programmatically.</p>
	 */
	@Override
	public String toString() {
		switch (state) {
		case PENDING:
			return "Promise (pending)";
		case RESOLVED:
			return "Promise (resolved, value = " + resolvedValue + ")";
		case REJECTED:
			return "Promise (rejected, value = " + rejectedException + ")";
		default:
			// Cannot happen
			throw new AssertionError();
		}
	}
	
	private static class Deferred<V, R> {
		private ResolveCallback<? super V, R> resolveCallback;
		private RejectCallback<R> rejectCallback;
		private Resolver<R> thenResolver;
	}
}
