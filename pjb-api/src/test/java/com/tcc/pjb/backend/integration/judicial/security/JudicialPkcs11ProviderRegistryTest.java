package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Provider;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudicialPkcs11ProviderRegistryTest {

    @Test
    void shouldTrimOverflowFromLocalProviderCache() throws Exception {
        JudicialPkcs11ProviderRegistry registry = new JudicialPkcs11ProviderRegistry(new JudicialConnectorSecurityProperties(), value -> value);
        @SuppressWarnings("unchecked")
        Map<String, Provider> cache = (Map<String, Provider>) field(registry, "cache").get(registry);
        @SuppressWarnings("unchecked")
        Map<String, Instant> touch = (Map<String, Instant>) field(registry, "touch").get(registry);

        Instant now = Instant.now();
        for (int i = 0; i < 40; i++) {
            cache.put("module-" + i, new FakeProvider("P" + i));
            touch.put("module-" + i, now.minusSeconds(10_000L - i));
        }

        Method trimOverflow = JudicialPkcs11ProviderRegistry.class.getDeclaredMethod("trimOverflow");
        trimOverflow.setAccessible(true);
        trimOverflow.invoke(registry);

        assertThat(cache).hasSizeLessThanOrEqualTo(32);
        assertThat(touch).hasSizeLessThanOrEqualTo(32);
        assertThat(cache).doesNotContainKeys("module-0", "module-1", "module-2", "module-3", "module-4", "module-5", "module-6", "module-7");
    }

    @Test
    void shouldCleanupExpiredProviders() throws Exception {
        JudicialPkcs11ProviderRegistry registry = new JudicialPkcs11ProviderRegistry(new JudicialConnectorSecurityProperties(), value -> value);
        @SuppressWarnings("unchecked")
        Map<String, Provider> cache = (Map<String, Provider>) field(registry, "cache").get(registry);
        @SuppressWarnings("unchecked")
        Map<String, Instant> touch = (Map<String, Instant>) field(registry, "touch").get(registry);

        cache.put("old-module", new FakeProvider("OLD"));
        touch.put("old-module", Instant.now().minusSeconds(7200));
        cache.put("fresh-module", new FakeProvider("NEW"));
        touch.put("fresh-module", Instant.now());

        Method cleanup = JudicialPkcs11ProviderRegistry.class.getDeclaredMethod("cleanup");
        cleanup.setAccessible(true);
        cleanup.invoke(registry);

        assertThat(cache).doesNotContainKey("old-module");
        assertThat(touch).doesNotContainKey("old-module");
        assertThat(cache).containsKey("fresh-module");
    }

    private Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class FakeProvider extends Provider {
        private FakeProvider(String name) {
            super(name, 1.0, "fake");
        }
    }
}
