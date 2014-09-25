package org.lucidfox.jpromises;

public interface Resolver<V> {
	void resolve(V value);
}
