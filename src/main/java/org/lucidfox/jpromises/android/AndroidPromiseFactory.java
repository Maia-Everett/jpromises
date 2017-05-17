package org.lucidfox.jpromises.android;

import android.app.Activity;
import android.os.Handler;

import org.lucidfox.jpromises.annotation.GwtIncompatible;
import org.lucidfox.jpromises.core.DeferredInvoker;
import org.lucidfox.jpromises.core.PromiseFactory;

/**
 * Promise factory that eases Android integration by providing standard deferred invokers for an activity's UI thread
 * and handler threads.
 */
@GwtIncompatible("android")
public class AndroidPromiseFactory extends PromiseFactory {
	/**
	 * Instantiates a new {@code AndroidPromiseFactory} that uses the given activity's {@code runOnUiThread} method
	 * as the deferred invoker.
	 *
	 * @param activity the activity to user
	 */
	public AndroidPromiseFactory(final Activity activity) {
		super(new DeferredInvoker() {
			@Override
			public void invokeDeferred(final Runnable task) {
				activity.runOnUiThread(task);
			}
		});
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
	}

	@Override
	public String toString() {
		return "AndroidPromiseFactory";
	}
}
