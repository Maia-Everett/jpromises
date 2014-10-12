package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * 2.2.2: If {@code onRejected} is a function...
 */
@JsAnalogue("2.2.3.js")
public class Test223 extends AbstractPromiseTestCase {
	private static class Dummy { }
	
	private static class SentinelException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -9020701537522840443L;
	}
	
	private static final SentinelException SENTINEL = new SentinelException();
	
	/**
	 * 2.2.2.1: it must be called after `promise` is rejected, with `promise`’s rejection reason as its
	 * first argument.
	 */
	@Test
	public void testOnRejectAfterReject() {
		testRejected(SENTINEL, new OnePromiseTest<Dummy>() {
			@Override
			public void run(final Promise<Dummy> promise, final PromiseTestHandler handler)
					throws Exception {
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertEquals(SENTINEL, exception);
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.2.2: it must not be called before `promise` is rejected.
	 * 
	 * Rejected after a delay.
	 */
	@Test
	public void testRejectedAfterADelay() {
		runTest(new PromiseTest() {
			private volatile boolean isRejected = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertTrue(isRejected);
						handler.done();
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(new Exception());
						isRejected = true;
					}
				}, 50);
			}
		});
	}
	
	/**
	 * 2.2.2.2: it must not be called before `promise` is rejected.
	 * 
	 * Never rejected.
	 */
	@Test
	public void testNeverRejected() {
		runTest(new PromiseTest() {
			private volatile boolean isRejected = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						isRejected = true;
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertFalse(isRejected);
						handler.done();
					}
				}, 150);
			}
		});
	}
	
	/**
	 * 2.2.3.3: it must not be called more than once.
	 * 
	 * Already rejected.
	 */
	@Test
	public void testAlreadyRejected() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				
				factory.reject(new Exception()).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.3.3: it must not be called more than once.
	 * 
	 * Trying to reject a pending promise more than once, immediately.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiRejectImmediate() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				});
				
				deferred.reject(new Exception());
				
				try {
					deferred.reject(new Exception());
					fail();
				} catch (final IllegalStateException expected) {
					// Do nothing
				}
			}
		});
	}
	
	/**
	 * 2.2.3.3: it must not be called more than once.
	 * 
	 * Trying to reject a pending promise more than once, delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiRejectDelayed() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(new Exception());
						
						try {
							deferred.reject(new Exception());
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
	 * 2.2.3.3: it must not be called more than once.
	 * 
	 * Trying to reject a pending promise more than once, immediately then delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testMultiRejectImmediateThenDelayed() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final AtomicInteger timesCalled = new AtomicInteger();
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.incrementAndGet());
						handler.done();
						return null;
					}
				});
				
				deferred.reject(new Exception());
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						try {
							deferred.reject(new Exception());
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
				
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.get(0).incrementAndGet());
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						promise.then(null, new RejectCallback<Void>() {
							@Override
							public Thenable<Void> onReject(final Throwable Exception) {
								assertEquals(1, timesCalled.get(1).incrementAndGet());
								return null;
							}
						});
					}
				}, 50);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						promise.then(null, new RejectCallback<Void>() {
							@Override
							public Thenable<Void> onReject(final Throwable Exception) {
								assertEquals(1, timesCalled.get(2).incrementAndGet());
								handler.done();
								return null;
							}
						});
					}
				}, 100);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(new Exception());
					}
				}, 150);
			}
		});
	}
	
	/**
	 * When `then` is interleaved with rejection.
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
				
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.get(0).incrementAndGet());
						return null;
					}
				});
				
				deferred.reject(new Exception());
				
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable Exception) {
						assertEquals(1, timesCalled.get(1).incrementAndGet());
						handler.done();
						return null;
					}
				});
			}
		});
	}
}
