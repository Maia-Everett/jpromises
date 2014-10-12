package org.lucidfox.jpromises.core.helpers;

import static org.junit.Assert.*;

import org.lucidfox.jpromises.core.PromiseFactory;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Thenable;

public abstract class SpyResolveCallback<V, R> implements ResolveCallback<V, R> {
	public static <V, R> SpyResolveCallback<V, R> nil() {
		return new SpyResolveCallback<V, R>(null) {
			@Override
			protected R onResolve0(final V unused) throws Exception {
				return null;
			}
		};
	}
	
	public static <V, R> SpyResolveCallback<V, R> returning(final PromiseFactory factory, final R value) {
		return new SpyResolveCallback<V, R>(factory) {
			@Override
			protected R onResolve0(final V unused) throws Exception {
				return value;
			}
		};
	}
	
	public static <V, R> SpyResolveCallback<V, R> throwing(final PromiseFactory factory, final Exception exception) {
		return new SpyResolveCallback<V, R>(factory) {
			@Override
			protected R onResolve0(final V unused) throws Exception {
				throw exception;
			}
		};
	}
	
	public static void assertCallOrder(final SpyResolveCallback<?, ?>... callbacks) {
		assertTrue(callbacks.length > 1);
		
		long calledAt = callbacks[0].calledAt;
		
		for (int i = 1; i < callbacks.length; i++) {
			// assert that the next callback was called after the previous one
			assertTrue(callbacks[i].calledAt - calledAt > 0);
			calledAt = callbacks[i].calledAt;
		}
	}

	private final PromiseFactory factory;
	private boolean called;
	private long calledAt;
	private V calledWith;
	
	SpyResolveCallback(final PromiseFactory factory) {
		this.factory = factory;
	}

	@Override
	public final Thenable<R> onResolve(final V value) throws Exception {
		assertNotCalled();
		called = true;
		calledAt = System.nanoTime();
		calledWith = value;
		
		final R result = onResolve0(value);
		return result == null ? null : factory.resolve(result);
	}
	
	public void assertNotCalled() {
		assertFalse(called);
	}
	
	public void assertCalledWith(final V value) {
		assertEquals(value, calledWith);
	}

	protected abstract R onResolve0(final V value) throws Exception;
}
