package com.tcc.pjb.backend.service.ministro.surface;

import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoAbrirSessaoRequest;
import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoProclamarSessaoRequest;
import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoRegistrarVotoRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaPrecedenteAplicacaoSurfaceRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaPrecedenteReconhecimentoSurfaceRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ministro.MinistroPlenarioAvancadoService;
import com.tcc.pjb.backend.service.ministro.TemaPrecedenteVinculanteService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MinistroPlenarioAvancadoSurfaceFacadeService {

    private final MinistroPlenarioAvancadoService ministroPlenarioAvancadoService;
    private final TemaPrecedenteVinculanteService temaPrecedenteVinculanteService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public MinistroPlenarioAvancadoSurfaceFacadeService(MinistroPlenarioAvancadoService ministroPlenarioAvancadoService,
                                                        TemaPrecedenteVinculanteService temaPrecedenteVinculanteService,
                                                        SurfaceProjectionSupport surfaceProjectionSupport) {
        this.ministroPlenarioAvancadoService = Objects.requireNonNull(ministroPlenarioAvancadoService);
        this.temaPrecedenteVinculanteService = Objects.requireNonNull(temaPrecedenteVinculanteService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceCollectionResponse listarSessoes() {
        return surfaceProjectionSupport.collection("ministro.plenario-avancado.sessoes", ministroPlenarioAvancadoService.listarMinhasSessoes());
    }

    public SurfaceSnapshotResponse abrirSessao(Long processoId, PlenarioAvancadoAbrirSessaoRequest request) {
        var result = ministroPlenarioAvancadoService.abrirSessao(
                processoId,
                new MinistroPlenarioAvancadoService.AbrirSessaoRequest(
                        request.orgaoJulgador(),
                        request.materiaResumo(),
                        request.observacoes(),
                        request.quorumMinimo(),
                        request.segredoAteProclamacao()
                )
        );
        return surfaceProjectionSupport.snapshot("ministro.plenario-avancado.abrir", result);
    }

    public SurfaceActionResponse votar(String codigoSessao, PlenarioAvancadoRegistrarVotoRequest request) {
        var result = ministroPlenarioAvancadoService.registrarVoto(
                codigoSessao,
                new MinistroPlenarioAvancadoService.RegistrarVotoRequest(
                        request.opcaoVoto(),
                        request.fundamentacaoResumo(),
                        request.ressalva()
                )
        );
        return surfaceProjectionSupport.action("ministro.plenario-avancado", "votar", null, result);
    }

    public SurfaceSnapshotResponse detalharSessao(String codigoSessao) {
        return surfaceProjectionSupport.snapshot("ministro.plenario-avancado.detalhe", ministroPlenarioAvancadoService.detalharSessao(codigoSessao));
    }

    public SurfaceSnapshotResponse integridadeSessao(String codigoSessao) {
        return surfaceProjectionSupport.snapshot("ministro.plenario-avancado.integridade", ministroPlenarioAvancadoService.integridadeSessao(codigoSessao));
    }

    public SurfaceSnapshotResponse proclamarSessao(String codigoSessao, PlenarioAvancadoProclamarSessaoRequest request) {
        var result = ministroPlenarioAvancadoService.proclamar(
                codigoSessao,
                new MinistroPlenarioAvancadoService.ProclamarSessaoRequest(
                        request.ementa(),
                        request.dispositivo(),
                        request.gerarTemaVinculante(),
                        request.tipoTema(),
                        request.abrangencia(),
                        request.fundamentosResumo()
                )
        );
        return surfaceProjectionSupport.snapshot("ministro.plenario-avancado.proclamar", result);
    }

    public SurfaceCollectionResponse listarTemas() {
        return surfaceProjectionSupport.collection("ministro.tema-precedente.listagem", temaPrecedenteVinculanteService.listarTemas());
    }

    public SurfaceSnapshotResponse reconhecerTema(Long processoId, TemaPrecedenteReconhecimentoSurfaceRequest request) {
        return surfaceProjectionSupport.snapshot(
                "ministro.tema-precedente.reconhecer",
                temaPrecedenteVinculanteService.reconhecer(
                        processoId,
                        new TemaPrecedenteVinculanteService.TemaPrecedenteReconhecimentoRequest(
                                request.tipo(),
                                request.ementa(),
                                request.abrangencia(),
                                request.fundamentosResumo(),
                                request.corteMinimoSimilaridade(),
                                request.limitProcessosRelacionados()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse aplicarTema(String codigo, TemaPrecedenteAplicacaoSurfaceRequest request) {
        return surfaceProjectionSupport.snapshot(
                "ministro.tema-precedente.aplicar",
                temaPrecedenteVinculanteService.aplicarResultado(
                        codigo,
                        new TemaPrecedenteVinculanteService.TemaPrecedenteAplicacaoRequest(
                                request.teseFirmada(),
                                request.efeitosProcessuais(),
                                request.limitProcessosAplicacao()
                        )
                )
        );
    }
}
