package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaNotificationEnvelope;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaNotificationDispatchService {

    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationService notificationService;
    private final PainelServiceCommons commons;

    public OficialJusticaNotificationDispatchService(UsuarioRepository usuarioRepository,
                                                     ProcessoRepository processoRepository,
                                                     NotificationHistoryRepository notificationHistoryRepository,
                                                     NotificationService notificationService,
                                                     PainelServiceCommons commons) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.commons = Objects.requireNonNull(commons);
    }

    @Transactional
    public void dispatch(OficialJusticaNotificationEnvelope envelope) {
        if (envelope == null || envelope.usuarioId() == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(envelope.usuarioId()).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return;
        }
        if (notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTitulo(envelope.usuarioId(), envelope.processoId(), envelope.title())) {
            return;
        }
        Processo processo = envelope.processoId() == null ? null : processoRepository.findById(envelope.processoId()).orElse(null);
        NotificationService.DispatchReport report = notificationService.notifyUserAdvanced(
                usuario,
                processo,
                envelope.title(),
                envelope.body(),
                envelope.detailsUrl(),
                envelope.highPriority()
        );
        if (!"USUARIO_AUSENTE".equalsIgnoreCase(report.status()) && !"MENSAGEM_INVALIDA".equalsIgnoreCase(report.status())) {
            commons.publishUserHistory(
                    usuario,
                    "OFICIAL",
                    envelope.notificationType() == null || envelope.notificationType().isBlank() ? "OFICIAL_NOTIFICATION" : envelope.notificationType(),
                    envelope.body(),
                    processo,
                    envelope.workItemId()
            );
        }
    }
}
