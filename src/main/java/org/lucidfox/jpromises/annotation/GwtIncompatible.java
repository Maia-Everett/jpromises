package org.lucidfox.jpromises.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation marking parts of the library (classes and methods) not supported under GWT. The GWT compiler ignores
 * classes, methods and fields annotated with a {@code @GwtIncompatible} annotation, regardless of the annotation's
 * package.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
public @interface GwtIncompatible {
	/**
	 * An attribute that can be used to explain why the code is incompatible.
	 */
	String value();
}
