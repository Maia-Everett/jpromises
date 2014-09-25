package org.lucidfox.jpromises.core;

public interface PromiseHandler<V> {
	void handle(Resolver<V> resolve, Rejector reject);
}
