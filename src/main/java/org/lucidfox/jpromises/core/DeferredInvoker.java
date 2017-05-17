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

import org.lucidfox.jpromises.PromiseFactory;

/**
 * <p>
 * Abstract interface specifying how promises constructed by a {@link PromiseFactory} defer processing of pending
 * {@code then} callbacks.
 * </p>
 * <p>
 * In practice, most systems with an event loop, including GUI systems, provide an "invoke later" method that allows
 * the application to post a task to be executed after all pending events in the event loop. For example, AWT/Swing
 * provides {@link java.awt.EventQueue#invokeLater EventQueue.invokeLater()} and GWT provides
 * {@link com.google.gwt.core.client.Scheduler#scheduleDeferred Scheduler.scheduleDeferred}. Adapters for AWT and GWT
 * are provided by the JPromises library. For other frameworks or toolkits, consult their documentation 
 * to find out how to bind them to the Promises system.
 * </p>
 */
public interface DeferredInvoker {
	/**
	 * Schedules the specified {@link Runnable} for running "later", in a way that guarantees that it will execute
	 * only after the stack unwinds to platform code - "platform code" generally meaning the low-level code running
	 * the application's main event loop.
	 * 
	 * The JPromises library does not impose any restrictions on the thread in which the task executes. In particular,
	 * it is not required to be either the same or different thread.
	 *
	 * @param task the task to invoke later
	 */
	void invokeDeferred(Runnable task);
}
