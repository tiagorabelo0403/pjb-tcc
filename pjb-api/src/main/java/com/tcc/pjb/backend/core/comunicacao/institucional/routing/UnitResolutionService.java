package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalResult;

@Service
public class UnitResolutionService {

    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final InstitutionalCatalogGovernanceOverlayService governanceOverlayService;

    public UnitResolutionService(CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                 InstitutionalCatalogGovernanceOverlayService governanceOverlayService) {
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.governanceOverlayService = Objects.requireNonNull(governanceOverlayService);
    }

    public ResolucaoDestinoInstitucionalResult resolver(ResolucaoRoteamentoInstitucionalRequest request) {
        Objects.requireNonNull(request, "request");
        ResolucaoDestinoInstitucionalRequest resolvedRequest = new ResolucaoDestinoInstitucionalRequest(
                request.processoId(),
                request.processoNumero(),
                request.destinatarioKind(),
                request.papelProcessual(),
                request.ramoDireito(),
                request.grauJurisdicao(),
                request.uf(),
                request.comarca(),
                request.foro(),
                request.unidadeSugerida(),
                request.nucleoSugerido(),
                request.fundamentoLegal(),
                request.exigeCienciaPessoal() || request.papelProcessual().exigeCienciaPessoalPreferencial()
        );
        return governanceOverlayService.preferredUnitCode(request)
                .map(unitCode -> catalogoInstitucionalUnificadoService.resolverPreferindoCodigo(resolvedRequest, unitCode, "unidade preferencial aplicada por regra de competência administrativa"))
                .orElseGet(() -> catalogoInstitucionalUnificadoService.resolver(resolvedRequest));
    }
}
