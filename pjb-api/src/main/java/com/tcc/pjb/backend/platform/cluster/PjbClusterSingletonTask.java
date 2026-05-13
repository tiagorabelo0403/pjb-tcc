package com.tcc.pjb.backend.platform.cluster;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PjbClusterSingletonTask {

    String key() default "";

    String ttl() default "";
}
