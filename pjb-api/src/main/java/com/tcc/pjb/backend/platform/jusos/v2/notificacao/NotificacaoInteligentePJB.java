package com.tcc.pjb.backend.platform.jusos.v2.notificacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.PrazoCalculado;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.TipoPrazo;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Service
public class NotificacaoInteligentePJB {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoInteligentePJB.class);
    private static final String RESOURCE_TYPE = "NOTIFICACAO_JUSOS";
    private static final int PAGE_SIZE = 200;
    private static final EnumSet<StatusProcesso> STATUS_PRAZO_SCAN_IGNORADOS = EnumSet.of(StatusProcesso.BAIXADO, StatusProcesso.ARQUIVADO, StatusProcesso.TRANSITO_EM_JULGADO);
    private static final long JANELA_SUPRESSAO_SEGUNDOS = 1_800L;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public enum CanalNotificacao {
        PUSH_APP_PJB,
        EMAIL_CERTIFICADO,
        WHATSAPP_GOVBR,
        DJE_DIARIO_ELETRONICO,
        DOMICILIO_JUDICIAL_ELETRONICO,
        WEBHOOK_SISTEMA_EXTERNO,
        SMS_AUTENTICADO,
        INTIMACAO_DIGITAL_MNI
    }

    public enum TipoAlerta {
        PRAZO_VENCENDO_24H,
        PRAZO_VENCENDO_48H,
        PRAZO_VENCENDO_SEMANA,
        PRAZO_VENCIDO,
        MOVIMENTACAO_NOVA,
        DECISAO_PUBLICADA,
        AUDIENCIA_AMANHA,
        ACORDO_PENDENTE_ASSINATURA,
        DOCUMENTO_JUNTADO,
        PROCESSO_DISTRIBUIDO,
        INTIMACAO_PENDENTE_LEITURA,
        ALERTA_PRESCRICAO,
        ALERTA_SIGILO_ALTERADO,
        HABILITACAO_APROVADA,
        ERRO_PROTOCOLO,
        ALERTA_BACKLOG_CRITICO,
        ALERTA_REGRA_CRITICA,
        ALERTA_PRIORIDADE_INFANCIA,
        ALERTA_PRIORIDADE_PENAL
    }

    public enum UrgenciaMensagem {
        CRITICA,
        ALTA,
        MEDIA,
        BAIXA,
        INFORMATIVA
    }

    public record NotificacaoPJB(
            UUID notificacaoId,
            Long usuarioId,
            Long processoId,
            String numeroUnificado,
            TipoAlerta tipo,
            UrgenciaMensagem urgencia,
            CanalNotificacao canal,
            String titulo,
            String corpo,
            String acaoSugerida,
            String linkDeepLink,
            Map<String, String> metadados,
            Instant geradaEm,
            Instant expiresAt,
            boolean lida,
            boolean entregue
    ) {
        public String chaveSupressao() {
            return (usuarioId == null ? 0L : usuarioId)
                    + ":" + (processoId == null ? 0L : processoId)
                    + ":" + (tipo == null ? "GERAL" : tipo.name())
                    + ":" + (canal == null ? "CANAL_PADRAO" : canal.name());
        }
    }

    public record AlertaPrazoProativo(
            Long processoId,
            String numeroUnificado,
            TipoAlerta tipoAlerta,
            LocalDate vencimento,
            int diasRestantes,
            String descricaoPrazo,
            String fundamentoLegal,
            List<Long> usuariosParaNotificar,
            UrgenciaMensagem urgencia,
            List<String> tags,
            String racional
    ) {}

    public record ConfiguracaoNotificacao(
            Long usuarioId,
            List<CanalNotificacao> canaisAtivos,
            List<TipoAlerta> alertasAtivos,
            int antecedenciaPrazoHoras,
            boolean receberDje,
            boolean receberWhatsappGov,
            boolean receberPush,
            boolean receberSomenteAltaPrioridade,
            boolean aceitarDigestOperacional
    ) {
        public static ConfiguracaoNotificacao padrao(Long usuarioId) {
            return new ConfiguracaoNotificacao(
                    usuarioId,
                    List.of(CanalNotificacao.PUSH_APP_PJB, CanalNotificacao.EMAIL_CERTIFICADO),
                    List.of(
                            TipoAlerta.PRAZO_VENCENDO_24H,
                            TipoAlerta.PRAZO_VENCENDO_48H,
                            TipoAlerta.PRAZO_VENCENDO_SEMANA,
                            TipoAlerta.PRAZO_VENCIDO,
                            TipoAlerta.MOVIMENTACAO_NOVA,
                            TipoAlerta.DECISAO_PUBLICADA,
                            TipoAlerta.AUDIENCIA_AMANHA,
                            TipoAlerta.ALERTA_PRESCRICAO,
                            TipoAlerta.ALERTA_REGRA_CRITICA,
                            TipoAlerta.ALERTA_PRIORIDADE_INFANCIA,
                            TipoAlerta.ALERTA_PRIORIDADE_PENAL
                    ),
                    48,
                    false,
                    false,
                    true,
                    false,
                    true
            );
        }
    }

    private record PrazoScanContext(LocalDate hoje,
                                    LocalDate horizonte,
                                    Long actorId,
                                    Map<String, List<Usuario>> operadoresPorComarca) {
    }

    public record PainelNotificacao(
            long processosVarredidos,
            long alertasGerados,
            long notificacoesPreparadas,
            long notificacoesEntregues,
            long notificacoesSuprimidas,
            long alertasCriticos,
            Instant geradoEm,
            List<String> destaques,
            String hashIntegridade
    ) {}

    public record ProcessoNotificacaoTriggerEvent(
            Long processoId,
            TipoAlerta tipo,
            String mensagem,
            boolean critico,
            Instant ocorridoEm
    ) {}

    public record NotificacaoEmitidaEvent(
            UUID notificacaoId,
            Long processoId,
            Long usuarioId,
            TipoAlerta tipo,
            Instant emitidaEm,
            boolean entregue,
            String hashIntegridade
    ) {}

    private final NotificationService notificationService;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NationalPrazoEngine prazoEngine;
    private static final int MAX_SUPRESSAO_CACHE = 50_000;

    private final AuditLedgerService auditLedger;
    private final CurrentUserService currentUserService;
    private static final java.time.Duration NOTIFICACAO_SCAN_TIMEOUT = java.time.Duration.ofSeconds(10);
    private static final java.time.Duration NOTIFICACAO_ENVIO_TIMEOUT = java.time.Duration.ofSeconds(5);

    private final UiHistoryService uiHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final NationalRulePackEngine rulePackEngine;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;
    private final Map<String, Instant> supressaoCache = new ConcurrentHashMap<>();
    private final Map<Long, ConfiguracaoNotificacao> configuracoes = new ConcurrentHashMap<>();

    public NotificacaoInteligentePJB(
            NotificationService notificationService,
            ProcessoRepository processoRepository,
            UsuarioRepository usuarioRepository,
            NationalPrazoEngine prazoEngine,
            AuditLedgerService auditLedger,
            CurrentUserService currentUserService,
            UiHistoryService uiHistoryService,
            ApplicationEventPublisher eventPublisher,
            NationalRulePackEngine rulePackEngine,
            PjbExecutionOrchestrator executionOrchestrator,
            PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport
    ) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.rulePackEngine = Objects.requireNonNull(rulePackEngine, "rulePackEngine");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        this.processoSigiloRlsEntryPointSupport = Objects.requireNonNull(processoSigiloRlsEntryPointSupport, "processoSigiloRlsEntryPointSupport");
    }

    public CompletableFuture<List<AlertaPrazoProativo>> varrerPrazosVencendo(int diasHorizonte) {
        int horizonte = Math.max(1, Math.min(diasHorizonte, 30));
        return executionOrchestrator.supply(
                PjbExecutionDescriptor.job("jusos.notificacao.varrer-prazos", NOTIFICACAO_SCAN_TIMEOUT),
                () -> scanAlertasPrazo(horizonte));
    }

    public CompletableFuture<List<NotificacaoPJB>> varrerPrazosEEnviar(int diasHorizonte) {
        int horizonte = Math.max(1, Math.min(diasHorizonte, 30));
        return executionOrchestrator.supply(
                PjbExecutionDescriptor.job("jusos.notificacao.varrer-e-enviar", NOTIFICACAO_SCAN_TIMEOUT),
                () -> {
                    List<AlertaPrazoProativo> alertas = scanAlertasPrazo(horizonte);
                    List<NotificacaoPJB> notificacoes = new ArrayList<>();
                    for (AlertaPrazoProativo alerta : alertas) {
                        for (Long usuarioId : alerta.usuariosParaNotificar()) {
                            ConfiguracaoNotificacao cfg = configuracao(usuarioId);
                            if (!aceitaAlerta(cfg, alerta.tipoAlerta(), alerta.urgencia())) {
                                continue;
                            }
                            NotificacaoPJB notificacao = construirParaAlerta(usuarioId, alerta, cfg);
                            notificacoes.add(notificacao);
                            enviarNotificacao(notificacao);
                        }
                    }
                    registrarPainelInterno(alertas, notificacoes);
                    return List.copyOf(notificacoes);
                });
    }

    public List<AlertaPrazoProativo> varrerPrazosVencendoSync(int diasHorizonte) {
        return scanAlertasPrazo(Math.max(1, Math.min(diasHorizonte, 30)));
    }

    public ConfiguracaoNotificacao salvarConfiguracao(ConfiguracaoNotificacao configuracao) {
        Objects.requireNonNull(configuracao, "configuracao");
        if (configuracao.usuarioId() == null) {
            throw new IllegalArgumentException("usuarioId");
        }
        ConfiguracaoNotificacao normalized = new ConfiguracaoNotificacao(
                configuracao.usuarioId(),
                distinct(configuracao.canaisAtivos()),
                distinct(configuracao.alertasAtivos()),
                Math.max(1, Math.min(configuracao.antecedenciaPrazoHoras(), 168)),
                configuracao.receberDje(),
                configuracao.receberWhatsappGov(),
                configuracao.receberPush(),
                configuracao.receberSomenteAltaPrioridade(),
                configuracao.aceitarDigestOperacional()
        );
        configuracoes.put(configuracao.usuarioId(), normalized);
        registrarObservabilidade(
                "NOTIFICACAO_CONFIG_SALVA",
                "cfg:" + configuracao.usuarioId(),
                sha256Hex(String.valueOf(normalized)),
                "Configuração de notificação atualizada"
        );
        return normalized;
    }

    public ConfiguracaoNotificacao configuracao(Long usuarioId) {
        if (usuarioId == null) {
            return ConfiguracaoNotificacao.padrao(null);
        }
        return configuracoes.computeIfAbsent(usuarioId, ConfiguracaoNotificacao::padrao);
    }

    public NotificacaoPJB construir(Long usuarioId,
                                    Long processoId,
                                    TipoAlerta tipo,
                                    UrgenciaMensagem urgencia,
                                    CanalNotificacao canal) {
        Processo processo = processoId != null ? processoRepository.findProcessoCompletoById(processoId).orElse(null) : null;
        String numero = processo != null ? processo.getNumeroUnificado() : "";
        String titulo = gerarTitulo(tipo, numero);
        String corpo = gerarCorpo(tipo, processo, null);
        String acao = gerarAcao(tipo);
        String deeplink = "/processo/" + (processoId != null ? processoId : "") + resolverSegmentoDeepLink(tipo);
        Instant agora = Instant.now();
        return new NotificacaoPJB(
                UUID.randomUUID(),
                usuarioId,
                processoId,
                numero,
                tipo,
                urgencia == null ? UrgenciaMensagem.MEDIA : urgencia,
                canal == null ? resolverCanalPreferencial(processo, tipo) : canal,
                titulo,
                corpo,
                acao,
                deeplink,
                construirMetadados(processo, tipo, null),
                agora,
                agora.plusSeconds(expiracaoSegundos(tipo, urgencia)),
                false,
                false
        );
    }

        public CompletableFuture<Void> enviarNotificacao(NotificacaoPJB notificacao) {
        Objects.requireNonNull(notificacao, "notificacao");
        return executionOrchestrator.run(
                PjbExecutionDescriptor.externalIo("jusos.notificacao.enviar", NOTIFICACAO_ENVIO_TIMEOUT),
                () -> entregarNotificacao(notificacao));
    }

    private void entregarNotificacao(NotificacaoPJB notificacao) {
        if (notificacao.usuarioId() == null) {
            return;
        }
        if (suprimir(notificacao)) {
            registrarObservabilidade(
                    "NOTIFICACAO_SUPRIMIDA",
                    resourceId(notificacao),
                    sha256Hex(notificacao.chaveSupressao()),
                    "Notificação suprimida por janela anti-ruído"
            );
            return;
        }

        Usuario usuario = usuarioRepository.findById(notificacao.usuarioId()).orElse(null);
        Processo processo = notificacao.processoId() != null
                ? processoRepository.findProcessoCompletoById(notificacao.processoId()).orElse(null)
                : null;
        if (usuario == null || !usuario.isAtivoESemanticoValido()) {
            return;
        }

        try {
            notificationService.notifyUser(
                    usuario,
                    processo,
                    notificacao.titulo(),
                    notificacao.corpo(),
                    notificacao.linkDeepLink()
            );
            registrarEntregaUi(usuario, processo, notificacao, true);
            registrarObservabilidade(
                    "NOTIFICACAO_ENVIADA",
                    resourceId(notificacao),
                    sha256Hex(notificacao.toString()),
                    notificacao.titulo()
            );
            eventPublisher.publishEvent(new NotificacaoEmitidaEvent(
                    notificacao.notificacaoId(),
                    notificacao.processoId(),
                    notificacao.usuarioId(),
                    notificacao.tipo(),
                    Instant.now(),
                    true,
                    sha256Hex(notificacao.toString())
            ));
        } catch (Exception e) {
            registrarEntregaUi(usuario, processo, notificacao, false);
            registrarObservabilidade(
                    "NOTIFICACAO_ERRO",
                    resourceId(notificacao),
                    sha256Hex(notificacao.notificacaoId().toString() + e.getMessage()),
                    e.getMessage()
            );
            log.warn("[Notificacao] Falha ao enviar notificacao={} usuario={} erro={}",
                    notificacao.notificacaoId(), notificacao.usuarioId(), e.getMessage());
        }
    }

    @EventListener
    public void onProcessoTrigger(ProcessoNotificacaoTriggerEvent event) {
        if (event == null || event.processoId() == null || event.tipo() == null) {
            return;
        }
        processoSigiloRlsEntryPointSupport.runWithProcessoContext(event.processoId(), "EVENT", () -> {
            Processo processo = processoRepository.findProcessoCompletoById(event.processoId()).orElse(null);
            if (processo == null) {
                return;
            }
            Set<Long> destinatarios = new LinkedHashSet<>(resolverDestinatarios(processo, event.tipo(), event.critico()));
            for (Long usuarioId : destinatarios) {
                ConfiguracaoNotificacao cfg = configuracao(usuarioId);
                UrgenciaMensagem urgencia = event.critico() ? UrgenciaMensagem.CRITICA : urgenciaPorTipo(event.tipo());
                if (!aceitaAlerta(cfg, event.tipo(), urgencia)) {
                    continue;
                }
                NotificacaoPJB base = construir(usuarioId, processo.getId(), event.tipo(), urgencia, null);
                NotificacaoPJB ajustada = new NotificacaoPJB(
                        base.notificacaoId(),
                        base.usuarioId(),
                        base.processoId(),
                        base.numeroUnificado(),
                        base.tipo(),
                        base.urgencia(),
                        base.canal(),
                        base.titulo(),
                        event.mensagem() == null || event.mensagem().isBlank() ? base.corpo() : event.mensagem(),
                        base.acaoSugerida(),
                        base.linkDeepLink(),
                        base.metadados(),
                        base.geradaEm(),
                        base.expiresAt(),
                        false,
                        false
                );
                enviarNotificacao(ajustada);
            }
        });
    }

    public PainelNotificacao gerarPainelOperacional(int diasHorizonte) {
        List<AlertaPrazoProativo> alertas = scanAlertasPrazo(Math.max(1, Math.min(diasHorizonte, 30)));
        long criticos = alertas.stream().filter(a -> a.urgencia() == UrgenciaMensagem.CRITICA).count();
        List<String> destaques = new ArrayList<>();
        if (criticos > 0) {
            destaques.add("Há " + criticos + " alertas críticos com ação imediata.");
        }
        long vencidos = alertas.stream().filter(a -> a.tipoAlerta() == TipoAlerta.PRAZO_VENCIDO).count();
        if (vencidos > 0) {
            destaques.add("Existem " + vencidos + " prazos potencialmente vencidos.");
        }
        long infancia = alertas.stream().filter(a -> a.tags().contains("INFANCIA")).count();
        if (infancia > 0) {
            destaques.add("Fila prioritária com " + infancia + " alertas de infância/juventude.");
        }
        String hash = sha256Hex(alertas.toString());
        PainelNotificacao painel = new PainelNotificacao(
                contarProcessosAtivos(),
                alertas.size(),
                estimarNotificacoes(alertas),
                0,
                0,
                criticos,
                Instant.now(),
                List.copyOf(destaques),
                hash
        );
        registrarObservabilidade(
                "NOTIFICACAO_PAINEL_GERADO",
                "painel:global",
                hash,
                destaques.isEmpty() ? "Painel de notificação atualizado" : destaques.get(0)
        );
        return painel;
    }

    private List<AlertaPrazoProativo> scanAlertasPrazo(int diasHorizonte) {
        LocalDate hoje = LocalDate.now(ZONE);
        PrazoScanContext context = new PrazoScanContext(
                hoje,
                hoje.plusDays(diasHorizonte),
                currentUserService.currentUserIdOrZero(),
                new HashMap<>()
        );
        List<AlertaPrazoProativo> alertas = new ArrayList<>();
        int page = 0;
        Slice<Processo> lote;
        do {
            lote = processoRepository.findAllForPrazoScan(STATUS_PRAZO_SCAN_IGNORADOS, PageRequest.of(page, PAGE_SIZE));
            for (Processo processo : lote.getContent()) {
                try {
                    alertas.addAll(detectarAlertasPrazo(processo, context));
                } catch (Exception e) {
                    log.warn("[Notificacao] Falha ao verificar prazos processo={}: {}", processo.getId(), e.getMessage());
                }
            }
            page++;
        } while (lote.hasNext());
        return List.copyOf(alertas);
    }

    private List<AlertaPrazoProativo> detectarAlertasPrazo(Processo processo, PrazoScanContext context) {
        List<AlertaPrazoProativo> alertas = new ArrayList<>();
        StatusProcesso status = processo != null ? processo.getStatusProcesso() : null;
        if (processo == null || processo.getRamoDireito() == null || (status != null && STATUS_PRAZO_SCAN_IGNORADOS.contains(status))) {
            return alertas;
        }

        LocalDate base = processo.getDataUltimaMovimentacao() != null
                ? processo.getDataUltimaMovimentacao().toLocalDate()
                : processo.getDataCriacao() != null ? processo.getDataCriacao().toLocalDate() : context.hoje().minusDays(15);
        GrauJurisdicao grau = processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null
                ? processo.getJurisdicao().getGrau()
                : GrauJurisdicao.PRIMEIRO_GRAU;
        String tribunal = processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;
        NationalRulePackEngine.ResultadoRegras regras = detectarCriticidadeRegra(processo, grau, tribunal);

        for (TipoPrazo tipo : priorizarPrazos(processo.getRamoDireito(), status)) {
            try {
                PrazoCalculado prazo = prazoEngine.calcularPorRamo(base, tipo, processo.getRamoDireito(), grau, tribunal);
                if (prazo == null || prazo.vencimento() == null) {
                    continue;
                }
                if (prazo.vencimento().isAfter(context.horizonte()) && !prazo.vencimento().isBefore(context.hoje())) {
                    continue;
                }
                int diasRestantes = (int) java.time.temporal.ChronoUnit.DAYS.between(context.hoje(), prazo.vencimento());
                TipoAlerta tipoAlerta = resolverTipoAlerta(diasRestantes);
                if (tipoAlerta == null) {
                    continue;
                }
                UrgenciaMensagem urgencia = elevarUrgencia(processo, tipoAlerta, diasRestantes, regras);
                List<String> tags = tagsAlerta(processo, tipoAlerta, urgencia);
                alertas.add(new AlertaPrazoProativo(
                        processo.getId(),
                        processo.getNumeroUnificado(),
                        tipoAlerta,
                        prazo.vencimento(),
                        diasRestantes,
                        descricaoPrazo(tipo),
                        prazo.fundamentoLegal(),
                        resolverDestinatarios(processo, tipoAlerta, urgencia == UrgenciaMensagem.CRITICA, context),
                        urgencia,
                        tags,
                        construirRacionalPrazo(processo, prazo, diasRestantes)
                ));
            } catch (Exception ignored) {
            }
        }

        verificarPrescricao(processo, context, alertas);
        verificarBacklogCritico(processo, context, alertas);
        verificarRegrasCriticas(processo, regras, context, alertas);
        verificarPrioridadesEspeciais(processo, context, alertas);
        return alertas;
    }

    private void verificarPrescricao(Processo processo, PrazoScanContext context, List<AlertaPrazoProativo> alertas) {
        if (processo.getDataUltimaMovimentacao() == null || processo.getRamoDireito() == null) {
            return;
        }
        LocalDate ultima = processo.getDataUltimaMovimentacao().toLocalDate();
        long diasSemMovimento = java.time.temporal.ChronoUnit.DAYS.between(ultima, context.hoje());
        int limiteRamo = switch (processo.getRamoDireito()) {
            case PENAL -> 365;
            case TRIBUTARIO -> 1460;
            case PREVIDENCIARIO -> 1095;
            case INFANCIA_JUVENTUDE -> 120;
            default -> 1825;
        };
        if (diasSemMovimento < (long) (limiteRamo * 0.8)) {
            return;
        }
        int diasRestantes = (int) Math.max(0L, limiteRamo - diasSemMovimento);
        alertas.add(new AlertaPrazoProativo(
                processo.getId(),
                processo.getNumeroUnificado(),
                TipoAlerta.ALERTA_PRESCRICAO,
                context.hoje().plusDays(diasRestantes),
                diasRestantes,
                "Possível prescrição ou perda de efetividade por inércia processual",
                processo.getRamoDireito() == RamoDireito.PENAL ? "CP/CPP + jurisprudência aplicável" : "Legislação processual específica do ramo",
                resolverDestinatarios(processo, TipoAlerta.ALERTA_PRESCRICAO, true, context),
                processo.getRamoDireito() == RamoDireito.PENAL ? UrgenciaMensagem.CRITICA : UrgenciaMensagem.ALTA,
                tagsAlerta(processo, TipoAlerta.ALERTA_PRESCRICAO, UrgenciaMensagem.ALTA),
                "Processo sem movimentação relevante há " + diasSemMovimento + " dias."
        ));
    }

    private void verificarBacklogCritico(Processo processo, PrazoScanContext context, List<AlertaPrazoProativo> alertas) {
        if (processo.getDataCriacao() == null || processo.getRamoDireito() == null) {
            return;
        }
        long idade = java.time.temporal.ChronoUnit.DAYS.between(processo.getDataCriacao().toLocalDate(), context.hoje());
        long limite = switch (processo.getRamoDireito()) {
            case INFANCIA_JUVENTUDE -> 180;
            case CONSUMIDOR, TRABALHISTA -> 365;
            case PENAL, FAMILIA -> 540;
            default -> 730;
        };
        if (idade <= limite) {
            return;
        }
        alertas.add(new AlertaPrazoProativo(
                processo.getId(),
                processo.getNumeroUnificado(),
                TipoAlerta.ALERTA_BACKLOG_CRITICO,
                context.hoje(),
                0,
                "Backlog processual acima do referencial do ramo",
                "Governança operacional + duração razoável do processo (CF art. 5º, LXXVIII)",
                resolverDestinatarios(processo, TipoAlerta.ALERTA_BACKLOG_CRITICO, true, context),
                UrgenciaMensagem.ALTA,
                tagsAlerta(processo, TipoAlerta.ALERTA_BACKLOG_CRITICO, UrgenciaMensagem.ALTA),
                "Idade do processo acima do benchmark operacional do ramo."
        ));
    }

    private void verificarRegrasCriticas(Processo processo,
                                         NationalRulePackEngine.ResultadoRegras regras,
                                         PrazoScanContext context,
                                         List<AlertaPrazoProativo> alertas) {
        if (regras == null || !regras.temAlertasCriticos()) {
            return;
        }
        alertas.add(new AlertaPrazoProativo(
                processo.getId(),
                processo.getNumeroUnificado(),
                TipoAlerta.ALERTA_REGRA_CRITICA,
                context.hoje(),
                0,
                "Regra crítica detectada pelo pacote nacional",
                String.join(" | ", regras.alertas()),
                resolverDestinatarios(processo, TipoAlerta.ALERTA_REGRA_CRITICA, true, context),
                UrgenciaMensagem.CRITICA,
                tagsAlerta(processo, TipoAlerta.ALERTA_REGRA_CRITICA, UrgenciaMensagem.CRITICA),
                regras.alertas().isEmpty() ? "Contexto sensível" : regras.alertas().get(0)
        ));
    }

    private void verificarPrioridadesEspeciais(Processo processo, PrazoScanContext context, List<AlertaPrazoProativo> alertas) {
        if (processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
            alertas.add(new AlertaPrazoProativo(
                    processo.getId(),
                    processo.getNumeroUnificado(),
                    TipoAlerta.ALERTA_PRIORIDADE_INFANCIA,
                    context.hoje(),
                    0,
                    "Fila prioritária absoluta",
                    "CF art. 227 + ECA art. 4º",
                    resolverDestinatarios(processo, TipoAlerta.ALERTA_PRIORIDADE_INFANCIA, true, context),
                    UrgenciaMensagem.CRITICA,
                    tagsAlerta(processo, TipoAlerta.ALERTA_PRIORIDADE_INFANCIA, UrgenciaMensagem.CRITICA),
                    "Processo com tutela prioritária absoluta."
            ));
        }
        if (processo.getRamoDireito() == RamoDireito.PENAL && processo.getStatusProcesso() == StatusProcesso.CITACAO_REALIZADA) {
            alertas.add(new AlertaPrazoProativo(
                    processo.getId(),
                    processo.getNumeroUnificado(),
                    TipoAlerta.ALERTA_PRIORIDADE_PENAL,
                    context.hoje(),
                    0,
                    "Janela penal sensível após citação",
                    "CPP + garantias do contraditório e ampla defesa",
                    resolverDestinatarios(processo, TipoAlerta.ALERTA_PRIORIDADE_PENAL, true, context),
                    UrgenciaMensagem.ALTA,
                    tagsAlerta(processo, TipoAlerta.ALERTA_PRIORIDADE_PENAL, UrgenciaMensagem.ALTA),
                    "Processo penal exige vigilância reforçada de prazo de defesa."
            ));
        }
    }

    private NotificacaoPJB construirParaAlerta(Long usuarioId,
                                               AlertaPrazoProativo alerta,
                                               ConfiguracaoNotificacao cfg) {
        Processo processo = alerta.processoId() != null
                ? processoRepository.findProcessoCompletoById(alerta.processoId()).orElse(null)
                : null;
        CanalNotificacao canal = escolherCanal(cfg, alerta, processo);
        NotificacaoPJB base = construir(usuarioId, alerta.processoId(), alerta.tipoAlerta(), alerta.urgencia(), canal);
        Map<String, String> metadados = new LinkedHashMap<>(base.metadados());
        metadados.put("vencimento", alerta.vencimento() != null ? alerta.vencimento().toString() : "");
        metadados.put("diasRestantes", String.valueOf(alerta.diasRestantes()));
        metadados.put("fundamento", alerta.fundamentoLegal() == null ? "" : alerta.fundamentoLegal());
        if (!alerta.tags().isEmpty()) {
            metadados.put("tags", String.join(",", alerta.tags()));
        }
        return new NotificacaoPJB(
                base.notificacaoId(),
                base.usuarioId(),
                base.processoId(),
                base.numeroUnificado(),
                base.tipo(),
                base.urgencia(),
                base.canal(),
                base.titulo(),
                gerarCorpo(alerta.tipoAlerta(), processo, alerta),
                base.acaoSugerida(),
                base.linkDeepLink(),
                Collections.unmodifiableMap(metadados),
                base.geradaEm(),
                base.expiresAt(),
                false,
                false
        );
    }

    private List<Long> resolverDestinatarios(Processo processo, TipoAlerta tipo, boolean incluirFallbackOperacional) {
        return resolverDestinatarios(
                processo,
                tipo,
                incluirFallbackOperacional,
                new PrazoScanContext(LocalDate.now(ZONE), LocalDate.now(ZONE), currentUserService.currentUserIdOrZero(), new HashMap<>())
        );
    }

    private List<Long> resolverDestinatarios(Processo processo, TipoAlerta tipo, boolean incluirFallbackOperacional, PrazoScanContext context) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (processo == null) {
            return List.of();
        }
        if (processo.getUsuario() != null && processo.getUsuario().getId() != null && processo.getUsuario().isAtivoESemanticoValido()) {
            ids.add(processo.getUsuario().getId());
        }
        Long actorId = context.actorId();
        if (actorId != null && actorId > 0L) {
            ids.add(actorId);
        }
        if (incluirFallbackOperacional && processo.getJurisdicao() != null && processo.getJurisdicao().getCidade() != null) {
            List<Usuario> operadores = context.operadoresPorComarca().computeIfAbsent(processo.getJurisdicao().getCidade(), usuarioRepository::findByComarcaAndAtivoTrue);
            for (Usuario usuario : operadores) {
                if (usuario == null || usuario.getId() == null) {
                    continue;
                }
                if (deveReceberFallback(usuario, processo, tipo)) {
                    ids.add(usuario.getId());
                }
                if (ids.size() >= 8) {
                    break;
                }
            }
        }
        return List.copyOf(ids);
    }

    private boolean deveReceberFallback(Usuario usuario, Processo processo, TipoAlerta tipo) {
        if (usuario.isMagistrado() || usuario.isServidorJudiciario()) {
            return true;
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            return usuario.isMinisterioPublico();
        }
        if (tipo == TipoAlerta.ALERTA_PRIORIDADE_INFANCIA) {
            return usuario.isDefensoriaPublica() || usuario.isMinisterioPublico();
        }
        return false;
    }

    private TipoAlerta resolverTipoAlerta(int diasRestantes) {
        if (diasRestantes < 0) return TipoAlerta.PRAZO_VENCIDO;
        if (diasRestantes <= 1) return TipoAlerta.PRAZO_VENCENDO_24H;
        if (diasRestantes <= 2) return TipoAlerta.PRAZO_VENCENDO_48H;
        if (diasRestantes <= 7) return TipoAlerta.PRAZO_VENCENDO_SEMANA;
        return null;
    }

    private List<TipoPrazo> priorizarPrazos(RamoDireito ramo, StatusProcesso status) {
        List<TipoPrazo> base = new ArrayList<>();
        if (ramo == RamoDireito.PENAL) {
            base.add(TipoPrazo.APRESENTACAO_DEFESA_PENAL);
            base.add(TipoPrazo.ALEGACOES_FINAIS_PENAL);
            base.add(TipoPrazo.APELACAO);
        } else if (ramo == RamoDireito.TRABALHISTA) {
            base.add(TipoPrazo.RESPOSTA_TRABALHISTA);
            base.add(TipoPrazo.RECURSO_TRABALHISTA);
            base.add(TipoPrazo.EMBARGOS_DECLARACAO);
        } else if (ramo == RamoDireito.ELEITORAL) {
            base.add(TipoPrazo.RECURSO_ELEITORAL);
            base.add(TipoPrazo.EMBARGOS_DECLARACAO);
        } else {
            base.add(TipoPrazo.CONTESTACAO);
            base.add(TipoPrazo.EMBARGOS_DECLARACAO);
            base.add(TipoPrazo.APELACAO);
            base.add(TipoPrazo.CONTRARRAZOES_APELACAO);
        }
        if (status == StatusProcesso.CUMPRIMENTO_SENTENCA) {
            base.add(TipoPrazo.CUMPRIMENTO_SENTENCA);
            base.add(TipoPrazo.IMPUGNACAO_CUMPRIMENTO);
        }
        return List.copyOf(new LinkedHashSet<>(base));
    }

    private UrgenciaMensagem urgenciaPorTipo(TipoAlerta tipo) {
        if (tipo == null) {
            return UrgenciaMensagem.MEDIA;
        }
        return switch (tipo) {
            case PRAZO_VENCIDO, ALERTA_PRESCRICAO, ERRO_PROTOCOLO, ALERTA_REGRA_CRITICA, ALERTA_PRIORIDADE_INFANCIA -> UrgenciaMensagem.CRITICA;
            case PRAZO_VENCENDO_24H, PRAZO_VENCENDO_48H, AUDIENCIA_AMANHA, ALERTA_BACKLOG_CRITICO, ALERTA_PRIORIDADE_PENAL -> UrgenciaMensagem.ALTA;
            case DECISAO_PUBLICADA, MOVIMENTACAO_NOVA, DOCUMENTO_JUNTADO, ALERTA_SIGILO_ALTERADO -> UrgenciaMensagem.MEDIA;
            default -> UrgenciaMensagem.BAIXA;
        };
    }

    private UrgenciaMensagem elevarUrgencia(Processo processo,
                                            TipoAlerta tipo,
                                            int diasRestantes,
                                            NationalRulePackEngine.ResultadoRegras regras) {
        UrgenciaMensagem urgencia = urgenciaPorTipo(tipo);
        if (processo != null) {
            if (processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
                urgencia = UrgenciaMensagem.CRITICA;
            } else if (processo.getRamoDireito() == RamoDireito.PENAL && diasRestantes <= 1) {
                urgencia = UrgenciaMensagem.CRITICA;
            } else if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO && urgencia.ordinal() > UrgenciaMensagem.ALTA.ordinal()) {
                urgencia = UrgenciaMensagem.ALTA;
            }
        }
        if (regras != null && regras.temAlertasCriticos()) {
            urgencia = UrgenciaMensagem.CRITICA;
        }
        return urgencia;
    }

    private NationalRulePackEngine.ResultadoRegras detectarCriticidadeRegra(Processo processo,
                                                                            GrauJurisdicao grau,
                                                                            String tribunal) {
        if (processo == null) {
            return null;
        }
        return rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getRamoDireito(),
                grau,
                tribunal,
                Map.of(
                        "status", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : "",
                        "sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "",
                        "valorCausa", processo.getValorCausa() != null ? processo.getValorCausa() : java.math.BigDecimal.ZERO
                )
        ));
    }

    private CanalNotificacao escolherCanal(ConfiguracaoNotificacao cfg,
                                           AlertaPrazoProativo alerta,
                                           Processo processo) {
        List<CanalNotificacao> canais = cfg.canaisAtivos();
        if (canais == null || canais.isEmpty()) {
            return resolverCanalPreferencial(processo, alerta.tipoAlerta());
        }
        if (alerta.urgencia() == UrgenciaMensagem.CRITICA) {
            if (canais.contains(CanalNotificacao.PUSH_APP_PJB)) {
                return CanalNotificacao.PUSH_APP_PJB;
            }
            if (canais.contains(CanalNotificacao.EMAIL_CERTIFICADO)) {
                return CanalNotificacao.EMAIL_CERTIFICADO;
            }
        }
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            if (canais.contains(CanalNotificacao.DOMICILIO_JUDICIAL_ELETRONICO)) {
                return CanalNotificacao.DOMICILIO_JUDICIAL_ELETRONICO;
            }
        }
        return canais.get(0);
    }

    private CanalNotificacao resolverCanalPreferencial(Processo processo, TipoAlerta tipo) {
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return CanalNotificacao.DOMICILIO_JUDICIAL_ELETRONICO;
        }
        if (tipo == TipoAlerta.PRAZO_VENCIDO || tipo == TipoAlerta.ALERTA_PRESCRICAO) {
            return CanalNotificacao.EMAIL_CERTIFICADO;
        }
        return CanalNotificacao.PUSH_APP_PJB;
    }

    private boolean aceitaAlerta(ConfiguracaoNotificacao cfg, TipoAlerta tipo, UrgenciaMensagem urgencia) {
        if (cfg == null) {
            return true;
        }
        if (cfg.receberSomenteAltaPrioridade() && urgencia.ordinal() > UrgenciaMensagem.ALTA.ordinal()) {
            return false;
        }
        return cfg.alertasAtivos() == null || cfg.alertasAtivos().isEmpty() || cfg.alertasAtivos().contains(tipo);
    }

    private boolean suprimir(NotificacaoPJB notificacao) {
        Instant agora = Instant.now();
        limparSupressaoCache(agora);
        String chave = notificacao.chaveSupressao();
        Instant ultima = supressaoCache.put(chave, agora);
        if (ultima == null) {
            return false;
        }
        return ultima.plusSeconds(JANELA_SUPRESSAO_SEGUNDOS).isAfter(agora);
    }

    private void limparSupressaoCache(Instant agora) {
        supressaoCache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().plusSeconds(JANELA_SUPRESSAO_SEGUNDOS).isBefore(agora));
        int overflow = supressaoCache.size() - MAX_SUPRESSAO_CACHE;
        if (overflow <= 0) {
            return;
        }
        supressaoCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(supressaoCache::remove);
    }

    private void registrarEntregaUi(Usuario usuario, Processo processo, NotificacaoPJB notificacao, boolean entregue) {
        List<UiToken> tokens = new ArrayList<>();
        tokens.add(UiToken.NOTIFICADO);
        if (entregue) {
            tokens.add(UiToken.INFO);
        } else {
            tokens.add(UiToken.ATRASADO);
        }
        if (notificacao.urgencia() == UrgenciaMensagem.CRITICA) {
            tokens.add(UiToken.URGENTE);
        }
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            tokens.add(UiToken.SIGILOSO);
        }
        String inbox = "notificacao:" + (usuario != null && usuario.getId() != null ? usuario.getId() : 0L);
        uiHistoryService.recordInboxEvent(
                inbox,
                processo != null ? processo.getId() : notificacao.processoId(),
                entregue ? "NOTIFICACAO_ENTREGUE" : "NOTIFICACAO_FALHA",
                EnumSet.copyOf(new LinkedHashSet<>(tokens)),
                usuario != null ? usuario.getId() : null,
                usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "SISTEMA",
                notificacao.titulo()
        );
    }

    private void registrarPainelInterno(List<AlertaPrazoProativo> alertas, List<NotificacaoPJB> notificacoes) {
        long criticos = alertas.stream().filter(a -> a.urgencia() == UrgenciaMensagem.CRITICA).count();
        List<String> destaques = new ArrayList<>();
        if (criticos > 0) {
            destaques.add("Varredura gerou " + criticos + " alertas críticos.");
        }
        if (!notificacoes.isEmpty()) {
            destaques.add("Foram preparadas " + notificacoes.size() + " notificações proativas.");
        }
        String hash = sha256Hex(alertas.toString() + notificacoes.toString());
        registrarObservabilidade(
                "NOTIFICACAO_VARREDURA_CONCLUIDA",
                "scan:" + Instant.now().toEpochMilli(),
                hash,
                destaques.isEmpty() ? "Varredura concluída" : destaques.get(0)
        );
    }

    private void registrarObservabilidade(String action, String resourceId, String payloadHash, String message) {
        auditLedger.appendSafely(action, RESOURCE_TYPE, resourceId, payloadHash);
        Usuario actor = currentUserService.getOrNull();
        uiHistoryService.recordInboxEvent(
                "notificacao:operacional",
                null,
                action,
                EnumSet.of(UiToken.NOTIFICADO, UiToken.INFO),
                actor != null ? actor.getId() : null,
                actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "SISTEMA",
                message
        );
    }

    private Map<String, String> construirMetadados(Processo processo, TipoAlerta tipo, AlertaPrazoProativo alerta) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("tipo", tipo != null ? tipo.name() : "GERAL");
        dados.put("ramo", processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "");
        dados.put("status", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : "");
        dados.put("sigilo", processo != null && processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "PUBLICO");
        if (alerta != null && alerta.vencimento() != null) {
            dados.put("vencimento", alerta.vencimento().toString());
        }
        return Collections.unmodifiableMap(dados);
    }

    private String gerarTitulo(TipoAlerta tipo, String numero) {
        String sufixo = numero == null || numero.isBlank() ? "" : " — " + numero;
        return switch (tipo) {
            case PRAZO_VENCENDO_24H -> "Prazo vence em até 24h" + sufixo;
            case PRAZO_VENCENDO_48H -> "Prazo vence em 48h" + sufixo;
            case PRAZO_VENCENDO_SEMANA -> "Prazo vence nesta semana" + sufixo;
            case PRAZO_VENCIDO -> "Prazo potencialmente vencido" + sufixo;
            case DECISAO_PUBLICADA -> "Nova decisão publicada" + sufixo;
            case MOVIMENTACAO_NOVA -> "Nova movimentação registrada" + sufixo;
            case AUDIENCIA_AMANHA -> "Audiência ocorre amanhã" + sufixo;
            case ACORDO_PENDENTE_ASSINATURA -> "Acordo pendente de assinatura" + sufixo;
            case ALERTA_PRESCRICAO -> "Atenção para prescrição" + sufixo;
            case ALERTA_SIGILO_ALTERADO -> "Nível de sigilo alterado" + sufixo;
            case ALERTA_BACKLOG_CRITICO -> "Processo com backlog crítico" + sufixo;
            case ALERTA_REGRA_CRITICA -> "Regra crítica detectada" + sufixo;
            case ALERTA_PRIORIDADE_INFANCIA -> "Prioridade absoluta de infância" + sufixo;
            case ALERTA_PRIORIDADE_PENAL -> "Janela penal prioritária" + sufixo;
            default -> "Notificação processual" + sufixo;
        };
    }

    private String gerarCorpo(TipoAlerta tipo, Processo processo, AlertaPrazoProativo alerta) {
        String ramo = processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().getDescricao() : "";
        return switch (tipo) {
            case PRAZO_VENCENDO_24H -> "Há prazo processual crítico em " + ramo + " com vencimento muito próximo. Priorize a conferência dos autos e a peça correspondente.";
            case PRAZO_VENCENDO_48H -> "Existe prazo processual relevante em andamento. Revise tarefas pendentes e prepare protocolo com antecedência.";
            case PRAZO_VENCENDO_SEMANA -> "O processo possui prazo nesta semana. Antecipe revisão documental e validação de assinatura.";
            case PRAZO_VENCIDO -> "Foi identificado prazo potencialmente vencido. Avalie imediatamente justa causa, nulidade ou medida saneadora aplicável.";
            case DECISAO_PUBLICADA -> "Uma nova decisão foi publicada. Verifique efeitos, prazos recursais e necessidade de cumprimento imediato.";
            case MOVIMENTACAO_NOVA -> "O processo recebeu movimentação recente. Confira o conteúdo e os reflexos operacionais.";
            case AUDIENCIA_AMANHA -> "Há audiência agendada para o próximo dia. Confirme presença, documentos e estratégia de atuação.";
            case ACORDO_PENDENTE_ASSINATURA -> "Há acordo aguardando assinatura ou aceite. Revise cláusulas, vencimentos e requisitos formais.";
            case ALERTA_PRESCRICAO -> alerta != null && alerta.racional() != null ? alerta.racional() : "O processo apresenta risco de prescrição ou perda de efetividade por inércia.";
            case ALERTA_SIGILO_ALTERADO -> "O nível de sigilo foi alterado. Revalide compartilhamentos, canais e visibilidade do processo.";
            case ALERTA_BACKLOG_CRITICO -> "O processo ultrapassou o benchmark operacional do ramo. Recomenda-se saneamento prioritário e revisão da fila.";
            case ALERTA_REGRA_CRITICA -> alerta != null && alerta.racional() != null ? alerta.racional() : "O rule pack nacional marcou o caso como crítico para atuação.";
            case ALERTA_PRIORIDADE_INFANCIA -> "Processo com tutela prioritária absoluta. A fila operacional deve refletir máxima precedência.";
            case ALERTA_PRIORIDADE_PENAL -> "Há janela penal sensível com impacto direto em contraditório, defesa e validade de atos.";
            default -> "Acesse o processo no PJB para verificar detalhes desta notificação.";
        };
    }

    private String gerarAcao(TipoAlerta tipo) {
        return switch (tipo) {
            case PRAZO_VENCENDO_24H, PRAZO_VENCENDO_48H, PRAZO_VENCENDO_SEMANA, PRAZO_VENCIDO -> "VER_PRAZOS";
            case DECISAO_PUBLICADA -> "LER_DECISAO";
            case MOVIMENTACAO_NOVA, DOCUMENTO_JUNTADO -> "VER_MOVIMENTACOES";
            case AUDIENCIA_AMANHA -> "VER_AGENDA";
            case ACORDO_PENDENTE_ASSINATURA -> "ASSINAR_ACORDO";
            case ALERTA_PRESCRICAO, ALERTA_REGRA_CRITICA, ALERTA_BACKLOG_CRITICO -> "ABRIR_PROCESSO";
            default -> "ABRIR_PROCESSO";
        };
    }

    private String resolverSegmentoDeepLink(TipoAlerta tipo) {
        return switch (tipo) {
            case DECISAO_PUBLICADA -> "/decisoes";
            case MOVIMENTACAO_NOVA, DOCUMENTO_JUNTADO -> "/movimentacoes";
            case AUDIENCIA_AMANHA -> "/agenda";
            case ACORDO_PENDENTE_ASSINATURA -> "/acordo";
            default -> "/prazos";
        };
    }

    private long expiracaoSegundos(TipoAlerta tipo, UrgenciaMensagem urgencia) {
        if (tipo == TipoAlerta.PRAZO_VENCIDO || tipo == TipoAlerta.ALERTA_PRESCRICAO) {
            return 86_400L * 14;
        }
        if (urgencia == UrgenciaMensagem.CRITICA) {
            return 86_400L * 3;
        }
        return 86_400L * 7;
    }

    private List<String> tagsAlerta(Processo processo, TipoAlerta tipo, UrgenciaMensagem urgencia) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (processo != null && processo.getRamoDireito() != null) {
            tags.add(processo.getRamoDireito().name());
            if (processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
                tags.add("INFANCIA");
            }
            if (processo.getRamoDireito() == RamoDireito.PENAL) {
                tags.add("PENAL");
            }
        }
        if (tipo != null) {
            tags.add(tipo.name());
        }
        if (urgencia != null) {
            tags.add(urgencia.name());
        }
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            tags.add("SIGILO");
        }
        return List.copyOf(tags);
    }

    private String descricaoPrazo(TipoPrazo tipoPrazo) {
        return tipoPrazo == null ? "prazo processual" : tipoPrazo.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String construirRacionalPrazo(Processo processo, PrazoCalculado prazo, int diasRestantes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Prazo ").append(descricaoPrazo(prazo.tipo())).append(" com vencimento em ").append(prazo.vencimento()).append('.');
        sb.append(" Dias restantes: ").append(diasRestantes).append('.');
        if (processo != null && processo.getRamoDireito() != null) {
            sb.append(" Ramo: ").append(processo.getRamoDireito().getDescricao()).append('.');
        }
        if (prazo.advertencias() != null && !prazo.advertencias().isEmpty()) {
            sb.append(' ').append(prazo.advertencias().get(0));
        }
        return sb.toString();
    }

    private long contarProcessosAtivos() {
        long total = processoRepository.count();
        long baixados = processoRepository.searchCidadao(null, null, null, StatusProcesso.BAIXADO, PageRequest.of(0, 1)).getTotalElements();
        long arquivados = processoRepository.searchCidadao(null, null, null, StatusProcesso.ARQUIVADO, PageRequest.of(0, 1)).getTotalElements();
        return Math.max(0L, total - baixados - arquivados);
    }

    private long estimarNotificacoes(List<AlertaPrazoProativo> alertas) {
        long total = 0L;
        for (AlertaPrazoProativo alerta : alertas) {
            total += alerta.usuariosParaNotificar() == null ? 0L : alerta.usuariosParaNotificar().size();
        }
        return total;
    }

    private static <T> List<T> distinct(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private static String resourceId(NotificacaoPJB notificacao) {
        return (notificacao.processoId() == null ? "0" : notificacao.processoId()) + ":" + notificacao.notificacaoId();
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.valueOf(raw).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }
}
