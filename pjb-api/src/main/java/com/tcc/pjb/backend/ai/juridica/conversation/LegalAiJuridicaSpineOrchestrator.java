package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHallucinationGuardService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaResearchDossierService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaValidationEnvelopeService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LegalAiConversationOrchestrator: agrupa os 3 servicos do pacote
 * juridica.spine -- research dossier (pesquisa juridica), validation envelope
 * (validacao estrutural) e hallucination guard (blindagem contra alucinacao). Todos
 * pass-through simples; converse() ainda controla a ordem de invocacao.
 */
@Service
public class LegalAiJuridicaSpineOrchestrator {

    private final JuridicaResearchDossierService researchDossierService;
    private final JuridicaValidationEnvelopeService validationEnvelopeService;
    private final JuridicaHallucinationGuardService hallucinationGuardService;

    public LegalAiJuridicaSpineOrchestrator(JuridicaResearchDossierService researchDossierService,
                                             JuridicaValidationEnvelopeService validationEnvelopeService,
                                             JuridicaHallucinationGuardService hallucinationGuardService) {
        this.researchDossierService = Objects.requireNonNull(researchDossierService);
        this.validationEnvelopeService = Objects.requireNonNull(validationEnvelopeService);
        this.hallucinationGuardService = Objects.requireNonNull(hallucinationGuardService);
    }

    public LegalResearchDossierResponse buildDossier(LegalResearchDossierRequest request) {
        return researchDossierService.build(request);
    }

    public LegalValidationResponse validate(LegalValidationRequest request) {
        return validationEnvelopeService.validate(request);
    }

    public LegalHallucinationGuardResponse evaluateGuard(LegalHallucinationGuardRequest request) {
        return hallucinationGuardService.evaluate(request);
    }
}
