package org.lucidfox.jpromises.core;

class DeferredPromiseHandler<V> implements PromiseHandler<V>, Resolver<V>, Rejector {
	private Resolver<V> resolver;
	private Rejector rejector;
	
	@Override
	public void handle(final Resolver<V> resolve, final Rejector reject) throws Exception {
		resolver = resolve;
		rejector = reject;
	}

	@Override
	public void reject(final Throwable exception) {
		rejector.reject(exception);
	}

	@Override
	public void resolve(final V value) {
		resolver.resolve(value);
	}

	@Override
	public void deferResolve(final Thenable<? extends V> thenable) {
		resolver.deferResolve(thenable);
	}
}
