package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingPayloadFactory {

    private final NationalProceduralProcessoRequestPayloadAssembler processoRequestPayloadAssembler;
    private final NationalProceduralLaianePayloadAssembler laianePayloadAssembler;
    private final NationalProceduralProcessoEntityPayloadAssembler processoEntityPayloadAssembler;
    private final NationalProceduralRoutingPayloadSecurityPolicy payloadSecurityPolicy;

    public NationalProceduralRoutingPayloadFactory(NationalProceduralProcessoRequestPayloadAssembler processoRequestPayloadAssembler,
                                                   NationalProceduralLaianePayloadAssembler laianePayloadAssembler,
                                                   NationalProceduralProcessoEntityPayloadAssembler processoEntityPayloadAssembler,
                                                   NationalProceduralRoutingPayloadSecurityPolicy payloadSecurityPolicy) {
        this.processoRequestPayloadAssembler = Objects.requireNonNull(processoRequestPayloadAssembler);
        this.laianePayloadAssembler = Objects.requireNonNull(laianePayloadAssembler);
        this.processoEntityPayloadAssembler = Objects.requireNonNull(processoEntityPayloadAssembler);
        this.payloadSecurityPolicy = Objects.requireNonNull(payloadSecurityPolicy);
    }

    public LinkedHashMap<String, Object> fromProcessoRequest(ProcessoRequest request) {
        return payloadSecurityPolicy.snapshot(processoRequestPayloadAssembler.assemble(request));
    }

    public LinkedHashMap<String, Object> fromLaianeRequest(LaianePeticaoAssistRequest request) {
        return payloadSecurityPolicy.snapshot(laianePayloadAssembler.assemble(request));
    }

    public LinkedHashMap<String, Object> fromProcesso(Processo processo, ProcessoRequest request) {
        return payloadSecurityPolicy.snapshot(processoEntityPayloadAssembler.assemble(processo, request));
    }

    public LinkedHashMap<String, Object> copyOf(Map<String, Object> source) {
        return payloadSecurityPolicy.snapshot(source);
    }
}
