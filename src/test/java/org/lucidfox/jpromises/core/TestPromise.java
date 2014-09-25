package org.lucidfox.jpromises.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class TestPromise {
	@Test
	public void testSimplePromise() throws InterruptedException {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final PromiseFactory factory = new PromiseFactory(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				executor.execute(task);
			}
		});
		
		final CountDownLatch promiseWaiter = new CountDownLatch(1);
		
		factory.promise(new PromiseHandler<String>() {
			@Override
			public void handle(final Resolver<String> resolve, final Rejector reject) {
				resolve.resolve("Hello World!");
			}
		}).then(new ResolveCallback<String, Void>() {
			@Override
			public Promise<Void> onResolve(final String value) {
				System.out.println("testSimplePromise: " + value);
				promiseWaiter.countDown();
				return null;
			}
		}, null);
		
		promiseWaiter.await();
		executor.shutdown();
	}

	@Test
	public void testTwoThens() throws InterruptedException {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final PromiseFactory factory = new PromiseFactory(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				executor.execute(task);
			}
		});
		
		final CountDownLatch promiseWaiter = new CountDownLatch(1);
		
		factory.promise(new PromiseHandler<String>() {
			@Override
			public void handle(final Resolver<String> resolve, final Rejector reject) {
				System.out.println("testTwoThens: In handle");
				resolve.resolve("Hello World!");
			}
		}).then(new ResolveCallback<String, String>() {
			@Override
			public Promise<String> onResolve(final String value) {
				System.out.println("testTwoThens: " + value);
				return null;
			}
		}, null).then(new ResolveCallback<String, Void>() {
			@Override
			public Promise<Void> onResolve(final String value) {
				System.out.println("testTwoThens: In second then");
				promiseWaiter.countDown();
				return null;
			}
		}, null);
		
		System.out.println("testTwoThens: After promise creation");
		promiseWaiter.await();
		executor.shutdown();
	}
}
