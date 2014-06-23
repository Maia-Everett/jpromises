package org.lucidfox.promises;

public interface ResolveCallback<V, R> {
	Promise<R> onResolve(V value);
}
