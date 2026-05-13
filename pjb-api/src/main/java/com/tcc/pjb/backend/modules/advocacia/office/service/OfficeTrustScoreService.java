package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

@Service
public class OfficeTrustScoreService {

    public record TrustScore(int score, boolean frozen, boolean hasTrustedDevice, boolean baptized, boolean strongAuthRecent, boolean newcomer) {
    }

    private final MembroEquipeRepository membroEquipeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserSecurityProfileRepository userSecurityProfileRepository;

    public OfficeTrustScoreService(MembroEquipeRepository membroEquipeRepository,
                                  TrustedDeviceRepository trustedDeviceRepository,
                                  UserSecurityProfileRepository userSecurityProfileRepository) {
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.trustedDeviceRepository = Objects.requireNonNull(trustedDeviceRepository);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
    }

    @Transactional(readOnly = true)
    public TrustScore avaliar(Long userId, Long equipeId) {

        MembroEquipe m = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(userId, equipeId)
                .filter(MembroEquipe::isAtivo)
                .orElse(null);

        PapelEquipe papel = m != null ? m.getPapel() : null;
        int base = baseByPapel(papel);

        UserSecurityProfile profile = userSecurityProfileRepository.findByUserId(userId).orElse(null);
        boolean frozen = profile != null && profile.isFrozenNow();

        boolean baptized = profile != null && profile.getAdvBaptizedAt() != null;

        boolean strongAuthRecent = profile != null && profile.getLastStrongAuthAt() != null
                && Duration.between(profile.getLastStrongAuthAt(), LocalDateTime.now()).toHours() <= 24;

        boolean newcomer = m != null && m.getDataEntrada() != null
                && Duration.between(m.getDataEntrada(), LocalDateTime.now()).toDays() < 14;

        boolean hasTrustedDevice = hasTrustedDevice(userId);

        int score = base;
        if (hasTrustedDevice) score += 2;
        if (baptized) score += 1;
        if (strongAuthRecent) score += 1;
        if (newcomer) score -= 2;
        if (frozen) score = 0;

        if (score < 0) score = 0;
        if (score > 10) score = 10;

        return new TrustScore(score, frozen, hasTrustedDevice, baptized, strongAuthRecent, newcomer);
    }

    private boolean hasTrustedDevice(Long userId) {
        List<TrustedDevice> devices = trustedDeviceRepository.findActiveByUser(userId);
        LocalDateTime now = LocalDateTime.now();
        for (TrustedDevice d : devices) {
            if (d.getRevogadoEm() != null) continue;
            if (d.getVerifiedAt() == null) continue;
            if (!d.isAttestationTrusted()) continue;
            if (d.getQuarentenaAte() != null && d.getQuarentenaAte().isAfter(now)) continue;
            return true;
        }
        return false;
    }

    private static int baseByPapel(PapelEquipe papel) {
        if (papel == null) return 3;
        if (papel == PapelEquipe.ADMINISTRADOR) return 9;
        if (papel == PapelEquipe.COORDENADOR) return 9;
        if (papel == PapelEquipe.ADVOGADO_SENIOR) return 7;
        if (papel == PapelEquipe.ADVOGADO_JUNIOR) return 6;
        if (papel == PapelEquipe.ESTAGIARIO) return 2;
        if (papel == PapelEquipe.CONSULTOR) return 1;
        return 4;
    }
}
