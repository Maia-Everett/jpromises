package org.lucidfox.jpromises;

final class MultiPromises {
	private MultiPromises() { }
	
	public static <V> Promise<V> all(final Iterable<Promise<? extends V>> promises) {
		throw new UnsupportedOperationException();
	}
	
	public static <V> Promise<V> race(final Iterable<Promise<? extends V>> promises) {
		throw new UnsupportedOperationException();
	}
}
