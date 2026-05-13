package com.tcc.pjb.backend.platform.jusos.v2.conciliacao;

import java.math.BigDecimal;
import java.time.Duration;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.IaSettings;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AcordoHomologadoRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.AcordoSuggestionPipelineAsyncService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class CejuscEngine {

    private static final Logger log = LoggerFactory.getLogger(CejuscEngine.class);
    private static final String RESOURCE_TYPE_CEJUSC = "CEJUSC";
    private static final String RESOURCE_TYPE_PROCESSO = "PROCESSO";
    private static final int JANELA_CRITICA_DIAS = 7;
    private static final Duration RESULTADO_CACHE_TTL = Duration.ofHours(6);
    private static final int RESULTADO_CACHE_MAX = 20000;
    private static final int RESULTADO_CACHE_TRIM_TO = 16000;
    private static final long RESULTADO_CACHE_CLEANUP_EVERY = 256L;

    public enum ModalidadeSessao {
        CONCILIACAO_PRE_PROCESSUAL,
        CONCILIACAO_INCIDENTAL,
        MEDIACAO_FAMILIAR,
        MEDIACAO_EMPRESARIAL,
        MEDIACAO_COMUNITARIA,
        ARBITRAGEM_INSTITUCIONAL,
        NEGOCIACAO_DIRETA_ASSISTIDA,
        PRATICA_COLABORATIVA
    }

    public enum StatusSessaoCejusc {
        AGENDADA,
        REALIZADA_COM_ACORDO,
        REALIZADA_SEM_ACORDO,
        CANCELADA_PARTE,
        CANCELADA_SISTEMA,
        SUSPENSA,
        TENTATIVA_FRUSTRADA
    }

    public enum ResultadoConciliacao {
        ACORDO_TOTAL,
        ACORDO_PARCIAL,
        SEM_ACORDO,
        PARTE_AUSENTE,
        REQUERENTE_AUSENTE,
        REQUERIDO_AUSENTE,
        DESISTENCIA_BILATERAL
    }

    public record SessaoCejusc(
            UUID sessaoId,
            String numeroProcesso,
            String cejuscCodigo,
            ModalidadeSessao modalidade,
            StatusSessaoCejusc status,
            Instant dataHora,
            String conciliadorNome,
            UUID conciliadorId,
            List<ParticipanteSessao> participantes,
            ResultadoConciliacao resultado,
            TermoAcordo termoAcordo,
            List<String> observacoes,
            Instant encerradaEm
    ) {
        public SessaoCejusc {
            sessaoId = sessaoId != null ? sessaoId : UUID.randomUUID();
            participantes = participantes == null ? List.of() : List.copyOf(participantes);
            observacoes = observacoes == null ? List.of() : List.copyOf(observacoes);
        }

        public boolean possuiAcordo() {
            return resultado == ResultadoConciliacao.ACORDO_TOTAL || resultado == ResultadoConciliacao.ACORDO_PARCIAL;
        }

        public boolean houveAusenciaRelevante() {
            return resultado == ResultadoConciliacao.PARTE_AUSENTE
                    || resultado == ResultadoConciliacao.REQUERENTE_AUSENTE
                    || resultado == ResultadoConciliacao.REQUERIDO_AUSENTE;
        }
    }

    public record ParticipanteSessao(
            String nome,
            String cpfMascarado,
            String papel,
            boolean compareceu,
            boolean representadoAdvogado
    ) {
        public boolean partePrincipal() {
            String token = normalize(papel);
            return token.contains("REQUERENTE")
                    || token.contains("REQUERIDO")
                    || token.contains("AUTOR")
                    || token.contains("REU")
                    || token.contains("PARTE");
        }
    }

    public record TermoAcordo(
            UUID termoId,
            String conteudo,
            BigDecimal valorAcordado,
            LocalDate dataCumprimento,
            List<String> obrigacoesFazer,
            List<String> obrigacoesNaoFazer,
            boolean precisaHomologacaoJudicial,
            boolean geraExecucaoExtrajudicial,
            String fundamentoLegal
    ) {
        public TermoAcordo {
            termoId = termoId != null ? termoId : UUID.randomUUID();
            obrigacoesFazer = obrigacoesFazer == null ? List.of() : List.copyOf(new LinkedHashSet<>(obrigacoesFazer));
            obrigacoesNaoFazer = obrigacoesNaoFazer == null ? List.of() : List.copyOf(new LinkedHashSet<>(obrigacoesNaoFazer));
        }

        public static TermoAcordo vazio() {
            return new TermoAcordo(UUID.randomUUID(), "", BigDecimal.ZERO, null, List.of(), List.of(), false, false, "");
        }

        public int totalObrigacoes() {
            return obrigacoesFazer.size() + obrigacoesNaoFazer.size();
        }
    }

    public record AnaliseAdequacaoMetodo(
            boolean admiteConciliacao,
            boolean admiteMediacao,
            boolean recomendaCejusc,
            String metodoPrioritario,
            List<String> motivosRecomendacao,
            List<String> alertas,
            String fundamentoLegal
    ) {
        public AnaliseAdequacaoMetodo {
            motivosRecomendacao = List.copyOf(new LinkedHashSet<>(motivosRecomendacao == null ? List.of() : motivosRecomendacao));
            alertas = List.copyOf(new LinkedHashSet<>(alertas == null ? List.of() : alertas));
        }
    }

    public record ResultadoRegistro(
            UUID sessaoId,
            boolean sucesso,
            boolean processoSuspenso,
            boolean acordoHomologado,
            List<String> proximosPassos
    ) {
        public ResultadoRegistro {
            proximosPassos = List.copyOf(new LinkedHashSet<>(proximosPassos == null ? List.of() : proximosPassos));
        }
    }

    public record RadarConciliacao(
            int scoreAdequacao,
            int scoreExecutabilidade,
            int scoreUrgencia,
            BigDecimal faixaMinimaSugerida,
            BigDecimal faixaMaximaSugerida,
            LocalDate prazoCritico,
            List<String> drivers,
            List<String> riscos,
            List<String> oportunidades
    ) {
        public RadarConciliacao {
            drivers = List.copyOf(new LinkedHashSet<>(drivers == null ? List.of() : drivers));
            riscos = List.copyOf(new LinkedHashSet<>(riscos == null ? List.of() : riscos));
            oportunidades = List.copyOf(new LinkedHashSet<>(oportunidades == null ? List.of() : oportunidades));
        }
    }

    public record ComplianceSessao(
            boolean aptaParaSessaoVirtual,
            boolean exigeAtuacaoMinisterioPublico,
            boolean exigeHomologacao,
            boolean envolveDireitoIndisponivel,
            List<String> pendencias,
            List<String> travas,
            List<String> salvaguardas
    ) {
        public ComplianceSessao {
            pendencias = List.copyOf(new LinkedHashSet<>(pendencias == null ? List.of() : pendencias));
            travas = List.copyOf(new LinkedHashSet<>(travas == null ? List.of() : travas));
            salvaguardas = List.copyOf(new LinkedHashSet<>(salvaguardas == null ? List.of() : salvaguardas));
        }
    }

    public record SessaoCejuscRegistradaEvent(
            UUID sessaoId,
            Long processoId,
            String numeroProcesso,
            ResultadoConciliacao resultado,
            String cejuscCodigo,
            Instant registradoEm,
            boolean criouOuAtualizouProposta,
            boolean requerHomologacao
    ) {}

    private final ProcessoRepository processoRepository;
    private final AcordoHomologadoRepository acordoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final AcordoSuggestionPipelineAsyncService sugestaoIA;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLedgerService auditLedgerService;
    private final UiHistoryService uiHistoryService;
    private final CurrentUserService currentUserService;
    private final NationalRulePackEngine rulePackEngine;
    private final NationalPrazoEngine prazoEngine;
    private final ConcurrentMap<UUID, CachedResultadoRegistro> cacheResultados = new ConcurrentHashMap<>();
    private final AtomicLong cacheResultadoTouches = new AtomicLong();

    public CejuscEngine(ProcessoRepository processoRepository,
                        AcordoHomologadoRepository acordoRepository,
                        PropostaAcordoRepository propostaAcordoRepository,
                        MembroEquipeRepository membroEquipeRepository,
                        AcordoSuggestionPipelineAsyncService sugestaoIA,
                        ApplicationEventPublisher eventPublisher,
                        AuditLedgerService auditLedgerService,
                        UiHistoryService uiHistoryService,
                        CurrentUserService currentUserService,
                        NationalRulePackEngine rulePackEngine,
                        NationalPrazoEngine prazoEngine) {
        this.processoRepository = processoRepository;
        this.acordoRepository = acordoRepository;
        this.propostaAcordoRepository = propostaAcordoRepository;
        this.membroEquipeRepository = membroEquipeRepository;
        this.sugestaoIA = sugestaoIA;
        this.eventPublisher = eventPublisher;
        this.auditLedgerService = auditLedgerService;
        this.uiHistoryService = uiHistoryService;
        this.currentUserService = currentUserService;
        this.rulePackEngine = rulePackEngine;
        this.prazoEngine = prazoEngine;
    }

    public AnaliseAdequacaoMetodo analisarAdequacao(Processo processo) {
        Objects.requireNonNull(processo, "processo");

        RamoDireito ramo = processo.getRamoDireito();
        GrauJurisdicao grau = processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null;
        String tribunalCodigo = processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;

        List<String> motivos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        String metodo = ModalidadeSessao.CONCILIACAO_INCIDENTAL.name();
        String fundamento = "Lei 13.140/2015 + CPC arts. 165-175";

        boolean admiteConciliacao = ramo == null || ramo.admiteConciliacao();
        boolean admiteMediacao = ramo == null;
        boolean recomenda = ramo == null;

        NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                ramo,
                grau,
                tribunalCodigo,
                Map.of(
                        "valorCausa", Optional.ofNullable(processo.getValorCausa()).orElse(BigDecimal.ZERO),
                        "conciliacao", true,
                        "mediacao", true,
                        "sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "PUBLICO"
                )
        ));

        NationalPrazoEngine.PrazoCalculado prazoCritico = resolverPrazoCritico(processo);
        if (prazoCritico != null && prazoCritico.vencimento() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), prazoCritico.vencimento());
            if (dias <= JANELA_CRITICA_DIAS) {
                motivos.add("Janela processual curta para autocomposição antes do vencimento de " + prazoCritico.tipo().name());
                alertas.add("Prazo crítico em " + prazoCritico.vencimento() + " — conduzir pauta CEJUSC com prioridade operacional");
                recomenda = true;
            }
        }

        if (ramo == null) {
            motivos.add("Ramo jurídico não identificado — CEJUSC pode servir como triagem e pré-composição inicial");
        } else {
            switch (ramo) {
                case CIVIL, CONSUMIDOR, EMPRESARIAL, ADMINISTRATIVO, AGRARIO -> {
                    admiteMediacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.CONCILIACAO_PRE_PROCESSUAL.name();
                    motivos.add("Direito patrimonial disponível com alta taxa de composição consensual");
                    motivos.add("Autocomposição reduz custo de litigância, deslocamento e tempo de resolução");
                }
                case FAMILIA -> {
                    admiteMediacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.MEDIACAO_FAMILIAR.name();
                    fundamento = "CPC arts. 694-699 + Lei 13.140/2015 + CF art. 227";
                    motivos.add("Conflito com vínculo continuado entre as partes recomenda mediação estruturada");
                    motivos.add("Sessão com foco restaurativo reduz reincidência do litígio e melhora adimplemento");
                    alertas.add("Se houver incapaz, o Ministério Público deve ser cientificado e monitorar o acordo");
                }
                case TRABALHISTA -> {
                    admiteConciliacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.CONCILIACAO_INCIDENTAL.name();
                    fundamento = "CLT arts. 764, 846, 850 + Res. CNJ 174/2016";
                    motivos.add("Conciliação é vetor estrutural da Justiça do Trabalho e deve ser tentada em audiência");
                    motivos.add("Composição pode liquidar parcelas controvertidas sem sacrificar verbas incontroversas");
                }
                case PREVIDENCIARIO -> {
                    admiteMediacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.NEGOCIACAO_DIRETA_ASSISTIDA.name();
                    motivos.add("Conciliação sobre retroativos, datas de início e critérios de cálculo é operacionalmente útil");
                    motivos.add("Saneamento prévio reduz perícias e impugnações repetitivas no JEF");
                }
                case INFANCIA_JUVENTUDE -> {
                    admiteMediacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.MEDIACAO_FAMILIAR.name();
                    fundamento = "ECA art. 100 + CPC art. 694 + CF art. 227";
                    motivos.add("Método autocompositivo pode reorganizar convivência e corresponsabilidade familiar");
                    alertas.add("Interesse superior da criança deve prevalecer sobre qualquer concessão patrimonial");
                }
                case PENAL -> {
                    admiteConciliacao = false;
                    fundamento = "Lei 9.099/95 arts. 72-74 + CPP art. 28-A";
                    alertas.add("Em matéria penal, CEJUSC não substitui audiência preliminar ou ANPP fora das hipóteses legais");
                    alertas.add("Admitir apenas composição civil dos danos em infrações de menor potencial ofensivo");
                }
                case TRIBUTARIO -> {
                    admiteConciliacao = false;
                    fundamento = "CTN art. 171 + Lei 13.988/2020";
                    alertas.add("Transação tributária depende de autorização legal específica e parâmetros fazendários");
                }
                case AMBIENTAL -> {
                    admiteConciliacao = false;
                    fundamento = "Lei 7.347/85 art. 5º §6º + Lei 13.140/2015";
                    alertas.add("Direito difuso exige cautela reforçada; TAC com legitimados coletivos é via preferencial");
                    alertas.add("Cláusulas reparatórias e recomposição ambiental demandam validação técnica" );
                }
                case INTERNACIONAL -> {
                    admiteMediacao = true;
                    recomenda = true;
                    metodo = ModalidadeSessao.NEGOCIACAO_DIRETA_ASSISTIDA.name();
                    fundamento = "CPC arts. 3º §§2º-3º + Lei 13.140/2015 + cooperação jurídica internacional";
                    motivos.add("Conflitos com elemento transnacional podem se beneficiar de negociação assistida e desenho procedimental cooperativo");
                    alertas.add("Validar lei aplicável, jurisdição competente, idioma, homologação e executabilidade internacional do ajuste");
                }
                case ELEITORAL, MILITAR, CONSTITUCIONAL -> {
                    admiteConciliacao = false;
                    recomenda = false;
                    alertas.add("Método autocompositivo é excepcional neste ramo e depende de permissivo jurídico estrito");
                }
                default -> {
                    if (ramo.admiteConciliacao()) {
                        admiteConciliacao = true;
                        recomenda = true;
                        metodo = ModalidadeSessao.CONCILIACAO_INCIDENTAL.name();
                        motivos.add("Ramo derivado mapeado por afinidade conciliatória exige avaliação assistida e pauta controlada");
                    } else {
                        recomenda = false;
                        alertas.add("Ramo derivado sem matriz CEJUSC explícita — exigir validação normativa antes da autocomposição");
                    }
                }
            }
        }

        if (processo.getValorCausa() != null && processo.getValorCausa().compareTo(new BigDecimal("100000")) > 0) {
            motivos.add("Valor relevante recomenda acordo parametrizado com cronograma, garantias e cláusulas de inadimplemento");
        }
        if (acordoRepository.findByProcesso_Id(processo.getId()).isPresent()) {
            alertas.add("Já existe acordo homologado vinculado ao processo — evitar fluxo conciliatório duplicado");
            recomenda = false;
        }
        if (regras.temAlertasCriticos()) {
            alertas.add("Regras críticas do ramo exigem triagem jurídica antes da composição final");
        }
        alertas.addAll(regras.alertas().stream().limit(3).toList());
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            alertas.add("Sessão deve observar proteção reforçada de sigilo e controle de participantes");
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            alertas.add("Atuação do Ministério Público pode ser necessária para validade e supervisão do acordo");
        }

        return new AnaliseAdequacaoMetodo(
                admiteConciliacao,
                admiteMediacao,
                recomenda,
                metodo,
                motivos,
                alertas,
                fundamento
        );
    }

    @Transactional
    public ResultadoRegistro registrarResultadoSessao(SessaoCejusc sessao, Processo processo) {
        Objects.requireNonNull(sessao, "sessao");

        ResultadoRegistro cached = lookupResultadoRegistrado(sessao.sessaoId());
        if (cached != null) {
            return cached;
        }

        Processo processoEfetivo = processo != null ? processo : buscarProcesso(sessao.numeroProcesso()).orElse(null);
        List<String> proximos = new ArrayList<>();
        boolean processoSuspenso = false;
        boolean acordoHomologado = false;
        boolean criouOuAtualizouProposta = false;

        if (processoEfetivo != null && acordoRepository.findByProcesso_Id(processoEfetivo.getId()).isPresent()) {
            proximos.add("Já existe acordo homologado para o processo — revisar necessidade de nova sessão");
            ResultadoRegistro resultado = new ResultadoRegistro(sessao.sessaoId(), true, false, true, proximos);
            rememberResultado(sessao.sessaoId(), resultado);
            return resultado;
        }

        ComplianceSessao compliance = avaliarCompliance(sessao, processoEfetivo);
        proximos.addAll(compliance.salvaguardas());
        proximos.addAll(compliance.pendencias().stream().limit(3).map(p -> "Sanear pendência: " + p).toList());
        proximos.addAll(compliance.travas().stream().limit(3).map(t -> "Resolver trava: " + t).toList());

        if (sessao.possuiAcordo()) {
            TermoAcordo termo = Optional.ofNullable(sessao.termoAcordo()).orElse(TermoAcordo.vazio());
            PropostaAcordo proposta = processoEfetivo != null ? sincronizarProposta(processoEfetivo, sessao, termo) : null;
            criouOuAtualizouProposta = proposta != null;

            if (processoEfetivo != null && termo.precisaHomologacaoJudicial()) {
                processoSuspenso = atualizarStatusProcesso(
                        processoEfetivo,
                        StatusProcesso.AGUARDANDO_PARECER,
                        "ACORDO_CEJUSC_PENDENTE_HOMOLOGACAO",
                        "Sessão CEJUSC com acordo pendente de homologação"
                );
                proximos.add("Submeter termo ao magistrado para homologação judicial");
                proximos.add("Controlar prazo interno para despacho homologatório");
            } else if (processoEfetivo != null && sessao.resultado() == ResultadoConciliacao.ACORDO_TOTAL) {
                acordoHomologado = true;
                atualizarResultadoFinal(processoEfetivo, "ACORDO_TOTAL_CEJUSC");
                proximos.add("Juntar termo aos autos e requerer extinção consensual do processo");
                proximos.add("Gerar calendário de cumprimento e monitoramento do acordo");
            } else if (processoEfetivo != null) {
                atualizarResultadoFinal(processoEfetivo, "ACORDO_PARCIAL_CEJUSC");
                proximos.add("Prosseguir apenas quanto aos pontos controvertidos remanescentes");
                proximos.add("Converter cláusulas incontroversas em cumprimento monitorado");
            }

            if (termo.geraExecucaoExtrajudicial()) {
                proximos.add("Termo contém eficácia executiva extrajudicial e deve receber trilha de cumprimento");
            }
            if (termo.dataCumprimento() != null) {
                proximos.add("Agendar marco de verificação de cumprimento para " + termo.dataCumprimento());
            }
            if (proposta != null) {
                agendarSugestaoIA(proposta.getId());
                proximos.add("Pipeline de IA acionado para refinamento seguro das cláusulas do termo");
            }

            log.info("[CEJUSC] Sessão com acordo registrada: sessao={} processo={} resultado={}",
                    sessao.sessaoId(),
                    processoEfetivo != null ? processoEfetivo.getNumeroUnificado() : "SEM_PROCESSO",
                    sessao.resultado());
        } else {
            if (processoEfetivo != null) {
                atualizarResultadoFinal(processoEfetivo, "SEM_ACORDO_CEJUSC");
            }
            proximos.add("Retomar tramitação processual regular com agenda prioritária se houver urgência");
            proximos.add("Registrar pontos de divergência para eventual proposta futura mais aderente");
            if (sessao.houveAusenciaRelevante() && processoEfetivo != null) {
                proximos.add("Certificar ausência e avaliar sanções processuais cabíveis pela não comparência");
            }
        }

        registrarAuditoria(sessao, processoEfetivo, compliance, criouOuAtualizouProposta);
        publicarEventos(sessao, processoEfetivo, criouOuAtualizouProposta);

        ResultadoRegistro resultado = new ResultadoRegistro(
                sessao.sessaoId(),
                true,
                processoSuspenso,
                acordoHomologado,
                proximos
        );
        rememberResultado(sessao.sessaoId(), resultado);
        return resultado;
    }


    private ResultadoRegistro lookupResultadoRegistrado(UUID sessaoId) {
        if (sessaoId == null) {
            return null;
        }
        Instant now = Instant.now();
        CachedResultadoRegistro cached = cacheResultados.get(sessaoId);
        if (cached == null) {
            cleanupCacheResultados(now);
            return null;
        }
        if (cached.expired(now)) {
            cacheResultados.remove(sessaoId, cached);
            cleanupCacheResultados(now);
            return null;
        }
        touchResultadoCache(now);
        return cached.value();
    }

    private void rememberResultado(UUID sessaoId, ResultadoRegistro resultado) {
        if (sessaoId == null || resultado == null) {
            return;
        }
        Instant now = Instant.now();
        cacheResultados.put(sessaoId, new CachedResultadoRegistro(resultado, now.plus(RESULTADO_CACHE_TTL), now));
        touchResultadoCache(now);
    }

    private void touchResultadoCache(Instant now) {
        long touches = cacheResultadoTouches.incrementAndGet();
        if (cacheResultados.size() > RESULTADO_CACHE_MAX || touches % RESULTADO_CACHE_CLEANUP_EVERY == 0L) {
            cleanupCacheResultados(now == null ? Instant.now() : now);
        }
    }

    private void cleanupCacheResultados(Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        cacheResultados.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expired(effectiveNow));
        int overflow = cacheResultados.size() - RESULTADO_CACHE_MAX;
        if (overflow <= 0) {
            return;
        }
        cacheResultados.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparing(entry -> entry.getValue().touchedAt(), Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(Math.max(overflow, cacheResultados.size() - RESULTADO_CACHE_TRIM_TO))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(cacheResultados::remove);
    }

    public List<String> gerarChecklist(ModalidadeSessao modalidade, RamoDireito ramo) {
        return gerarChecklist(null, modalidade, ramo);
    }

    public List<String> gerarChecklist(Processo processo, ModalidadeSessao modalidade) {
        RamoDireito ramo = processo != null ? processo.getRamoDireito() : null;
        return gerarChecklist(processo, modalidade, ramo);
    }

    public RadarConciliacao gerarRadarConciliacao(Processo processo) {
        Objects.requireNonNull(processo, "processo");

        List<String> drivers = new ArrayList<>();
        List<String> oportunidades = new ArrayList<>();

        AnaliseAdequacaoMetodo adequacao = analisarAdequacao(processo);
        NationalPrazoEngine.PrazoCalculado prazoCritico = resolverPrazoCritico(processo);
        NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getRamoDireito(),
                processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null,
                processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null,
                Map.of(
                        "valorCausa", Optional.ofNullable(processo.getValorCausa()).orElse(BigDecimal.ZERO),
                        "conciliacao", true,
                        "sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "PUBLICO"
                )
        ));

        int scoreAdequacao = 40;
        if (adequacao.recomendaCejusc()) scoreAdequacao += 25;
        if (adequacao.admiteMediacao()) scoreAdequacao += 10;
        if (adequacao.admiteConciliacao()) scoreAdequacao += 10;
        if (processo.getRamoDireito() != null && processo.getRamoDireito().admiteConciliacao()) scoreAdequacao += 10;
        if (regras.temAlertasCriticos()) scoreAdequacao -= 15;
        if (acordoRepository.findByProcesso_Id(processo.getId()).isPresent()) scoreAdequacao = 0;
        scoreAdequacao = clamp(scoreAdequacao);

        int scoreExecutabilidade = 50;
        BigDecimal valor = Optional.ofNullable(processo.getValorCausa()).orElse(BigDecimal.ZERO);
        if (valor.signum() > 0 && valor.compareTo(new BigDecimal("50000")) <= 0) scoreExecutabilidade += 15;
        if (valor.compareTo(new BigDecimal("250000")) > 0) scoreExecutabilidade -= 10;
        if (processo.getRamoDireito() == RamoDireito.FAMILIA || processo.getRamoDireito() == RamoDireito.TRABALHISTA) scoreExecutabilidade += 10;
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) scoreExecutabilidade -= 5;
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) scoreExecutabilidade -= 5;
        scoreExecutabilidade = clamp(scoreExecutabilidade);

        int scoreUrgencia = 35;
        if (prazoCritico != null && prazoCritico.vencimento() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), prazoCritico.vencimento());
            if (dias <= 3) scoreUrgencia = 95;
            else if (dias <= 7) scoreUrgencia = 80;
            else if (dias <= 15) scoreUrgencia = 65;
            drivers.add("Prazo processual de " + prazoCritico.tipo().name() + " em " + prazoCritico.vencimento());
        }
        if (processo.getStatusProcesso() == StatusProcesso.AUDIENCIA_DESIGNADA) {
            scoreUrgencia = Math.max(scoreUrgencia, 70);
            drivers.add("Processo já tem audiência designada e exige estratégia de composição rápida");
        }

        drivers.addAll(adequacao.motivosRecomendacao().stream().limit(3).toList());
        List<String> riscos = new ArrayList<>(adequacao.alertas().stream().limit(4).toList());
        if (regras.temAlertasCriticos()) {
            riscos.add("Há alertas críticos do rule pack exigindo validação humana das cláusulas");
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            oportunidades.add("Engajar Ministério Público cedo pode evitar invalidação posterior do termo");
        }
        oportunidades.add("Cláusulas de cumprimento escalonado podem elevar a taxa de adimplemento");
        oportunidades.add("Sessão virtual com pré-minuta reduz tempo de mesa e melhora taxa de fechamento");

        BigDecimal minima = valor.signum() > 0 ? valor.multiply(new BigDecimal("0.55")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal maxima = valor.signum() > 0 ? valor.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new RadarConciliacao(
                scoreAdequacao,
                scoreExecutabilidade,
                scoreUrgencia,
                minima,
                maxima,
                prazoCritico != null ? prazoCritico.vencimento() : null,
                drivers,
                riscos,
                oportunidades
        );
    }

    public ComplianceSessao avaliarCompliance(SessaoCejusc sessao, Processo processo) {
        Objects.requireNonNull(sessao, "sessao");

        List<String> pendencias = new ArrayList<>();
        List<String> travas = new ArrayList<>();
        List<String> salvaguardas = new ArrayList<>();

        RamoDireito ramo = processo != null ? processo.getRamoDireito() : null;
        boolean exigeMp = ramo != null && ramo.exigeAtuacaoMP();
        boolean exigeHomologacao = sessao.termoAcordo() != null && sessao.termoAcordo().precisaHomologacaoJudicial();
        boolean envolveIndisponivel = ramo == RamoDireito.AMBIENTAL
                || ramo == RamoDireito.TRIBUTARIO
                || ramo == RamoDireito.CONSTITUCIONAL
                || ramo == RamoDireito.ELEITORAL;
        boolean aptaVirtual = ramo != RamoDireito.PENAL && ramo != RamoDireito.MILITAR;

        if (sessao.participantes().stream().filter(ParticipanteSessao::partePrincipal).noneMatch(ParticipanteSessao::compareceu)) {
            travas.add("Nenhuma parte principal compareceu; sessão não produz composição válida");
        }
        if (sessao.possuiAcordo() && (sessao.termoAcordo() == null || blank(sessao.termoAcordo().conteudo()))) {
            travas.add("Sessão com resultado de acordo precisa de termo materializado");
        }
        if (sessao.possuiAcordo() && sessao.participantes().stream().filter(ParticipanteSessao::partePrincipal).anyMatch(p -> !p.representadoAdvogado())) {
            pendencias.add("Verificar assistência técnica adequada para reduzir alegação futura de vício de consentimento");
        }
        if (exigeMp) {
            pendencias.add("Controlar ciência ou manifestação do Ministério Público antes da estabilização do acordo");
        }
        if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            salvaguardas.add("Aplicar controle de acesso, sala restrita e registro nominal de presença por sigilo");
        }
        if (envolveIndisponivel) {
            travas.add("O objeto do acordo pode envolver direito indisponível ou disponibilidade limitada");
        }
        if (sessao.termoAcordo() != null && sessao.termoAcordo().dataCumprimento() != null
                && sessao.termoAcordo().dataCumprimento().isBefore(LocalDate.now())) {
            pendencias.add("A data de cumprimento do termo está no passado e precisa ser corrigida");
        }
        salvaguardas.add("Registrar ata, presença, manifestação de vontade e resumo das cláusulas essenciais");
        salvaguardas.add("Gerar trilha de monitoramento de cumprimento com alertas por data e valor");
        salvaguardas.add("Usar linguagem simples no termo e evitar cláusulas abertas sem parâmetro objetivo");

        return new ComplianceSessao(
                aptaVirtual,
                exigeMp,
                exigeHomologacao,
                envolveIndisponivel,
                pendencias,
                travas,
                salvaguardas
        );
    }

    public Map<String, Object> gerarPainelConciliacao(Processo processo) {
        Objects.requireNonNull(processo, "processo");

        AnaliseAdequacaoMetodo adequacao = analisarAdequacao(processo);
        RadarConciliacao radar = gerarRadarConciliacao(processo);
        List<PropostaAcordo> propostas = propostaAcordoRepository.findByProcesso_Id(processo.getId());
        PropostaAcordo ultima = propostas.stream()
                .max(Comparator.comparing(PropostaAcordo::getDataAtualizacao, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElse(null);
        boolean possuiAcordoHomologado = acordoRepository.findByProcesso_Id(processo.getId()).isPresent();

        Map<String, Object> painel = new LinkedHashMap<>();
        painel.put("processoId", processo.getId());
        painel.put("numeroProcesso", processo.getNumeroUnificado());
        painel.put("ramo", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        painel.put("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        painel.put("recomendaCejusc", adequacao.recomendaCejusc());
        painel.put("metodoPrioritario", adequacao.metodoPrioritario());
        painel.put("scoreAdequacao", radar.scoreAdequacao());
        painel.put("scoreExecutabilidade", radar.scoreExecutabilidade());
        painel.put("scoreUrgencia", radar.scoreUrgencia());
        painel.put("faixaMinima", radar.faixaMinimaSugerida());
        painel.put("faixaMaxima", radar.faixaMaximaSugerida());
        painel.put("prazoCritico", radar.prazoCritico());
        painel.put("propostasAcordo", propostas.size());
        painel.put("ultimaPropostaStatus", ultima != null && ultima.getStatus() != null ? ultima.getStatus().name() : null);
        painel.put("temAcordoHomologado", possuiAcordoHomologado);
        painel.put("drivers", radar.drivers());
        painel.put("riscos", radar.riscos());
        painel.put("oportunidades", radar.oportunidades());
        painel.put("alertas", adequacao.alertas());
        return painel;
    }

    private List<String> gerarChecklist(Processo processo, ModalidadeSessao modalidade, RamoDireito ramo) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Confirmar intimação das partes com antecedência adequada e ciência do objeto da sessão");
        checklist.add("Validar identidade, representação processual e poderes para transigir");
        checklist.add("Reservar ambiente seguro ou link autenticado de videoconferência com trilha de presença");
        checklist.add("Garantir ata digital com horário, presença, propostas, contrapropostas e resultado final");
        checklist.add("Preparar minuta-base com cláusulas objetivas de prazo, valor, inadimplemento e foro de cumprimento");
        checklist.add("Definir operador responsável por monitoramento posterior do cumprimento do acordo");

        if (processo != null) {
            RadarConciliacao radar = gerarRadarConciliacao(processo);
            if (radar.prazoCritico() != null) {
                checklist.add("Ajustar pauta à urgência do prazo crítico em " + radar.prazoCritico());
            }
            if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
                checklist.add("Habilitar sessão reservada com controle de acesso reforçado em razão do sigilo");
            }
            if (acordoRepository.findByProcesso_Id(processo.getId()).isPresent()) {
                checklist.add("Verificar acordo homologado anterior para impedir duplicidade ou conflito de obrigações");
            }
        }
        if (modalidade == ModalidadeSessao.MEDIACAO_FAMILIAR) {
            checklist.add("Disponibilizar apoio multidisciplinar e checar eventual medida protetiva ou vulnerabilidade");
        }
        if (modalidade == ModalidadeSessao.CONCILIACAO_PRE_PROCESSUAL) {
            checklist.add("Preparar termo com foco em prevenção de litígio e título executivo, se juridicamente possível");
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            checklist.add("Conferir verbas incontroversas, discriminação de parcelas e reflexos trabalhistas");
        }
        if (ramo == RamoDireito.PENAL) {
            checklist.add("Checar cabimento legal estrito da composição e eventual interface com audiência preliminar");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            checklist.add("Avaliar impacto do acordo sobre incapazes e interesse superior da criança ou adolescente");
        }
        return List.copyOf(new LinkedHashSet<>(checklist));
    }

    private PropostaAcordo sincronizarProposta(Processo processo, SessaoCejusc sessao, TermoAcordo termo) {
        if (processo == null || processo.getId() == null) {
            return null;
        }

        Usuario usuario = currentUserService.getOrNull();
        Equipe equipe = processo.getEquipe();
        if (equipe == null && usuario != null && usuario.getId() != null) {
            equipe = membroEquipeRepository.carregarComEquipe(usuario.getId()).stream()
                    .filter(MembroEquipe::isAtivo)
                    .map(MembroEquipe::getEquipe)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (usuario == null) {
            usuario = processo.getUsuario();
        }
        if (usuario == null || equipe == null) {
            log.warn("[CEJUSC] Proposta não sincronizada por falta de usuário/equipe. processoId={}", processo.getId());
            return null;
        }

        PropostaAcordo proposta = propostaAcordoRepository.findByProcesso_Id(processo.getId()).stream()
                .filter(p -> p.getStatus() == null || !p.getStatus().isTerminal())
                .max(Comparator.comparing(PropostaAcordo::getDataAtualizacao, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElseGet(PropostaAcordo::new);

        boolean novo = proposta.getId() == null;
        proposta.setProcesso(processo);
        proposta.setProponente(usuario);
        proposta.setEquipe(equipe);
        proposta.setTermosHtml(materializarTermo(sessao, processo, termo));
        proposta.setValorAcordo(termo.valorAcordado() != null ? termo.valorAcordado() : Optional.ofNullable(processo.getValorCausa()).orElse(BigDecimal.ZERO));
        proposta.setStatus(resolverStatusProposta(sessao, termo));
        proposta.setSettings(settingsCejusc());
        if (proposta.getUuid() == null) {
            proposta.setUuid(UUID.randomUUID());
        }

        PropostaAcordo salvo = propostaAcordoRepository.save(proposta);
        auditLedgerService.appendSafely(
                novo ? "CEJUSC_PROPOSTA_CRIADA" : "CEJUSC_PROPOSTA_ATUALIZADA",
                RESOURCE_TYPE_CEJUSC,
                String.valueOf(salvo.getId()),
                salvo.getUuid().toString()
        );
        return salvo;
    }

    private NationalPrazoEngine.PrazoCalculado resolverPrazoCritico(Processo processo) {
        if (processo == null) {
            return null;
        }
        List<NationalPrazoEngine.TipoPrazo> candidatos = new ArrayList<>();
        switch (processo.getRamoDireito() != null ? processo.getRamoDireito() : RamoDireito.CIVIL) {
            case TRABALHISTA -> candidatos.add(NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA);
            case PENAL -> candidatos.add(NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL);
            case ELEITORAL -> candidatos.add(NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL);
            default -> {
                candidatos.add(NationalPrazoEngine.TipoPrazo.CONTESTACAO);
                candidatos.add(NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO);
            }
        }
        return candidatos.stream()
                .map(tipo -> prazoEngine.calcular(processo, tipo))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(NationalPrazoEngine.PrazoCalculado::vencimento, Comparator.nullsLast(LocalDate::compareTo)))
                .orElse(null);
    }

    private Optional<Processo> buscarProcesso(String numeroProcesso) {
        if (blank(numeroProcesso)) {
            return Optional.empty();
        }
        return processoRepository.findByNumeroUnificado(numeroProcesso)
                .or(() -> processoRepository.findByNumeroProcesso(numeroProcesso));
    }

    private boolean atualizarStatusProcesso(Processo processo, StatusProcesso novoStatus, String novoResultado, String mensagem) {
        StatusProcesso anterior = processo.getStatusProcesso();
        String resultadoAnterior = processo.getResultadoFinal();
        if (anterior == novoStatus && Objects.equals(resultadoAnterior, novoResultado)) {
            return anterior == StatusProcesso.AGUARDANDO_PARECER;
        }
        processo.setStatusProcesso(novoStatus);
        processo.setResultadoFinal(novoResultado);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        Processo salvo = processoRepository.save(processo);
        uiHistoryService.recordProcessoStatusChange(salvo, anterior, resultadoAnterior, novoStatus, novoResultado, mensagem);
        return novoStatus == StatusProcesso.AGUARDANDO_PARECER;
    }

    private void atualizarResultadoFinal(Processo processo, String novoResultado) {
        if (processo == null || Objects.equals(processo.getResultadoFinal(), novoResultado)) {
            return;
        }
        processo.setResultadoFinal(novoResultado);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);
    }

    private void agendarSugestaoIA(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sugestaoIA.runForProposal(propostaId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sugestaoIA.runForProposal(propostaId);
            }
        });
    }

    private void publicarEventos(SessaoCejusc sessao, Processo processo, boolean criouOuAtualizouProposta) {
        eventPublisher.publishEvent(new SessaoCejuscRegistradaEvent(
                sessao.sessaoId(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroUnificado() : sessao.numeroProcesso(),
                sessao.resultado(),
                sessao.cejuscCodigo(),
                Instant.now(),
                criouOuAtualizouProposta,
                sessao.termoAcordo() != null && sessao.termoAcordo().precisaHomologacaoJudicial()
        ));

    }

    private void registrarAuditoria(SessaoCejusc sessao, Processo processo, ComplianceSessao compliance, boolean criouOuAtualizouProposta) {
        String resourceId = processo != null && processo.getId() != null
                ? String.valueOf(processo.getId())
                : sessao.sessaoId().toString();
        String payload = String.join("|",
                String.valueOf(sessao.sessaoId()),
                String.valueOf(sessao.resultado()),
                String.valueOf(sessao.modalidade()),
                processo != null ? String.valueOf(processo.getNumeroUnificado()) : "SEM_PROCESSO",
                String.valueOf(criouOuAtualizouProposta),
                String.valueOf(compliance.exigeHomologacao())
        );
        auditLedgerService.appendSafely(
                "CEJUSC_RESULTADO_REGISTRADO",
                processo != null ? RESOURCE_TYPE_PROCESSO : RESOURCE_TYPE_CEJUSC,
                resourceId,
                payload,
                summarizeJustification(compliance)
        );
    }

    private static String summarizeJustification(ComplianceSessao compliance) {
        List<String> pontos = new ArrayList<>();
        if (compliance.exigeAtuacaoMinisterioPublico()) {
            pontos.add("MP");
        }
        if (compliance.exigeHomologacao()) {
            pontos.add("HOMOLOGACAO");
        }
        if (compliance.envolveDireitoIndisponivel()) {
            pontos.add("INDISPONIBILIDADE");
        }
        if (!compliance.pendencias().isEmpty()) {
            pontos.add("PENDENCIAS=" + compliance.pendencias().size());
        }
        return pontos.isEmpty() ? "CEJUSC" : String.join(",", pontos);
    }

    private StatusAcordo resolverStatusProposta(SessaoCejusc sessao, TermoAcordo termo) {
        if (!sessao.possuiAcordo()) {
            return StatusAcordo.EM_NEGOCIACAO;
        }
        if (termo.precisaHomologacaoJudicial()) {
            return StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ;
        }
        long partesPresentes = sessao.participantes().stream()
                .filter(ParticipanteSessao::partePrincipal)
                .filter(ParticipanteSessao::compareceu)
                .count();
        if (partesPresentes >= 2) {
            return StatusAcordo.AGUARDANDO_ASSINATURA_PARTE1;
        }
        return StatusAcordo.EM_NEGOCIACAO;
    }

    private IaSettings settingsCejusc() {
        IaSettings settings = new IaSettings();
        settings.setSuggestionsEnabled(true);
        settings.setPreserveEssence(true);
        settings.setSendToJudgeEnabled(true);
        settings.setSendToProcessEnabled(true);
        settings.setTimeCutoff(Instant.now().plus(2, ChronoUnit.HOURS));
        return settings;
    }

    private String materializarTermo(SessaoCejusc sessao, Processo processo, TermoAcordo termo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section><h2>Termo CEJUSC</h2>");
        if (processo != null) {
            sb.append("<p><strong>Processo:</strong> ").append(safe(processo.getNumeroUnificado())).append("</p>");
        }
        sb.append("<p><strong>Modalidade:</strong> ").append(sessao.modalidade() != null ? sessao.modalidade().name() : "NAO_INFORMADA").append("</p>");
        sb.append("<p><strong>Resultado:</strong> ").append(sessao.resultado() != null ? sessao.resultado().name() : "NAO_INFORMADO").append("</p>");
        if (!blank(termo.conteudo())) {
            sb.append("<div>").append(escapeHtml(termo.conteudo())).append("</div>");
        }
        if (termo.valorAcordado() != null && termo.valorAcordado().signum() > 0) {
            sb.append("<p><strong>Valor:</strong> ").append(termo.valorAcordado().setScale(2, RoundingMode.HALF_UP)).append("</p>");
        }
        if (termo.dataCumprimento() != null) {
            sb.append("<p><strong>Cumprimento:</strong> ").append(termo.dataCumprimento()).append("</p>");
        }
        if (!termo.obrigacoesFazer().isEmpty()) {
            sb.append("<ul>");
            termo.obrigacoesFazer().forEach(o -> sb.append("<li>FAZER: ").append(escapeHtml(o)).append("</li>"));
            sb.append("</ul>");
        }
        if (!termo.obrigacoesNaoFazer().isEmpty()) {
            sb.append("<ul>");
            termo.obrigacoesNaoFazer().forEach(o -> sb.append("<li>NÃO FAZER: ").append(escapeHtml(o)).append("</li>"));
            sb.append("</ul>");
        }
        sb.append("<p><strong>Fundamento:</strong> ").append(safe(blank(termo.fundamentoLegal()) ? "Lei 13.140/2015 + CPC arts. 165-175" : termo.fundamentoLegal())).append("</p>");
        sb.append("</section>");
        return sb.toString();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Ô', 'O').replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }
}
