package org.lucidfox.jpromises.core;

public interface DeferredInvoker {
	void invokeDeferred(Runnable task);
}
