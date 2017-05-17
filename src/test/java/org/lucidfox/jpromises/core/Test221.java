package org.lucidfox.jpromises.core;

import org.junit.Test;
import org.lucidfox.jpromises.PromiseFactory;
import org.lucidfox.jpromises.core.helpers.JsAnalogue;

/**
 * 2.2.1: Both {@code onFulfilled} and {@code onRejected} are optional arguments.
 * 
 * Note: Only {@code null}, the only non-functional-interface value permitted by the Java type system, is tested
 * for {@code onResolve} and {@code onReject}. 
 */
@JsAnalogue("2.2.1.js")
public class Test221 extends AbstractPromiseTestCase {
	private static class Dummy { }
	
	/**
	 * 2.2.1.1: If `onFulfilled` is not a function, it must be ignored.
	 * 
	 * Applied to a directly-rejected promise.
	 */
	@Test
	public void testOnResolveNull() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.reject(new Exception()).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.1.1: If `onFulfilled` is not a function, it must be ignored.
	 * 
	 * Applied to a promise rejected and then chained off of.
	 */
	@Test
	public void testOnRejectNullThenOnResolveNull() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.<Dummy>reject(new Exception()).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						return null;
					}
				}, null).then(null, new RejectCallback<Void>() {
					@Override
					public Thenable<Void> onReject(final Throwable exception) {
						handler.done();
						return null;
					}
				});
			}
		});
	}
	
	/**
	 * 2.2.1.2: If `onRejected` is not a function, it must be ignored.
	 * 
	 * Applied to a directly fulfilled promise.
	 */
	@Test
	public void testOnRejectNull() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.resolve(new Dummy()).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
	
	/**
	 * 2.2.1.2: If `onRejected` is not a function, it must be ignored.
	 * 
	 * Applied to a promise fulfilled and then chained off of.
	 */
	@Test
	public void testOnResolveNullThenOnRejectNull() {
		runTest(new PromiseTest() {
			@Override
			public void run(final PromiseFactory factory, final PromiseTestHandler handler) throws Exception {
				factory.resolve(new Dummy()).then(null, new RejectCallback<Dummy>() {
					@Override
					public Thenable<Dummy> onReject(final Throwable exception) {
						return null;
					}
				}).then(new ResolveCallback<Dummy, Void>() {
					@Override
					public Thenable<Void> onResolve(final Dummy value) {
						handler.done();
						return null;
					}
				}, null);
			}
		});
	}
}
