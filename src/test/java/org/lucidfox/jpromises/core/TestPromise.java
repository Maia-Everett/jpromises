package org.lucidfox.jpromises.core;

import org.junit.Test;
import org.lucidfox.jpromises.core.Promise;
import org.lucidfox.jpromises.core.PromiseHandler;
import org.lucidfox.jpromises.core.Rejector;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Resolver;

public class TestPromise {
	@Test
	public void testBlah() {
		new Promise<>(new PromiseHandler<String>() {
			@Override
			public void handle(final Resolver<String> resolve, final Rejector reject) {
				resolve.resolve("Hello World!");
			}
		}).then(new ResolveCallback<String, Void>() {
			@Override
			public Promise<Void> onResolve(final String value) {
				System.out.println(value);
				return Promise.nil();
			}
		}, null);
	}
}
