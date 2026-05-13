package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;

@Service
public class OfficePersonalScopeService {

    public record ScopeDecision(boolean personalBlocked, Long resolvedEquipeId, boolean requireEquipeHeader, List<Long> candidateEquipeIds) {
    }

    private final EquipeOfficePolicyRepository policyRepo;
    private final EquipeOfficeDelegacaoRegraRepository regraRepo;
    private final MembroEquipeRepository membroEquipeRepository;

    public OfficePersonalScopeService(EquipeOfficePolicyRepository policyRepo,
                                     EquipeOfficeDelegacaoRegraRepository regraRepo,
                                     MembroEquipeRepository membroEquipeRepository) {
        this.policyRepo = Objects.requireNonNull(policyRepo);
        this.regraRepo = Objects.requireNonNull(regraRepo);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
    }

    @Transactional(readOnly = true)
    public ScopeDecision decide(Long userId) {

        List<Long> enforced = new ArrayList<>();

        for (var regra : regraRepo.findActiveByUser(userId)) {
            if (!regra.isBloqueiaPessoal()) continue;
            Long equipeId = regra.getEquipe() != null ? regra.getEquipe().getId() : null;
            if (equipeId == null) continue;
            EquipeOfficePolicy p = policyRepo.findByEquipeId(equipeId).orElse(null);
            if (p == null || !p.isEnabled()) continue;
            if (membroEquipeRepository.existsByUsuario_IdAndEquipe_IdAndAtivoTrue(userId, equipeId)) {
                enforced.add(equipeId);
            }
        }

        for (MembroEquipe m : membroEquipeRepository.findByUsuario_Id(userId)) {
            if (m == null || !m.isAtivo() || m.getEquipe() == null) continue;
            Long equipeId = m.getEquipe().getId();
            if (equipeId == null) continue;
            EquipeOfficePolicy p = policyRepo.findByEquipeId(equipeId).orElse(null);
            if (p == null || !p.isEnabled() || !p.isBloqueiaCausasProprias()) continue;
            PapelEquipe papel = m.getPapel();
            boolean isAdmin = papel == PapelEquipe.ADMINISTRADOR || papel == PapelEquipe.COORDENADOR;
            if (!isAdmin) {
                enforced.add(equipeId);
            }
        }

        List<Long> uniq = enforced.stream().distinct().toList();

        if (uniq.isEmpty()) {
            return new ScopeDecision(false, null, false, List.of());
        }

        if (uniq.size() == 1) {
            return new ScopeDecision(true, uniq.get(0), false, uniq);
        }

        return new ScopeDecision(true, null, true, uniq);
    }
}
