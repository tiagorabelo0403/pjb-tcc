package com.tcc.pjb.backend.service.timeline.surface;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TimelineSurfaceFacadeService {

    private final MovimentacaoProcessualRepository movimentacaoProcessualRepository;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final TimelineAnalyticProjectionService analyticProjectionService;

    public TimelineSurfaceFacadeService(MovimentacaoProcessualRepository movimentacaoProcessualRepository,
                                        ProcessoRepository processoRepository,
                                        PjbAuthorizationService authorizationService,
                                        RecursalEffectiveSecrecyService secrecyService,
                                        TimelineAnalyticProjectionService analyticProjectionService) {
        this.movimentacaoProcessualRepository = movimentacaoProcessualRepository;
        this.processoRepository = processoRepository;
        this.authorizationService = authorizationService;
        this.secrecyService = secrecyService;
        this.analyticProjectionService = analyticProjectionService;
    }

    public List<TimelineItemResponse> timeline(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        authorizationService.requireReadProcesso(processo);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processoId));
        List<MovimentacaoProcessual> movimentacoes = movimentacaoProcessualRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        var analytics = analyticProjectionService.project(processo, movimentacoes);
        return movimentacoes.stream()
                .map(movimentacao -> toResponse(movimentacao, analytics.get(movimentacao.getId())))
                .toList();
    }

    private TimelineItemResponse toResponse(MovimentacaoProcessual movimentacao, TimelineAnalyticProjectionService.AnalyticProjection analytic) {
        return new TimelineItemResponse(
                movimentacao.getId(),
                movimentacao.getDataMovimentacao(),
                movimentacao.getFaseDe() != null ? movimentacao.getFaseDe().name() : null,
                movimentacao.getFasePara() != null ? movimentacao.getFasePara().name() : null,
                movimentacao.getDescricao(),
                movimentacao.getAtor() != null ? movimentacao.getAtor().getId() : null,
                movimentacao.getAtor() != null ? movimentacao.getAtor().getNome() : null,
                analytic != null && analytic.gerouPrazo(),
                analytic != null && analytic.consumiuPrazo(),
                analytic == null ? 0L : analytic.prazoPrevistoDias(),
                analytic == null ? 0L : analytic.prazoConsumidoDias(),
                analytic == null ? "INFORMATIVO" : analytic.prazoStatus(),
                analytic == null ? 0L : analytic.diasParado(),
                analytic == null ? null : analytic.causaProvavelParada(),
                analytic == null ? null : analytic.proximaJanelaTeorica(),
                analytic != null && analytic.bloqueioOperacional(),
                analytic == null ? null : analytic.deadlineOperacionalAberto()
        );
    }
}
