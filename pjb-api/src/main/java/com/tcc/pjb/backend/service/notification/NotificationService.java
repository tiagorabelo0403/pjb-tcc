package com.tcc.pjb.backend.service.notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationChannel> canais;
    private final NotificationHistoryRepository historyRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final NotificationTrackingService trackingService;

    public void notifyJudge(Usuario juiz, Processo processo, String titulo, String mensagem, String url) {
        if (juiz == null || processo == null) {
            return;
        }
        dispatch(new NotificationMessage(juiz, processo, titulo, mensagem, url), false, "[NOTIFY][JUDGE]");
    }

    public void notifyLawyers(Processo processo, String title, String message) {
        if (processo == null) {
            return;
        }

        List<Usuario> destinatarios = new ArrayList<>();
        if (processo.getUsuario() != null && processo.getUsuario().isAdvogado()) {
            destinatarios.add(processo.getUsuario());
        }

        if (destinatarios.isEmpty()) {
            log.info("[NOTIFY][LAWYERS][NO_BINDING] processo={} title={}", processo.getId(), title);
            return;
        }

        for (Usuario advogado : destinatarios) {
            dispatch(new NotificationMessage(advogado, processo, title, message, null), false, "[NOTIFY][LAWYERS]");
        }
    }

    public void notifyUser(Usuario usuario, Processo processo, String titulo, String mensagem, String url) {
        if (usuario == null) {
            return;
        }
        dispatch(new NotificationMessage(usuario, processo, titulo, mensagem, url), false, "[NOTIFY][USER]");
    }

    public DispatchReport notifyUserAdvanced(Usuario usuario,
                                             Processo processo,
                                             String titulo,
                                             String mensagem,
                                             String url,
                                             boolean prioridadeAlta) {
        if (usuario == null) {
            return DispatchReport.empty("USUARIO_AUSENTE");
        }
        return dispatch(new NotificationMessage(usuario, processo, titulo, mensagem, url), prioridadeAlta, "[NOTIFY][MULTICANAL]");
    }

    public void notifyUsers(List<Usuario> usuarios, Processo processo, String titulo, String mensagem, String url) {
        if (usuarios == null || usuarios.isEmpty()) {
            return;
        }
        for (Usuario usuario : usuarios) {
            notifyUser(usuario, processo, titulo, mensagem, url);
        }
    }

    private DispatchReport dispatch(NotificationMessage message, boolean prioridadeAlta, String logPrefix) {
        if (message == null || message.destinatario() == null) {
            return DispatchReport.empty("MENSAGEM_INVALIDA");
        }

        Usuario destinatario = message.destinatario();
        UserNotificationPreference preference = loadPreference(destinatario.getId());
        if (preference != null && preference.isOnlyHighPriority() && !prioridadeAlta) {
            return DispatchReport.onlySuppressed("SUPRIMIDA_PRIORIDADE", "Preferência do usuário permite apenas notificações de alta prioridade.");
        }

        try {
            long sentRecently = historyRepository.countByUsuarioIdAndStatusAndEnviadoEmAfter(
                    destinatario.getId(),
                    "ENVIADO",
                    LocalDateTime.now().minusMinutes(5)
            );
            if (sentRecently > 50) {
                log.warn("{} throttled: usuarioId={} sentRecently={}", logPrefix, destinatario.getId(), sentRecently);
                return DispatchReport.onlySuppressed("SUPRIMIDA_THROTTLE", "Janela de contenção operacional acima do limite." );
            }
        } catch (Exception e) {
            log.debug("{} anti-spam unavailable: {}", logPrefix, e.getMessage());
        }

        if (canais == null || canais.isEmpty()) {
            log.info("{} sem canais configurados: usuarioId={} processoId={} title={}",
                    logPrefix,
                    destinatario.getId(),
                    message.processo() == null ? null : message.processo().getId(),
                    message.titulo());
            persistHistorySafe(message, "NONE", "ENVIADO", null);
            return new DispatchReport(List.of("NONE"), List.of(), List.of(), List.of(), "NO_CHANNELS", List.of("Entrega registrada sem adaptadores reais."));
        }

        ArrayList<String> delivered = new ArrayList<>();
        ArrayList<String> suppressed = new ArrayList<>();
        ArrayList<String> failed = new ArrayList<>();
        ArrayList<String> trackingTokens = new ArrayList<>();
        ArrayList<String> rationale = new ArrayList<>();
        int antiSpamWindow = preference != null && preference.getAntiSpamWindowMinutes() != null ? preference.getAntiSpamWindowMinutes() : 30;

        for (NotificationChannel canal : canais) {
            if (canal == null) {
                continue;
            }
            String canalName = normalizeChannel(canal.channelId());
            if (!isEnabledForPreference(preference, canal)) {
                suppressed.add(canalName);
                rationale.add("Canal " + canalName + " indisponível para a preferência cadastrada.");
                continue;
            }
            if (isDuplicateSuppressed(message, canalName, antiSpamWindow)) {
                suppressed.add(canalName);
                rationale.add("Canal " + canalName + " suprimido por duplicidade dentro da janela anti-spam.");
                continue;
            }
            String trackingToken = trackingService.newTrackingToken();
            NotificationDispatchContext context = new NotificationDispatchContext(
                    message,
                    preference,
                    trackingToken,
                    trackingService.buildPixelUrl(trackingToken),
                    trackingService.buildCienciaUrl(trackingToken)
            );
            try {
                canal.send(context);
                persistHistorySafe(message, canalName, "ENVIADO", trackingToken);
                delivered.add(canalName);
                trackingTokens.add(trackingToken);
            } catch (Exception e) {
                log.warn("{} falha canal={} usuarioId={} processoId={} err={}",
                        logPrefix,
                        canalName,
                        destinatario.getId(),
                        message.processo() == null ? null : message.processo().getId(),
                        e.getMessage());
                persistHistorySafe(message, canalName, "ERRO", trackingToken);
                failed.add(canalName);
                rationale.add("Falha no canal " + canalName + ": " + e.getMessage());
            }
        }

        String status = !delivered.isEmpty() ? "ENTREGA_PARCIAL_OU_TOTAL" : !failed.isEmpty() ? "FALHA_ENVIO" : "SUPRIMIDA";
        return new DispatchReport(List.copyOf(delivered), List.copyOf(suppressed), List.copyOf(failed), List.copyOf(trackingTokens), status, List.copyOf(rationale));
    }

    private boolean isDuplicateSuppressed(NotificationMessage message, String canalName, int antiSpamWindow) {
        try {
            return historyRepository.existsByUsuarioIdAndProcessoIdAndTituloAndCanalAndStatusAndEnviadoEmAfter(
                    message.destinatario().getId(),
                    message.processo() == null ? null : message.processo().getId(),
                    message.titulo(),
                    canalName,
                    "ENVIADO",
                    LocalDateTime.now().minusMinutes(Math.max(1, antiSpamWindow))
            );
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isEnabledForPreference(UserNotificationPreference preference, NotificationChannel canal) {
        if (preference == null) {
            return true;
        }
        return canal.supports(preference);
    }

    private UserNotificationPreference loadPreference(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        try {
            return preferenceRepository.findByUsuario_Id(usuarioId).orElse(null);
        } catch (Exception e) {
            log.debug("[NOTIFY][PREF] unavailable usuarioId={} err={}", usuarioId, e.getMessage());
            return null;
        }
    }

    private String normalizeChannel(String value) {
        return value == null || value.isBlank() ? "CANAL_INDEFINIDO" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void persistHistorySafe(NotificationMessage message, String canal, String status, String trackingToken) {
        try {
            historyRepository.save(NotificationHistory.builder()
                    .usuarioId(message.destinatario().getId())
                    .processoId(message.processo() == null ? null : message.processo().getId())
                    .canal(canal)
                    .titulo(message.titulo())
                    .mensagem(message.mensagem())
                    .enviadoEm(LocalDateTime.now())
                    .status(status)
                    .trackingToken(trackingToken)
                    .trackingHash(trackingToken != null ? com.tcc.pjb.backend.core.util.Hashes.sha256Hex(trackingToken) : null)
                    .build());
        } catch (Exception e) {
            log.debug("[NOTIFY][HISTORY] fail canal={} status={} err={}", canal, status, e.getMessage());
        }
    }

    public record DispatchReport(
            List<String> deliveredChannels,
            List<String> suppressedChannels,
            List<String> failedChannels,
            List<String> trackingTokens,
            String status,
            List<String> rationale
    ) {
        public static DispatchReport empty(String status) {
            return new DispatchReport(List.of(), List.of(), List.of(), List.of(), status, List.of());
        }

        public static DispatchReport onlySuppressed(String status, String rationale) {
            return new DispatchReport(List.of(), List.of("ALL"), List.of(), List.of(), status, List.of(Objects.requireNonNullElse(rationale, "SUPRIMIDA")));
        }
    }
}
