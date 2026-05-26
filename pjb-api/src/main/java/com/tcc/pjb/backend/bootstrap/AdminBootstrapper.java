package com.tcc.pjb.backend.bootstrap;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Inject;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@Component
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);
    private static final Set<String> PERFIS_SEGUROS_BOOTSTRAP = Set.of("dev", "local", "test");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final AuditLedgerService auditLedgerService;
    private final Function<String, String> envProvider;

    @Inject
    public AdminBootstrapper(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             Environment environment,
                             AuditLedgerService auditLedgerService) {
        this(usuarioRepository, passwordEncoder, environment, auditLedgerService, System::getenv);
    }

    AdminBootstrapper(UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder,
                      Environment environment,
                      AuditLedgerService auditLedgerService,
                      Function<String, String> envProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.auditLedgerService = auditLedgerService;
        this.envProvider = envProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapPermitido()) {
            return;
        }

        String email = envOrDefault("PJB_ADMIN_USERNAME", "admin@pjb.local").toLowerCase(Locale.ROOT);
        String cpf = envOrDefault("PJB_ADMIN_CPF", "00000000000").replaceAll("\\D", "");
        String rawPassword = envOrDefault("PJB_ADMIN_PASSWORD", "admin123");

        if (!credenciaisValidas(email, cpf, rawPassword)) {
            log.warn("Bootstrap administrativo ignorado por credenciais inválidas.");
            return;
        }

        Optional<Usuario> existing = usuarioRepository.findByCpf(cpf);
        if (existing.isEmpty()) {
            existing = usuarioRepository.findByEmail(email);
        }
        if (existing.isPresent()) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nome(resolveAdminName())
                .email(email)
                .cpf(cpf)
                .senha(passwordEncoder.encode(rawPassword))
                .ativo(true)
                .tipoUsuario(TipoUsuario.ADMINISTRADOR)
                .build();

        usuarioRepository.save(admin);
        log.warn("Bootstrap administrativo aplicado para o perfil atual.");
    }

    private boolean bootstrapPermitido() {
        String explicit = envProvider.apply("PJB_ADMIN_BOOTSTRAP_ENABLED");
        if (explicit != null && !explicit.isBlank() && Boolean.parseBoolean(explicit.trim())) {
            if (!isProfileSeguro()) {
                log.error("SECURITY: PJB_ADMIN_BOOTSTRAP_ENABLED=true detectado em perfil não seguro. Bootstrap BLOQUEADO.");
                auditLedgerService.appendSafely("ADMIN_BOOTSTRAP_BLOQUEADO_PROD",
                        "PJB_ADMIN_BOOTSTRAP_ENABLED ativo em perfil não seguro — bootstrap bloqueado.");
                return false;
            }
        }
        return isProfileSeguro();
    }

    private boolean isProfileSeguro() {
        for (String profile : environment.getActiveProfiles()) {
            if (PERFIS_SEGUROS_BOOTSTRAP.contains(profile.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean credenciaisValidas(String email, String cpf, String rawPassword) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return false;
        }
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        return rawPassword != null && rawPassword.length() >= 8;
    }

    private String resolveAdminName() {
        String nome = envProvider.apply("PJB_ADMIN_NAME");
        if (nome == null || nome.isBlank()) {
            return "Admin PJB";
        }
        return nome.trim();
    }

    private String envOrDefault(String key, String def) {
        String v = envProvider.apply(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return v.trim();
    }
}
