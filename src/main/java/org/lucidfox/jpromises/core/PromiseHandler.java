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

import org.lucidfox.jpromises.Promise;

/**
 * A handler that loads the initial value into the {@link Promise} using the {@link Resolver}, or fails using the
 * {@link Rejector} (but not both). It is passed to the promise at its creation time.
 *
 * @param <V> the value type
 */
public interface PromiseHandler<V> {
	/**
	 * <p>
	 * Evaluates the promise's value, either successfully (resolving the promise with its new value) or failing
	 * (rejecting the promise with an exception).
	 * </p><p>
	 * This method should <em>eventually</em> (synchronously or asynchronously) call either {@code resolve} or
	 * {@code reject}, but not both. Subsequent calls to any of these callbacks after one of them has been called
	 * will throw {@link IllegalStateException}.
	 * </p> 
	 *
	 * @param resolve the resolver, which sets the promise into a resolved state with a given value
	 * @param reject the rejector, which sets the promise into a rejected state with a given exception
	 * @throws Exception This method is allowed to throw exceptions, mostly for implementor convenience. Any exception
	 * 			thrown by {#link #handle} is called and passed to {@code reject}.
	 */
	void handle(Resolver<V> resolve) throws Exception;
}
