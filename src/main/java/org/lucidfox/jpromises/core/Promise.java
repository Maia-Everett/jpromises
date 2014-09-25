package org.lucidfox.jpromises.core;

import java.util.LinkedList;
import java.util.Queue;

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
	public <R> Promise<R> then(final ResolveCallback<V, R> onResolve, final RejectCallback<R> onReject) {
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
			final Promise<R> nextPromise;
			
			if (state == State.RESOLVED) {
				if (deferred.resolveCallback == null) {
					nextPromise = null;
				} else {
					nextPromise = deferred.resolveCallback.onResolve(resolvedValue);
				}
			} else if (state == State.REJECTED) {
				if (deferred.rejectCallback == null) {
					nextPromise = null;
				} else {
					nextPromise = deferred.rejectCallback.onReject(rejectedException);
				}
			} else {
				throw new AssertionError(); // Cannot be called from a PENDING state
			}
			
			if (nextPromise == null) {
				deferred.thenResolver.resolve(null);
			} else {
				nextPromise.then(new ResolveCallback<R, Object>() {
					@Override
					public Promise<Object> onResolve(final R value) {
						deferred.thenResolver.resolve(value);
						return null;
					}
				}, new RejectCallback<Object>() {
					@Override
					public Promise<Object> onReject(final Throwable exception) {
						deferred.thenRejector.reject(exception);
						return null;
					}
				});
			}
		}
	}
	
	private static class Deferred<V, R> {
		private ResolveCallback<V, R> resolveCallback;
		private RejectCallback<R> rejectCallback;
		private Resolver<R> thenResolver;
		private Rejector thenRejector;
	}
}
