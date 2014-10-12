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
	private static final int TEST_METHOD_TIMEOUT_SECONDS = 5;
	
	/**
	 * A helper object passed into {@link #runTest} to allow test methods to control the test harness.
	 */
	protected interface PromiseTestHandler {
		/**
		 * Emulates the JavaScript {@code setTimeout} function by scheduling the given {@code task} to run
		 * within {@code milliseconds} milliseconds. Any exception thrown inside the task is interpreted
		 * as a test failure.
		 *
		 * @param task the task
		 * @param milliseconds the milliseconds
		 */
		void setTimeout(Runnable task, int milliseconds);
		
		/**
		 * Signals the test harness that the test has finished and control can be returned to JUnit.
		 */
		void done();
	}
	
	protected interface PromiseTest {
		void run(PromiseFactory factory, PromiseTestHandler handler) throws Exception;
	}
	
	protected interface OnePromiseTest<V> {
		void run(Promise<V> promise, PromiseTestHandler handler) throws Exception;
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
					test.run(factory, new PromiseTestHandler() {
						@Override
						public void setTimeout(final Runnable task, final int milliseconds) {
							executor.submit(new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									try {
										Thread.sleep(milliseconds);
										task.run();
									} catch (final Exception e) {
										caught.set(e);
										executor.shutdown();
									}
									
									return null;
								}
							});
						}
						
						@Override
						public void done() {
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
	
	@JsAnalogue("testThreeCases.js")
	protected final <V> void testFulfilled(final V value, final OnePromiseTest<V> test) {
		// Already fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				test.run(factory.resolve(value), handler);
			}
		});
		
		// Immediately fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<V> deferred = new DeferredPromiseHandler<V>();
				final Promise<V> promise = factory.promise(deferred);
				test.run(promise, handler);
				deferred.resolve(value);
			}
		});
		
		// Eventually fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<V> deferred = new DeferredPromiseHandler<V>();
				final Promise<V> promise = factory.promise(deferred);
				test.run(promise, handler);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(value);
					}
				}, 50);
			}
		});
	}
	
	@JsAnalogue("testThreeCases.js")
	protected final <V> void testRejected(final Throwable exception, final OnePromiseTest<V> test) {
		// Already fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				test.run(factory.<V>reject(exception), handler);
			}
		});
		
		// Immediately fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<V> deferred = new DeferredPromiseHandler<V>();
				final Promise<V> promise = factory.promise(deferred);
				test.run(promise, handler);
				deferred.reject(exception);
			}
		});
		
		// Eventually fulfilled
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<V> deferred = new DeferredPromiseHandler<V>();
				final Promise<V> promise = factory.promise(deferred);
				test.run(promise, handler);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(exception);
					}
				}, 50);
			}
		});
	}
}
