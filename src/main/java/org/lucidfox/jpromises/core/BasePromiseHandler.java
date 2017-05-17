package org.lucidfox.jpromises.core;

/**
 * Base interface for {@code PromiseHandler} that can be used for an arbitrary thenable.
 *
 * @param <V> promise value type
 * @param <T> thenable type
 * @param <R> resolver type
 */
public interface BasePromiseHandler<V, T extends Thenable<V>, R extends BaseResolver<V, T>> {
	/**
	 * <p>
	 * Evaluates the promise's value, either successfully (resolving the promise with its new value) or failing
	 * (rejecting the promise with an exception).
	 * </p><p>
	 * This method should <em>eventually</em> (synchronously or asynchronously) call either {@code resolve} or
	 * {@code reject}, but not both. Subsequent calls to any of these callbacks after one of them has been called
	 * will throw {@link IllegalStateException}.
	 * </p> 
	 *
	 * @param resolve the resolver, which sets the promise into a resolved state with a given value
	 * @throws Exception This method is allowed to throw exceptions, mostly for implementor convenience. Any exception
	 * 			thrown by {#link #handle} is called and passed to {@code reject}.
	 */
	void handle(R resolve) throws Exception;
}