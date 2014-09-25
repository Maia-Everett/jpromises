package org.lucidfox.jpromises.core;

/**
 * A helper interface used when initializing a promise with a {@link PromiseHandler}, to allow the promise handler
 * to reject the promise with an exception, indicating that an error occurred while evaluating the promise's value.
 */
public interface Rejector {
	/**
	 * Rejects the promise with the given rejection reason.
	 *
	 * @param exception the exception (rejection reason)
	 * @throws IllegalStateException if the promise is already resolved or rejected
	 */
	void reject(Throwable exception);
}
