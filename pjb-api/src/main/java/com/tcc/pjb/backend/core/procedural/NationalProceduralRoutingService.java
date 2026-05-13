package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;

@Service
@PjbPublicApi(module = PjbModuleId.COMPETENCIA_ROTEAMENTO)
public class NationalProceduralRoutingService {

    private final NationalProceduralRoutingPayloadFactory payloadFactory;
    private final NationalProceduralRoutingCoreAnalyzer coreAnalyzer;
    private final NationalProceduralRoutingFinalizationResolver finalizationResolver;

    public NationalProceduralRoutingService(NationalProceduralRoutingPayloadFactory payloadFactory,
                                            NationalProceduralRoutingCoreAnalyzer coreAnalyzer,
                                            NationalProceduralRoutingFinalizationResolver finalizationResolver) {
        this.payloadFactory = Objects.requireNonNull(payloadFactory);
        this.coreAnalyzer = Objects.requireNonNull(coreAnalyzer);
        this.finalizationResolver = Objects.requireNonNull(finalizationResolver);
    }

    public ProceduralRoutingReport analyzeRequest(ProcessoRequest request) {
        return analyzePayload(payloadFactory.fromProcessoRequest(request), "processo_request");
    }

    public ProceduralRoutingReport analyzeRequest(LaianePeticaoAssistRequest request) {
        return analyzePayload(payloadFactory.fromLaianeRequest(request), "laiane_request");
    }

    public ProceduralRoutingReport analyzeProcess(Processo processo) {
        return analyzeProcess(processo, null);
    }

    public ProceduralRoutingReport analyzeProcess(Processo processo, ProcessoRequest request) {
        return analyzePayload(payloadFactory.fromProcesso(processo, request), "processo_entity");
    }

    public ProceduralRoutingReport analyzeContext(Map<String, Object> payload) {
        return analyzePayload(payload == null ? Map.of() : payload, "context");
    }

    private ProceduralRoutingReport analyzePayload(Map<String, Object> source, String sourceLabel) {
        LinkedHashMap<String, Object> payload = payloadFactory.copyOf(source);
        NationalProceduralRoutingCoreResolution resolution = coreAnalyzer.analyze(payload, sourceLabel);
        return finalizationResolver.finalize(resolution);
    }
}
