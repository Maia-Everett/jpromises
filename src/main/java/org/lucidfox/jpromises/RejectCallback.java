package org.lucidfox.jpromises;

public interface RejectCallback<R> {
	Promise<R> onReject(Throwable exception);
}
