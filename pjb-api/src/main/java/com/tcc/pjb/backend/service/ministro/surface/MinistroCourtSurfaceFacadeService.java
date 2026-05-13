package com.tcc.pjb.backend.service.ministro.surface;

import com.tcc.pjb.backend.model.dto.ministro.MinistroDecisaoMonocraticaRequest;
import com.tcc.pjb.backend.model.dto.ministro.MinistroDecisaoPlenariaRequest;
import com.tcc.pjb.backend.model.dto.ministro.MinistroPautaRequest;
import com.tcc.pjb.backend.model.dto.ministro.RepercussaoGeralJulgamentoRequest;
import com.tcc.pjb.backend.model.dto.ministro.RepercussaoGeralReconhecimentoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ministro.MinistroCompetenciaOriginariaService;
import com.tcc.pjb.backend.service.ministro.MinistroPlenarioService;
import com.tcc.pjb.backend.service.ministro.RepercussaoGeralService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MinistroCourtSurfaceFacadeService {

    private final MinistroPlenarioService ministroPlenarioService;
    private final RepercussaoGeralService repercussaoGeralService;
    private final MinistroCompetenciaOriginariaService ministroCompetenciaOriginariaService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public MinistroCourtSurfaceFacadeService(MinistroPlenarioService ministroPlenarioService,
                                             RepercussaoGeralService repercussaoGeralService,
                                             MinistroCompetenciaOriginariaService ministroCompetenciaOriginariaService,
                                             SurfaceProjectionSupport surfaceProjectionSupport) {
        this.ministroPlenarioService = Objects.requireNonNull(ministroPlenarioService);
        this.repercussaoGeralService = Objects.requireNonNull(repercussaoGeralService);
        this.ministroCompetenciaOriginariaService = Objects.requireNonNull(ministroCompetenciaOriginariaService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceSnapshotResponse snapshotPlenario() {
        return surfaceProjectionSupport.snapshot("ministro.plenario.snapshot", ministroPlenarioService.bootstrapPlenario());
    }

    public SurfaceSnapshotResponse malhaProcesso(Long processoId) {
        return surfaceProjectionSupport.snapshot("ministro.plenario.malha", ministroPlenarioService.malhaProcesso(processoId));
    }

    public SurfaceActionResponse proferirDecisaoMonocratica(Long processoId, MinistroDecisaoMonocraticaRequest request) {
        return surfaceProjectionSupport.action(
                "ministro.plenario",
                "decisao-monocratica",
                processoId,
                ministroPlenarioService.proferirDecisaoMonocratica(processoId, request.relatorio(), request.fundamentacao(), request.dispositivo())
        );
    }

    public SurfaceActionResponse incluirPauta(Long processoId, MinistroPautaRequest request) {
        return surfaceProjectionSupport.action(
                "ministro.plenario",
                "incluir-pauta",
                processoId,
                ministroPlenarioService.incluirPautaPlenario(processoId, request.dataSessao(), request.orgao())
        );
    }

    public SurfaceActionResponse registrarDecisaoPlenaria(Long processoId, MinistroDecisaoPlenariaRequest request) {
        return surfaceProjectionSupport.action(
                "ministro.plenario",
                "decisao-plenaria",
                processoId,
                ministroPlenarioService.registrarDecisaoPlenaria(processoId, request.votacao(), request.ementa(), request.dispositivo())
        );
    }

    public SurfaceCollectionResponse listarTemasRepercussao() {
        return surfaceProjectionSupport.collection("ministro.repercussao-geral.listagem", repercussaoGeralService.listarTemas());
    }

    public SurfaceSnapshotResponse reconhecerTema(Long processoId, RepercussaoGeralReconhecimentoRequest request) {
        return surfaceProjectionSupport.snapshot(
                "ministro.repercussao-geral.reconhecimento",
                repercussaoGeralService.reconhecer(
                        processoId,
                        new RepercussaoGeralService.ReconhecimentoRequest(
                                request.modalidade(),
                                request.ementa(),
                                request.fundamentosResumo(),
                                request.limitProcessosRelacionados(),
                                request.corteMinimoSimilaridadePercent()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse aplicarTema(String codigo, RepercussaoGeralJulgamentoRequest request) {
        return surfaceProjectionSupport.snapshot(
                "ministro.repercussao-geral.aplicacao",
                repercussaoGeralService.aplicarResultado(codigo, new RepercussaoGeralService.JulgamentoRequest(request.teseFirmada(), request.efeitosProcessuais()))
        );
    }

    public SurfaceCollectionResponse catalogoCompetenciasOriginarias() {
        return surfaceProjectionSupport.collection("ministro.competencias-originarias.catalogo", ministroCompetenciaOriginariaService.listarCatalogo());
    }

    public SurfaceSnapshotResponse sugerirCompetenciaOriginaria(String classe) {
        return surfaceProjectionSupport.snapshot("ministro.competencias-originarias.sugestao", ministroCompetenciaOriginariaService.sugerir(classe));
    }
}
