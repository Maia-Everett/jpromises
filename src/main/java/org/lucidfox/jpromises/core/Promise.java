package org.lucidfox.jpromises.core;

import java.util.LinkedList;
import java.util.Queue;

public final class Promise<V> {
	private enum State { PENDING, RESOLVED, REJECTED }
	
	private final DeferredInvoker deferredInvoker;
	private final Queue<Deferred<V, ?>> deferreds = new LinkedList<>();
	private State state = State.PENDING;
	private V resolvedValue;
	private Throwable rejectedException;
	
	/* package */ Promise(final DeferredInvoker deferredInvoker, final PromiseHandler<V> handler) {
		this.deferredInvoker = deferredInvoker;
		
		deferredInvoker.invokeDeferred(new Runnable() {
			@Override
			public void run() {
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
		});
	}

	public <R> Promise<R> then(final ResolveCallback<V, R> onResolve, final RejectCallback<R> onReject) {
		final Promise<R> result = new Promise<>(deferredInvoker, new PromiseHandler<R>() {
			@Override
			public void handle(final Resolver<R> resolve, final Rejector reject) {
				final Deferred<V, R> deferred = new Deferred<>();
				deferred.resolveCallback = onResolve;
				deferred.rejectCallback = onReject;
				deferred.resolver = resolve;
				deferred.rejector = reject;
				
				deferreds.add(deferred);
			}
		});
		
		if (state != State.PENDING) {
			processStateUpdate();
		}
		
		return result;
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
		while (!deferreds.isEmpty()) {
			// Hack around generics
			@SuppressWarnings("unchecked")
			final Deferred<V, Object> deferred = (Deferred<V, Object>) deferreds.remove();
			
			if (state == State.RESOLVED) {
				if (deferred.resolveCallback == null) {
					deferred.resolver.resolve(resolvedValue); // Assign state to wrapping promise
				} else {
					resolvePromise(deferred, deferred.resolveCallback.onResolve(resolvedValue));
				}
			} else if (state == State.REJECTED) {
				if (deferred.rejectCallback == null) {
					deferred.rejector.reject(rejectedException); // Assign state to wrapping promise
				} else {
					resolvePromise(deferred, deferred.rejectCallback.onReject(rejectedException));
				}
			} else {
				// Must never be PENDING
				throw new AssertionError();
			}
		}
	}
	
	private void resolvePromise(final Deferred<V, Object> deferred, final Promise<Object> returned) {
		if (returned == null) {
			deferred.resolver.resolve(null);
		} else {
			returned.thenNoCreatePromise(deferred.resolver, deferred.rejector);
		}
	}
	
	private static class Deferred<V, R> {
		private ResolveCallback<V, R> resolveCallback;
		private RejectCallback<R> rejectCallback;
		private Resolver<R> resolver;
		private Rejector rejector;
	}
}
