package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PjbInstituicaoRegistradaOnboardingListener {

    private final PjbDigitalCoreOnboardingProjectionService projectionService;

    public PjbInstituicaoRegistradaOnboardingListener(PjbDigitalCoreOnboardingProjectionService projectionService) {
        this.projectionService = Objects.requireNonNull(projectionService);
    }

    @EventListener
    public PjbDigitalCoreOnboardingProjection on(PjbInstituicaoRegistradaEvent event) {
        if (event == null) return null;
        return projectionService.project(event);
    }
}
