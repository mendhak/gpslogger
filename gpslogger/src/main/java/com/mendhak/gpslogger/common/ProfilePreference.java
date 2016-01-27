package com.mendhak.gpslogger.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * This annotation indicates that it's pointing at a getter method for a preference and that
 * the preference should be saved to file when switching profiles.
 */
public @interface ProfilePreference {
    public String name();
}