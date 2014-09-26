package org.lucidfox.jpromises.core;

import java.util.LinkedList;
import java.util.Queue;

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
					synchronized (lock) {
						Promise.this.resolve(value);
					}
				}
			}, new Rejector() {
				@Override
				public void reject(final Throwable exception) {
					synchronized (lock) {
						Promise.this.reject(exception);
					}
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
			
			// Promise Resolution Procedure:
			// https://github.com/promises-aplus/promises-spec#the-promise-resolution-procedure
			if (value instanceof Thenable) {
				@SuppressWarnings("unchecked")
				final Thenable<V> thenable = (Thenable<V>) value;
				
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
			} else {
				state = State.RESOLVED;
				resolvedValue = value;
				processDeferred();
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
			processDeferred();
		}
	}

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
		}
		
		deferredInvoker.invokeDeferred(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					if (state != State.PENDING) {
						processDeferred();
					}
				}
			}
		});
		
		return result;
	}
	
	private <R> void processDeferred() {
		assert state == State.RESOLVED || state == State.REJECTED;
		
		while (!deferreds.isEmpty()) {
			@SuppressWarnings("unchecked")
			final Deferred<V, R> deferred = (Deferred<V, R>) deferreds.remove();
			final Thenable<R> next;
			
			if (state == State.RESOLVED) {
				if (deferred.resolveCallback == null) {
					next = null;
				} else {
					next = deferred.resolveCallback.onResolve(resolvedValue);
				}
			} else if (state == State.REJECTED) {
				if (deferred.rejectCallback == null) {
					next = null;
				} else {
					next = deferred.rejectCallback.onReject(rejectedException);
				}
			} else {
				throw new AssertionError(); // Cannot be called from a PENDING state
			}
			
			if (next == null) {
				if (state == State.RESOLVED) {
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
	
	private static class Deferred<V, R> {
		private ResolveCallback<? super V, R> resolveCallback;
		private RejectCallback<R> rejectCallback;
		private Resolver<R> thenResolver;
		private Rejector thenRejector;
	}
}
