package com.tcc.pjb.backend.service.conciliacao.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.conciliacao.ConciliadorMediadorEnhancedService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConciliacaoOperationalSurfaceFacadeService {

    private final ConciliadorMediadorEnhancedService service;
    private final SurfaceProjectionSupport projectionSupport;

    public ConciliacaoOperationalSurfaceFacadeService(ConciliadorMediadorEnhancedService service,
                                                      SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse snapshot() {
        return projectionSupport.snapshot("conciliacao.operacional", service.bootstrapPainel());
    }

    public SurfaceActionResponse registrarResultado(Long processoId, String resultado, String observacoes, boolean acordoFirmado) {
        return projectionSupport.action("conciliacao.operacional", "registrarResultado", processoId,
                service.registrarResultadoSessao(processoId, resultado, observacoes, acordoFirmado));
    }

    public SurfaceActionResponse agendarSessao(Long processoId, Instant dataHora, String sala, String modalidade) {
        return projectionSupport.action("conciliacao.operacional", "agendarSessao", processoId,
                service.agendarSessao(processoId, dataHora, sala, modalidade));
    }

    public SurfaceActionResponse lavrarTermoAcordo(Long processoId, String clausulas, List<String> partes, double valor) {
        return projectionSupport.action("conciliacao.operacional", "lavrarTermoAcordo", processoId,
                service.lavrarTermoAcordo(processoId, clausulas, partes, valor));
    }
}
