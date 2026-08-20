package com.tcc.pjb.backend.configs.security;

import com.tcc.pjb.backend.ai.financeira.security.FinanceiraCapabilityCatalogProperties;
import com.tcc.pjb.backend.ai.juridica.security.JuridicaCapabilityCatalogProperties;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceProperties;
import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.configs.security.hardening.PjbFunctionalAvailabilityProperties;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyProperties;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitProperties;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationProfilesBindingTest {

    @Test
    void shouldBindBaseSecurityConfigurationFromApplicationYaml() throws Exception {
        ConfigurableEnvironment env = loadEnvironment("application.yml", "application-security.yml", "application-api-governance.yml");

        assertEquals("v1", env.getProperty("pjb.security.policy.version"));
        assertEquals("true", env.getProperty("pjb.security.perimeter.enabled"));
        assertNull(env.getProperty("pjb.crypto.perimeter.enabled"));

        PolicyProperties policy = bind(env, "pjb.security.policy", PolicyProperties.class);
        SecurityPerimeterProperties perimeter = bind(env, "pjb.security.perimeter", SecurityPerimeterProperties.class);
        ApiRequestOriginGovernanceProperties originGovernance = bind(env, "pjb.security.perimeter.origin-governance", ApiRequestOriginGovernanceProperties.class);
        CapabilityRateLimitProperties capabilityRateLimit = bind(env, "pjb.security.capability-ratelimit", CapabilityRateLimitProperties.class);
        FinanceiraCapabilityCatalogProperties financeira = bind(env, "pjb.security.financeira.capability-catalog", FinanceiraCapabilityCatalogProperties.class);
        JuridicaCapabilityCatalogProperties juridica = bind(env, "pjb.security.juridica.capability-catalog", JuridicaCapabilityCatalogProperties.class);
        PjbFunctionalAvailabilityProperties functionalAvailability = bind(env, "pjb.api.functional-availability", PjbFunctionalAvailabilityProperties.class);

        assertEquals("v1", policy.getVersion());
        assertEquals("classpath:policies/security/pjb-access-policy.json", policy.getDescriptorResource());
        assertTrue(perimeter.isEnabled());
        assertFalse(perimeter.isTrustProxyHeaders());
        assertEquals("memory", perimeter.getBlocklist().getStore());
        assertFalse(originGovernance.isEnabled());
        assertFalse(perimeter.getCorsAllowedOrigins().contains("*"));
        assertEquals("memory", perimeter.getRatelimit().getStore());
        assertEquals(3, perimeter.getRatelimit().getRules().size());
        assertTrue(perimeter.getRatelimit().getRules().stream().anyMatch(rule -> "admin-api".equals(rule.getName())));
        assertTrue(perimeter.getRatelimit().getRules().stream().anyMatch(rule -> "auth-credential-endpoints".equals(rule.getName())));
        assertEquals("redis", capabilityRateLimit.getStore());
        assertEquals(60, capabilityRateLimit.getWindowSeconds());
        assertEquals(60, capabilityRateLimit.getDefaultLimitTokens());
        assertTrue(financeira.isDenyUnknown());
        assertTrue(juridica.isDenyUnknown());
        assertTrue(functionalAvailability.isEnabled());
        assertTrue(functionalAvailability.isAvailable(com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain.PETICIONAMENTO));
        assertTrue(functionalAvailability.isAvailable(com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain.CONSULTA));
    }

    @Test
    void shouldUseRedisBackedPerimeterStoresOnK8sProfile() throws Exception {
        ConfigurableEnvironment env = loadEnvironment("application.yml", "application-k8s.yml");

        SecurityPerimeterProperties perimeter = bind(env, "pjb.security.perimeter", SecurityPerimeterProperties.class);
        ApiRequestOriginGovernanceProperties originGovernance = bind(env, "pjb.security.perimeter.origin-governance", ApiRequestOriginGovernanceProperties.class);
        CapabilityRateLimitProperties capabilityRateLimit = bind(env, "pjb.security.capability-ratelimit", CapabilityRateLimitProperties.class);

        assertTrue(perimeter.isTrustProxyHeaders());
        assertEquals("redis", perimeter.getBlocklist().getStore());
        assertTrue(originGovernance.isEnabled());
        assertEquals("redis", perimeter.getRatelimit().getStore());
        assertEquals("redis", capabilityRateLimit.getStore());
        assertFalse(perimeter.getProxy().getTrustedCidrs().isEmpty());
    }

    @Test
    void shouldKeepSecurityPolicyAndRedisStoresAlignedAcrossProdAndDocker() throws Exception {
        for (String profileFile : List.of("application-prod.yml", "application-docker.yml")) {
            ConfigurableEnvironment env = loadEnvironment("application.yml", "application-api-governance.yml", profileFile);
            PolicyProperties policy = bind(env, "pjb.security.policy", PolicyProperties.class);
            SecurityPerimeterProperties perimeter = bind(env, "pjb.security.perimeter", SecurityPerimeterProperties.class);
            ApiRequestOriginGovernanceProperties originGovernance = bind(env, "pjb.security.perimeter.origin-governance", ApiRequestOriginGovernanceProperties.class);
            CapabilityRateLimitProperties capabilityRateLimit = bind(env, "pjb.security.capability-ratelimit", CapabilityRateLimitProperties.class);
            PjbFunctionalAvailabilityProperties functionalAvailability = bind(env, "pjb.api.functional-availability", PjbFunctionalAvailabilityProperties.class);

            assertEquals("v1", policy.getVersion(), profileFile);
            assertEquals("classpath:policies/security/pjb-access-policy.json", policy.getDescriptorResource(), profileFile);
            assertEquals("redis", perimeter.getBlocklist().getStore(), profileFile);
            assertTrue(originGovernance.isEnabled(), profileFile);
            assertEquals("redis", perimeter.getRatelimit().getStore(), profileFile);
            assertEquals("redis", capabilityRateLimit.getStore(), profileFile);
            assertTrue(functionalAvailability.isEnabled(), profileFile);
        }
    }


    @Test
    void shouldEnableOriginGovernanceForFrontendDevWithTrustedLocalOrigins() throws Exception {
        ConfigurableEnvironment env = loadEnvironment("application.yml", "application-frontend-dev.yml");

        ApiRequestOriginGovernanceProperties originGovernance = bind(env, "pjb.security.perimeter.origin-governance", ApiRequestOriginGovernanceProperties.class);
        SecurityPerimeterProperties perimeter = bind(env, "pjb.security.perimeter", SecurityPerimeterProperties.class);

        assertTrue(originGovernance.isEnabled());
        assertEquals(4, originGovernance.getTrustedBrowserOrigins().size());
        assertTrue(perimeter.getCorsAllowedOrigins().contains("http://localhost:3000"));
    }

    private static ConfigurableEnvironment loadEnvironment(String... resourceNames) throws IOException {
        ConfigurableEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        int order = 0;
        for (String resourceName : resourceNames) {
            Resource resource = resourceLoader.getResource("classpath:" + resourceName);
            assertTrue(resource.exists(), () -> "Resource not found: " + resourceName);
            List<PropertySource<?>> sources = loader.load(resourceName + "-" + order, resource);
            for (PropertySource<?> source : sources) {
                environment.getPropertySources().addFirst(source);
            }
            order++;
        }
        ConfigurationPropertySources.attach(environment);
        return environment;
    }

    private static <T> T bind(ConfigurableEnvironment environment, String prefix, Class<T> type) {
        return Binder.get(environment)
                .bind(prefix, Bindable.of(type))
                .orElseThrow(() -> new IllegalStateException("Missing bindable prefix: " + prefix));
    }
}
