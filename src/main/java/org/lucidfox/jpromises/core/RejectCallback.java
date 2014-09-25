package org.lucidfox.jpromises.core;

public interface RejectCallback<R> {
	Promise<R> onReject(Throwable exception);
}
