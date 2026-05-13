package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.inovacao.batna.FacilitadorBatnaService;
import com.tcc.pjb.backend.model.dto.batna.BatnaGenerateRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BatnaSurfaceFacadeService {

    private final FacilitadorBatnaService service;
    private final SurfaceProjectionSupport projectionSupport;

    public BatnaSurfaceFacadeService(FacilitadorBatnaService service,
                                     SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse gerarPorProcesso(Long processoId, java.math.BigDecimal valorAcordo, boolean estritoTeto) {
        return projectionSupport.snapshot("batna", service.gerarParaProcesso(processoId, valorAcordo, estritoTeto));
    }

    public Optional<SurfaceSnapshotResponse> ultimoPorProcesso(Long processoId) {
        return service.buscarUltimoPorProcesso(processoId).map(report -> projectionSupport.snapshot("batna", report));
    }

    public Optional<SurfaceSnapshotResponse> ultimoPorProposta(Long propostaId) {
        return service.buscarUltimoPorProposta(propostaId).map(report -> projectionSupport.snapshot("batna", report));
    }

    public SurfaceSnapshotResponse gerar(BatnaGenerateRequest request) {
        FacilitadorBatnaService.ContextoProcesso contexto = new FacilitadorBatnaService.ContextoProcesso(
                request.processoId(),
                request.propostaAcordoId(),
                request.nupn(),
                request.tribunalCodigo(),
                request.ramoDireito(),
                request.classeTpu(),
                request.valorCausa(),
                request.valorPedidoPrincipal(),
                service.parseFase(request.faseAtual()),
                request.diasEmAndamento() == null ? 0 : request.diasEmAndamento(),
                Boolean.TRUE.equals(request.temRecursoProvavel()),
                !Boolean.FALSE.equals(request.autorAssistidoPorAdvogado()),
                !Boolean.FALSE.equals(request.reuAssistidoPorAdvogado()),
                Boolean.TRUE.equals(request.autorBeneficiarioJg()),
                Boolean.TRUE.equals(request.reuBeneficiarioJg()),
                Boolean.TRUE.equals(request.autorPessoaJuridica()),
                Boolean.TRUE.equals(request.reuPessoaJuridica()),
                request.uf(),
                request.valorAcordoEmDiscussao(),
                Boolean.TRUE.equals(request.modoEstritoTeto())
        );
        return projectionSupport.snapshot("batna", service.gerar(contexto));
    }
}
