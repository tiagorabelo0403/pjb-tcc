package com.tcc.pjb.backend.service.magistratura.surface;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaContextResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaOperationalContextResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.magistratura.MagistraturaContextService;
import com.tcc.pjb.backend.service.magistratura.MagistraturaOperationalContextService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaContextSurfaceFacadeService {

    private final MagistraturaContextService contextService;
    private final MagistraturaOperationalContextService operationalContextService;
    private final PessoaLocalizacaoService pessoaLocalizacaoService;
    private final SurfaceProjectionSupport projectionSupport;

    public MagistraturaContextSurfaceFacadeService(MagistraturaContextService contextService,
                                                   MagistraturaOperationalContextService operationalContextService,
                                                   PessoaLocalizacaoService pessoaLocalizacaoService,
                                                   SurfaceProjectionSupport projectionSupport) {
        this.contextService = contextService;
        this.operationalContextService = operationalContextService;
        this.pessoaLocalizacaoService = pessoaLocalizacaoService;
        this.projectionSupport = projectionSupport;
    }

    public MagistraturaContextResponse context() {
        return contextService.context();
    }

    public MagistraturaOperationalContextResponse operacional() {
        return operationalContextService.operacional();
    }

    public SurfaceCollectionResponse consultasRecentes() {
        return projectionSupport.collection("magistratura-localizador-recentes",
                pessoaLocalizacaoService.listarRecentes(PessoaLocalizacaoService.CanalConsulta.MAGISTRATURA, 20));
    }

    public SurfaceSnapshotResponse metricas() {
        return projectionSupport.snapshot("magistratura-localizador-metricas",
                pessoaLocalizacaoService.metricas(PessoaLocalizacaoService.CanalConsulta.MAGISTRATURA, 10));
    }

    public PessoaLocalizacaoResponse localizarPessoa(PessoaLocalizacaoRequest request) {
        return pessoaLocalizacaoService.localizar(request, PessoaLocalizacaoService.CanalConsulta.MAGISTRATURA);
    }
}
