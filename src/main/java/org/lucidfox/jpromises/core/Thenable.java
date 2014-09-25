package org.lucidfox.jpromises.core;

/**
 * <p>
 * Abstract interface for "promise-like" objects containing a {@code then} method.
 * </p>
 * <p>
 * You will rarely implement this interface yourself. It is mostly provided for writing adapters to other
 * promise libraries. A {@link Thenable} can be wrapped into a {@link Promise} using
 * {@link PromiseFactory#resolve(Thenable)}.
 * </p>
 *
 * @param <V> the value type
 */
public interface Thenable<V> {
	/**
	 * <p>
	 * Specifies code to be run after the thenable is resolved (either to a value, or a rejection exception).
	 * </p><p>
	 * After the callbacks are registered using {@code then}, if the thenable's computation completes successfully,
	 * it must call {@code onResolve} with the computed value. If it fails, it must call {@code onReject} with
	 * the rejection reason (exception). Only one of these callbacks must be called, and only once.
	 * </p><p>
	 * If either callback is null, or returns a null promise, the thenable returned by the method must in turn
	 * be resolved or rejected, depending on whether this thenable was resolved or rejected. If a valid promise was
	 * returned, the thenable returned by this method must be chained after the returned promise.
	 * </p><p>
	 * Implementors are encouraged to override the return type with their implementation class and restrict the range
	 * of exceptions potentially thrown by this method.
	 * </p>
	 *
	 * @param <R> the type of the result promise
	 * @param onResolve the resolve callback (optional)
	 * @param onReject the reject callback (optional)
	 * @return (optionally) a {@link Thenable} that is chained after the current thenable
	 * @throws Exception Optionally, {@link Thenable} can throw exceptions, though its main implementation,
	 * 			{@link Promise}, does not. The {@link Promise} class treats thrown exceptions as rejection.
	 */
	<R> Thenable<R> then(final ResolveCallback<V, R> onResolve, final RejectCallback<R> onReject) throws Exception;
}
