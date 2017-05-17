package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.lucidfox.jpromises.Promise;
import org.lucidfox.jpromises.PromiseFactory;
import org.lucidfox.jpromises.core.helpers.DeferredPromiseHandler;
import org.lucidfox.jpromises.core.helpers.JsAnalogue;

@JsAnalogue("2.1.3.js")
public class Test213 extends AbstractPromiseTestCase {
	private static class Dummy { }
	
	/**
	 * 2.1.3.1: When rejected, a promise: must not transition to any other state.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testNoTransition() {
		testRejected(new Exception(), new OnePromiseTest<Dummy>() {
			private volatile boolean onRejectedCalled;
			
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertFalse(onRejectedCalled);
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onRejectedCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to reject then immediately fulfill.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillThenReject() {
		runTest(new PromiseTest() {
			private volatile boolean onRejectedCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertFalse(onRejectedCalled);
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						return null;
					}
				});
				
				deferred.reject(new Exception());
				
				try {
					deferred.resolve(new Dummy());
					fail();
				} catch (final IllegalStateException expected) {
					// Do nothing
				}
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onRejectedCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to reject then fulfill, delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillThenRejectDelayed() {
		runTest(new PromiseTest() {
			private volatile boolean onRejectedCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertFalse(onRejectedCalled);
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						return null;
					}
				});
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(new Exception());
						
						try {
							deferred.resolve(new Dummy());
							fail();
						} catch (final IllegalStateException expected) {
							// Do nothing
						}
					}
				}, 50);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onRejectedCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
	
	/**
	 * Trying to reject immediately then fulfill delayed.
	 * 
	 * Note: Unlike in the Promises/A+ specification, JPromises promises throw an exception when trying to
	 * change their state.
	 */
	@Test
	public void testFulfillImmediatelyThenRejectDelayed() {
		runTest(new PromiseTest() {
			private volatile boolean onRejectedCalled;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertFalse(onRejectedCalled);
						return null;
					}
				}, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						return null;
					}
				});
				
				deferred.reject(new Exception());
				
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
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						assertTrue(onRejectedCalled);
						handler.done();
					}
				}, 100);
			}
		});
	}
}
