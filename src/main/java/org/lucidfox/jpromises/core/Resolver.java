package org.lucidfox.jpromises.core;

public interface Resolver<V> {
	void resolve(V value);
}
