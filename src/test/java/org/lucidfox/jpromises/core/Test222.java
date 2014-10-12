package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * 2.2.2: If {@code onFulfilled} is a function...
 */
@JsAnalogue("2.2.2.js")
public class Test222 extends AbstractPromiseTestCase {
	private static class Dummy { }
	private static class Sentinel { }
	
	private static final Sentinel SENTINEL = new Sentinel();
	
	/**
	 * 2.2.2.1: it must be called after `promise` is fulfilled, with `promise`’s fulfillment value as its "
	 * first argument.
	 */
	@Test
	public void testOnResolveAfterResolve() {
		testFulfilled(SENTINEL, new OnePromiseTest<Sentinel>() {
			@Override
			public void run(final Promise<Sentinel> promise, final PromiseTestHandler handler) throws Exception {
				promise.then(new ResolveCallback<Sentinel, Void>() {
					@Override
					public Thenable<Void> onResolve(final Sentinel value) {
						assertEquals(SENTINEL, value);
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.2.2: it must not be called before `promise` is fulfilled.
	 * 
	 * Fulfilled after a delay.
	 */
	@Test
	public void testResolvedAfterADelay() {
		runTest(new PromiseTest() {
			private volatile boolean isFulfilled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertTrue(isFulfilled);
						handler.done();
						return null;
					}
				}, null);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(new Dummy());
						isFulfilled = true;
					}
				}, 50);
			}
		});
	}
	
	/**
	 * 2.2.2.2: it must not be called before `promise` is fulfilled.
	 * 
	 * Never fulfilled.
	 */
	@Test
	public void testNeverResolved() {
		runTest(new PromiseTest() {
			private volatile boolean isFulfilled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						isFulfilled = true;
						return null;
					}
				}, null);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertFalse(isFulfilled);
						handler.done();
					}
				}, 150);
			}
		});
	}
	
	/**
	 * 2.2.2.3: it must not be called more than once.
	 * 
	 * Already fulfilled.
	 */
	@Test
	public void testAlreadyResolved() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				
				factory.resolve(new Dummy()).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.2.3: it must not be called more than once.
	 * 
	 * Trying to fulfill a pending promise more than once, immediately.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiResolveImmediate() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				}, null);
				
				deferred.resolve(new Dummy());
				
				try {
					deferred.resolve(new Dummy());
					fail();
				} catch (final IllegalStateException expected) {
					// Do nothing
				}
			}
		});
	}
	
	/**
	 * 2.2.2.3: it must not be called more than once.
	 * 
	 * Trying to fulfill a pending promise more than once, delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiResolveDelayed() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				}, null);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(new Dummy());
						
						try {
							deferred.resolve(new Dummy());
							fail();
						} catch (final IllegalStateException expected) {
							// Do nothing
						}
					}
				}, 50);
			}
		});
	}
	
	/**
	 * 2.2.2.3: it must not be called more than once.
	 * 
	 * Trying to fulfill a pending promise more than once, immediately then delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiResolveImmediateThenDelayed() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				}, null);
				
				deferred.resolve(new Dummy());
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						try {
							deferred.resolve(new Dummy());
							fail();
						} catch (final IllegalStateException expected) {
							// Do nothing
						}
					}
				}, 50);
			}
		});
	}
	
	/**
	 * When multiple `then` calls are made, spaced apart in time.
	 */
	@Test
	public void testMultipleThen() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final List<AtomicInteger> timesCalled = Arrays.asList(
						new AtomicInteger(),
						new AtomicInteger(),
						new AtomicInteger());
				
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.get(0).incrementAndGet());
						return null;
					}
				}, null);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						promise.then(new ResolveCallback<Dummy, Void>() {
							@Override
							public Thenable<Void> onResolve(final Dummy value) {
								assertEquals(1, timesCalled.get(1).incrementAndGet());
								return null;
							}
						}, null);
					}
				}, 50);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						promise.then(new ResolveCallback<Dummy, Void>() {
							@Override
							public Thenable<Void> onResolve(final Dummy value) {
								assertEquals(1, timesCalled.get(2).incrementAndGet());
								handler.done();
								return null;
							}
						}, null);
					}
				}, 100);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(new Dummy());
					}
				}, 150);
			}
		});
	}
	
	/**
	 * When `then` is interleaved with fulfillment.
	 */
	@Test
	public void testThenInterleavedWithResolve() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final List<AtomicInteger> timesCalled = Arrays.asList(
						new AtomicInteger(),
						new AtomicInteger());
				
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.get(0).incrementAndGet());
						return null;
					}
				}, null);
				
				deferred.resolve(new Dummy());
				
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertEquals(1, timesCalled.get(1).incrementAndGet());
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
}
