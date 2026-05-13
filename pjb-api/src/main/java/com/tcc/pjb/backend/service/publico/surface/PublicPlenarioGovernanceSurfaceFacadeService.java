package com.tcc.pjb.backend.service.publico.surface;

import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioEsclarecimentoRequest;
import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioMediaRegistrationRequest;
import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioRespostaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.publico.PublicPlenarioGovernanceService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PublicPlenarioGovernanceSurfaceFacadeService {

    private final PublicPlenarioGovernanceService service;
    private final SurfaceProjectionSupport projectionSupport;

    public PublicPlenarioGovernanceSurfaceFacadeService(PublicPlenarioGovernanceService service,
                                                        SurfaceProjectionSupport projectionSupport) {
        this.service = Objects.requireNonNull(service);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceSnapshotResponse detalhar(Long sessaoId) {
        return projectionSupport.snapshot("plenario-governanca.sessao", service.detalharGovernanca(sessaoId));
    }

    public SurfaceActionResponse registrarMidia(Long sessaoId, PublicPlenarioMediaRegistrationRequest request) {
        var response = service.registrarMidia(sessaoId, new PublicPlenarioGovernanceService.MediaRegistrationRequest(
                request.tipo(), request.titulo(), request.urlPublica(), request.hashIntegridade(), request.ordemExibicao(), request.publico()
        ));
        return projectionSupport.action("plenario-governanca.midia", "registrar-midia", sessaoId, response);
    }

    public SurfaceActionResponse registrarEsclarecimento(Long sessaoId, PublicPlenarioEsclarecimentoRequest request) {
        var response = service.registrarEsclarecimento(sessaoId, new PublicPlenarioGovernanceService.EsclarecimentoRequest(
                request.resumoDuvida(), request.visivelPublicamente()
        ));
        return projectionSupport.action("plenario-governanca.esclarecimento", "registrar-esclarecimento", sessaoId, response);
    }

    public SurfaceActionResponse responder(Long esclarecimentoId, PublicPlenarioRespostaRequest request) {
        var response = service.responderEsclarecimento(esclarecimentoId, new PublicPlenarioGovernanceService.RespostaRequest(
                request.respostaPublica(), request.visivelPublicamente()
        ));
        return projectionSupport.action("plenario-governanca.resposta", "responder-esclarecimento", esclarecimentoId, response);
    }
}
