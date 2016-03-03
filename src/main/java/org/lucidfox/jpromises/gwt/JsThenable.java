/**
 * Copyright 2014-2016 Maia Everett <maia@lucidfox.org>
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
package org.lucidfox.jpromises.gwt;

import org.lucidfox.jpromises.annotation.GwtCompatible;
import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Thenable;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * <p>
 * A wrapper for a native JavaScript thenable satisfying the Promises/A+ specification - for example, a
 * JQuery {@code Deferred} object or a {@code Promise} as implemented natively by modern browsers.
 * </p>
 * <p>
 * This class enables GWT code to mix Java promises with JavaScript thenables. To cast an arbitrary
 * {@code JavaScriptObject} to a {@code JsThenable}, use the {@link JavaScriptObject#cast} method:
 * </p>
 * <pre>
 * JsThenable&lt;ValueType&gt; thenable = javaScriptObject.cast();
 * </pre>
 *
 * @param <V> the value type
 */
@GwtCompatible
public class JsThenable<V> extends JavaScriptObject implements Thenable<V> {
	protected JsThenable() { }

	@Override
	public native <R> JsThenable<R> then(final ResolveCallback<? super V, R> onResolve,
			final RejectCallback<R> onReject) throws Exception /*-{
		return this.then(onResolve == null ? null : function(value) {
			onResolve.@org.lucidfox.jpromises.core.ResolveCallback::onResolve(Ljava/lang/Object;)(value);
		}, onReject == null ? null : function(exception) {
			onReject.@org.lucidfox.jpromises.core.RejectCallback::onReject(Ljava/lang/Throwable;)(exception);
		});
	}-*/;
}
