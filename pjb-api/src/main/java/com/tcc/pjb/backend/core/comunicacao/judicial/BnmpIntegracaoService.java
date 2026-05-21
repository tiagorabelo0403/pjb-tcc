package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Service
public class BnmpIntegracaoService {

    private static final Logger log = LoggerFactory.getLogger(BnmpIntegracaoService.class);
    private static final String RESOURCE_TYPE = "BNMP_INTEGRACAO";
    private static final String DOMAIN_BNMP_MANDADO = "BNMP_MANDADO_REGISTRO";
    private static final String DOMAIN_BNMP_NUMERO = "BNMP_NUMERO_REGISTRO";
    private static final int TIMEOUT_SEG = 15;

    public enum StatusMandadoBnmp {
        PENDENTE_REGISTRO,
        REGISTRADO_BNMP,
        CUMPRIDO_BNMP,
        REVOGADO_BNMP,
        SUSPENSO_BNMP,
        ERRO_REGISTRO
    }

    public enum TipoMandadoPrisao {
        PREVENTIVA,
        TEMPORARIA,
        CONDENATORIA,
        CIVIL,
        MEDIDA_CAUTELAR_DETENTIVA
    }

    public record MandadoPrisaoPjb(
            String mandadoUuid,
            Long processoId,
            String numeroUnificado,
            TipoMandadoPrisao tipo,
            @JsonIgnore String nomePreso,
            @JsonIgnore String cpfPreso,
            String rg,
            String dataExpedicao,
            String juizExpedidor,
            String tribunalCodigo,
            String comarcaUf,
            String fundamentoLegal,
            boolean urgente,
            String hashIntegridade
    ) {
    }

    public record RegistroBnmp(
            String registroUuid,
            String mandadoUuid,
            Long processoId,
            String numeroBnmp,
            StatusMandadoBnmp status,
            Instant registradoEm,
            Instant atualizadoEm,
            String hashPjb,
            String hashBnmp,
            PjbHardwareSecurityModule.AssinaturaHsm assinatura,
            boolean sincronizado
    ) {
    }

    public record EventoCumprimentoBnmp(
            String eventoUuid,
            String numeroBnmp,
            Long processoId,
            String responsavelCumprimento,
            String orgaoCumpridor,
            Instant cumprimentoEm,
            String localCumprimento,
            String observacoes
    ) {
    }

    private final HttpClient httpClient;
    private final PjbHardwareSecurityModule hsm;
    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final ComunicacaoJudicialStateStore stateStore;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final Cache<String, RegistroBnmp> registrosPorMandado = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(Duration.ofHours(24))
            .build();
    private final Cache<String, RegistroBnmp> registrosPorNumeroBnmp = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    @Value("${pjb.bnmp.base-url:https://bnmp.cnj.jus.br/api/v1}")
    private String bnmpBaseUrl;

    @Value("${pjb.bnmp.enabled:false}")
    private boolean bnmpEnabled;

    @Value("${pjb.bnmp.token:}")
    private String bnmpToken;

    public BnmpIntegracaoService(@Qualifier("hsmInterceptacaoHttpClient") HttpClient httpClient,
                                 PjbHardwareSecurityModule hsm,
                                 ProcessoRepository processoRepository,
                                 AuditLedgerService auditLedger,
                                 NotificacaoInteligentePJB notificacaoEngine,
                                 ComunicacaoJudicialStateStore stateStore,
                                 PjbExecutionOrchestrator executionOrchestrator) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    @Transactional
    public RegistroBnmp registrar(MandadoPrisaoPjb mandado) {
        Objects.requireNonNull(mandado, "mandado");
        return executionOrchestrator.supply(PjbExecutionDescriptor.externalIo("bnmp-integracao.registrar", Duration.ofSeconds(TIMEOUT_SEG)), () -> {
            String payloadJson = serializarMandado(mandado);
            byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
            PjbHardwareSecurityModule.AssinaturaHsm assinatura = hsm.assinar(payloadBytes);
            String numeroBnmp = bnmpEnabled ? chamarApiBnmpRegistro(payloadJson) : "BNMP-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            StatusMandadoBnmp status = numeroBnmp != null ? StatusMandadoBnmp.REGISTRADO_BNMP : StatusMandadoBnmp.ERRO_REGISTRO;
            String hashBnmp = numeroBnmp != null ? sha256Hex(numeroBnmp + mandado.processoId()) : null;
            RegistroBnmp registro = new RegistroBnmp(
                    UUID.randomUUID().toString(),
                    mandado.mandadoUuid(),
                    mandado.processoId(),
                    numeroBnmp,
                    status,
                    Instant.now(),
                    Instant.now(),
                    mandado.hashIntegridade(),
                    hashBnmp,
                    assinatura,
                    numeroBnmp != null
            );
            persistirRegistro(registro);
            auditLedger.appendSafely(
                    status == StatusMandadoBnmp.REGISTRADO_BNMP ? "BNMP_MANDADO_REGISTRADO" : "BNMP_REGISTRO_FALHOU",
                    RESOURCE_TYPE,
                    mandado.mandadoUuid(),
                    registro.hashPjb(),
                    "BNMP %s. Numero: %s. Processo: %s".formatted(status, numeroBnmp, mandado.numeroUnificado())
            );
            log.info("[BNMP] Mandado {}. processo={} numeroBnmp={}", status, mandado.processoId(), numeroBnmp);
            return registro;
        }).join();
    }

    @Transactional
    public RegistroBnmp processarCumprimento(EventoCumprimentoBnmp evento) {
        Objects.requireNonNull(evento, "evento");
        return executionOrchestrator.supply(PjbExecutionDescriptor.externalIo("bnmp-integracao.processar-cumprimento", Duration.ofSeconds(TIMEOUT_SEG)), () -> {
            RegistroBnmp registro = consultarPorNumeroBnmp(evento.numeroBnmp()).orElse(null);
            if (registro == null) {
                log.warn("[BNMP] Cumprimento recebido para numero nao registrado localmente: {}", evento.numeroBnmp());
                return null;
            }
            RegistroBnmp atualizado = new RegistroBnmp(
                    registro.registroUuid(),
                    registro.mandadoUuid(),
                    registro.processoId(),
                    registro.numeroBnmp(),
                    StatusMandadoBnmp.CUMPRIDO_BNMP,
                    registro.registradoEm(),
                    Instant.now(),
                    registro.hashPjb(),
                    registro.hashBnmp(),
                    registro.assinatura(),
                    true
            );
            persistirRegistro(atualizado);
            auditLedger.appendSafely(
                    "BNMP_MANDADO_CUMPRIDO",
                    RESOURCE_TYPE,
                    registro.mandadoUuid(),
                    sha256Hex(evento.numeroBnmp() + evento.cumprimentoEm().toEpochMilli()),
                    "Cumprimento externo via BNMP. Orgao: %s. Local: %s".formatted(evento.orgaoCumpridor(), evento.localCumprimento())
            );
            notificarJuizCumprimento(registro.processoId(), evento);
            log.info("[BNMP] Cumprimento processado. numeroBnmp={} processo={}", evento.numeroBnmp(), registro.processoId());
            return atualizado;
        }).join();
    }

    @Transactional
    public void revogar(String mandadoUuid, String motivo) {
        Objects.requireNonNull(mandadoUuid, "mandadoUuid");
        RegistroBnmp registro = consultarPorMandado(mandadoUuid).orElse(null);
        if (registro == null) {
            return;
        }
        if (bnmpEnabled && registro.numeroBnmp() != null) {
            chamarApiBnmpRevogacao(registro.numeroBnmp(), motivo);
        }
        RegistroBnmp revogado = new RegistroBnmp(
                registro.registroUuid(),
                registro.mandadoUuid(),
                registro.processoId(),
                registro.numeroBnmp(),
                StatusMandadoBnmp.REVOGADO_BNMP,
                registro.registradoEm(),
                Instant.now(),
                registro.hashPjb(),
                registro.hashBnmp(),
                registro.assinatura(),
                true
        );
        persistirRegistro(revogado);
        auditLedger.appendSafely("BNMP_MANDADO_REVOGADO", RESOURCE_TYPE, mandadoUuid, sha256Hex(mandadoUuid + motivo), "Revogacao: " + motivo);
    }

    public Optional<RegistroBnmp> consultarPorMandado(String mandadoUuid) {
        RegistroBnmp cache = registrosPorMandado.getIfPresent(mandadoUuid);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<RegistroBnmp> persisted = stateStore.find(DOMAIN_BNMP_MANDADO, mandadoUuid, RegistroBnmp.class);
        persisted.ifPresent(this::cacheRegistro);
        return persisted;
    }

    public Optional<RegistroBnmp> consultarPorNumeroBnmp(String numeroBnmp) {
        RegistroBnmp cache = registrosPorNumeroBnmp.getIfPresent(numeroBnmp);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<RegistroBnmp> persisted = stateStore.find(DOMAIN_BNMP_NUMERO, numeroBnmp, RegistroBnmp.class);
        persisted.ifPresent(this::cacheRegistro);
        return persisted;
    }

    public Map<String, Long> estatisticas() {
        Map<String, Long> stats = new LinkedHashMap<>();
        List<RegistroBnmp> registros = todosRegistros();
        for (StatusMandadoBnmp status : StatusMandadoBnmp.values()) {
            long count = registros.stream().filter(r -> r.status() == status).count();
            stats.put(status.name(), count);
        }
        stats.put("TOTAL", (long) registros.size());
        return Map.copyOf(stats);
    }

    private void persistirRegistro(RegistroBnmp registro) {
        cacheRegistro(registro);
        stateStore.save(
                DOMAIN_BNMP_MANDADO,
                registro.mandadoUuid(),
                registro.numeroBnmp(),
                registro,
                registro.processoId(),
                null,
                registro.mandadoUuid(),
                registro.status().name()
        );
        if (registro.numeroBnmp() != null && !registro.numeroBnmp().isBlank()) {
            stateStore.save(
                    DOMAIN_BNMP_NUMERO,
                    registro.numeroBnmp(),
                    registro.mandadoUuid(),
                    registro,
                    registro.processoId(),
                    null,
                    registro.mandadoUuid(),
                    registro.status().name()
            );
        }
    }

    private void cacheRegistro(RegistroBnmp registro) {
        registrosPorMandado.put(registro.mandadoUuid(), registro);
        if (registro.numeroBnmp() != null && !registro.numeroBnmp().isBlank()) {
            registrosPorNumeroBnmp.put(registro.numeroBnmp(), registro);
        }
    }

    private List<RegistroBnmp> todosRegistros() {
        Set<String> vistos = new HashSet<>();
        Map<String, RegistroBnmp> consolidados = new LinkedHashMap<>();
        for (RegistroBnmp registro : stateStore.findAll(DOMAIN_BNMP_MANDADO, RegistroBnmp.class)) {
            if (vistos.add(registro.mandadoUuid())) {
                consolidados.put(registro.mandadoUuid(), registro);
            }
        }
        for (RegistroBnmp registro : registrosPorMandado.asMap().values()) {
            if (vistos.add(registro.mandadoUuid())) {
                consolidados.put(registro.mandadoUuid(), registro);
            }
        }
        return List.copyOf(consolidados.values());
    }

    private String chamarApiBnmpRegistro(String payloadJson) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(bnmpBaseUrl + "/mandados"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bnmpToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEG))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return extrairNumeroBnmp(resp.body());
            }
            log.warn("[BNMP] Registro falhou. status={}", resp.statusCode());
            return null;
        } catch (Exception e) {
            log.error("[BNMP] Erro ao registrar. {}", e.getMessage());
            return null;
        }
    }

    private void chamarApiBnmpRevogacao(String numeroBnmp, String motivo) {
        try {
            String body = "{\"motivo\":\"" + escapeJson(motivo) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(bnmpBaseUrl + "/mandados/" + numeroBnmp + "/revogar"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bnmpToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEG))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.error("[BNMP] Erro ao revogar numeroBnmp={}. {}", numeroBnmp, e.getMessage());
        }
    }

    private String extrairNumeroBnmp(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        int idx = responseBody.indexOf("\"numeroBnmp\":");
        if (idx < 0) {
            return "BNMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        int inicio = responseBody.indexOf('"', idx + 13) + 1;
        int fim = responseBody.indexOf('"', inicio);
        if (inicio > 0 && fim > inicio) {
            return responseBody.substring(inicio, fim);
        }
        return null;
    }

    private String serializarMandado(MandadoPrisaoPjb m) {
        return "{" +
                "\"processoId\":" + m.processoId() + ',' +
                "\"numeroUnificado\":\"" + escapeJson(m.numeroUnificado()) + "\"," +
                "\"tipo\":\"" + m.tipo() + "\"," +
                "\"nomePreso\":\"" + escapeJson(m.nomePreso()) + "\"," +
                "\"cpfPreso\":\"" + escapeJson(m.cpfPreso()) + "\"," +
                "\"fundamentoLegal\":\"" + escapeJson(m.fundamentoLegal()) + "\"," +
                "\"urgente\":" + m.urgente() +
                '}';
    }

    private void notificarJuizCumprimento(Long processoId, EventoCumprimentoBnmp evento) {
        processoRepository.findProcessoCompletoById(processoId).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            notificacaoEngine.enviarNotificacao(notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    processoId,
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.ALTA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            ));
        });
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
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
