package org.lucidfox.jpromises.core;

public interface Thenable<V> {
	public <R> Thenable<R> then(final ResolveCallback<V, R> onResolve, final RejectCallback<R> onReject)
			throws Exception;
}
