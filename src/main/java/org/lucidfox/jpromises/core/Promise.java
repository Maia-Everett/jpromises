/**
 * Copyright 2014 Maia Everett <maia@lucidfox.org>
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
package org.lucidfox.jpromises.core;

import java.util.LinkedList;
import java.util.Queue;

import org.lucidfox.jpromises.annotation.GwtCompatible;

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
			}, new Rejector() {
				@Override
				public void reject(final Throwable exception) {
					Promise.this.reject(exception);
				}
			});
		} catch (final Exception e) {
			synchronized (lock) {
				reject(e);
			}
		}
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
	 * Specifies code to be run after the promise is resolved (either to a value, or a rejection exception).
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
			public void handle(final Resolver<R> resolve, final Rejector reject) {
				deferred.thenResolver = resolve;
				deferred.thenRejector = reject;
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
					deferred.thenRejector.reject(exceptionInCallback);
				} else if (state == State.RESOLVED) {
					deferred.thenResolver.resolve(null);
				} else {
					deferred.thenRejector.reject(rejectedException);
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
							deferred.thenRejector.reject(exception);
							return null;
						}
					});
				} catch (final Exception e) {
					deferred.thenRejector.reject(e);
				}
			}
		}
	}
	
	/**
	 * Convenience method. Calls {@code then(onResolve, null)}.
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
	 * Convenience method. Calls {@code then(null, onReject)}.
	 * 
	 * @param <R> the value type of the result promise
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public <R> Promise<R> onException(final RejectCallback<R> onReject) {
		return then(null, onReject);
	}
	
	/**
	 * Convenience method. Works like the regular {@code then} method, except the callbacks passed to it cannot return
	 * a promise. It works as if a {@code return null} was appended to {@code onResolve} and {@code onReject}, which
	 * causes the promise to return a new {@code Promise<Void>} that is resolved to {@code null} after the current
	 * promise is resolved.
	 * 
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> then(final VoidResolveCallback<? super V> onResolve, final VoidRejectCallback onReject) {
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
	 * Convenience method. Calls {@code then(onResolve, null)}.
	 * 
	 * @see #then(VoidResolveCallback,VoidRejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> then(final VoidResolveCallback<? super V> onResolve) {
		return then(onResolve, null);
	}
	/**
	 * Convenience method. Calls {@code then(null, onReject)}.
	 * 
	 * @see #then(VoidResolveCallback,VoidRejectCallback)
	 * @param onReject the reject callback (optional)
	 * @return a {@link Promise} that is chained after the current promise
	 */
	public Promise<Void> onException(final VoidRejectCallback onReject) {
		return then(null, onReject);
	}
	
	/**
	 * Convenience method. Works like the regular {@code then} method, except the callbacks passed to it cannot return
	 * a promise. It works as if a {@code return null} was appended to {@code onResolve} and {@code onReject}. No
	 * new promise is returned, and this method is intended to be the last in a chain of {@code then} calls.
	 * 
	 * @see #then(ResolveCallback,RejectCallback)
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 */
	public void done(final VoidResolveCallback<? super V> onResolve, final VoidRejectCallback onReject) {
		then(onResolve, onReject);
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
		private Rejector thenRejector;
	}
}
