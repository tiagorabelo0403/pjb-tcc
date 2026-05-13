package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalDocumentSecurityGate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalDocumentSecurityGateApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService;

    public InstitutionalDocumentSecurityGateApplicationService(CurrentUserService currentUserService,
                                                               InstitutionalNominationStateRepository nominationRepository,
                                                               InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.sensitiveActAuthorizationApplicationService = Objects.requireNonNull(sensitiveActAuthorizationApplicationService);
    }

    public InstitutionalDocumentSecurityGate avaliar(String unidadeCodigo,
                                                     String caixaCodigo,
                                                     InstitutionalSensitiveAct act,
                                                     String operationCode,
                                                     boolean allowLegacyFallback) {
        Usuario usuario = currentUserService.getRequired();
        InstitutionalNomination nomination = resolveNomination(usuario.getId(), unidadeCodigo, caixaCodigo);
        if (nomination == null && allowLegacyFallback) {
            return new InstitutionalDocumentSecurityGate(
                    operationCode,
                    null,
                    null,
                    unidadeCodigo,
                    caixaCodigo,
                    false,
                    true,
                    act != null && act.requireCertificate(),
                    act == null || act.requireCertificate(),
                    act != null && act.requireMfa(),
                    false,
                    false,
                    List.of("fluxo_legado_sem_nomeacao_institucional_materializada"),
                    List.of("governanca_documental_mantida_sem_quebrar_fluxo_legado", "operation=" + operationCode),
                    Instant.now());
        }
        if (nomination == null) {
            return new InstitutionalDocumentSecurityGate(
                    operationCode,
                    null,
                    null,
                    unidadeCodigo,
                    caixaCodigo,
                    true,
                    false,
                    act != null && act.requireCertificate(),
                    true,
                    act != null && act.requireMfa(),
                    false,
                    true,
                    List.of("nomeacao_institucional_nao_localizada_para_fluxo_documental"),
                    List.of("operation=" + operationCode),
                    Instant.now());
        }
        var authorization = sensitiveActAuthorizationApplicationService.autorizar(act, nomination.affiliationId(), nomination.nominationId());
        ArrayList<String> fundamentos = new ArrayList<>(authorization.fundamentos());
        fundamentos.add("operation=" + operationCode);
        if (unidadeCodigo != null && !unidadeCodigo.isBlank()) {
            fundamentos.add("requestedUnit=" + unidadeCodigo);
        }
        return new InstitutionalDocumentSecurityGate(
                operationCode,
                nomination.affiliationId(),
                nomination.nominationId(),
                nomination.unidadeCodigo(),
                nomination.caixaCodigo(),
                true,
                authorization.allowed(),
                act != null && act.requireCertificate(),
                act == null || act.requireCertificate(),
                act != null && act.requireMfa(),
                authorization.requiresManualApproval(),
                authorization.blocked(),
                authorization.findings(),
                List.copyOf(fundamentos),
                authorization.evaluatedAt());
    }

    public InstitutionalDocumentSecurityGate enforce(String unidadeCodigo,
                                                     String caixaCodigo,
                                                     InstitutionalSensitiveAct act,
                                                     String operationCode,
                                                     boolean allowLegacyFallback) {
        InstitutionalDocumentSecurityGate gate = avaliar(unidadeCodigo, caixaCodigo, act, operationCode, allowLegacyFallback);
        if (gate.enforced() && !gate.allowed()) {
            throw new RegraNegocioException("Ato documental institucional bloqueado. Motivo: " + String.join(" | ", gate.findings()));
        }
        return gate;
    }

    private InstitutionalNomination resolveNomination(Long userId, String unidadeCodigo, String caixaCodigo) {
        Instant now = Instant.now();
        if (unidadeCodigo != null && !unidadeCodigo.isBlank()) {
            var exact = nominationRepository.findActiveFor(userId, unidadeCodigo, caixaCodigo, now);
            if (exact.isPresent()) {
                return exact.get();
            }
        }
        return nominationRepository.findByNominatedUserId(userId).stream()
                .filter(item -> item.ativaEm(now))
                .sorted(Comparator.comparing((InstitutionalNomination item) -> item.trustFloor() == null ? 0 : item.trustFloor().ordem()).reversed()
                        .thenComparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }
}
