package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStepUpAuthenticationPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalStepUpAuthenticationPolicyApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService;

    public InstitutionalStepUpAuthenticationPolicyApplicationService(CurrentUserService currentUserService,
                                                                     InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.sensitiveActAuthorizationApplicationService = Objects.requireNonNull(sensitiveActAuthorizationApplicationService);
    }

    public InstitutionalStepUpAuthenticationPolicy avaliarAtual(String affiliationId,
                                                                String nominationId,
                                                                String sensitiveActCode) {
        Usuario user = currentUserService.getRequired();
        InstitutionalSensitiveAct act = resolveAct(sensitiveActCode);
        var authorization = sensitiveActAuthorizationApplicationService.autorizar(act, affiliationId, nominationId);
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("ato_sensivel=" + act.name());
        fundamentos.addAll(authorization.fundamentos());
        return new InstitutionalStepUpAuthenticationPolicy(
                user.getId(),
                user.getNome(),
                authorization.affiliationId(),
                authorization.nominationId(),
                authorization.sensitiveAct().name(),
                act.requireMfa(),
                act.requireCertificate(),
                act.requireNetworkOrRemoteAuthorization(),
                act.requireNetworkOrRemoteAuthorization(),
                authorization.requiresManualApproval(),
                authorization.blocked(),
                authorization.findings(),
                fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList(),
                Instant.now());
    }

    private InstitutionalSensitiveAct resolveAct(String raw) {
        if (raw == null || raw.isBlank()) {
            return InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO;
        }
        InstitutionalSensitiveAct act = InstitutionalSensitiveAct.fromTexto(raw);
        if (act == null) {
            throw new IllegalArgumentException("ato_sensivel_institucional_nao_reconhecido");
        }
        return act;
    }
}
