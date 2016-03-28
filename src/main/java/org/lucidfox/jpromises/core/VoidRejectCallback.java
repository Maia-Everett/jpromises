package org.lucidfox.jpromises.core;

/**
 * Convenience callback type that works like {@link RejectCallback}, but returns no value.
 */
public interface VoidRejectCallback {
	
	/**
	 * Called when the promise (thenable) is rejected.
	 *
	 * @param exception the exception with which the promise was rejected
	 * @throws Exception Signals that an error occurred when handling the exception.
	 */
	void onReject(Throwable exception) throws Exception;
}
