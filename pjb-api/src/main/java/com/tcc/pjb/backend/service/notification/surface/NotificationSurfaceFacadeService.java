package com.tcc.pjb.backend.service.notification.surface;

import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchRequest;
import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchResponse;
import com.tcc.pjb.backend.model.dto.notification.NotificationTrackingCienciaResponse;
import com.tcc.pjb.backend.service.notification.IntimacaoMulticanalService;
import com.tcc.pjb.backend.service.notification.NotificationTrackingService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NotificationSurfaceFacadeService {

    private final IntimacaoMulticanalService intimacaoMulticanalService;
    private final NotificationTrackingService notificationTrackingService;

    public NotificationSurfaceFacadeService(IntimacaoMulticanalService intimacaoMulticanalService,
                                            NotificationTrackingService notificationTrackingService) {
        this.intimacaoMulticanalService = Objects.requireNonNull(intimacaoMulticanalService);
        this.notificationTrackingService = Objects.requireNonNull(notificationTrackingService);
    }

    public IntimacaoMulticanalDispatchResponse dispatch(Long processoId, Long usuarioId, IntimacaoMulticanalDispatchRequest request) {
        IntimacaoMulticanalService.DispatchPlanResponse report = intimacaoMulticanalService.dispatch(
                processoId,
                usuarioId,
                request.titulo(),
                request.mensagem(),
                request.urlAcesso(),
                Boolean.TRUE.equals(request.prioridadeAlta()));
        return new IntimacaoMulticanalDispatchResponse(
                report.processoId(),
                report.usuarioId(),
                report.deliveredChannels(),
                report.suppressedChannels(),
                report.failedChannels(),
                report.trackingTokens(),
                report.status(),
                report.generatedAt(),
                report.rationale(),
                report.documentoFormalAssinado(),
                report.assinaturaQualificada(),
                report.validacaoSoberana());
    }

    public NotificationTrackingCienciaResponse ciencia(String token) {
        notificationTrackingService.markCiencia(token);
        return new NotificationTrackingCienciaResponse("CIENCIA_CONFIRMADA", token);
    }
}
