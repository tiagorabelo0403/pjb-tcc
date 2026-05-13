package com.tcc.pjb.backend.service.security.govbr;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrUserInfoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovBrAccountProfileSynchronizationService {

    private static final HexFormat HEX = HexFormat.of();

    private final UserSecurityProfileRepository profileRepository;
    private final IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService;

    public GovBrAccountProfileSynchronizationService(UserSecurityProfileRepository profileRepository,
                                                     IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService) {
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.identidadeJuridicaNacionalService = Objects.requireNonNull(identidadeJuridicaNacionalService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "govbr.account-profile.sync.persist", maxMillis = 2500, critical = true)
    public void sincronizar(Usuario usuario, String subjectCpf, GovBrUserInfoResponse info, Instant verifiedAt) {
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("usuario");
        }
        String normalizedSubject = normalizeCpf(subjectCpf);
        identidadeJuridicaNacionalService.sincronizarUsuario(usuario);

        UserSecurityProfile profile = profileRepository.findByUserId(usuario.getId()).orElseGet(() -> {
            UserSecurityProfile created = new UserSecurityProfile();
            created.setUsuario(usuario);
            return created;
        });
        profile.setGovVerifiedAt(LocalDateTime.ofInstant(verifiedAt == null ? Instant.now() : verifiedAt, ZoneId.systemDefault()));
        profile.setGovVerifiedSubHash(normalizedSubject != null ? sha256Hex(normalizedSubject) : null);
        if (info != null) {
            profile.setGovEmailHash(hashLower(info.email()));
            profile.setGovPhoneHash(hashDigits(info.phoneNumber()));
            profile.setGovEmailVerified(Boolean.TRUE.equals(info.emailVerified()));
            profile.setGovPhoneVerified(Boolean.TRUE.equals(info.phoneNumberVerified()));
        }
        profileRepository.save(profile);

        String sourceCpf = normalizeCpf(usuario.getCpf());
        if (sourceCpf != null && normalizedSubject != null) {
            identidadeJuridicaNacionalService.vincularGovBrPorDocumento(sourceCpf, normalizedSubject);
        }
    }

    private String hashLower(String value) {
        return sha256Hex(lower(value));
    }

    private String hashDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : sha256Hex(digits);
    }

    private String lower(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return digits.length() == 11 ? digits : null;
    }

    private String sha256Hex(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return HEX.formatHex(Hashes.sha256(value.getBytes(StandardCharsets.UTF_8)));
    }
}