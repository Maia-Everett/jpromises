package org.lucidfox.jpromises.core;

/**
 * Convenience callback type that works like {@link ResolveCallback}, but returns no value.
 *
 * @param <V> the value type of the promise the callback is being bound to via
 * {@link Promise#then(VoidResolveCallback,VoidRejectCallback)}
 */
public interface VoidResolveCallback<V> {
	
	/**
	 * Called when the promise (thenable) is resolved.
	 *
	 * @param value the value with which the promise is resolved
	 * @throws Exception Signals that an error occurred when handling the result of the promise execution.
	 */
	void onResolve(V value) throws Exception;
}
