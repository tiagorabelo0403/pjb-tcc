package com.tcc.pjb.backend.platform.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PjbTransactionalBudget {

    String operation() default "";

    long maxMillis() default -1L;

    boolean critical() default false;
}
