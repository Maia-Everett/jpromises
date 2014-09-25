package org.lucidfox.jpromises;

public interface ResolveCallback<V, R> {
	Promise<R> onResolve(V value);
}
