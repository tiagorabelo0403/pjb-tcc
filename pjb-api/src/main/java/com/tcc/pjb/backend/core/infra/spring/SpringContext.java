package com.tcc.pjb.backend.core.infra.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy(false)
public class SpringContext implements ApplicationContextAware {

    private static volatile ApplicationContext CTX;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        CTX = applicationContext;
    }

    public static <T> T getBean(Class<T> type) {
        ApplicationContext c = CTX;
        if (c == null) {
            throw new IllegalStateException("ApplicationContext ainda não inicializado");
        }
        return c.getBean(type);
    }
}
