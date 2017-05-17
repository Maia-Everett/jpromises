/*
 * Copyright 2014 Maia Everett <maia@lucidfox.org>
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
 * <p>
 * A helper interface used when initializing a promise with a {@link PromiseHandler}, to allow the promise handler
 * to resolve the promise to a specific value.
 * </p>
 * <p>
 * This interface is not intended to be implemented by library users.
 * </p>
 *
 * @param <V> the value type
 */
public interface Resolver<V> {
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
}
