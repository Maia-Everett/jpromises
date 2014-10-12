package org.lucidfox.jpromises.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.lucidfox.jpromises.core.helpers.JsAnalogue;
import org.lucidfox.jpromises.core.helpers.SpyRejectCallback;
import org.lucidfox.jpromises.core.helpers.SpyResolveCallback;

/**
 * 2.2.6: `then` may be called multiple times on the same promise.
 */
@JsAnalogue("2.2.6.js")
public class Test226 extends AbstractPromiseTestCase {
	private static class Dummy { }
	private static class Other { }
	private static class Sentinel { }

	private static final Sentinel SENTINEL = new Sentinel();
	private static final Sentinel SENTINEL3 = new Sentinel();

	private static class SentinelException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 5390032391368013915L;
	}
	
	private static final SentinelException SENTINEL_EXCEPTION = new SentinelException();
	
	private static class CallbackAggregator {
		private final AtomicInteger soFar;
		private final PromiseTestHandler handler;
		
		public CallbackAggregator(final int times, final PromiseTestHandler handler) {
			soFar = new AtomicInteger(times);
			this.handler = handler;
		}
		
		public void oneDone() {
			if (soFar.decrementAndGet() == 0) {
				handler.done();
			}
		}
	}
		
	/**
	 * 2.2.6.1: If/when `promise` is fulfilled, all respective `onFulfilled` callbacks must execute in the
             order of their originating calls to `then`.
             
       Multiple boring fulfillment handlers.
	 */
	@Test
	public void testMultipleThenCalls() {
		testFulfilled(SENTINEL, new OnePromiseTest<Sentinel>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Sentinel> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyResolveCallback<Sentinel, Other> handler1 = SpyResolveCallback.returning(factory, new Other());
				final SpyResolveCallback<Sentinel, Other> handler2 = SpyResolveCallback.returning(factory, new Other());
				final SpyResolveCallback<Sentinel, Other> handler3 = SpyResolveCallback.returning(factory, new Other());
				final SpyRejectCallback<Other> spy = SpyRejectCallback.nil();
				
				promise.then(handler1, spy);
				promise.then(handler2, spy);
				promise.then(handler3, spy);
				
				promise.then(new ResolveCallback<Sentinel, Other>() {
					@Override
					public Thenable<Other> onResolve(final Sentinel value) throws Exception {
						assertEquals(SENTINEL, value);
						
						handler1.assertCalledWith(SENTINEL);
						handler2.assertCalledWith(SENTINEL);
						handler3.assertCalledWith(SENTINEL);
						spy.assertNotCalled();
						
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.1: If/when `promise` is fulfilled, all respective `onFulfilled` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Multiple fulfillment handlers, one of which throws.
	 */
	@Test
	public void testMultipleThenCallsWithThrow() {
		testFulfilled(SENTINEL, new OnePromiseTest<Sentinel>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Sentinel> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyResolveCallback<Sentinel, Other> handler1 = SpyResolveCallback.returning(factory, new Other());
				final SpyResolveCallback<Sentinel, Other> handler2 = SpyResolveCallback.throwing(factory,
						new Exception());
				final SpyResolveCallback<Sentinel, Other> handler3 = SpyResolveCallback.returning(factory, new Other());
				final SpyRejectCallback<Other> spy = SpyRejectCallback.nil();
				
				promise.then(handler1, spy);
				promise.then(handler2, spy);
				promise.then(handler3, spy);
				
				promise.then(new ResolveCallback<Sentinel, Other>() {
					@Override
					public Thenable<Other> onResolve(final Sentinel value) throws Exception {
						assertEquals(SENTINEL, value);
						
						handler1.assertCalledWith(SENTINEL);
						handler2.assertCalledWith(SENTINEL);
						handler3.assertCalledWith(SENTINEL);
						spy.assertNotCalled();
						
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.1: If/when `promise` is fulfilled, all respective `onFulfilled` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Results in multiple branching chains with their own fulfillment values.
	 */
	@Test
	public void testBranchingResolve() {
		testFulfilled(new Dummy(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final CallbackAggregator semiDone = new CallbackAggregator(3, handler);
				
				promise.then(new ResolveCallback<Dummy, Sentinel>() {
					@Override
					public Thenable<Sentinel> onResolve(final Dummy value) {
						return factory.resolve(SENTINEL);
					}
				}, null).then(new ResolveCallback<Sentinel, Void>() {
					@Override
					public Thenable<Void> onResolve(final Sentinel value) {
						assertEquals(SENTINEL, value);
						semiDone.oneDone();
						return null;
					}
				}, null);
				
				promise.then(new ResolveCallback<Dummy, Sentinel>() {
					@Override
					public Thenable<Sentinel> onResolve(final Dummy value) throws Exception {
						throw SENTINEL_EXCEPTION;
					}
				}, null).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertEquals(SENTINEL_EXCEPTION, exception);
						semiDone.oneDone();
						return null;
					}
				});

				promise.then(new ResolveCallback<Dummy, Sentinel>() {
					@Override
					public Thenable<Sentinel> onResolve(final Dummy value) {
						return factory.resolve(SENTINEL3);
					}
				}, null).then(new ResolveCallback<Sentinel, Void>() {
					@Override
					public Thenable<Void> onResolve(final Sentinel value) {
						assertEquals(SENTINEL3, value);
						semiDone.oneDone();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.1: If/when `promise` is fulfilled, all respective `onFulfilled` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   `onFulfilled` handlers are called in the original order.
	 */
	@Test
	public void testResolveOrder() {
		testFulfilled(new Dummy(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyResolveCallback<Dummy, Other> handler1 = SpyResolveCallback.nil();
				final SpyResolveCallback<Dummy, Other> handler2 = SpyResolveCallback.nil();
				final SpyResolveCallback<Dummy, Other> handler3 = SpyResolveCallback.nil();
				
				promise.then(handler1, null);
				promise.then(handler2, null);
				promise.then(handler3, null);
				
				promise.then(new ResolveCallback<Dummy, Other>() {
					@Override
					public Thenable<Other> onResolve(final Dummy value) throws Exception {
						SpyResolveCallback.assertCallOrder(handler1, handler2, handler3);
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.1: If/when `promise` is fulfilled, all respective `onFulfilled` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Even when one handler is added inside another handler.
	 */
	@Test
	public void testResolveOrderNested() {
		testFulfilled(new Dummy(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyResolveCallback<Dummy, Other> handler1 = SpyResolveCallback.nil();
				final SpyResolveCallback<Dummy, Other> handler2 = SpyResolveCallback.nil();
				final SpyResolveCallback<Dummy, Other> handler3 = SpyResolveCallback.nil();
				
				promise.then(new ResolveCallback<Dummy, Other>() {
					@Override
					public Thenable<Other> onResolve(final Dummy value) throws Exception {
						handler1.onResolve(value);
						promise.then(handler3, null);
						return null;
					}
				}, null);
				
				promise.then(handler2, null);
				
				promise.then(new ResolveCallback<Dummy, Other>() {
					@Override
					public Thenable<Other> onResolve(final Dummy value) throws Exception {
						// Give implementations a bit of extra time to flush their internal queue, if necessary.
						handler.setTimeout(new Runnable() {
							@Override
							public void run() {
								SpyResolveCallback.assertCallOrder(handler1, handler2, handler3);
								handler.done();
							}
						}, 15);
						
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.2: If/when `promise` is rejected, all respective `onRejected` callbacks must execute in the
             order of their originating calls to `then`.
             
       Multiple boring rejection handlers.
	 */
	@Test
	public void testMultipleThenCallsReject() {
		testRejected(SENTINEL_EXCEPTION, new OnePromiseTest<Sentinel>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Sentinel> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyRejectCallback<Other> handler1 = SpyRejectCallback.returning(factory, new Other());
				final SpyRejectCallback<Other> handler2 = SpyRejectCallback.returning(factory, new Other());
				final SpyRejectCallback<Other> handler3 = SpyRejectCallback.returning(factory, new Other());
				final SpyResolveCallback<Sentinel, Other> spy = SpyResolveCallback.nil();
				
				promise.then(spy, handler1);
				promise.then(spy, handler2);
				promise.then(spy, handler3);
				
				promise.then(null, new RejectCallback<Other>() {
					@Override
					public Thenable<Other> onReject(final Throwable exception) throws Exception {
						assertEquals(SENTINEL_EXCEPTION, exception);
						
						handler1.assertCalledWith(SENTINEL_EXCEPTION);
						handler2.assertCalledWith(SENTINEL_EXCEPTION);
						handler3.assertCalledWith(SENTINEL_EXCEPTION);
						spy.assertNotCalled();
						
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.6.2: If/when `promise` is rejected, all respective `onRejected` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Multiple rejection handlers, one of which throws.
	 */
	@Test
	public void testMultipleThenCallsRejectWithThrow() {
		testRejected(SENTINEL_EXCEPTION, new OnePromiseTest<Sentinel>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Sentinel> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyRejectCallback<Other> handler1 = SpyRejectCallback.returning(factory, new Other());
				final SpyRejectCallback<Other> handler2 = SpyRejectCallback.throwing(factory, new Exception());
				final SpyRejectCallback<Other> handler3 = SpyRejectCallback.returning(factory, new Other());
				final SpyResolveCallback<Sentinel, Other> spy = SpyResolveCallback.nil();
				
				promise.then(spy, handler1);
				promise.then(spy, handler2);
				promise.then(spy, handler3);
				
				promise.then(null, new RejectCallback<Other>() {
					@Override
					public Thenable<Other> onReject(final Throwable exception) throws Exception {
						assertEquals(SENTINEL_EXCEPTION, exception);
						
						handler1.assertCalledWith(SENTINEL_EXCEPTION);
						handler2.assertCalledWith(SENTINEL_EXCEPTION);
						handler3.assertCalledWith(SENTINEL_EXCEPTION);
						spy.assertNotCalled();
						
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.6.2: If/when `promise` is rejected, all respective `onRejected` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Results in multiple branching chains with their own rejection values.
	 */
	@Test
	public void testBranchingReject() {
		testRejected(new Exception(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final CallbackAggregator semiDone = new CallbackAggregator(3, handler);
				
				promise.then(null, new RejectCallback<Sentinel>() {
					@Override
					public Thenable<Sentinel> onReject(final Throwable exception) {
						return factory.resolve(SENTINEL);
					}
				}).then(new ResolveCallback<Sentinel, Void>() {
					@Override
					public Thenable<Void> onResolve(final Sentinel value) {
						assertEquals(SENTINEL, value);
						semiDone.oneDone();
						return null;
					}
				}, null);
				
				promise.then(null, new RejectCallback<Sentinel>() {
					@Override
					public Thenable<Sentinel> onReject(final Throwable exception) throws Exception {
						throw SENTINEL_EXCEPTION;
					}
				}).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						assertEquals(SENTINEL_EXCEPTION, exception);
						semiDone.oneDone();
						return null;
					}
				});

				promise.then(null, new RejectCallback<Sentinel>() {
					@Override
					public Thenable<Sentinel> onReject(final Throwable exception) {
						return factory.resolve(SENTINEL3);
					}
				}).then(new ResolveCallback<Sentinel, Void>() {
					@Override
					public Thenable<Void> onResolve(final Sentinel value) {
						assertEquals(SENTINEL3, value);
						semiDone.oneDone();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.6.2: If/when `promise` is rejected, all respective `onRejected` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   `onRejected` handlers are called in the original order.
	 */
	@Test
	public void testRejectOrder() {
		testRejected(new Exception(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyRejectCallback<Other> handler1 = SpyRejectCallback.nil();
				final SpyRejectCallback<Other> handler2 = SpyRejectCallback.nil();
				final SpyRejectCallback<Other> handler3 = SpyRejectCallback.nil();
				
				promise.then(null, handler1);
				promise.then(null, handler2);
				promise.then(null, handler3);
				
				promise.then(null, new RejectCallback<Other>() {
					@Override
					public Thenable<Other> onReject(final Throwable exception) throws Exception {
						SpyRejectCallback.assertCallOrder(handler1, handler2, handler3);
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.6.2: If/when `promise` is rejected, all respective `onRejected` callbacks must execute in the
	         order of their originating calls to `then`.
	         
	   Even when one handler is added inside another handler.
	 */
	@Test
	public void testRejectOrderNested() {
		testRejected(new Exception(), new OnePromiseTest<Dummy>() {
			@Override
			public void run(final PromiseFactory factory, final Promise<Dummy> promise,
					final PromiseTestHandler handler) throws Exception {
				final SpyRejectCallback<Other> handler1 = SpyRejectCallback.nil();
				final SpyRejectCallback<Other> handler2 = SpyRejectCallback.nil();
				final SpyRejectCallback<Other> handler3 = SpyRejectCallback.nil();
				
				promise.then(null, new RejectCallback<Other>() {
					@Override
					public Thenable<Other> onReject(final Throwable exception) throws Exception {
						handler1.onReject(exception);
						promise.then(null, handler3);
						return null;
					}
				});
				
				promise.then(null, handler2);
				
				promise.then(null, new RejectCallback<Other>() {
					@Override
					public Thenable<Other> onReject(final Throwable exception) throws Exception {
						// Give implementations a bit of extra time to flush their internal queue, if necessary.
						handler.setTimeout(new Runnable() {
							@Override
							public void run() {
								SpyRejectCallback.assertCallOrder(handler1, handler2, handler3);
								handler.done();
							}
						}, 15);
						
						return null;
					}
				});
			}
		});
	}
}
