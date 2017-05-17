package org.lucidfox.jpromises.core.helpers;

import org.lucidfox.jpromises.Promise;
import org.lucidfox.jpromises.core.PromiseHandler;
import org.lucidfox.jpromises.core.Resolver;
import org.lucidfox.jpromises.core.Thenable;

public class DeferredPromiseHandler<V> implements PromiseHandler<V>, Resolver<V> {
	private Resolver<V> resolver;
	
	@Override
	public void handle(final Resolver<V> resolve) throws Exception {
		resolver = resolve;
	}

	@Override
	public void reject(final Throwable exception) {
		resolver.reject(exception);
	}

	@Override
	public void resolve(final V value) {
		resolver.resolve(value);
	}

	@Override
	public void deferResolve(final Thenable<? extends V> thenable) {
		resolver.deferResolve(thenable);
	}

	@Override
	public Promise<V> getPromise() {
		return resolver.getPromise();
	}
}
