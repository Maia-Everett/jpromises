package org.lucidfox.jpromises.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Test;

public class TestPromise {
	@Test
	public void testBlah() {
		final Executor executor = Executors.newSingleThreadExecutor();
		final PromiseFactory factory = new PromiseFactory(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				executor.execute(task);
			}
		});
		
		factory.promise(new PromiseHandler<String>() {
			@Override
			public void handle(final Resolver<String> resolve, final Rejector reject) {
				resolve.resolve("Hello World!");
			}
		}).then(new ResolveCallback<String, Void>() {
			@Override
			public Promise<Void> onResolve(final String value) {
				System.out.println(value);
				return factory.nil();
			}
		}, null);
	}
}
