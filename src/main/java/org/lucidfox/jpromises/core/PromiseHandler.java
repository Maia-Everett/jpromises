package org.lucidfox.jpromises.core;

/**
 * A handler that loads the initial value into the {@link Promise} using the {@link Resolver}, or fails using the
 * {@link Rejector} (but not both). It is passed to the promise at its creation time.
 *
 * @param <V> the value type
 */
public interface PromiseHandler<V> {
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
	 * @param reject the rejector, which sets the promise into a rejected state with a given exception
	 * @throws Exception This method is allowed to throw exceptions, mostly for implementor convenience. Any exception
	 * 			thrown by {#link #handle} is called and passed to {@code reject}.
	 */
	void handle(Resolver<V> resolve, Rejector reject) throws Exception;
}
