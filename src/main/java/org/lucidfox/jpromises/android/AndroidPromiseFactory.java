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
package org.lucidfox.jpromises.android;

import android.os.Handler;
import android.os.Looper;

import org.lucidfox.jpromises.PromiseFactory;
import org.lucidfox.jpromises.annotation.GwtIncompatible;
import org.lucidfox.jpromises.core.DeferredInvoker;

/**
 * Promise factory that eases Android integration by providing standard deferred invokers for the main thread
 * and handler threads.
 */
@GwtIncompatible("android")
public class AndroidPromiseFactory extends PromiseFactory {
	private final Thread thread;
	
	/**
	 * Instantiates a new {@code AndroidPromiseFactory} that runs tasks deferred on the main thread.
	 */
	public AndroidPromiseFactory() {
		this(new Handler(Looper.getMainLooper()));
	}

	/**
	 * Instantiates a new {@code AndroidPromiseFactory} that uses the given handler's {@code post} method
	 * as the deferred invoker.
	 *
	 * @param handler the handler to use
	 */
	public AndroidPromiseFactory(final Handler handler) {
		super(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				handler.post(task);
			}
		});

		thread = handler.getLooper().getThread();
	}

	/**
	 * Returns the string {@code "AndroidPromiseFactory (thread = %THREAD%)"}, where {@code %THREAD%} is the result
	 * of calling {@link Thread#toString()} on the thread to which this promise factory is bound
	 * (usually the main thread, but possibly also a message loop thread associated with a custom {@link Handler}).
	 */
	@Override
	public String toString() {
		return "AndroidPromiseFactory (thread = " + thread + ")";
	}
}
