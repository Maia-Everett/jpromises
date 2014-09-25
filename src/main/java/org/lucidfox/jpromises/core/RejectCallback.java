package org.lucidfox.jpromises.core;

/**
 * A callback to be invoked when a {@link Promise} or arbitrary {@link Thenable} is rejected.
 *
 * @param <R> the value type of the returned promise for chaining
 */
public interface RejectCallback<R> {
	/**
	 * Called when the promise (thenable) is rejected.
	 *
	 * @param exception the exception with which the promise was rejected
	 * @return the promise to be chained after the current promise is rejected (optional)
	 */
	Promise<R> onReject(Throwable exception);
}
