package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 2.2.4: `onFulfilled` or `onRejected` must not be called until the execution context stack contains only 
         platform code.
 */
@JsAnalogue("2.2.4.js")
public class Test224 extends AbstractPromiseTestCase {
	private static class Dummy { }
	
	/**
	 * `then` returns before the promise becomes fulfilled or rejected.
	 */
	@Test
	public void thenReturnsBeforeResolveOrReject() {
		testFulfilled(new Dummy(), new OnePromiseTest<Dummy>() {
			private volatile boolean thenHasReturned = false;
			
			@Override
			public void run(final Promise<Dummy> promise, final PromiseTestHandler handler)
					throws Exception {
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertTrue(thenHasReturned);
						handler.done();
						return null;
					}
				}, null);
				
				thenHasReturned = true;
			}
		});
		
		testRejected(new Exception(), new OnePromiseTest<Dummy>() {
			private volatile boolean thenHasReturned = false;
			
			@Override
			public void run(final Promise<Dummy> promise, final PromiseTestHandler handler)
					throws Exception {
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertTrue(thenHasReturned);
						handler.done();
						return null;
					}
				});
				
				thenHasReturned = true;
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (fulfillment case).
	 * 
	 * When `onFulfilled` is added immediately before the promise is fulfilled.
	 */
	@Test
	public void testOnResolveBeforeResolve() {
		runTest(new PromiseTest() {
			private volatile boolean onFulfilledCalled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						handler.done();
						return null;
					}
				}, null);
				
				deferred.resolve(new Dummy());
				assertFalse(onFulfilledCalled);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (fulfillment case).
	 * 
	 * When `onFulfilled` is added immediately after the promise is fulfilled.
	 */
	@Test
	public void testOnResolveAfterResolve() {
		runTest(new PromiseTest() {
			private volatile boolean onFulfilledCalled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				deferred.resolve(new Dummy());

				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						onFulfilledCalled = true;
						handler.done();
						return null;
					}
				}, null);
				
				assertFalse(onFulfilledCalled);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (fulfillment case).
	 * 
	 * When one `onFulfilled` is added inside another `onFulfilled`.
	 */
	@Test
	public void testOnResolveInOnResolve() {
		runTest(new PromiseTest() {
			private volatile boolean firstOnFulfilledFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final Promise<Dummy> promise = factory.resolve(new Dummy());
				
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						promise.then(new ResolveCallback<Dummy, Void>() {
							@Override
							public Thenable<Void> onResolve(final Dummy value) {
								assertTrue(firstOnFulfilledFinished);
								handler.done();
								return null;
							}
						}, null);
						
						firstOnFulfilledFinished = true;
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (fulfillment case).
	 * 
	 * When `onFulfilled` is added inside an `onRejected`.
	 */
	@Test
	public void testOnResolveInOnReject() {
		runTest(new PromiseTest() {
			private volatile boolean firstOnRejectedFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.reject(new Exception()).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						factory.resolve(new Dummy()).then(new ResolveCallback<Dummy, Void>() {
							@Override
							public Thenable<Void> onResolve(final Dummy value) {
								assertTrue(firstOnRejectedFinished);
								handler.done();
								return null;
							}
						}, null);
						
						firstOnRejectedFinished = true;
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (fulfillment case).
	 * 
	 * When the promise is fulfilled asynchronously.
	 */
	@Test
	public void testAsyncResolve() {
		runTest(new PromiseTest() {
			private volatile boolean firstStackFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.resolve(new Dummy());
						firstStackFinished = true;
					}
				}, 0);
				
				promise.then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						assertTrue(firstStackFinished);
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (rejection case).
	 * 
	 * When `onRejected` is added immediately before the promise is rejected.
	 */
	@Test
	public void testOnRejectBeforeReject() {
		runTest(new PromiseTest() {
			private volatile boolean onRejectedCalled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				
				factory.promise(deferred).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						handler.done();
						return null;
					}
				});
				
				deferred.reject(new Exception());
				assertFalse(onRejectedCalled);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (rejection case).
	 * 
	 * When `onRejected` is added immediately after the promise is rejected.
	 */
	@Test
	public void testOnRejectAfterReject() {
		runTest(new PromiseTest() {
			private volatile boolean onRejectedCalled = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				deferred.reject(new Exception());

				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						onRejectedCalled = true;
						handler.done();
						return null;
					}
				});
				
				assertFalse(onRejectedCalled);
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (rejection case).
	 * 
	 * When `onRejected` is added inside an `onFulfilled`.
	 */
	@Test
	public void testOnRejectInOnResolve() {
		runTest(new PromiseTest() {
			private volatile boolean firstOnFulfilledFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.resolve(new Dummy()).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						factory.reject(new Exception()).then(null, new RejectCallback<Void>() {
							@Override
							public Thenable<Void> onReject(final Throwable exception) {
								assertTrue(firstOnFulfilledFinished);
								handler.done();
								return null;
							}
						});
						
						firstOnFulfilledFinished = true;
						return null;
					}
				}, null);
				
				
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (rejection case).
	 * 
	 * When one `onRejected` is added inside another `onRejected`.
	 */
	@Test
	public void testOnRejectInOnReject() {
		runTest(new PromiseTest() {
			private volatile boolean firstOnRejectedFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final Promise<Dummy> promise = factory.reject(new Exception());
				
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						promise.then(null, new RejectCallback<Void>() {
							@Override
							public Thenable<Void> onReject(final Throwable exception) {
								assertTrue(firstOnRejectedFinished);
								handler.done();
								return null;
							}
						});
						
						firstOnRejectedFinished = true;
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * Clean-stack execution ordering tests (rejection case).
	 * 
	 * When the promise is fulfilled asynchronously.
	 */
	@Test
	public void testAsyncReject() {
		runTest(new PromiseTest() {
			private volatile boolean firstStackFinished = false;
			
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				final DeferredPromiseHandler<Dummy> deferred = new DeferredPromiseHandler<>();
				final Promise<Dummy> promise = factory.promise(deferred);
				
				handler.setTimeout(new Runnable() {
					@Override
					public void run() {
						deferred.reject(new Exception());
						firstStackFinished = true;
					}
				}, 0);
				
				promise.then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertTrue(firstStackFinished);
						handler.done();
						return null;
					}
				});
			}
		});
	}
}
