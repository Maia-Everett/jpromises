package org.lucidfox.promises;

public interface Resolver<V> {
	void resolve(V value);
}
