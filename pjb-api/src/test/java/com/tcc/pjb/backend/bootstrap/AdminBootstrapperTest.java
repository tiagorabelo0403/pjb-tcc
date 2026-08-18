package com.tcc.pjb.backend.bootstrap;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapperTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Environment environment;
    @Mock private AuditLedgerService auditLedgerService;
    @Mock private ApplicationArguments applicationArguments;

    private AdminBootstrapper bootstrapper(Function<String, String> envProvider) {
        return new AdminBootstrapper(usuarioRepository, passwordEncoder, environment, auditLedgerService, envProvider);
    }

    @Test
    void bootstrapComEnvVarTrue_perfilNaoSeguro_bloqueiaERegistraAuditoria() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminBootstrapper sut = bootstrapper(key -> "PJB_ADMIN_BOOTSTRAP_ENABLED".equals(key) ? "true" : null);
        sut.run(applicationArguments);

        // Falha com o bug atual: bootstrap procede (env var bypassa o guard de perfil)
        verify(usuarioRepository, never()).save(any());
        verify(auditLedgerService).appendSafely(eq("ADMIN_BOOTSTRAP_BLOQUEADO_PROD"), any(String.class));
    }

    @Test
    void bootstrapComEnvVarTrue_perfilSeguro_permiteBootstrap() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(usuarioRepository.findByCpf(any())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}$2a$10$teste");

        AdminBootstrapper sut = bootstrapper(key -> "PJB_ADMIN_BOOTSTRAP_ENABLED".equals(key) ? "true" : null);
        sut.run(applicationArguments);

        verify(usuarioRepository).save(any());
        verify(auditLedgerService, never()).appendSafely(eq("ADMIN_BOOTSTRAP_BLOQUEADO_PROD"), any(String.class));
    }

    @Test
    void bootstrapSemEnvVar_perfilNaoSeguro_bloqueiaBootstrap() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminBootstrapper sut = bootstrapper(key -> null);
        sut.run(applicationArguments);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bootstrapSemEnvVar_perfilSeguro_permiteBootstrap() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(usuarioRepository.findByCpf(any())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}$2a$10$teste");

        AdminBootstrapper sut = bootstrapper(key -> null);
        sut.run(applicationArguments);

        verify(usuarioRepository).save(any());
    }
}
