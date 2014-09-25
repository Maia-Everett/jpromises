package org.lucidfox.jpromises.gwt;

import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class GwtPromiseFactory extends PromiseFactory {
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
