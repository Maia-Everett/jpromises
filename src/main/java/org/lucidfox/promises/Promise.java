package org.lucidfox.promises;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class Promise<V> {
	public static <V> Promise<V> nil() {
		return Promise.resolve(null);
	}
	
	public static <V> Promise<V> resolve(final V value) {
		return new Promise<V>(value);
	}
	
	public static <V> Promise<V> reject(final Throwable exception) {
		return new Promise<V>(exception);
	}
	
	@SafeVarargs
	public static <V> Promise<V> all(final Promise<? extends V>... promises) {
		return MultiPromises.all(Arrays.asList(promises));
	}
	
	public static <V> Promise<V> all(final Iterable<Promise<? extends V>> promises) {
		return MultiPromises.all(promises);
	}
	
	@SafeVarargs
	public static <V> Promise<V> race(final Promise<? extends V>... promises) {
		return MultiPromises.race(Arrays.asList(promises));
	}
	
	public static <V> Promise<V> race(final Iterable<Promise<? extends V>> promises) {
		return MultiPromises.race(promises);
	}
	
	private enum State { PENDING, RESOLVED, REJECTED }
	
	private final Queue<Deferred<V, ?>> deferreds = new LinkedList<>();
	private State state = State.PENDING;
	private V resolvedValue;
	private Throwable rejectedException;
	
	public Promise(final PromiseHandler<V> handler) {
		handler.handle(new Resolver<V>() {
			@Override
			public void resolve(final V value) {
				if (state != State.PENDING) {
					throw new IllegalStateException("Promise state already defined");
				}
				
				state = State.RESOLVED;
				resolvedValue = value;
				processStateUpdate();
			}
		}, new Rejector() {
			@Override
			public void reject(final Throwable exception) {
				if (state != State.PENDING) {
					throw new IllegalStateException("Promise state already defined");
				}
				
				state = State.REJECTED;
				rejectedException = exception;
				processStateUpdate();
			}
		});
	}
	
	private Promise(final V value) {
		state = State.RESOLVED;
		resolvedValue = value;
	}
	
	private Promise(final Throwable exception) {
		state = State.REJECTED;
		rejectedException = exception;
	}

	public <R> Promise<R> then(final ResolveCallback<V, R> onResolve, final RejectCallback<R> onReject) {
		final AtomicReference<Promise<R>> result = new AtomicReference<>(); // hack for inner class
		
		result.set(new Promise<>(new PromiseHandler<R>() {
			@Override
			public void handle(final Resolver<R> resolve, final Rejector reject) {
				final Deferred<V, R> deferred = new Deferred<>();
				deferred.resolveCallback = onResolve;
				deferred.rejectCallback = onReject;
				deferred.promise = result.get();
				deferred.resolver = resolve;
				deferred.rejector = reject;
				
				deferreds.add(deferred);
			}
		}));
		
		if (state != State.PENDING) {
			processStateUpdate();
		}
		
		return result.get();
	}

	private void thenNoCreatePromise(final Resolver<Object> resolver, final Rejector rejector) {
		final Deferred<V, Object> deferred = new Deferred<>();
		deferred.resolver = resolver;
		deferred.rejector = rejector;
		
		deferreds.add(deferred);
		
		if (state != State.PENDING) {
			processStateUpdate();
		}
	}

	private void processStateUpdate() {
		for (final Deferred<V, ?> deferred: deferreds) {
			// Hack around generics
			@SuppressWarnings("unchecked")
			final Deferred<V, Object> deferredCast = (Deferred<V, Object>) deferred;
			
			if (state == State.RESOLVED) {
				if (deferredCast.resolveCallback == null) {
					deferredCast.resolver.resolve(resolvedValue); // Assign state to wrapping promise
				} else {
					resolvePromise(deferredCast, deferredCast.resolveCallback.onResolve(resolvedValue));
				}
			} else if (state == State.REJECTED) {
				if (deferredCast.rejectCallback == null) {
					deferredCast.rejector.reject(rejectedException); // Assign state to wrapping promise
				} else {
					resolvePromise(deferredCast, deferredCast.rejectCallback.onReject(rejectedException));
				}
			} else {
				// Must never be PENDING
				throw new AssertionError();
			}
		}
	}
	
	private void resolvePromise(final Deferred<V, Object> deferred, final Promise<Object> returned) {
		final Promise<Object> promise = deferred.promise;
		
		if (promise == returned) {
			throw new IllegalStateException("Returned promise cannot be the same as wrapping promise");
		}
		
		if (returned == null) {
			deferred.resolver.resolve(null);
		} else {
			returned.thenNoCreatePromise(deferred.resolver, deferred.rejector);
		}
	}
	
	private static class Deferred<V, R> {
		private ResolveCallback<V, R> resolveCallback;
		private RejectCallback<R> rejectCallback;
		private Promise<R> promise;
		private Resolver<R> resolver;
		private Rejector rejector;
	}
}
