package org.lucidfox.jpromises.core;

/**
 * A helper interface used when initializing a promise with a {@link PromiseHandler}, to allow the promise handler
 * to resolve the promise to a specific value.
 *
 * @param <V> the value type
 */
public interface Resolver<V> {
	/**
	 * Resolves the promise to the given value.
	 *
	 * @param value the promise's new value
	 * @throws IllegalStateException if the promise is already resolved or rejected
	 */
	void resolve(V value);
}
