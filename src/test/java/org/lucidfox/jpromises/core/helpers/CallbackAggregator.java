package org.lucidfox.jpromises.core.helpers;

import java.util.concurrent.atomic.AtomicInteger;

public final class CallbackAggregator {
	private final AtomicInteger soFar;
	private final Runnable onAllFinished;
	
	public CallbackAggregator(final int times, final Runnable onAllFinished) {
		soFar = new AtomicInteger(times);
		this.onAllFinished = onAllFinished;
	}
	
	public void oneDone() {
		if (soFar.decrementAndGet() == 0) {
			onAllFinished.run();
		}
	}
}
