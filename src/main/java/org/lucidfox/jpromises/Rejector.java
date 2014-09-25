package org.lucidfox.jpromises;

public interface Rejector {
	void reject(Throwable exception);
}
