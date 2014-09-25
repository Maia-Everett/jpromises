package org.lucidfox.jpromises.gwt;

import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * A {@link PromiseFactory} specialized for GWT. It uses the GWT/JavaScript event queue for deferred invocation,
 * using {@link Scheduler#scheduleDeferred}.
 */
public class GwtPromiseFactory extends PromiseFactory {
	/**
	 * Instantiates a new GWT promise factory.
	 */
	public GwtPromiseFactory() {
		super(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						task.run();
					}
				});
			}
		});
	}
}
