package com.tcc.pjb.backend.service.criminal.surface;

import com.tcc.pjb.backend.model.dto.criminal.InqueritoCadastroRequest;
import com.tcc.pjb.backend.model.dto.criminal.InqueritoMovimentacaoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.criminal.InqueritoPolicialDigitalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InqueritoPolicialDigitalSurfaceFacadeService {

    private final InqueritoPolicialDigitalService inqueritoPolicialDigitalService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public InqueritoPolicialDigitalSurfaceFacadeService(InqueritoPolicialDigitalService inqueritoPolicialDigitalService,
                                                        SurfaceProjectionSupport surfaceProjectionSupport) {
        this.inqueritoPolicialDigitalService = Objects.requireNonNull(inqueritoPolicialDigitalService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceCollectionResponse listarMeus(String status) {
        return surfaceProjectionSupport.collection("criminal.inquerito.meus", inqueritoPolicialDigitalService.listarMeus(status));
    }

    public SurfaceCollectionResponse listarPorProcesso(Long processoId) {
        return surfaceProjectionSupport.collection("criminal.inquerito.processo", inqueritoPolicialDigitalService.listarPorProcesso(processoId));
    }

    public SurfaceSnapshotResponse registrar(InqueritoCadastroRequest request) {
        return surfaceProjectionSupport.snapshot(
                "criminal.inquerito.registrar",
                inqueritoPolicialDigitalService.registrar(
                        new InqueritoPolicialDigitalService.InqueritoCadastroRequest(
                                request.numeroProcedimento(),
                                request.tipo(),
                                request.naturezaFato(),
                                request.resumoFatos(),
                                request.investigadosResumo(),
                                request.vitimasResumo(),
                                request.indiciosResumo(),
                                request.diligenciasPendentes(),
                                request.orgaoApuracao(),
                                request.unidadeApuracaoId(),
                                request.uf(),
                                request.municipio(),
                                request.nivelSigilo(),
                                request.prazoConclusao(),
                                request.processoVinculadoId()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse movimentar(Long inqueritoId, InqueritoMovimentacaoRequest request) {
        return surfaceProjectionSupport.snapshot(
                "criminal.inquerito.movimentar",
                inqueritoPolicialDigitalService.movimentar(
                        inqueritoId,
                        new InqueritoPolicialDigitalService.InqueritoMovimentacaoRequest(
                                request.status(),
                                request.faseAtual(),
                                request.diligenciasPendentes(),
                                request.indiciosResumo(),
                                request.ultimaMovimentacaoResumo(),
                                request.prazoConclusao(),
                                request.remeterAoMinisterioPublico(),
                                request.encaminharAoJudiciario()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse vincularProcesso(Long inqueritoId, Long processoId) {
        return surfaceProjectionSupport.snapshot("criminal.inquerito.vincular-processo", inqueritoPolicialDigitalService.vincularProcesso(inqueritoId, processoId));
    }
}
