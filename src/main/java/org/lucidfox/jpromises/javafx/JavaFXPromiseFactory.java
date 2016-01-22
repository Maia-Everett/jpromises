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
package org.lucidfox.jpromises.javafx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.lucidfox.jpromises.annotation.GwtIncompatible;
import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

/**
 * <p>
 * A {@link PromiseFactory} specialized for JavaFX. This factory's {@link DeferredInvoker} executes tasks
 * on the JavaFX event queue, using {@code Platform.invokeLater}.
 * </p>
 * <p>
 * If JavaFX is not available (such as on early Java 7 builds, or Linux OpenJDK without OpenJFX), this class will fail
 * to instantiate at runtime.
 * </p>
 */
@GwtIncompatible("javafx.application.Platform")
public class JavaFXPromiseFactory extends PromiseFactory {
	/**
	 * Instantiates a new AWT promise factory.
	 * 
	 * @throws RuntimeException If JavaFX is not available on the system
	 */
	public JavaFXPromiseFactory() {
		super(new JavaFXDeferredInvoker());
	}
	
	private static class JavaFXDeferredInvoker implements DeferredInvoker {
		private final Method platformRunLater;
		
		private JavaFXDeferredInvoker() {
			try {
				platformRunLater = Class.forName("javafx.application.Platform").getMethod("runLater", Runnable.class);
			} catch (final NoSuchMethodException | SecurityException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void invokeDeferred(Runnable task) {
			try {
				platformRunLater.invoke(null, task);
			} catch (final IllegalAccessException | IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (final InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			}
		}
	}
}
