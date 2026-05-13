package com.tcc.pjb.backend.service.distribuicao.surface;

import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.core.distribuicao.DistributionWorkbenchAssemblerService;
import com.tcc.pjb.backend.model.dto.distribuicao.DistribuicaoProcessualPayloadResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistribuicaoProcessualResultResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualDiagnosticoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualRequest;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DistribuicaoProcessualSurfaceFacadeService {

    private final DistribuicaoProcessualNacionalEngine engine;
    private final DistributionWorkbenchAssemblerService distributionWorkbenchAssemblerService;
    private final DistribuicaoProcessualSurfaceRequestMapper requestMapper;

    public DistribuicaoProcessualSurfaceFacadeService(DistribuicaoProcessualNacionalEngine engine,
                                                      DistributionWorkbenchAssemblerService distributionWorkbenchAssemblerService,
                                                      DistribuicaoProcessualSurfaceRequestMapper requestMapper) {
        this.engine = Objects.requireNonNull(engine);
        this.distributionWorkbenchAssemblerService = Objects.requireNonNull(distributionWorkbenchAssemblerService);
        this.requestMapper = Objects.requireNonNull(requestMapper);
    }

    public DistribuicaoProcessualResultResponse distribuir(DistribuicaoProcessualRequest request) {
        DistribuicaoProcessualNacionalEngine.DistribuicaoResult result = engine.distribuir(requestMapper.toDistribuicaoRequest(request));
        return new DistribuicaoProcessualResultResponse(result.workItemId(), result.status(), result.filaDistribuicao(), result.inboxKey(), result.varaDestino(), result.comarcaDestino(), result.trilhoCompetencia());
    }

    public DistribuicaoProcessualPayloadResponse redistribuir(Long processoId, String motivoImpedimento) {
        return new DistribuicaoProcessualPayloadResponse("redistribuicao", engine.redistribuirPorImpedimento(processoId, motivoImpedimento));
    }

    public DistribuicaoProcessualPayloadResponse consultar(String numeroProcesso) {
        return new DistribuicaoProcessualPayloadResponse("consulta", engine.consultarDistribuicao(numeroProcesso));
    }

    public DistributionWorkbenchResponse workbench(String numeroProcesso) {
        return distributionWorkbenchAssemblerService.assemble(numeroProcesso);
    }

    public DistribuicaoProcessualPayloadResponse diagnosticar(DistribuicaoProcessualDiagnosticoRequest request) {
        Map<String, Object> payload = engine.diagnosticarCompetencia(requestMapper.toDiagnosticoRequest(request));
        return new DistribuicaoProcessualPayloadResponse("diagnostico", payload);
    }
}
