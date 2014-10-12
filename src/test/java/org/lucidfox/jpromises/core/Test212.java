package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.lucidfox.jpromises.core.helpers.DeferredPromiseHandler;
import org.lucidfox.jpromises.core.helpers.JsAnalogue;

@JsAnalogue("2.1.2.js")
public class Test212 extends AbstractPromiseTestCase {
	private static class Dummy { }
	
	/**
	 * 2.1.2.1: When fulfilled, a promise: must not transition to any other state.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testNoTransition() {
		testFulfilled(new Dummy(), new OnePromiseTest<Dummy>() {
			private volatile boolean onFulfilledCalled;
			
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertFalse(onFulfilledCalled);
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onFulfilledCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to fulfill then immediately reject.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillThenReject() {
		runTest(new PromiseTest() {
			private volatile boolean onFulfilledCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertFalse(onFulfilledCalled);
						return null;
					}
				});
				
				deferred.resolve(new Dummy());
				
				try {
					deferred.reject(new Exception());
					fail();
				} catch (final IllegalStateException expected) {
					// Do nothing
				}
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onFulfilledCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to fulfill then reject, delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillThenRejectDelayed() {
		runTest(new PromiseTest() {
			private volatile boolean onFulfilledCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertFalse(onFulfilledCalled);
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(new Dummy());
						
						try {
							deferred.reject(new Exception());
							fail();
						} catch (final IllegalStateException expected) {
							// Do nothing
						}
					}
				}, 50);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onFulfilledCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to fulfill immediately then reject delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillImmediatelyThenRejectDelayed() {
		runTest(new PromiseTest() {
			private volatile boolean onFulfilledCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertFalse(onFulfilledCalled);
						return null;
					}
				});
				
				deferred.resolve(new Dummy());
				
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
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onFulfilledCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
}
