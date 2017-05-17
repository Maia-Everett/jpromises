package org.lucidfox.jpromises.core.helpers;

import static org.junit.Assert.*;

import org.lucidfox.jpromises.PromiseFactory;
import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.Thenable;

public abstract class SpyRejectCallback<R> implements RejectCallback<R> {
	public static <R> SpyRejectCallback<R> nil() {
		return new SpyRejectCallback<R>(null) {
			@Override
			protected R onReject0(final Throwable unused) throws Exception {
				return null;
			}
		};
	}
	
	public static <R> SpyRejectCallback<R> returning(final PromiseFactory factory, final R value) {
		return new SpyRejectCallback<R>(factory) {
			@Override
			protected R onReject0(final Throwable unused) throws Exception {
				return value;
			}
		};
	}
	
	public static <R> SpyRejectCallback<R> throwing(final PromiseFactory factory, final Exception exception) {
		return new SpyRejectCallback<R>(factory) {
			@Override
			protected R onReject0(final Throwable unused) throws Exception {
				throw exception;
			}
		};
	}
	
	public static void assertCallOrder(final SpyRejectCallback<?>... callbacks) {
		assertTrue(callbacks.length > 1);
		
		long calledAt = callbacks[0].calledAt;
		
		for (final SpyRejectCallback<?> callback: callbacks) {
			// assert that the next callback was called after the previous one
			assertTrue(callback.calledAt - calledAt >= 0);
			calledAt = callback.calledAt;
		}
	}

	private final PromiseFactory factory;
	private boolean called;
	private long calledAt;
	private Throwable calledWith;
	
	SpyRejectCallback(final PromiseFactory factory) {
		this.factory = factory;
	}

	@Override
	public final Thenable<R> onReject(final Throwable exception) throws Exception {
		assertNotCalled();
		called = true;
		calledAt = System.nanoTime();
		calledWith = exception;
		
		final R result = onReject0(exception);
		return result == null ? null : factory.resolve(result);
	}
	
	public void assertNotCalled() {
		assertFalse(called);
	}
	
	public void assertCalledWith(final Throwable value) {
		assertEquals(value, calledWith);
	}

	protected abstract R onReject0(final Throwable exception) throws Exception;
}
