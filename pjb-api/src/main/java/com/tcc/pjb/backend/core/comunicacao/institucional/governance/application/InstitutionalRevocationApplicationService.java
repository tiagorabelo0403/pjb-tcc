package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRevocationResult;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalRevocationApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final CurrentUserService currentUserService;

    public InstitutionalRevocationApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                     InstitutionalNominationStateRepository nominationRepository,
                                                     CurrentUserService currentUserService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public InstitutionalRevocationResult revogar(String affiliationId,
                                                 Long nominatedUserId,
                                                 String unidadeCodigo,
                                                 boolean revogarAfiliacao,
                                                 List<String> fundamentos) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationId(affiliationId);
        List<InstitutionalNomination> targeted = nominations.stream()
                .filter(item -> nominatedUserId == null || Objects.equals(item.nominatedUserId(), nominatedUserId))
                .filter(item -> unidadeCodigo == null || unidadeCodigo.isBlank() || item.unidadeCodigo().equalsIgnoreCase(unidadeCodigo.trim()))
                .filter(item -> item.status() != InstitutionalNominationStatus.REVOGADA)
                .toList();
        List<String> revokedIds = new ArrayList<>();
        for (InstitutionalNomination item : targeted) {
            nominationRepository.save(item.withStatus(InstitutionalNominationStatus.REVOGADA, now));
            revokedIds.add(item.nominationId());
        }
        List<InstitutionalNomination> remainingActive = nominationRepository.findByAffiliationId(affiliationId).stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        long remainingActiveAdministrators = remainingActive.stream()
                .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                .count();
        InstitutionalAffiliation savedAffiliation = affiliation;
        boolean hardCut = revogarAfiliacao;
        if (revogarAfiliacao) {
            savedAffiliation = affiliationRepository.save(affiliation.withStatus(InstitutionalAffiliationStatus.REVOGADA, now,
                    appendFundamentos(fundamentos,
                            "revogacao_afiliacao_total",
                            "revogacao_usuario=" + usuario.getId(),
                            "revogacao_em=" + now)));
        } else if (remainingActive.isEmpty() || (affiliation.requerDuplaAprovacaoAdministrador() && remainingActiveAdministrators < 2)) {
            hardCut = true;
            savedAffiliation = affiliationRepository.save(affiliation.withStatus(InstitutionalAffiliationStatus.SUSPENSA, now,
                    appendFundamentos(fundamentos,
                            "suspensao_automatica_pos_revogacao",
                            "revogacao_usuario=" + usuario.getId(),
                            "revogacao_em=" + now)));
        }
        return new InstitutionalRevocationResult(
                affiliationId,
                affiliation.orgaoSigla(),
                affiliation.unidadeCodigo(),
                nominatedUserId,
                unidadeCodigo,
                revogarAfiliacao,
                savedAffiliation.status().name(),
                revokedIds.size(),
                remainingActive.size(),
                remainingActiveAdministrators,
                hardCut,
                List.copyOf(revokedIds),
                appendFundamentos(fundamentos,
                        "corte_imediato_contexto=" + hardCut,
                        "nomeacoes_revogadas=" + revokedIds.size(),
                        "administradores_restantes=" + remainingActiveAdministrators),
                now
        );
    }

    private List<String> appendFundamentos(List<String> fundamentos, String... extras) {
        ArrayList<String> out = new ArrayList<>();
        if (fundamentos != null) {
            out.addAll(fundamentos);
        }
        if (extras != null) {
            for (String extra : extras) {
                if (extra != null && !extra.isBlank()) {
                    out.add(extra.trim());
                }
            }
        }
        return List.copyOf(out.stream().distinct().toList());
    }
}
