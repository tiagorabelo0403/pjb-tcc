package com.tcc.pjb.backend.core.guard;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Trava de boot: no perfil PROD, ICP-Brasil e HSM não podem subir desligados. Os defaults de
 * desenvolvimento continuam {@code false} (ver application-security.yml) — só o perfil prod exige
 * explicitamente {@code true}, como falha de inicialização, não warning.
 *
 * <p>Assinatura com validade jurídica (ICP-Brasil) é requisito legal, não recurso opcional; HSM é a
 * guarda de chave que sustenta essa assinatura. Antes desta trava, nada impedia subir produção sem
 * nenhuma das duas.</p>
 */
public final class ProductionCriticalControlValidator implements EnvironmentPostProcessor {

    public static final String ICP_ENABLED_PROPERTY = "pjb.icp.enabled";
    public static final String HSM_ENABLED_PROPERTY = "pjb.hsm.enabled";

    private static final Logger log = LoggerFactory.getLogger(ProductionCriticalControlValidator.class);
    private static final Marker PRODUCTION_CONTROL_VIOLATION = MarkerFactory.getMarker("PRODUCTION_CONTROL_VIOLATION");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.stream()
                .anyMatch(p -> MockGuardProfile.fromProfileName(p) == MockGuardProfile.PROD);

        if (!isProd) {
            return;
        }

        requireEnabled(environment, ICP_ENABLED_PROPERTY, "ICP-Brasil");
        requireEnabled(environment, HSM_ENABLED_PROPERTY, "HSM");
    }

    private static void requireEnabled(ConfigurableEnvironment env, String property, String controlName) {
        String value = env.getProperty(property);
        if (!"true".equalsIgnoreCase(value)) {
            log.error(PRODUCTION_CONTROL_VIOLATION,
                    "[PRODUCTION-CONTROL-GUARD] {} desligado em PROD. Property: {}", controlName, property);
            throw new ProductionCriticalControlViolationException(property, controlName);
        }
    }
}
