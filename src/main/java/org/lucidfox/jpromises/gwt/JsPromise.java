/*
 * Copyright 2016 Maia Everett <maia@lucidfox.org>
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

import org.lucidfox.jpromises.core.PromiseHandler;
import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.Rejector;
import org.lucidfox.jpromises.core.ResolveCallback;
import org.lucidfox.jpromises.core.Resolver;
import org.lucidfox.jpromises.core.Thenable;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * GWT JSNI type wrapping a native, ES6-compatible JavaScript promise. 
 * 
 * @param <V> value type
 */
public final class JsPromise<V> extends JavaScriptObject implements Thenable<V> {
	protected JsPromise() { }
	
	/**
	 * Creates a native promise resolved to the given value.
	 *
	 * @param <V> the value type
	 * @param value the value to resolve the promise with
	 * @return the new JS promise
	 */
	public static native <V> JsPromise<V> resolve(final V value) /*-{
		return $wnd.Promise.resolve(value);
	}-*/;
	
	/**
	 * Creates a native promise that defers its resolution to the given thenable.
	 *
	 * @param <V> the value type
	 * @param thenable the wrapped thenable
	 * @return the new JS promise
	 */
	public static <V> JsPromise<V> deferResolve(final Thenable<? extends V> thenable) {
		return create(new PromiseHandler<V>() {
			@Override
			public void handle(final Resolver<V> resolve, final Rejector reject) {
				resolve.deferResolve(thenable);
			}
		});
	};
	
	/**
	 * Creates a native promise rejected with the given exception.
	 *
	 * @param <V> the value type
	 * @param exception the exception to reject the promise with
	 * @return the new JS promise
	 */
	public static native <V> JsPromise<V> reject(final Throwable exception) /*-{
		return $wnd.Promise.reject(@JsPromise::toJsError(Ljava/lang/Throwable;)(exception));
	}-*/;
	
	/**
	 * Creates a native JS promise using the given {@link PromiseHandler}.
	 *
	 * @param <V> the value type
	 * @param handler the promise handler that can resolve or reject the promise inside it
	 * @return the new JS promise
	 */
	public static native <V> JsPromise<V> create(final PromiseHandler<V> handler) /*-{
		return new $wnd.Promise(function(resolve, reject) {
			@JsPromise::handle(Lorg/lucidfox/jpromises/core/PromiseHandler;
				Lcom/google/gwt/core/client/JavaScriptObject;
				Lcom/google/gwt/core/client/JavaScriptObject;)(handler, resolve, reject);
		});
	}-*/;
	
	private static <V> void handle(final PromiseHandler<V> handler, final JavaScriptObject resolve,
			final JavaScriptObject reject) {
		try {
			handler.handle(new Resolver<V>() {
				@Override
				public void resolve(final V value) {
					resolveValue(resolve, value);
				}
	
				@Override
				public void deferResolve(final Thenable<? extends V> thenable) {
					if (thenable instanceof JavaScriptObject) {
						@SuppressWarnings("unchecked")
						final JsPromise<V> jsPromise = (JsPromise<V>) (JavaScriptObject) thenable;
						resolveValue(resolve, jsPromise);
					} else {
						try {
							thenable.then(new ResolveCallback<V, V>() {
								@Override
								public Thenable<V> onResolve(final V value) {
									resolveValue(resolve, value);
									return null;
								}
							}, new RejectCallback<V>() {
								@Override
								public Thenable<V> onReject(final Throwable e) {
									rejectThrown(reject, e);
									return null;
								}
							});
						} catch (final Exception e) {
							rejectThrown(reject, e);
						}
					}
				}
			}, new Rejector() {
				@Override
				public void reject(final Throwable e) {
					rejectThrown(reject, e);
				};
			});
		} catch (final Exception e) {
			rejectThrown(reject, e);
		}
	}
	
	private static native void resolveValue(final JavaScriptObject resolve, final Object obj) /*-{
		resolve(obj);
	}-*/;
	
	private static native void rejectThrown(final JavaScriptObject reject, final Throwable exception) /*-{
		reject(@JsPromise::toJsError(Ljava/lang/Throwable;)(exception));
	}-*/;
	
	private static Object toJsError(final Throwable exception) {
		if (exception instanceof JavaScriptException) {
			// unwrap and return inner error
			return ((JavaScriptException) exception).getThrown();
		} else {
			return toJsError0(exception, exception.getMessage());
		}
	}
	
	private static native JavaScriptObject toJsError0(final Throwable exception, final String message) /*-{
		var error = new Error(message);
		error.__jsPromiseWrappedException = exception;
		return error;
	}-*/;
	
	/**
	 * Convenience method. Calls {@code then(onResolve, null)}.
	 *
	 * @param <R> the result type
	 * @param onResolve the resolve callback
	 * @return the composed JS promise fulfilled when both this promise and then the return result of the resolve
	 * callback are fulfilled
	 */
	public <R> JsPromise<R> then(final ResolveCallback<? super V, R> onResolve) {
		return then(onResolve, null);
	}

	@Override
	public native <R> JsPromise<R> then(final ResolveCallback<? super V, R> onResolve,
			final RejectCallback<R> onReject) /*-{
		return this.then(function(value) {
			if (!onResolve) {
				return null;
			}
		
			return onResolve.@ResolveCallback::onResolve(Ljava/lang/Object;)(value);
		}, function(err) {
			if (!onReject) {
				return null;
			}
		
			var exception = error.__jsPromiseWrappedException || @JsPromise::toException(Ljava/lang/Object;)(err);
			return onReject.@RejectCallback::onReject(Ljava/lang/Throwable;)(exception);
		});
	}-*/;
	
	private static Exception toException(final Object jsError) {
		return new JavaScriptException(jsError);
	}
}
