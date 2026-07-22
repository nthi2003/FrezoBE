package com.frezo.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Path-keyed permission check. Aspect lives in qtht; annotation is in common
 * so domain controllers can compile without depending on module-qtht.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckPermission {
    String api();
    String action();
}