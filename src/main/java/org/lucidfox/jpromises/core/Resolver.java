/*
 * Copyright 2014-2017 Maia Everett <maia@lucidfox.org>
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

import org.lucidfox.jpromises.Promise;

/**
 * <p>
 * A helper interface used when initializing a promise with a {@link PromiseHandler}, to allow the promise handler
 * to resolve the promise to a specific value or reject it with a specific exception.
 * </p>
 * <p>
 * This interface is not intended to be implemented by library users.
 * </p>
 *
 * @param <V> the value type
 */
public interface Resolver<V> extends BaseResolver<V, Promise<V>> {
	@Override
	void resolve(V value);
	
	@Override
	void deferResolve(Thenable<? extends V> thenable);
	
	@Override
	void reject(Throwable exception);
	
	@Override
	Promise<V> getPromise();
}
