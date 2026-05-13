package com.tcc.pjb.backend.service.leilao.surface;

import com.tcc.pjb.backend.model.dto.leilao.LeilaoAnaliseLanceHistoricoItem;
import com.tcc.pjb.backend.model.dto.leilao.LeilaoAnaliseLanceRequest;
import com.tcc.pjb.backend.model.dto.leilao.LeilaoAvaliacaoBemRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.leilao.LeilaoJudicialAnalyticsService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeilaoAnalyticsSurfaceFacadeService {

    private final LeilaoJudicialAnalyticsService service;
    private final SurfaceProjectionSupport projectionSupport;

    public LeilaoAnalyticsSurfaceFacadeService(LeilaoJudicialAnalyticsService service,
                                               SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse avaliarBem(LeilaoAvaliacaoBemRequest request) {
        double riscoJuridico = 0.0d;
        if (request.possuiRestricaoRegistral()) {
            riscoJuridico += 0.20d;
        }
        if (!request.emComarcaLiquida()) {
            riscoJuridico += 0.10d;
        }
        boolean ocupado = request.ocupacao() != null && !request.ocupacao().isBlank() && !"DESOCUPADO".equalsIgnoreCase(request.ocupacao());
        if (ocupado) {
            riscoJuridico += 0.15d;
        }
        return projectionSupport.snapshot("leilao-analytics", service.avaliarBem(new LeilaoJudicialAnalyticsService.AvaliacaoBemRequest(
                request.tipoBem(),
                request.valorMercado(),
                request.estadoConservacao(),
                Math.min(0.45d, riscoJuridico),
                ocupado
        )));
    }

    public SurfaceSnapshotResponse analisarLance(LeilaoAnaliseLanceRequest request) {
        List<LeilaoJudicialAnalyticsService.LanceHistorico> historico = request.historicoLances() == null ? List.of() : request.historicoLances().stream()
                .map(this::toHistorico)
                .toList();
        return projectionSupport.snapshot("leilao-analytics", service.analisarLances(new LeilaoJudicialAnalyticsService.AnaliseLanceRequest(
                request.valorLance(),
                request.lanceMinimo(),
                request.incrementoMinimo(),
                request.documentoLicitante(),
                request.ipOrigem(),
                request.licitanteHabilitado(),
                historico
        )));
    }

    private LeilaoJudicialAnalyticsService.LanceHistorico toHistorico(LeilaoAnaliseLanceHistoricoItem item) {
        return new LeilaoJudicialAnalyticsService.LanceHistorico(item.valor(), item.documentoLicitante(), item.ipOrigem(), item.ofertadoEm());
    }
}
