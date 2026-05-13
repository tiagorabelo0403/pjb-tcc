package com.tcc.pjb.backend.service.julgamento.safety;

import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionStepUpConsumption;
import com.tcc.pjb.backend.model.repository.julgamento.DecisionStepUpConsumptionRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;

@Service
public class DecisionBiometricStepUpService {

    private static final long MAX_AGE_SECONDS = 120L;

    private final DecisionStepUpConsumptionRepository consumptionRepository;
    private final AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService;
    private final DecisionClientBindingGuardService decisionClientBindingGuardService;

    public DecisionBiometricStepUpService(DecisionStepUpConsumptionRepository consumptionRepository,
                                          AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService,
                                          DecisionClientBindingGuardService decisionClientBindingGuardService) {
        this.consumptionRepository = consumptionRepository;
        this.atoProcessualSecurityPolicyService = atoProcessualSecurityPolicyService;
        this.decisionClientBindingGuardService = decisionClientBindingGuardService;
    }

    @Transactional
    public void requireAndConsume(Usuario usuario,
                                  Processo processo,
                                  DecisionFocusSession session,
                                  String actType,
                                  String normalizedDecisionText) {
        AtoProcessualDescriptor descriptor = atoProcessualSecurityPolicyService.descriptorForActType(actType);
        AtoProcessualSecurityProfile securityProfile = descriptor.securityProfile();
        String canonicalActType = atoProcessualSecurityPolicyService.canonicalActType(actType);
        if (securityProfile.requiresHumanReason() && (normalizedDecisionText == null || normalizedDecisionText.isBlank())) {
            throw validation("O ato sensível exige conteúdo decisório identificável e fundamentação útil para a blindagem final.");
        }
        if (!securityProfile.requiresStepUp()) {
            return;
        }
        DecisionStepUpTokenPayload payload = RequestContext.getDecisionStepUpCredential()
                .orElseThrow(() -> validation("É obrigatório concluir o step-up biométrico no último clique antes do ato decisório."));
        long now = Instant.now().getEpochSecond();
        if (payload.exp() < now || payload.iat() < now - MAX_AGE_SECONDS) {
            throw validation("O step-up biométrico decisório expirou. Refaça a confirmação final.");
        }
        if (!usuario.getId().equals(payload.userId())) {
            throw validation("O step-up biométrico não pertence ao usuário autenticado.");
        }
        if (!processo.getId().equals(payload.processoId())) {
            throw validation("O step-up biométrico foi emitido para outro processo.");
        }
        if (!session.getId().equals(payload.focusSessionId())) {
            throw validation("O step-up biométrico não corresponde ao foco decisional armado.");
        }
        if (!normalize(canonicalActType).equals(normalize(atoProcessualSecurityPolicyService.canonicalActType(payload.actType())))) {
            throw validation("O step-up biométrico foi emitido para outro tipo de ato decisório.");
        }
        if (securityProfile.requiresBindingCheck()) {
            decisionClientBindingGuardService.assertBoundActiveClient(session, processo.getId());
            if (session.getWindowBinding() != null && !session.getWindowBinding().equals(payload.windowBinding())) {
                throw validation("O step-up biométrico foi emitido para outra janela ativa.");
            }
            if (session.getTabBinding() != null && !session.getTabBinding().equals(payload.tabBinding())) {
                throw validation("O step-up biométrico foi emitido para outra aba ativa.");
            }
            if (session.getRouteBinding() != null) {
                if (payload.routeBinding() == null || payload.routeBinding().isBlank() || !session.getRouteBinding().equalsIgnoreCase(payload.routeBinding())) {
                    throw validation("O step-up biométrico foi emitido para outra rota ativa.");
                }
            }
        }
        String expectedHash = Hashes.sha256Hex(normalizedDecisionText == null ? "" : normalizedDecisionText);
        if (securityProfile.requiresSemanticHash()) {
            if (payload.requestHash() == null || payload.requestHash().isBlank() || !payload.requestHash().equals(expectedHash)) {
                throw validation("O step-up biométrico não corresponde exatamente ao conteúdo decisório do último clique.");
            }
        }
        if (consumptionRepository.existsByTokenJti(payload.jti())) {
            throw validation("O token de step-up biométrico decisório já foi consumido e não pode ser reutilizado.");
        }
        DecisionStepUpConsumption consumption = new DecisionStepUpConsumption();
        consumption.setTokenJti(payload.jti());
        consumption.setActType(normalize(canonicalActType));
        consumption.setRequestHash(expectedHash);
        consumption.setUsuario(usuario);
        consumption.setProcesso(processo);
        consumption.setFocusSession(session);
        consumptionRepository.save(consumption);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ErroDeValidacaoException validation(String detail) {
        return new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, detail);
    }
}
