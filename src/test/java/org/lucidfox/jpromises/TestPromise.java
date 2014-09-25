package org.lucidfox.jpromises;

import org.junit.Test;

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
