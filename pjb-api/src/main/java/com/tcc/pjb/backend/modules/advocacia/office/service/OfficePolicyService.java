package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.dto.DelegacaoRegraDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficePolicyDto;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;

@Service
public class OfficePolicyService {

    private final EquipeOfficePolicyRepository policyRepo;
    private final EquipeOfficeDelegacaoRegraRepository regraRepo;
    private final EquipeRepository equipeRepo;
    private final UsuarioRepository usuarioRepo;
    private final OfficeAuthorizationService authz;

    public OfficePolicyService(EquipeOfficePolicyRepository policyRepo,
                               EquipeOfficeDelegacaoRegraRepository regraRepo,
                               EquipeRepository equipeRepo,
                               UsuarioRepository usuarioRepo,
                               OfficeAuthorizationService authz) {
        this.policyRepo = Objects.requireNonNull(policyRepo);
        this.regraRepo = Objects.requireNonNull(regraRepo);
        this.equipeRepo = Objects.requireNonNull(equipeRepo);
        this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
        this.authz = Objects.requireNonNull(authz);
    }

    @Transactional
    public OfficePolicyDto getOrCreatePolicy(Long currentUserId, Long equipeId) {
        authz.requireActiveMembership(currentUserId, equipeId);
        EquipeOfficePolicy p = policyRepo.findByEquipeId(equipeId).orElseGet(() -> {
            Equipe e = equipeRepo.findById(equipeId).orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
            EquipeOfficePolicy np = new EquipeOfficePolicy();
            np.setEquipe(e);
            np.setEnabled(false);
            np.setSignerUserId(currentUserId);
            np.setBloqueiaCausasProprias(false);
            np.setForcePatronoCertificate(true);
            np.setMinTrustAuto(10);
            np.setMaxAutoPorDia(200);
            np.setAutoActions(defaultAutoActions());
            return policyRepo.save(np);
        });
        return toDto(p);
    }

    @Transactional
    public OfficePolicyDto updatePolicy(Long currentUserId, Long equipeId, OfficePolicyDto req) {
        authz.requireOfficeAdmin(currentUserId, equipeId);
        EquipeOfficePolicy p = policyRepo.findByEquipeId(equipeId).orElseGet(() -> {
            Equipe e = equipeRepo.findById(equipeId).orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
            EquipeOfficePolicy np = new EquipeOfficePolicy();
            np.setEquipe(e);
            np.setAutoActions(defaultAutoActions());
            return np;
        });

        if (req != null) {
            p.setEnabled(req.isEnabled());
            p.setBloqueiaCausasProprias(req.isBloqueiaCausasProprias());
            p.setForcePatronoCertificate(req.isForcePatronoCertificate());
            p.setMinTrustAuto(clampTrust(req.getMinTrustAuto()));
            p.setMaxAutoPorDia(clampMax(req.getMaxAutoPorDia()));

            Long signerUserId = req.getSignerUserId();
            if (signerUserId != null) {
                authz.requireActiveMembership(signerUserId, equipeId);
                Usuario s = usuarioRepo.findById(signerUserId).orElseThrow(() -> new EntityNotFoundException("Signatário não encontrado."));
                if (!s.isAdvogado()) {
                    throw new IllegalArgumentException("Signatário deve ser advogado.");
                }
                p.setSignerUserId(signerUserId);
            }

            Set<RamoDireito> allowedRamos = req.getAllowedRamos();
            if (allowedRamos != null) {
                p.setAllowedRamos(allowedRamos.isEmpty() ? EnumSet.noneOf(RamoDireito.class) : EnumSet.copyOf(allowedRamos));
            }

            Set<OfficeActionType> actions = req.getAutoActions();
            if (actions != null && !actions.isEmpty()) {
                p.setAutoActions(EnumSet.copyOf(actions));
            } else if (actions != null) {
                p.setAutoActions(EnumSet.noneOf(OfficeActionType.class));
            }
        }

        policyRepo.save(p);
        return toDto(p);
    }

    @Transactional
    public DelegacaoRegraDto upsertRegra(Long currentUserId, Long equipeId, Long usuarioId, DelegacaoRegraDto req) {
        authz.requireOfficeAdmin(currentUserId, equipeId);
        authz.requireActiveMembership(usuarioId, equipeId);

        EquipeOfficeDelegacaoRegra r = regraRepo.findByEquipeAndUser(equipeId, usuarioId).orElseGet(() -> {
            Equipe e = equipeRepo.findById(equipeId).orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
            Usuario u = usuarioRepo.findById(usuarioId).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
            EquipeOfficeDelegacaoRegra nr = new EquipeOfficeDelegacaoRegra();
            nr.setEquipe(e);
            nr.setUsuario(u);
            nr.setAtivo(true);
            nr.setBloqueiaPessoal(false);
            nr.setAutoActionsOverride(EnumSet.noneOf(OfficeActionType.class));
            return nr;
        });

        if (req != null) {
            r.setAtivo(req.isAtivo());
            r.setBloqueiaPessoal(req.isBloqueiaPessoal());
            r.setMinTrustAutoOverride(req.getMinTrustAutoOverride() == null ? null : clampTrust(req.getMinTrustAutoOverride()));
            r.setMaxAutoPorDiaOverride(req.getMaxAutoPorDiaOverride() == null ? null : clampMax(req.getMaxAutoPorDiaOverride()));
            Set<RamoDireito> allowedRamos = req.getAllowedRamosOverride();
            if (allowedRamos != null) {
                r.setAllowedRamosOverride(allowedRamos.isEmpty() ? EnumSet.noneOf(RamoDireito.class) : EnumSet.copyOf(allowedRamos));
            }
            Set<OfficeActionType> a = req.getAutoActionsOverride();
            if (a != null) {
                r.setAutoActionsOverride(a.isEmpty() ? EnumSet.noneOf(OfficeActionType.class) : EnumSet.copyOf(a));
            }
        }

        regraRepo.save(r);
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public DelegacaoRegraDto getRegra(Long currentUserId, Long equipeId, Long usuarioId) {
        authz.requireOfficeAdmin(currentUserId, equipeId);
        EquipeOfficeDelegacaoRegra r = regraRepo.findByEquipeAndUser(equipeId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Regra não encontrada."));
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public java.util.List<DelegacaoRegraDto> listarRegras(Long currentUserId, Long equipeId) {
        authz.requireOfficeAdmin(currentUserId, equipeId);
        return regraRepo.findByEquipe(equipeId).stream().map(this::toDto).toList();
    }

    private OfficePolicyDto toDto(EquipeOfficePolicy p) {
        return OfficePolicyDto.builder()
                .equipeId(p.getEquipe() != null ? p.getEquipe().getId() : null)
                .enabled(p.isEnabled())
                .signerUserId(p.getSignerUserId())
                .bloqueiaCausasProprias(p.isBloqueiaCausasProprias())
                .forcePatronoCertificate(p.isForcePatronoCertificate())
                .minTrustAuto(p.getMinTrustAuto())
                .maxAutoPorDia(p.getMaxAutoPorDia())
                .allowedRamos(p.getAllowedRamos())
                .autoActions(p.getAutoActions())
                .build();
    }

    private DelegacaoRegraDto toDto(EquipeOfficeDelegacaoRegra r) {
        return DelegacaoRegraDto.builder()
                .equipeId(r.getEquipe() != null ? r.getEquipe().getId() : null)
                .usuarioId(r.getUsuario() != null ? r.getUsuario().getId() : null)
                .ativo(r.isAtivo())
                .bloqueiaPessoal(r.isBloqueiaPessoal())
                .minTrustAutoOverride(r.getMinTrustAutoOverride())
                .maxAutoPorDiaOverride(r.getMaxAutoPorDiaOverride())
                .allowedRamosOverride(r.getAllowedRamosOverride())
                .autoActionsOverride(r.getAutoActionsOverride())
                .build();
    }

    private static int clampTrust(int v) {
        if (v < 0) return 0;
        if (v > 10) return 10;
        return v;
    }

    private static int clampMax(int v) {
        if (v < 0) return 0;
        if (v > 5000) return 5000;
        return v;
    }

    private static Set<OfficeActionType> defaultAutoActions() {
        return EnumSet.of(OfficeActionType.PROTOCOL_SUBMIT_PJE,
                OfficeActionType.PETICIONAR,
                OfficeActionType.RECORRER,
                OfficeActionType.JUNTAR_DOCUMENTO);
    }
}
