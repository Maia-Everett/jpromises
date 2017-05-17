/*
 * Copyright 2017 Maia Everett <maia@lucidfox.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lucidfox.jpromises.core;

/**
 * Base interface for {@code Resolver} that can be used for an arbitrary thenable.
 *
 * @param <V> promise value type
 * @param <T> thenable type
 */
public interface BaseResolver<V, T extends Thenable<V>> {

	/**
	 * Resolves the promise to the given value.
	 *
	 * @param value the promise's new value
	 * @throws IllegalStateException if the promise is already resolved or rejected
	 */
	void resolve(V value);

	/**
	 * Defers resolution of the promise to the completion of the given {@link Thenable}.
	 *
	 * @param thenable the thenable that must be resolved first to resolve promise's new value
	 * @throws IllegalStateException if the promise is already resolved or rejected
	 */
	void deferResolve(Thenable<? extends V> thenable);

	/**
	 * Rejects the promise with the given rejection reason.
	 *
	 * @param exception the exception (rejection reason)
	 * @throws IllegalStateException if the promise is already resolved or rejected
	 */
	void reject(Throwable exception);

	/**
	 * Returns the promise on which this resolver's methods operate.
	 * 
	 * @apiNote Unlike JavaScript, the Java language does not allow referring to a promise variable while it is being
	 * assigned. In some cases, it is desirable to have access to the promise instance inside its resolution handler
	 * code, for example, for adding the promise to collections.
	 * 
	 * @return the promise on which this resolver's methods operate
	 */
	T getPromise();
}