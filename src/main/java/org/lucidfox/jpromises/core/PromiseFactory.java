package org.lucidfox.jpromises.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PromiseFactory {
	private final DeferredInvoker deferredInvoker;
	
	public PromiseFactory(final DeferredInvoker deferredInvoker) {
		this.deferredInvoker = deferredInvoker;
	}
	
	public <V> Promise<V> promise(final PromiseHandler<V> handler) {
		return new Promise<>(deferredInvoker, handler);
	}
	
	public final <V> Promise<V> nil() {
		return resolve(null);
	}
	
	public final <V> Promise<V> resolve(final V value) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve, final Rejector reject) {
				resolve.resolve(value);
			}
		});
	}
	
	public final <V> Promise<V> resolve(final Thenable<? extends V> thenable) {
		if (thenable instanceof Promise) {
			// Short-circuit
			@SuppressWarnings("unchecked")
			final Promise<V> promise = (Promise<V>) thenable;
			return promise;
		}
		
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve, final Rejector reject) throws Exception {
				@SuppressWarnings("unchecked")
				final Thenable<V> thenableCast = (Thenable<V>) thenable;
				
				thenableCast.then(new ResolveCallback<V, Void>() {
					@Override
					public Promise<Void> onResolve(final V value) {
						resolve.resolve(value);
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Promise<Void> onReject(final Throwable exception) {
						reject.reject(exception);
						return null;
					}
				});
			}
		});
	}
	
	public final <V> Promise<V> reject(final Throwable exception) {
		return promise(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve, final Rejector reject) {
				reject.reject(exception);
			}
		});
	}
	
	@SafeVarargs
	public final <V> Promise<List<V>> all(final Thenable<? extends V>... thenables) {
		return all(Arrays.asList(thenables));
	}
	
	public <V> Promise<List<V>> all(final Collection<? extends Thenable<? extends V>> thenables) {
		return promise(new PromiseHandler<List<V>>() {
			private int remaining;
			private Object lock = new Object();
			
			@Override
			public void handle(final Resolver<List<V>> resolve, final Rejector reject) throws Exception {
				try {
					remaining = thenables.size();
					
					final List<V> result = new ArrayList<>(remaining);
					int i = 0;
					
					for (final Thenable<? extends V> thenable: thenables) {
						@SuppressWarnings("unchecked")
						final Thenable<V> thenableCast = (Thenable<V>) thenable;
						final int index = i;
						i++;
						result.add(null);
						
						thenableCast.then(new ResolveCallback<V, Void>() {
							@Override
							public Promise<Void> onResolve(final V value) {
								synchronized (lock) {
									if (remaining > 0) {
										remaining--;
										result.set(index, value);
										
										if (remaining == 0) {
											resolve(result);
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
									reject(exception);
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
	
	@SafeVarargs
	public final <V> Promise<V> race(final Thenable<? extends V>... thenables) {
		return race(Arrays.asList(thenables));
	}
	
	public <V> Promise<V> race(final Iterable<? extends Thenable<? extends V>> thenables) {
		return promise(new PromiseHandler<V>() {
			private volatile boolean anyFinished = false;
			
			@Override
			public void handle(final Resolver<V> resolve, final Rejector reject) throws Exception {
				try {
					for (final Thenable<? extends V> thenable: thenables) {
						@SuppressWarnings("unchecked")
						final Thenable<V> thenableCast = (Promise<V>) thenable;
						
						thenableCast.then(new ResolveCallback<V, Void>() {
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
									reject.reject(exception);
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
}
