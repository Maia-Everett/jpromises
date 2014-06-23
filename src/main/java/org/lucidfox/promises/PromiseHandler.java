package org.lucidfox.promises;

public interface PromiseHandler<V> {
	void handle(Resolver<V> resolve, Rejector reject);
}
