package org.lucidfox.promises;

public interface RejectCallback<R> {
	Promise<R> onReject(Throwable exception);
}
