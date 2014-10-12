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
