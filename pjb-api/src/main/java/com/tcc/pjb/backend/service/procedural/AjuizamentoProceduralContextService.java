package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AjuizamentoProceduralContextService {

    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService;
    private final TerritorialProcessualService territorialProcessualService;

    public AjuizamentoProceduralContextService(NationalProceduralRoutingService nationalProceduralRoutingService,
                                               AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService,
                                               TerritorialProcessualService territorialProcessualService) {
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.ajuizamentoCanonicalContextService = Objects.requireNonNull(ajuizamentoCanonicalContextService);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
    }

    public void consolidateAndValidate(Processo processo, LegalCompilerService.CompiledProcess compiled) {
        Objects.requireNonNull(processo, "processo");
        var routing = nationalProceduralRoutingService.analyzeProcess(processo);
        ajuizamentoCanonicalContextService.consolidate(processo, compiled, routing);
        var diagnosticoTerritorial = territorialProcessualService.diagnosticar(processo, routing);
        if (diagnosticoTerritorial.bloqueante()) {
            throw territorialProcessualService.toException(diagnosticoTerritorial);
        }
    }
}
