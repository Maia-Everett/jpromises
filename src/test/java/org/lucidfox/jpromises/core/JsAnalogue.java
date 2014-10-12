package org.lucidfox.jpromises.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation specifying the path to the broadly analogous JavaScript file in the
 * Promises/A+ test suite.
 * </p>
 * <p>
 * This annotation is purely informative.
 * <p>
 * @see <a href="https://github.com/promises-aplus/promises-tests">github.com/promises-aplus/promises-tests</a>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface JsAnalogue {
	/**
	 * The path to the analogous file in the Promises/A+ test suite.
	 */
	String value();
}
