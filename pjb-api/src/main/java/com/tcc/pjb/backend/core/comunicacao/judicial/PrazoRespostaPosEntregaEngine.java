package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrazoRespostaPosEntregaEngine {

    private static final Logger log = LoggerFactory.getLogger(PrazoRespostaPosEntregaEngine.class);
    private static final String RESOURCE_TYPE = "PRAZO_RESPOSTA";
    private static final String DOMAIN_PRAZO = "PRAZO_RESPOSTA_ATIVO";
    private static final String DOMAIN_SUSPENSAO = "PRAZO_RESPOSTA_SUSPENSAO";
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final List<String> STATUS_PRAZO_ATIVO = List.of(
            StatusPrazoResposta.PRAZO_INICIADO.name(),
            StatusPrazoResposta.PRAZO_RETOMADO.name(),
            StatusPrazoResposta.PRAZO_SUSPENSO.name()
    );
    private static final int MAX_PRAZO_CACHE = 20_000;
    private static final int MAX_SUSPENSAO_CACHE = 10_000;
    private static final long TERMINAL_CACHE_TTL_SECONDS = 172_800L;

    public enum StatusPrazoResposta {
        AGUARDANDO_ENTREGA,
        PRAZO_INICIADO,
        PRAZO_SUSPENSO,
        PRAZO_RETOMADO,
        PRAZO_VENCIDO,
        RESPOSTA_PROTOCOLADA,
        REBELDIA_CONFIGURADA
    }

    public enum MotorMultiplicador {
        NORMAL(1),
        DOBRO_FAZENDA_PUBLICA(2),
        DOBRO_DEFENSORIA(2),
        QUADRUPLO_ESPECIAL(4);

        private final int fator;

        MotorMultiplicador(int fator) {
            this.fator = fator;
        }

        public int getFator() {
            return fator;
        }
    }

    public record PrazoResposta(
            String prazoUuid,
            String expedicaoUuid,
            Long processoId,
            String numeroUnificado,
            NationalPrazoEngine.TipoPrazo tipoPrazo,
            LocalDate inicioEm,
            LocalDate vencimentoEm,
            int diasTotais,
            int diasUteis,
            boolean emDiasUteis,
            MotorMultiplicador multiplicador,
            StatusPrazoResposta status,
            String fundamentoLegal,
            String tribunalCodigo,
            GrauJurisdicao grau,
            RamoDireito ramo,
            String hashIntegridade
    ) {
        public LocalDate vencimento() {
            return vencimentoEm();
        }
    }

    public record SuspensaoPrazo(
            String prazoUuid,
            Instant suspensaEm,
            Instant retomadaEm,
            String motivoSuspensao,
            int diasSuspensos
    ) {
    }

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final ProcessoRepository processoRepository;
    private final NationalPrazoEngine prazoEngine;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final ComunicacaoJudicialStateStore stateStore;
    private final Map<String, PrazoResposta> prazosPorExpedicao = new ConcurrentHashMap<>();
    private final Map<String, SuspensaoPrazo> suspensoesPorPrazo = new ConcurrentHashMap<>();
    private final Map<String, Instant> prazoTouch = new ConcurrentHashMap<>();
    private final Map<String, Instant> suspensaoTouch = new ConcurrentHashMap<>();

    public PrazoRespostaPosEntregaEngine(ExpedicaoJudicialRepository expedicaoRepository,
                                         ProcessoRepository processoRepository,
                                         NationalPrazoEngine prazoEngine,
                                         AuditLedgerService auditLedger,
                                         NotificacaoInteligentePJB notificacaoEngine,
                                         ComunicacaoJudicialStateStore stateStore) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @Transactional
    public PrazoResposta iniciarPrazoAposEntrega(String expedicaoUuid,
                                                 TipoUsuario tipoDestinatario,
                                                 NationalPrazoEngine.TipoPrazo tipoPrazo) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(tipoPrazo, "tipoPrazo");
        PrazoResposta existente = consultarPorExpedicao(expedicaoUuid).orElse(null);
        if (existente != null) {
            return existente;
        }
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + expedicaoUuid));
        if (!expedicao.isEntregueOuLida()) {
            throw new IllegalStateException("Prazo não pode ser iniciado - expedição ainda não entregue: " + expedicaoUuid);
        }
        MotorMultiplicador multiplicador = resolverMultiplicador(tipoDestinatario);
        RamoDireito ramo = expedicao.getRamoDireito() != null ? RamoDireito.valueOf(expedicao.getRamoDireito()) : RamoDireito.CIVIL;
        GrauJurisdicao grau = expedicao.getGrauJurisdicao() != null ? GrauJurisdicao.valueOf(expedicao.getGrauJurisdicao()) : GrauJurisdicao.PRIMEIRO_GRAU;
        LocalDate inicio = LocalDate.ofInstant(
                expedicao.getPrazoRespostaInicioEm() != null ? expedicao.getPrazoRespostaInicioEm() : Instant.now(),
                ZONE
        );
        NationalPrazoEngine.PrazoCalculado prazoCalculado = prazoEngine.calcularPorRamo(
                inicio,
                tipoPrazo,
                ramo,
                grau,
                expedicao.getInstanciaExpedidora()
        );
        LocalDate vencimento = prazoCalculado.vencimento();
        if (multiplicador.getFator() > 1) {
            int diasBase = Math.max(0, (int) ChronoUnit.DAYS.between(inicio, vencimento));
            vencimento = inicio.plusDays((long) diasBase * multiplicador.getFator());
            if (tipoPrazo.emDiasUteis) {
                vencimento = ajustarParaDiaUtil(vencimento);
            }
        }
        int diasTotais = Math.max(0, (int) ChronoUnit.DAYS.between(inicio, vencimento));
        String uuid = UUID.randomUUID().toString();
        String hash = sha256Hex(uuid + expedicaoUuid + inicio + vencimento);
        PrazoResposta prazo = new PrazoResposta(
                uuid,
                expedicaoUuid,
                expedicao.getProcessoId(),
                expedicao.getNumeroUnificado(),
                tipoPrazo,
                inicio,
                vencimento,
                diasTotais,
                tipoPrazo.diasPadrao * multiplicador.getFator(),
                tipoPrazo.emDiasUteis,
                multiplicador,
                StatusPrazoResposta.PRAZO_INICIADO,
                prazoCalculado.fundamentoLegal(),
                expedicao.getInstanciaExpedidora(),
                grau,
                ramo,
                hash
        );
        persistirPrazo(prazo);
        auditLedger.appendSafely(
                "PRAZO_RESPOSTA_INICIADO",
                RESOURCE_TYPE,
                expedicaoUuid,
                hash,
                "Prazo %s iniciado em %s, vencimento %s. Multiplicador: %s".formatted(tipoPrazo, inicio, vencimento, multiplicador)
        );
        log.info(
                "[PrazoResposta] Prazo iniciado. expedicao={} tipo={} vencimento={} mult={}",
                expedicaoUuid,
                tipoPrazo,
                vencimento,
                multiplicador
        );
        return prazo;
    }

    @Transactional
    public PrazoResposta suspenderPrazo(String prazoUuid, String motivo) {
        Objects.requireNonNull(prazoUuid, "prazoUuid");
        PrazoResposta prazo = buscarPrazoPorUuid(prazoUuid);
        SuspensaoPrazo suspensao = new SuspensaoPrazo(prazoUuid, Instant.now(), null, motivo, 0);
        persistirSuspensao(suspensao, prazo);
        PrazoResposta suspenso = new PrazoResposta(
                prazo.prazoUuid(),
                prazo.expedicaoUuid(),
                prazo.processoId(),
                prazo.numeroUnificado(),
                prazo.tipoPrazo(),
                prazo.inicioEm(),
                prazo.vencimentoEm(),
                prazo.diasTotais(),
                prazo.diasUteis(),
                prazo.emDiasUteis(),
                prazo.multiplicador(),
                StatusPrazoResposta.PRAZO_SUSPENSO,
                prazo.fundamentoLegal(),
                prazo.tribunalCodigo(),
                prazo.grau(),
                prazo.ramo(),
                prazo.hashIntegridade()
        );
        persistirPrazo(suspenso);
        auditLedger.appendSafely(
                "PRAZO_SUSPENSO",
                RESOURCE_TYPE,
                prazoUuid,
                sha256Hex(prazoUuid + motivo),
                "Suspensão: " + motivo
        );
        return suspenso;
    }

    @Transactional
    public PrazoResposta retomar(String prazoUuid) {
        Objects.requireNonNull(prazoUuid, "prazoUuid");
        PrazoResposta prazo = buscarPrazoPorUuid(prazoUuid);
        SuspensaoPrazo suspensao = consultarSuspensao(prazoUuid).orElse(null);
        if (suspensao == null) {
            throw new IllegalStateException("Prazo não está suspenso: " + prazoUuid);
        }
        long diasSuspensos = ChronoUnit.DAYS.between(
                suspensao.suspensaEm().atZone(ZONE).toLocalDate(),
                LocalDate.now(ZONE)
        );
        LocalDate novoVencimento = ajustarParaDiaUtil(prazo.vencimentoEm().plusDays(Math.max(0, diasSuspensos)));
        suspensoesPorPrazo.remove(prazoUuid);
        suspensaoTouch.remove(prazoUuid);
        stateStore.delete(DOMAIN_SUSPENSAO, prazoUuid);
        String hash = sha256Hex(prazoUuid + novoVencimento);
        PrazoResposta retomado = new PrazoResposta(
                prazo.prazoUuid(),
                prazo.expedicaoUuid(),
                prazo.processoId(),
                prazo.numeroUnificado(),
                prazo.tipoPrazo(),
                prazo.inicioEm(),
                novoVencimento,
                Math.max(0, (int) ChronoUnit.DAYS.between(prazo.inicioEm(), novoVencimento)),
                prazo.diasUteis(),
                prazo.emDiasUteis(),
                prazo.multiplicador(),
                StatusPrazoResposta.PRAZO_RETOMADO,
                prazo.fundamentoLegal(),
                prazo.tribunalCodigo(),
                prazo.grau(),
                prazo.ramo(),
                hash
        );
        persistirPrazo(retomado);
        auditLedger.appendSafely(
                "PRAZO_RETOMADO",
                RESOURCE_TYPE,
                prazoUuid,
                hash,
                "Retomado após %d dias suspenso. Novo vencimento: %s".formatted(diasSuspensos, novoVencimento)
        );
        return retomado;
    }

    public Optional<PrazoResposta> consultarPorExpedicao(String expedicaoUuid) {
        PrazoResposta cache = prazosPorExpedicao.get(expedicaoUuid);
        if (cache != null) {
            touchPrazo(expedicaoUuid, Instant.now());
            return Optional.of(cache);
        }
        Optional<PrazoResposta> persisted = stateStore.find(DOMAIN_PRAZO, expedicaoUuid, PrazoResposta.class);
        persisted.ifPresent(prazo -> {
            prazosPorExpedicao.put(expedicaoUuid, prazo);
            touchPrazo(expedicaoUuid, Instant.now());
            trimCachesIfNeeded();
        });
        return persisted;
    }

    public List<PrazoResposta> listarPrazos() {
        return todosPrazos();
    }

    @PjbClusterSingletonTask(key = "prazo-resposta-pos-entrega", ttl = "PT2M")
    @Scheduled(fixedDelay = 300_000)
    public void monitorarVencimentos() {
        trimCachesIfNeeded();
        LocalDate hoje = LocalDate.now(ZONE);
        prazosAtivos().stream()
                .filter(p -> p.status() == StatusPrazoResposta.PRAZO_INICIADO || p.status() == StatusPrazoResposta.PRAZO_RETOMADO)
                .filter(p -> !p.vencimentoEm().isAfter(hoje))
                .forEach(p -> {
                    log.warn("[PrazoResposta] Vencimento atingido. expedicao={} processo={}", p.expedicaoUuid(), p.processoId());
                    PrazoResposta vencido = new PrazoResposta(
                            p.prazoUuid(),
                            p.expedicaoUuid(),
                            p.processoId(),
                            p.numeroUnificado(),
                            p.tipoPrazo(),
                            p.inicioEm(),
                            p.vencimentoEm(),
                            p.diasTotais(),
                            p.diasUteis(),
                            p.emDiasUteis(),
                            p.multiplicador(),
                            StatusPrazoResposta.PRAZO_VENCIDO,
                            p.fundamentoLegal(),
                            p.tribunalCodigo(),
                            p.grau(),
                            p.ramo(),
                            p.hashIntegridade()
                    );
                    persistirPrazo(vencido);
                    auditLedger.appendSafely(
                            "PRAZO_RESPOSTA_VENCIDO",
                            RESOURCE_TYPE,
                            p.prazoUuid(),
                            p.hashIntegridade(),
                            "Prazo vencido em " + p.vencimentoEm()
                    );
                    processoRepository.findProcessoCompletoById(p.processoId()).ifPresent(processo -> {
                        if (processo.getUsuario() != null) {
                            notificacaoEngine.enviarNotificacao(notificacaoEngine.construir(
                                    processo.getUsuario().getId(),
                                    p.processoId(),
                                    NotificacaoInteligentePJB.TipoAlerta.PRAZO_VENCIDO,
                                    NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA,
                                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
                            ));
                        }
                    });
                });
    }

    private PrazoResposta buscarPrazoPorUuid(String prazoUuid) {
        return todosPrazos().stream()
                .filter(p -> p.prazoUuid().equals(prazoUuid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Prazo não encontrado: " + prazoUuid));
    }



    private Optional<SuspensaoPrazo> consultarSuspensao(String prazoUuid) {
        SuspensaoPrazo cache = suspensoesPorPrazo.get(prazoUuid);
        if (cache != null) {
            touchSuspensao(prazoUuid, Instant.now());
            return Optional.of(cache);
        }
        Optional<SuspensaoPrazo> persisted = stateStore.find(DOMAIN_SUSPENSAO, prazoUuid, SuspensaoPrazo.class);
        persisted.ifPresent(suspensao -> {
            suspensoesPorPrazo.put(prazoUuid, suspensao);
            touchSuspensao(prazoUuid, Instant.now());
            trimCachesIfNeeded();
        });
        return persisted;
    }

    private List<PrazoResposta> todosPrazos() {
        Map<String, PrazoResposta> consolidados = new LinkedHashMap<>();
        stateStore.findAll(DOMAIN_PRAZO, PrazoResposta.class).forEach(prazo -> consolidados.put(prazo.expedicaoUuid(), prazo));
        prazosPorExpedicao.values().forEach(prazo -> consolidados.put(prazo.expedicaoUuid(), prazo));
        return List.copyOf(consolidados.values());
    }

    private List<PrazoResposta> prazosAtivos() {
        Map<String, PrazoResposta> consolidados = new LinkedHashMap<>();
        stateStore.findByStatusCodes(DOMAIN_PRAZO, STATUS_PRAZO_ATIVO, PrazoResposta.class).forEach(prazo -> consolidados.put(prazo.expedicaoUuid(), prazo));
        prazosPorExpedicao.values().stream()
                .filter(this::isPrazoAtivo)
                .forEach(prazo -> consolidados.put(prazo.expedicaoUuid(), prazo));
        return List.copyOf(consolidados.values());
    }

    private boolean isPrazoAtivo(PrazoResposta prazo) {
        return prazo != null && (prazo.status() == StatusPrazoResposta.PRAZO_INICIADO
                || prazo.status() == StatusPrazoResposta.PRAZO_RETOMADO
                || prazo.status() == StatusPrazoResposta.PRAZO_SUSPENSO);
    }

    private void persistirPrazo(PrazoResposta prazo) {
        prazosPorExpedicao.put(prazo.expedicaoUuid(), prazo);
        touchPrazo(prazo.expedicaoUuid(), Instant.now());
        trimCachesIfNeeded();
        stateStore.save(DOMAIN_PRAZO, prazo.expedicaoUuid(), prazo.prazoUuid(), prazo, prazo.processoId(), prazo.expedicaoUuid(), null, prazo.status().name());
    }

    private void persistirSuspensao(SuspensaoPrazo suspensao, PrazoResposta prazo) {
        suspensoesPorPrazo.put(suspensao.prazoUuid(), suspensao);
        touchSuspensao(suspensao.prazoUuid(), Instant.now());
        trimCachesIfNeeded();
        stateStore.save(DOMAIN_SUSPENSAO, suspensao.prazoUuid(), prazo.expedicaoUuid(), suspensao, prazo.processoId(), prazo.expedicaoUuid(), null, StatusPrazoResposta.PRAZO_SUSPENSO.name());
    }

    private MotorMultiplicador resolverMultiplicador(TipoUsuario tipo) {
        if (tipo == null) {
            return MotorMultiplicador.NORMAL;
        }
        return switch (tipo) {
            case SERVIDOR, SERVIDOR_FORUM, PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL -> MotorMultiplicador.DOBRO_FAZENDA_PUBLICA;
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> MotorMultiplicador.DOBRO_DEFENSORIA;
            default -> MotorMultiplicador.NORMAL;
        };
    }

    private void touchPrazo(String expedicaoUuid, Instant now) {
        if (expedicaoUuid != null) {
            prazoTouch.put(expedicaoUuid, now);
        }
    }

    private void touchSuspensao(String prazoUuid, Instant now) {
        if (prazoUuid != null) {
            suspensaoTouch.put(prazoUuid, now);
        }
    }

    private void trimCachesIfNeeded() {
        Instant now = Instant.now();
        Instant terminalCutoff = now.minusSeconds(TERMINAL_CACHE_TTL_SECONDS);
        prazosPorExpedicao.entrySet().removeIf(entry -> {
            PrazoResposta prazo = entry.getValue();
            if (prazo == null || isPrazoAtivo(prazo)) {
                return false;
            }
            Instant touched = prazoTouch.getOrDefault(entry.getKey(), Instant.EPOCH);
            boolean remove = touched.isBefore(terminalCutoff);
            if (remove) {
                prazoTouch.remove(entry.getKey());
            }
            return remove;
        });
        suspensoesPorPrazo.entrySet().removeIf(entry -> {
            Instant touched = suspensaoTouch.getOrDefault(entry.getKey(), Instant.EPOCH);
            boolean remove = touched.isBefore(terminalCutoff);
            if (remove) {
                suspensaoTouch.remove(entry.getKey());
            }
            return remove;
        });
        trimOverflow(prazosPorExpedicao, prazoTouch, MAX_PRAZO_CACHE);
        trimOverflow(suspensoesPorPrazo, suspensaoTouch, MAX_SUSPENSAO_CACHE);
    }

    private <T> void trimOverflow(Map<String, T> store, Map<String, Instant> touches, int maxEntries) {
        int overflow = store.size() - maxEntries;
        if (overflow <= 0) {
            return;
        }
        List<Map.Entry<String, Instant>> entries = new java.util.ArrayList<>(touches.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        for (Map.Entry<String, Instant> entry : entries) {
            if (overflow <= 0) {
                break;
            }
            String key = entry.getKey();
            if (store.remove(key) != null) {
                touches.remove(key, entry.getValue());
                overflow--;
            }
        }
    }

    private LocalDate ajustarParaDiaUtil(LocalDate data) {
        LocalDate ajustada = data;
        while (ajustada.getDayOfWeek().getValue() >= 6) {
            ajustada = ajustada.plusDays(1);
        }
        return ajustada;
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
