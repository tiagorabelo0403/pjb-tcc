package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReveliaAutomaticaEngine {

    private static final Logger log = LoggerFactory.getLogger(ReveliaAutomaticaEngine.class);
    private static final String RESOURCE_TYPE = "REVELIA_AUTOMATICA";
    private static final String DOMAIN_MONITORAMENTO = "REVELIA_MONITORAMENTO";
    private static final String DOMAIN_EVENTO = "REVELIA_EVENTO";
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public enum StatusRevelia {
        MONITORANDO,
        ALERTA_PRAZO_PROXIMO,
        REVELIA_CONFIGURADA,
        REVELIA_DECRETADA,
        RESPOSTA_RECEBIDA,
        CANCELADA
    }

    public record MonitoramentoRevelia(
            String monitoramentoUuid,
            String expedicaoUuid,
            Long processoId,
            String numeroUnificado,
            LocalDate prazoRespostaAte,
            StatusRevelia status,
            Instant iniciadoEm,
            Instant resolvidoEm,
            String hashIntegridade
    ) {
    }

    public record EventoRevelia(
            String eventoUuid,
            Long processoId,
            String numeroUnificado,
            StatusRevelia statusAnterior,
            StatusRevelia statusNovo,
            Instant ocorridoEm,
            String descricao,
            String hashIntegridade
    ) {
    }

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrazoRespostaPosEntregaEngine prazoEngine;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private final ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider;
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final ComunicacaoJudicialStateStore stateStore;
    private final Cache<String, MonitoramentoRevelia> monitoramentos = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();

    public ReveliaAutomaticaEngine(ProcessoRepository processoRepository,
                                   UsuarioRepository usuarioRepository,
                                   PrazoRespostaPosEntregaEngine prazoEngine,
                                   AuditLedgerService auditLedger,
                                   NotificacaoInteligentePJB notificacaoEngine,
                                   ObjectProvider<WebhookOutboundService> webhookProvider,
                                   ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider,
                                   ComunicacaoJudicialStateStore stateStore) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.curadorProvider = Objects.requireNonNull(curadorProvider, "curadorProvider");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @Transactional
    public MonitoramentoRevelia iniciarMonitoramento(String expedicaoUuid, Long processoId, LocalDate prazoAte) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(prazoAte, "prazoAte");
        MonitoramentoRevelia existente = consultar(expedicaoUuid).orElse(null);
        if (existente != null) {
            return existente;
        }
        String numeroUnificado = processoRepository.findProcessoCompletoById(processoId)
                .map(p -> p.getNumeroUnificado() != null ? p.getNumeroUnificado() : String.valueOf(processoId))
                .orElse(String.valueOf(processoId));
        String uuid = UUID.randomUUID().toString();
        String hash = sha256Hex(uuid + expedicaoUuid + prazoAte);
        MonitoramentoRevelia monitoramento = new MonitoramentoRevelia(
                uuid,
                expedicaoUuid,
                processoId,
                numeroUnificado,
                prazoAte,
                StatusRevelia.MONITORANDO,
                Instant.now(),
                null,
                hash
        );
        persistirMonitoramento(monitoramento);
        auditLedger.appendSafely(
                "REVELIA_MONITORAMENTO_INICIADO",
                RESOURCE_TYPE,
                expedicaoUuid,
                hash,
                "Monitoramento de revelia iniciado. Prazo: " + prazoAte
        );
        log.info("[Revelia] Monitoramento iniciado. expedicao={} prazoAte={}", expedicaoUuid, prazoAte);
        return monitoramento;
    }

    @Transactional
    public void registrarRespostaProtocolada(String expedicaoUuid) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        MonitoramentoRevelia monitoramento = consultar(expedicaoUuid).orElse(null);
        if (monitoramento == null) {
            return;
        }
        MonitoramentoRevelia resolvido = new MonitoramentoRevelia(
                monitoramento.monitoramentoUuid(),
                monitoramento.expedicaoUuid(),
                monitoramento.processoId(),
                monitoramento.numeroUnificado(),
                monitoramento.prazoRespostaAte(),
                StatusRevelia.RESPOSTA_RECEBIDA,
                monitoramento.iniciadoEm(),
                Instant.now(),
                monitoramento.hashIntegridade()
        );
        persistirMonitoramento(resolvido);
        auditLedger.appendSafely(
                "REVELIA_CANCELADA_POR_RESPOSTA",
                RESOURCE_TYPE,
                expedicaoUuid,
                monitoramento.hashIntegridade(),
                "Resposta protocolada. Revelia não configurada."
        );
        log.info("[Revelia] Resposta recebida. Monitoramento encerrado. expedicao={}", expedicaoUuid);
    }

    @PjbClusterSingletonTask(key = "revelia-varredura", ttl = "PT2M")
    @Scheduled(fixedDelay = 300_000)
    public void varrerVencimentos() {
        LocalDate hoje = LocalDate.now(ZONE);
        monitoramentosAtivos().stream()
                .filter(m -> m.status() == StatusRevelia.MONITORANDO || m.status() == StatusRevelia.ALERTA_PRAZO_PROXIMO)
                .forEach(m -> {
                    try {
                        processar(m, hoje);
                    } catch (Exception e) {
                        log.warn("[Revelia] Falha ao processar monitoramento. expedicao={} erro={}", m.expedicaoUuid(), e.getMessage());
                    }
                });
    }

    public List<MonitoramentoRevelia> listarReveliasConfiguradas() {
        return stateStore.findByStatusCode(DOMAIN_MONITORAMENTO, StatusRevelia.REVELIA_CONFIGURADA.name(), MonitoramentoRevelia.class);
    }

    public Optional<MonitoramentoRevelia> consultar(String expedicaoUuid) {
        MonitoramentoRevelia cache = monitoramentos.getIfPresent(expedicaoUuid);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<MonitoramentoRevelia> persisted = stateStore.find(DOMAIN_MONITORAMENTO, expedicaoUuid, MonitoramentoRevelia.class);
        persisted.ifPresent(monitoramento -> monitoramentos.put(expedicaoUuid, monitoramento));
        return persisted;
    }



    private List<MonitoramentoRevelia> monitoramentosAtivos() {
        Map<String, MonitoramentoRevelia> consolidados = new LinkedHashMap<>();
        stateStore.findByStatusCodes(
                DOMAIN_MONITORAMENTO,
                List.of(StatusRevelia.MONITORANDO.name(), StatusRevelia.ALERTA_PRAZO_PROXIMO.name()),
                MonitoramentoRevelia.class
        ).forEach(monitoramento -> consolidados.put(monitoramento.expedicaoUuid(), monitoramento));
        monitoramentos.asMap().values().stream()
                .filter(monitoramento -> monitoramento.status() == StatusRevelia.MONITORANDO || monitoramento.status() == StatusRevelia.ALERTA_PRAZO_PROXIMO)
                .forEach(monitoramento -> consolidados.put(monitoramento.expedicaoUuid(), monitoramento));
        return List.copyOf(consolidados.values());
    }

    private void persistirMonitoramento(MonitoramentoRevelia monitoramento) {
        monitoramentos.put(monitoramento.expedicaoUuid(), monitoramento);
        stateStore.save(DOMAIN_MONITORAMENTO, monitoramento.expedicaoUuid(), monitoramento.monitoramentoUuid(), monitoramento, monitoramento.processoId(), monitoramento.expedicaoUuid(), null, monitoramento.status().name());
    }

    private void processar(MonitoramentoRevelia m, LocalDate hoje) {
        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoje, m.prazoRespostaAte());
        if (diasRestantes <= 2 && diasRestantes >= 0 && m.status() == StatusRevelia.MONITORANDO) {
            transicionarStatus(m, StatusRevelia.ALERTA_PRAZO_PROXIMO, "Prazo de resposta vence em " + diasRestantes + " dia(s).");
            notificarPartes(m, NotificacaoInteligentePJB.TipoAlerta.PRAZO_VENCENDO_48H, NotificacaoInteligentePJB.UrgenciaMensagem.ALTA);
        }
        if (!m.prazoRespostaAte().isAfter(hoje) && m.status() != StatusRevelia.REVELIA_CONFIGURADA) {
            transicionarStatus(m, StatusRevelia.REVELIA_CONFIGURADA, "Prazo de resposta expirado em " + m.prazoRespostaAte() + ". Revelia configurada.");
            notificarJuizRevelia(m);
            notificarPartes(m, NotificacaoInteligentePJB.TipoAlerta.PRAZO_VENCIDO, NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA);
            publicarWebhookRevelia(m);
            registrarNecessidadeCuradorSeCouber(m);
            log.warn("[Revelia] Configurada. processo={} expedicao={}", m.processoId(), m.expedicaoUuid());
        }
    }

    private void transicionarStatus(MonitoramentoRevelia m, StatusRevelia novoStatus, String descricao) {
        String eventoUuid = UUID.randomUUID().toString();
        String hash = sha256Hex(eventoUuid + m.processoId() + novoStatus + Instant.now().toEpochMilli());
        EventoRevelia evento = new EventoRevelia(
                eventoUuid,
                m.processoId(),
                m.numeroUnificado(),
                m.status(),
                novoStatus,
                Instant.now(),
                descricao,
                hash
        );
        stateStore.save(DOMAIN_EVENTO, evento.eventoUuid(), m.expedicaoUuid(), evento, m.processoId(), m.expedicaoUuid(), null, novoStatus.name());
        MonitoramentoRevelia atualizado = new MonitoramentoRevelia(
                m.monitoramentoUuid(),
                m.expedicaoUuid(),
                m.processoId(),
                m.numeroUnificado(),
                m.prazoRespostaAte(),
                novoStatus,
                m.iniciadoEm(),
                novoStatus == StatusRevelia.REVELIA_CONFIGURADA ? Instant.now() : null,
                hash
        );
        persistirMonitoramento(atualizado);
        auditLedger.appendSafely(
                "REVELIA_STATUS_" + novoStatus.name(),
                RESOURCE_TYPE,
                m.expedicaoUuid(),
                hash,
                descricao
        );
    }

    private void notificarJuizRevelia(MonitoramentoRevelia m) {
        processoRepository.findProcessoCompletoById(m.processoId()).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            notificacaoEngine.enviarNotificacao(notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    m.processoId(),
                    NotificacaoInteligentePJB.TipoAlerta.PRAZO_VENCIDO,
                    NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            ));
        });
    }

    private void notificarPartes(MonitoramentoRevelia m,
                                 NotificacaoInteligentePJB.TipoAlerta tipo,
                                 NotificacaoInteligentePJB.UrgenciaMensagem urgencia) {
        processoRepository.findProcessoCompletoById(m.processoId()).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            notificacaoEngine.enviarNotificacao(notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    m.processoId(),
                    tipo,
                    urgencia,
                    NotificacaoInteligentePJB.CanalNotificacao.EMAIL_CERTIFICADO
            ));
        });
    }

    private void publicarWebhookRevelia(MonitoramentoRevelia m) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null) {
            return;
        }
        webhook.publicar(new WebhookOutboundService.WebhookPayload(
                UUID.randomUUID().toString(),
                WebhookOutboundService.EventoWebhook.REVELIA_CONFIGURADA,
                m.processoId(),
                m.numeroUnificado(),
                m.expedicaoUuid(),
                m.status().name(),
                Instant.now(),
                java.util.Map.of("prazoRespostaAte", String.valueOf(m.prazoRespostaAte()))
        ));
    }

    private void registrarNecessidadeCuradorSeCouber(MonitoramentoRevelia m) {
        CuradorEspecialAutomaticoService curador = curadorProvider.getIfAvailable();
        if (curador == null) {
            return;
        }
        processoRepository.findProcessoCompletoById(m.processoId()).ifPresent(processo -> {
            if (processo.getParteReuCpf() == null || processo.getParteReuCpf().isBlank()) {
                curador.registrarNecessidadeSeAusente(
                        m.processoId(),
                        m.expedicaoUuid(),
                        CuradorEspecialAutomaticoService.TipoCuradoria.REVEL_SEM_REPRESENTANTE
                );
            }
        });
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }
}
