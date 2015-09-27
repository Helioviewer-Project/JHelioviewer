package org.helioviewer.jhv.base;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Documented
@Nonnull(when=When.ALWAYS)
@TypeQualifierDefault({/*ElementType.FIELD, ElementType.METHOD,*/ ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullByDefault
{
}
