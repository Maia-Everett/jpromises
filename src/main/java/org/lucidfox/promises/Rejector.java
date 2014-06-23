package org.lucidfox.promises;

public interface Rejector {
	void reject(Throwable exception);
}
