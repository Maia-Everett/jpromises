package org.lucidfox.jpromises.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class setting up some plumbing for JPromises test cases.
 */
abstract class AbstractPromiseTestCase {
	private static final int TEST_METHOD_TIMEOUT_SECONDS = 20;
	
	protected interface PromiseTestEnder {
		void end();
	}
	
	protected interface PromiseTest {
		void run(PromiseFactory factory, PromiseTestEnder ender) throws Exception;
	}
	
	protected final void runTest(final PromiseTest test) {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final AtomicReference<Throwable> caught = new AtomicReference<>();
		
		final PromiseFactory factory = new PromiseFactory(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						try {
							task.run();
						} catch (final Exception e) {
							caught.set(e);
							executor.shutdown();
						}
						
						return null;
					}
				});
			}
		});
		
		executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					test.run(factory, new PromiseTestEnder() {
						@Override
						public void end() {
							executor.shutdown();
						}
					});
				} catch (final Exception e) {
					caught.set(e);
					executor.shutdown();
				}
				
				return null;
			}
		});
		
		try {
			executor.awaitTermination(TEST_METHOD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			throw new AssertionError(e); // Fail
		}
		
		if (caught.get() != null) {
			throw new AssertionError(caught.get());
		}
	}
}
