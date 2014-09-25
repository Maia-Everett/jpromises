package org.lucidfox.jpromises;

public interface PromiseHandler<V> {
	void handle(Resolver<V> resolve, Rejector reject);
}
