package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeTrustLevel;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceMembershipService {

    private final MembroEquipeRepository membroEquipeRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final EquipeOfficeDelegacaoRegraRepository regraRepository;
    private final UsuarioRepository usuarioRepository;
    private final OfficeTrustScoreService trustScoreService;

    public OfficeWorkspaceMembershipService(MembroEquipeRepository membroEquipeRepository,
                                            EquipeOfficePolicyRepository policyRepository,
                                            EquipeOfficeDelegacaoRegraRepository regraRepository,
                                            UsuarioRepository usuarioRepository,
                                            OfficeTrustScoreService trustScoreService) {
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.regraRepository = Objects.requireNonNull(regraRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.trustScoreService = Objects.requireNonNull(trustScoreService);
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeMembershipView> memberships(Long usuarioId) {
        List<PjbFrontendOfficeMembershipView> out = new ArrayList<>();
        for (MembroEquipe membro : membroEquipeRepository.carregarComEquipe(usuarioId)) {
            if (!membro.isAtivo()) {
                continue;
            }
            if (membro.getEquipe() == null || !membro.getEquipe().isAtivo()) {
                continue;
            }
            out.add(toMembership(membro));
        }
        out.sort(Comparator.comparing(PjbFrontendOfficeMembershipView::workspacePriority)
                .thenComparing(PjbFrontendOfficeMembershipView::equipeNome, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    private PjbFrontendOfficeMembershipView toMembership(MembroEquipe membro) {
        Long equipeId = membro.getEquipe() == null ? null : membro.getEquipe().getId();
        EquipeOfficePolicy policy = equipeId == null ? null : policyRepository.findByEquipeId(equipeId).orElse(null);
        EquipeOfficeDelegacaoRegra regra = equipeId == null || membro.getUsuario() == null || membro.getUsuario().getId() == null
                ? null
                : regraRepository.findByEquipeAndUser(equipeId, membro.getUsuario().getId()).orElse(null);
        Long seniorUserId = policy == null ? null : policy.getSignerUserId();
        Usuario senior = seniorUserId == null ? null : usuarioRepository.findById(seniorUserId).orElse(null);
        boolean blocked = policy != null && policy.isEnabled() && policy.isBloqueiaCausasProprias() && !isAdminRole(membro.getPapel());
        Set<RamoDireito> ramos = effectiveAllowedRamos(policy, regra);
        boolean canViewAllRamos = ramos == null || ramos.isEmpty();
        OfficeTrustScoreService.TrustScore trust = trustScoreService.avaliar(membro.getUsuario().getId(), equipeId);
        int minTrustRequired = regra != null && regra.getMinTrustAutoOverride() != null ? regra.getMinTrustAutoOverride() : policy == null ? 0 : policy.getMinTrustAuto();
        boolean patronCertificateRequired = policy != null
                && policy.isEnabled()
                && policy.isForcePatronoCertificate()
                && seniorUserId != null
                && !Objects.equals(seniorUserId, membro.getUsuario().getId());
        int workspacePriority = regra == null ? 100 : regra.getWorkspacePriority();
        boolean autoActivateWorkspace = regra != null && regra.isAutoActivateWorkspace();
        return new PjbFrontendOfficeMembershipView(
                equipeId,
                membro.getEquipe() == null ? null : membro.getEquipe().getNome(),
                membro.getPapel() == null ? null : membro.getPapel().name(),
                membro.getCargo(),
                seniorUserId,
                senior == null ? null : senior.getNome(),
                policy != null && policy.isEnabled(),
                blocked,
                membro.isAtivo(),
                autoActivateWorkspace,
                false,
                canViewAllRamos ? sortedRamos(EnumSet.allOf(RamoDireito.class)) : sortedRamos(ramos),
                canViewAllRamos,
                trust.score(),
                OfficeTrustLevel.fromScore(trust.score()).name(),
                minTrustRequired,
                patronCertificateRequired,
                workspacePriority);
    }

    private Set<RamoDireito> effectiveAllowedRamos(EquipeOfficePolicy policy, EquipeOfficeDelegacaoRegra regra) {
        if (regra != null && regra.getAllowedRamosOverride() != null && !regra.getAllowedRamosOverride().isEmpty()) {
            return regra.getAllowedRamosOverride();
        }
        if (policy != null && policy.getAllowedRamos() != null && !policy.getAllowedRamos().isEmpty()) {
            return policy.getAllowedRamos();
        }
        return EnumSet.noneOf(RamoDireito.class);
    }

    private List<String> sortedRamos(Set<RamoDireito> ramos) {
        if (ramos == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>(ramos.size());
        for (RamoDireito ramo : ramos) {
            out.add(ramo.name());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(out);
    }

    private boolean isAdminRole(PapelEquipe papel) {
        return papel == PapelEquipe.ADMINISTRADOR || papel == PapelEquipe.COORDENADOR;
    }
}
