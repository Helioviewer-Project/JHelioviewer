package org.helioviewer.jhv.base;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierDefault;

@Documented
@TypeQualifierDefault({/*ElementType.FIELD, ElementType.METHOD,*/ ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullByDefault
{
}
