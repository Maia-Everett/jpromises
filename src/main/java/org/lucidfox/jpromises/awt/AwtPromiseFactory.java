/**
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
package org.lucidfox.jpromises.awt;

import java.awt.EventQueue;

import org.lucidfox.jpromises.annotation.GwtIncompatible;
import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

/**
 * A {@link PromiseFactory} specialized for AWT and Swing. This factory's {@link DeferredInvoker} executes tasks
 * on the AWT event queue in the event dispatch thread, using {@link EventQueue#invokeLater}.
 */
@GwtIncompatible("java.awt.EventQueue")
public class AwtPromiseFactory extends PromiseFactory {
	/**
	 * Instantiates a new AWT promise factory.
	 */
	public AwtPromiseFactory() {
		super(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				EventQueue.invokeLater(task);
			}
		});
	}
}
