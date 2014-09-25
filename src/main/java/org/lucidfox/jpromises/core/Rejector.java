package org.lucidfox.jpromises.core;

public interface Rejector {
	void reject(Throwable exception);
}
