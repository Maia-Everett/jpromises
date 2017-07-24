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

import org.lucidfox.jpromises.core.RejectCallback;
import org.lucidfox.jpromises.core.ResolveCallback;
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
	public static native <V> JsPromise<V> resolve(V value) /*-{
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
		return create(new JsPromiseHandler<V>() {
			@Override
			public void handle(final JsResolver<V> resolve) {
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
	public static native <V> JsPromise<V> reject(Throwable exception) /*-{
		return $wnd.Promise.reject(@JsPromise::toJsError(Ljava/lang/Throwable;)(exception));
	}-*/;
	
	/**
	 * Creates a native JS promise using the given {@link JsPromiseHandler}.
	 *
	 * @param <V> the value type
	 * @param handler the promise handler that can resolve or reject the promise inside it
	 * @return the new JS promise
	 */
	public static native <V> JsPromise<V> create(JsPromiseHandler<V> handler) /*-{
		// Hack so we can access the promise instance
		var _resolve;
		var _reject;
	
		var promise = new $wnd.Promise(function(resolve, reject) {
			_resolve = resolve;
			_reject = reject;
		});
		
		@JsPromise::handle(Lorg/lucidfox/jpromises/gwt/JsPromiseHandler;
				Lcom/google/gwt/core/client/JavaScriptObject;
				Lcom/google/gwt/core/client/JavaScriptObject;
				Lorg/lucidfox/jpromises/gwt/JsPromise;)(handler, _resolve, _reject, promise);
		
		return promise;
	}-*/;
	
	private static <V> void handle(final JsPromiseHandler<V> handler, final JavaScriptObject resolve,
			final JavaScriptObject reject, final JsPromise<V> promise) {
		try {
			handler.handle(new JsResolver<V>() {
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

				@Override
				public void reject(final Throwable e) {
					rejectThrown(reject, e);
				}

				@Override
				public JsPromise<V> getPromise() {
					return promise;
				};
			});
		} catch (final Exception e) {
			rejectThrown(reject, e);
		}
	}
	
	private static native void resolveValue(JavaScriptObject resolve, Object obj) /*-{
		resolve(obj);
	}-*/;
	
	private static native void rejectThrown(JavaScriptObject reject, Throwable exception) /*-{
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
	
	private static native JavaScriptObject toJsError0(Throwable exception, String message) /*-{
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
	
	// We wrap then0 (which in turn wraps native then) for better handling of Java objects and exceptions
	@Override
	public <R> JsPromise<R> then(final ResolveCallback<? super V, ? extends R> onResolve,
								 final RejectCallback<? extends R> onReject) {
		return then0(onResolve == null ? null : new ResolveCallback<V, R>() {
			@Override
			public Thenable<R> onResolve(final V value) {
				try {
					return coerceToNativePromise(onResolve.onResolve(value));
				} catch (final Exception e) {
					// A Java exception propagating to a native then-handler would be bad. Coerce it to a native error.
					return JsPromise.reject(e);
				}
			}
		}, onReject == null ? null : new RejectCallback<R>() {
			@Override
			public Thenable<R> onReject(final Throwable exception) {
				try {
					return coerceToNativePromise(onReject.onReject(exception));
				} catch (final Error e) {
					throw e;
				} catch (final Throwable e) {
					// A Java exception propagating to a native then-handler would be bad. Coerce it to a native error.
					return JsPromise.reject(e);
				}
			}
		});
	}

	// Note that handling of onResolve and onReject is different in native code.
	// If onResolve is null, we resolve the resulting promise to null. This guarantees compatibility with JPromises
	// semantics and avoids propagation of original value (type V being interpreted as type R), at the cost of slight
	// incompatibility with JS promise semantics.
	// If onReject is null, we return null as the error handler, allowing exceptions to propagate to the next
	// reject handler in the then-chain.
	private native <R> JsPromise<R> then0(ResolveCallback<? super V, ? extends R> onResolve,
										  RejectCallback<R> onReject) /*-{
		return this.then(function(value) {
			if (!onResolve) {
				return null;
			}
		
			return onResolve.@ResolveCallback::onResolve(Ljava/lang/Object;)(value);
		}, !onReject ? null : function(err) {
			var exception = error.__jsPromiseWrappedException || @JsPromise::toException(Ljava/lang/Object;)(err);
			return onReject.@RejectCallback::onReject(Ljava/lang/Throwable;)(exception);
		});
	}-*/;
	
	private static <V> Thenable<V> coerceToNativePromise(final Thenable<? extends V> thenable) {
		if (thenable == null || thenable instanceof JavaScriptObject) {
			// Compatible with native promises, no need to wrap
			@SuppressWarnings("unchecked")
			final Thenable<V> result = (Thenable<V>) thenable;
			return result;
		} else {
			// Java object; need to wrap in native promise
			return deferResolve(thenable);
		}
	}
	
	private static Exception toException(final Object jsError) {
		return new JavaScriptException(jsError);
	}
}
