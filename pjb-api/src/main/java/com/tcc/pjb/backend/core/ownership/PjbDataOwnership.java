package com.tcc.pjb.backend.core.ownership;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface PjbDataOwnership {

    PjbModuleId module();

    PjbOwnershipMode mode() default PjbOwnershipMode.OWNER_ONLY;

    boolean publishedReadModel() default false;
}
