package com.tcc.pjb.backend.core.protocolo.completude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloCompletudeOutboxEntity;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloCompletudeOutboxRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "pjb.jobs.protocolo-completude", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ProtocoloCompletudeNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProtocoloCompletudeNotificationScheduler.class);
    private static final int MAX_TENTATIVAS = 3;

    private final ProtocoloCompletudeOutboxRepository outboxRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ProtocoloCompletudeNotificationScheduler(ProtocoloCompletudeOutboxRepository outboxRepository,
                                                    NotificationService notificationService,
                                                    ObjectMapper objectMapper) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Scheduled(cron = "0 */5 * * * MON-FRI", zone = "America/Sao_Paulo")
    @Transactional
    public void processarOutbox() {
        List<ProtocoloCompletudeOutboxEntity> pendentes = outboxRepository.findPendentes();
        for (ProtocoloCompletudeOutboxEntity entry : pendentes) {
            if (entry.getTentativas() >= MAX_TENTATIVAS) {
                entry.setProcessado(true);
                entry.setProcessadoEm(Instant.now());
                outboxRepository.save(entry);
                log.warn("Outbox completude abandonado após {} tentativas: id={} protocolo={}",
                        MAX_TENTATIVAS, entry.getId(), entry.getProtocoloId());
                continue;
            }
            try {
                enviarNotificacao(entry);
                entry.setProcessado(true);
                entry.setProcessadoEm(Instant.now());
            } catch (Exception e) {
                entry.setTentativas(entry.getTentativas() + 1);
                log.warn("Falha ao processar outbox completude id={} tentativa={}: {}",
                        entry.getId(), entry.getTentativas(), e.getMessage());
            }
            outboxRepository.save(entry);
        }
    }

    private void enviarNotificacao(ProtocoloCompletudeOutboxEntity entry) throws Exception {
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(entry.getPayload());
        String tipo = node.path("tipo").asText();
        String prazo = node.path("prazoRegularizacao").asText(null);

        String titulo;
        String mensagem;
        if ("PROTOCOLO_PENDENTE_DOCUMENTACAO".equals(tipo)) {
            titulo = "Documentação pendente — protocolo bloqueado";
            mensagem = "Seu protocolo foi retido por documentação insuficiente."
                    + (prazo != null ? " Prazo para regularização: " + prazo + "." : "")
                    + " Acesse o sistema para verificar os documentos necessários.";
        } else if ("PROTOCOLO_EXPIRADO".equals(tipo)) {
            titulo = "Prazo de regularização expirado";
            mensagem = "O prazo para regularização da documentação expirou. Protocolo encaminhado para análise da secretaria.";
        } else {
            titulo = "Atualização de protocolo";
            mensagem = "Seu protocolo foi atualizado. Acesse o sistema para verificar.";
        }

        notificationService.notifyUsers(
                java.util.List.of(),
                null,
                titulo,
                mensagem,
                "/peticionamento/completude/" + entry.getProtocoloId());
    }
}
