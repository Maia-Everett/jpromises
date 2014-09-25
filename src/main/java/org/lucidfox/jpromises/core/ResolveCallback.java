package org.lucidfox.jpromises.core;

public interface ResolveCallback<V, R> {
	Promise<R> onResolve(V value);
}
