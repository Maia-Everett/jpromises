package org.lucidfox.jpromises.core;

/**
 * A callback to be invoked when a {@link Promise} or arbitrary {@link Thenable} is resolved.
 *
 * @param <V> the value type of the promise the callback is being bound to via {@link Thenable#then}
 * @param <R> the value type of the returned promise for chaining
 */
public interface ResolveCallback<V, R> {
	/**
	 * Called when the promise (thenable) is resolved.
	 *
	 * @param value the value with which the promise is resolved
	 * @return the promise (thenable) to be chained after the current promise is resolved (optional)
	 */
	Thenable<R> onResolve(V value);
}
