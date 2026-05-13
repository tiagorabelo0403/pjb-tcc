package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;

@Service
public class CuradorEspecialAutomaticoService {

    private static final Logger log = LoggerFactory.getLogger(CuradorEspecialAutomaticoService.class);
    private static final String RESOURCE_TYPE = "CURADOR_ESPECIAL";
    private static final String DOMAIN_NECESSIDADE = "CURADOR_NECESSIDADE";
    private static final String DOMAIN_NOMEACAO = "CURADOR_NOMEACAO";

    public enum StatusCuradoria {
        PENDENTE_NOMEACAO,
        NOMEADO,
        ACEITO,
        RECUSADO,
        SUBSTITUIDO,
        ENCERRADO
    }

    public enum TipoCuradoria {
        REU_EM_LUGAR_INCERTO,
        REU_INCAPAZ_SEM_REPRESENTANTE,
        PRESO_SEM_DEFENSOR,
        CITADO_POR_HORA_CERTA,
        REVEL_SEM_REPRESENTANTE
    }

    public record NecessidadeCurador(
            Long processoId,
            String numeroUnificado,
            TipoCuradoria tipo,
            String fundamentoLegal,
            String expedicaoUuid,
            Instant detectadaEm,
            int prazoNomeacaoDias,
            Instant prazoNomeacaoAte,
            boolean escalonadoAoJuiz
    ) {
    }

    public record NomeacaoCurador(
            String nomeacaoUuid,
            Long processoId,
            TipoCuradoria tipo,
            String nomeCurador,
            String oabOuFuncional,
            Long juizNomeadoPorId,
            Instant nomeadoEm,
            Instant aceitouEm,
            StatusCuradoria status,
            String hashIntegridade
    ) {
    }

    public record ExpedicaoPublicadaEditalEvent(String expedicaoUuid, Long processoId) {
    }

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final Duration CURADOR_EVENT_TIMEOUT = Duration.ofSeconds(20);

    private final ComunicacaoJudicialStateStore stateStore;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;
    private final Cache<Long, NecessidadeCurador> necessidadesPorProcesso = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();
    private final Cache<Long, NomeacaoCurador> nomeacoesPorProcesso = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();

    public CuradorEspecialAutomaticoService(ProcessoRepository processoRepository,
                                            UsuarioRepository usuarioRepository,
                                            ExpedicaoJudicialRepository expedicaoRepository,
                                            AuditLedgerService auditLedger,
                                            NotificacaoInteligentePJB notificacaoEngine,
                                            ObjectProvider<WebhookOutboundService> webhookProvider,
                                            ComunicacaoJudicialStateStore stateStore,
                                            PjbTransactionalExecutionSupport transactionalExecutionSupport,
                                            PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport, "transactionalExecutionSupport");
        this.processoSigiloRlsEntryPointSupport = Objects.requireNonNull(processoSigiloRlsEntryPointSupport, "processoSigiloRlsEntryPointSupport");
    }

    @EventListener
    public void onEditalPublicado(ExpedicaoPublicadaEditalEvent evento) {
        Objects.requireNonNull(evento, "evento");
        processoSigiloRlsEntryPointSupport.runWithProcessoContext(
                evento.processoId(),
                "EVENT",
                () -> transactionalExecutionSupport.runInNewTransaction(
                        PjbExecutionDescriptor.job("curador-especial.evento-edital-publicado", CURADOR_EVENT_TIMEOUT),
                        () -> processarNecessidadeCurador(evento.expedicaoUuid(), evento.processoId(), TipoCuradoria.REU_EM_LUGAR_INCERTO)
                )
        );
    }

    @Transactional
    public NecessidadeCurador registrarNecessidade(Long processoId,
                                                   String expedicaoUuid,
                                                   TipoCuradoria tipo) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(tipo, "tipo");
        int prazoDias = prazoPorTipo(tipo);
        Instant agora = Instant.now();
        NecessidadeCurador necessidade = new NecessidadeCurador(
                processoId,
                resolverNumeroUnificado(processoId),
                tipo,
                fundamentoPorTipo(tipo),
                expedicaoUuid,
                agora,
                prazoDias,
                agora.plus(prazoDias, ChronoUnit.DAYS),
                false
        );
        persistirNecessidade(necessidade);
        auditLedger.appendSafely(
                "CURADOR_NECESSIDADE_REGISTRADA",
                RESOURCE_TYPE,
                String.valueOf(processoId),
                sha256Hex(processoId + tipo.name() + agora.toEpochMilli()),
                "CPC art. 72 - curador especial necessário. Tipo: " + tipo + ". Prazo: " + prazoDias + " dias."
        );
        publicarWebhookNecessidade(necessidade);
        notificarJuizNecessidade(necessidade);
        log.info("[Curador] Necessidade registrada. processo={} tipo={} prazo={}d", processoId, tipo, prazoDias);
        return necessidade;
    }

    @Transactional
    public NecessidadeCurador registrarNecessidadeSeAusente(Long processoId,
                                                            String expedicaoUuid,
                                                            TipoCuradoria tipo) {
        NecessidadeCurador existente = consultarNecessidade(processoId).orElse(null);
        if (existente != null) {
            return existente;
        }
        return registrarNecessidade(processoId, expedicaoUuid, tipo);
    }

    @Transactional
    public NomeacaoCurador nomear(Long processoId,
                                  Long juizId,
                                  String nomeCurador,
                                  String oabOuFuncional) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(nomeCurador, "nomeCurador");
        NecessidadeCurador necessidade = consultarNecessidade(processoId).orElse(null);
        if (necessidade == null) {
            throw new IllegalStateException("Nenhuma necessidade de curador registrada para o processo: " + processoId);
        }
        String uuid = UUID.randomUUID().toString();
        String hash = sha256Hex(uuid + processoId + nomeCurador + Instant.now().toEpochMilli());
        NomeacaoCurador nomeacao = new NomeacaoCurador(
                uuid,
                processoId,
                necessidade.tipo(),
                nomeCurador,
                oabOuFuncional,
                juizId,
                Instant.now(),
                null,
                StatusCuradoria.NOMEADO,
                hash
        );
        persistirNomeacao(nomeacao);
        persistirStatusNecessidade(necessidade, StatusCuradoria.NOMEADO.name());
        auditLedger.appendSafely(
                "CURADOR_NOMEADO",
                RESOURCE_TYPE,
                String.valueOf(processoId),
                hash,
                "Curador especial nomeado: " + nomeCurador + " | OAB/Funcional: " + oabOuFuncional
        );
        log.info("[Curador] Nomeado. processo={} curador={}", processoId, nomeCurador);
        return nomeacao;
    }

    public Optional<NecessidadeCurador> consultarNecessidade(Long processoId) {
        NecessidadeCurador cache = necessidadesPorProcesso.getIfPresent(processoId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<NecessidadeCurador> persisted = stateStore.find(DOMAIN_NECESSIDADE, String.valueOf(processoId), NecessidadeCurador.class);
        persisted.ifPresent(necessidade -> necessidadesPorProcesso.put(processoId, necessidade));
        return persisted;
    }

    public Optional<NomeacaoCurador> consultarNomeacao(Long processoId) {
        NomeacaoCurador cache = nomeacoesPorProcesso.getIfPresent(processoId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<NomeacaoCurador> persisted = stateStore.find(DOMAIN_NOMEACAO, String.valueOf(processoId), NomeacaoCurador.class);
        persisted.ifPresent(nomeacao -> nomeacoesPorProcesso.put(processoId, nomeacao));
        return persisted;
    }

    @PjbClusterSingletonTask(key = "curador-monitor", ttl = "PT4M")
    @Scheduled(fixedDelay = 600_000)
    public void monitorarPrazosNomeacao() {
        Instant agora = Instant.now();
        necessidadesMonitoradas().stream()
                .filter(n -> consultarNomeacao(n.processoId()).isEmpty())
                .filter(n -> n.prazoNomeacaoAte().isBefore(agora))
                .forEach(n -> {
                    log.warn("[Curador] Prazo de nomeação vencido. processo={}", n.processoId());
                    auditLedger.appendSafely(
                            "CURADOR_PRAZO_NOMEACAO_VENCIDO",
                            RESOURCE_TYPE,
                            String.valueOf(n.processoId()),
                            sha256Hex(String.valueOf(n.processoId())),
                            "Curador especial não nomeado no prazo. Requer ação imediata do juízo."
                    );
                    notificarJuizPrazoVencido(n);
                });
    }



    private List<NecessidadeCurador> necessidadesMonitoradas() {
        Map<Long, NecessidadeCurador> consolidados = new LinkedHashMap<>();
        stateStore.findByStatusCodes(DOMAIN_NECESSIDADE, statusNecessidadeAtiva(), NecessidadeCurador.class)
                .forEach(necessidade -> consolidados.put(necessidade.processoId(), necessidade));
        necessidadesPorProcesso.asMap().values().forEach(necessidade -> consolidados.put(necessidade.processoId(), necessidade));
        return List.copyOf(consolidados.values());
    }

    private List<String> statusNecessidadeAtiva() {
        java.util.ArrayList<String> codigos = new java.util.ArrayList<>(TipoCuradoria.values().length + 1);
        codigos.add(StatusCuradoria.PENDENTE_NOMEACAO.name());
        for (TipoCuradoria tipo : TipoCuradoria.values()) {
            codigos.add(tipo.name());
        }
        return List.copyOf(codigos);
    }

    private void persistirNecessidade(NecessidadeCurador necessidade) {
        necessidadesPorProcesso.put(necessidade.processoId(), necessidade);
        stateStore.save(DOMAIN_NECESSIDADE, String.valueOf(necessidade.processoId()), necessidade.expedicaoUuid(), necessidade, necessidade.processoId(), necessidade.expedicaoUuid(), null, StatusCuradoria.PENDENTE_NOMEACAO.name());
    }

    private void persistirStatusNecessidade(NecessidadeCurador necessidade, String statusCode) {
        necessidadesPorProcesso.put(necessidade.processoId(), necessidade);
        stateStore.save(DOMAIN_NECESSIDADE, String.valueOf(necessidade.processoId()), necessidade.expedicaoUuid(), necessidade, necessidade.processoId(), necessidade.expedicaoUuid(), null, statusCode);
    }

    private void persistirNomeacao(NomeacaoCurador nomeacao) {
        nomeacoesPorProcesso.put(nomeacao.processoId(), nomeacao);
        stateStore.save(DOMAIN_NOMEACAO, String.valueOf(nomeacao.processoId()), nomeacao.nomeacaoUuid(), nomeacao, nomeacao.processoId(), null, null, nomeacao.status().name());
    }

    private void processarNecessidadeCurador(String expedicaoUuid, Long processoId, TipoCuradoria tipo) {
        if (processoId == null || consultarNecessidade(processoId).isPresent()) {
            return;
        }
        try {
            registrarNecessidade(processoId, expedicaoUuid, tipo);
        } catch (Exception e) {
            log.error("[Curador] Falha ao registrar necessidade. processo={} erro={}", processoId, e.getMessage());
        }
    }

    private int prazoPorTipo(TipoCuradoria tipo) {
        return switch (tipo) {
            case PRESO_SEM_DEFENSOR -> 2;
            case REVEL_SEM_REPRESENTANTE -> 5;
            case CITADO_POR_HORA_CERTA -> 5;
            case REU_EM_LUGAR_INCERTO -> 15;
            case REU_INCAPAZ_SEM_REPRESENTANTE -> 10;
        };
    }

    private String fundamentoPorTipo(TipoCuradoria tipo) {
        return switch (tipo) {
            case REU_EM_LUGAR_INCERTO -> "CPC art. 72, II - réu em lugar incerto e não sabido";
            case REU_INCAPAZ_SEM_REPRESENTANTE -> "CPC art. 72, I - incapaz sem representante";
            case PRESO_SEM_DEFENSOR -> "CPC art. 72, III + CF art. 5º, LXIII";
            case CITADO_POR_HORA_CERTA -> "CPC art. 72, II c/c art. 253";
            case REVEL_SEM_REPRESENTANTE -> "CPC art. 72, II - revel sem advogado constituído";
        };
    }

    private String resolverNumeroUnificado(Long processoId) {
        return processoRepository.findProcessoCompletoById(processoId)
                .map(p -> p.getNumeroUnificado() != null ? p.getNumeroUnificado() : String.valueOf(processoId))
                .orElse(String.valueOf(processoId));
    }

    private void notificarJuizNecessidade(NecessidadeCurador necessidade) {
        processoRepository.findProcessoCompletoById(necessidade.processoId()).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    necessidade.processoId(),
                    NotificacaoInteligentePJB.TipoAlerta.ALERTA_REGRA_CRITICA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.ALTA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private void notificarJuizPrazoVencido(NecessidadeCurador necessidade) {
        processoRepository.findProcessoCompletoById(necessidade.processoId()).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    necessidade.processoId(),
                    NotificacaoInteligentePJB.TipoAlerta.PRAZO_VENCIDO,
                    NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private void publicarWebhookNecessidade(NecessidadeCurador necessidade) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null) {
            return;
        }
        webhook.publicar(new WebhookOutboundService.WebhookPayload(
                UUID.randomUUID().toString(),
                WebhookOutboundService.EventoWebhook.CURADOR_NECESSARIO,
                necessidade.processoId(),
                necessidade.numeroUnificado(),
                necessidade.expedicaoUuid(),
                necessidade.tipo().name(),
                Instant.now(),
                Map.of("fundamentoLegal", necessidade.fundamentoLegal(), "prazoNomeacaoDias", String.valueOf(necessidade.prazoNomeacaoDias()))
        ));
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
