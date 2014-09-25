package org.lucidfox.jpromises.awt;

import java.awt.EventQueue;

import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

public class AwtPromiseFactory extends PromiseFactory {
	public AwtPromiseFactory() {
		super(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				EventQueue.invokeLater(task);
			}
		});
	}
}
