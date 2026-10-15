package com.tcc.pjb.backend.core.quality.apisurface;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

final class ControllersDaAplicacao {

    private ControllersDaAplicacao() {
    }

    static List<Class<?>> concretos(JavaClasses classes) {
        return classes.stream()
                .filter(ControllersDaAplicacao::podeSerController)
                .<Class<?>>map(JavaClass::reflect)
                .filter(ControllersDaAplicacao::ehController)
                .toList();
    }

    static boolean podeSerController(JavaClass classe) {
        return Stream.of(Stream.of(classe), classe.getAllRawSuperclasses().stream(), classe.getAllRawInterfaces().stream())
                .flatMap(tipos -> tipos)
                .anyMatch(tipo -> tipo.isMetaAnnotatedWith(Controller.class));
    }

    static boolean ehController(Class<?> classe) {
        return !classe.isInterface()
                && !Modifier.isAbstract(classe.getModifiers())
                && AnnotatedElementUtils.hasAnnotation(classe, Controller.class);
    }

    static Set<Method> endpoints(Class<?> controller) {
        return MethodIntrospector.selectMethods(controller, (MethodIntrospector.MetadataLookup<Boolean>) metodo ->
                AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class)
                        || AnnotatedElementUtils.hasAnnotation(metodo, HttpExchange.class) ? Boolean.TRUE : null).keySet();
    }
}
