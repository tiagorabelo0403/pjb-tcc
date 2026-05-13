package com.tcc.pjb.backend.configs;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EquipeSwitchInterceptor equipeSwitchInterceptor;
    private final OfficeWorkspacePresenceInterceptor officeWorkspacePresenceInterceptor;
    private final CalculoJudicialApiMetricsInterceptor calculoJudicialApiMetricsInterceptor;

    public WebConfig(ObjectProvider<EquipeSwitchInterceptor> equipeSwitchInterceptor,
                     ObjectProvider<OfficeWorkspacePresenceInterceptor> officeWorkspacePresenceInterceptor,
                     ObjectProvider<CalculoJudicialApiMetricsInterceptor> calculoJudicialApiMetricsInterceptor) {
        this.equipeSwitchInterceptor = equipeSwitchInterceptor.getIfAvailable();
        this.officeWorkspacePresenceInterceptor = officeWorkspacePresenceInterceptor.getIfAvailable();
        this.calculoJudicialApiMetricsInterceptor = calculoJudicialApiMetricsInterceptor.getIfAvailable();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (equipeSwitchInterceptor != null) {
            registry.addInterceptor(equipeSwitchInterceptor)
                    .addPathPatterns("/api/v1/**");
        }
        if (officeWorkspacePresenceInterceptor != null) {
            registry.addInterceptor(officeWorkspacePresenceInterceptor)
                    .addPathPatterns("/api/v1/**");
        }
        if (calculoJudicialApiMetricsInterceptor != null) {
            registry.addInterceptor(calculoJudicialApiMetricsInterceptor)
                    .addPathPatterns("/api/v1/processual/calculos/**", "/api/v1/processual/trabalhista/verbas-rescisorias");
        }
    }
}
