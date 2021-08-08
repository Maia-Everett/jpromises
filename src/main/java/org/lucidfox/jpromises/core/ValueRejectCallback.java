/*
 * Copyright 2017 Maia Everett <maia@everett.one>
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
 * A callback to be invoked when a {@link Promise} or arbitrary {@link Thenable} is rejected.
 *
 * @param <R> the type of the returned value for chaining
 */
public interface ValueRejectCallback<R> {
	
	/**
	 * Called when the promise (thenable) is rejected.
	 *
	 * @param exception the exception with which the promise was rejected
	 * @return the value to pass to the promise returned by {@code thenApply}, and thus the next resolve callback
	 * in the {@code then} chain
	 * @throws Throwable Signals that an error occurred when handling the exception.
	 */
	R onReject(Throwable exception) throws Throwable;
}
