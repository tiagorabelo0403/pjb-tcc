package com.tcc.pjb.backend.core.modularity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE, ElementType.CONSTRUCTOR})
public @interface PjbInternal {

    PjbModuleId module();
}
