package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceDispatchResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MagistraturaJudicialProvidenceAutomationService {

    private final SecretariatOperationalRoutingResolver routingResolver;
    private final MagistraturaJudicialProvidencePlanningSupport planningSupport;
    private final MagistraturaJudicialProvidenceDispatchSupport dispatchSupport;

    public MagistraturaJudicialProvidenceAutomationService(SecretariatOperationalRoutingResolver routingResolver,
                                                           MagistraturaJudicialProvidencePlanningSupport planningSupport,
                                                           MagistraturaJudicialProvidenceDispatchSupport dispatchSupport) {
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.planningSupport = Objects.requireNonNull(planningSupport);
        this.dispatchSupport = Objects.requireNonNull(dispatchSupport);
    }

    @Transactional(readOnly = true)
    public List<MagistraturaJudicialProvidenceResponse> preview(Processo processo,
                                                                Usuario usuario,
                                                                MagistraturaJudicialActCode action,
                                                                MagistraturaJudicialActCommandRequest request) {
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return planningSupport.preview(processo, usuario, action, request, profile);
    }

    @Transactional
    public List<MagistraturaJudicialProvidenceDispatchResponse> dispatch(Processo processo,
                                                                         Usuario usuario,
                                                                         MagistraturaJudicialActCode action,
                                                                         MagistraturaJudicialActCommandRequest request,
                                                                         Map<String, Object> actPayload) {
        if (request != null && Boolean.FALSE.equals(request.encaminharAutomaticamenteSecretaria())) {
            return List.of();
        }
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        List<ProvidencePlan> plans = planningSupport.buildPlans(processo, usuario, action, request, actPayload, profile);
        return dispatchSupport.dispatch(processo, usuario, action, actPayload, profile, plans);
    }
}
