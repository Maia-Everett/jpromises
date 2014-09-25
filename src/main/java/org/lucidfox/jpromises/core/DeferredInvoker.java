package org.lucidfox.jpromises.core;

/**
 * <p>
 * Abstract interface specifying how promises constructed by a {@link PromiseFactory} defer processing of pending
 * {@link Promise#then} callbacks.
 * </p>
 * <p>
 * In practice, most systems with an event loop, including GUI systems, provide an "invoke later" method that allows
 * the application to post a task to be executed after all pending events in the event loop. For example, AWT/Swing
 * provides {@link java.awt.EventQueue#invokeLater() EventQueue.invokeLater()} and GWT provides
 * {@link com.google.gwt.core.client.Scheduler#scheduleDeferred() Scheduler.scheduleDeferred}. Adapters for AWT and GWT
 * are provided by the JPromises library. For other frameworks or toolkits, consult their documentation 
 * to find out how to bind them to the Promises system.
 * </p>
 */
public interface DeferredInvoker {
	/**
	 * Schedules the specified {@link Runnable} for running "later", in a way that guarantees that it will execute
	 * only after the stack unwinds to platform code - "platform code" generally meaning the low-level code running
	 * the application's main event loop.
	 * 
	 * The JPromises library does not impose any restrictions on the thread in which the task executes. In particular,
	 * it is not required to be either the same or different thread.
	 *
	 * @param task the task to invoke later
	 */
	void invokeDeferred(Runnable task);
}
