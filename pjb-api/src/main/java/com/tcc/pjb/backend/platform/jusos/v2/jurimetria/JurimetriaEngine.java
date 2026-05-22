package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import com.tcc.pjb.backend.ai.jurimetria.JurimetriaService;
import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsAggregationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.jurisprudencia.JurisprudenciaService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class JurimetriaEngine {

    private static final Logger log = LoggerFactory.getLogger(JurimetriaEngine.class);
    private static final String RESOURCE_TYPE_PROCESSO = "PROCESSO";
    private static final String EVENT_TYPE_JURIMETRIA = "JURIMETRIA_RELATORIO_GERADO";

    public record IndicadorJurimetrico(
            String nome,
            double valor,
            String unidade,
            double confianca,
            String fonte,
            String interpretacao
    ) {}

    public record PerfisDecisional(
            String tribunal,
            String orgaoJulgador,
            double taxaProvimento,
            double taxaDesprovimento,
            double taxaParcialProvimento,
            int totalDecisoes,
            double tempoMedioJulgamentoDias,
            double indiceCongestionamento,
            double taxaAcordo,
            List<String> tesesMaisFrequentes
    ) {
        public PerfisDecisional {
            tesesMaisFrequentes = JurimetriaSupportUtils.immutableDistinct(tesesMaisFrequentes);
        }
    }

    public record RiscoJuridico(
            double probabilidadeExito,
            double probabilidadeDerrota,
            double probabilidadeAcordo,
            NivelRisco nivel,
            List<String> fatoresPositivos,
            List<String> fatoresNegativos,
            List<String> recomendacoesEstrategicas,
            BigDecimal estimativaCondenaMinima,
            BigDecimal estimativaCondenaMaxima,
            BigDecimal estimativaCondenaMedia,
            BigDecimal exposicaoFinanceiraProjetada
    ) {
        public RiscoJuridico {
            fatoresPositivos = JurimetriaSupportUtils.immutableDistinct(fatoresPositivos);
            fatoresNegativos = JurimetriaSupportUtils.immutableDistinct(fatoresNegativos);
            recomendacoesEstrategicas = JurimetriaSupportUtils.immutableDistinct(recomendacoesEstrategicas);
            estimativaCondenaMinima = JurimetriaSupportUtils.scale(estimativaCondenaMinima);
            estimativaCondenaMaxima = JurimetriaSupportUtils.scale(estimativaCondenaMaxima);
            estimativaCondenaMedia = JurimetriaSupportUtils.scale(estimativaCondenaMedia);
            exposicaoFinanceiraProjetada = JurimetriaSupportUtils.scale(exposicaoFinanceiraProjetada);
        }
    }

    public enum NivelRisco {
        MUITO_FAVORAVEL(0.75, 1.01, "Alta probabilidade de êxito"),
        FAVORAVEL(0.55, 0.75, "Probabilidade moderada de êxito"),
        NEUTRO(0.40, 0.55, "Resultado incerto — depende de prova, timing e órgão julgador"),
        DESFAVORAVEL(0.20, 0.40, "Probabilidade de insucesso elevada"),
        MUITO_DESFAVORAVEL(0.00, 0.20, "Altíssimo risco — estratégia deve ser recalibrada");

        public final double limiteInferior;
        public final double limiteSuperior;
        public final String descricao;

        NivelRisco(double limiteInferior, double limiteSuperior, String descricao) {
            this.limiteInferior = limiteInferior;
            this.limiteSuperior = limiteSuperior;
            this.descricao = descricao;
        }

        public static NivelRisco from(double probabilidade) {
            for (NivelRisco nivel : values()) {
                if (probabilidade >= nivel.limiteInferior && probabilidade < nivel.limiteSuperior) {
                    return nivel;
                }
            }
            return NEUTRO;
        }
    }

    public record CenarioEstrategico(
            String nome,
            double probabilidade,
            String orientacao,
            BigDecimal impactoFinanceiro,
            List<String> acoesRecomendadas,
            List<String> gatilhosMonitoramento
    ) {
        public CenarioEstrategico {
            impactoFinanceiro = JurimetriaSupportUtils.scale(impactoFinanceiro);
            acoesRecomendadas = JurimetriaSupportUtils.immutableDistinct(acoesRecomendadas);
            gatilhosMonitoramento = JurimetriaSupportUtils.immutableDistinct(gatilhosMonitoramento);
        }
    }

    public record RelatorioJurimetrico(
            String numeroUnificado,
            RamoDireito ramo,
            GrauJurisdicao grau,
            String tribunal,
            RiscoJuridico risco,
            List<IndicadorJurimetrico> indicadores,
            PerfisDecisional perfilTribunal,
            List<String> precedentesRelevantes,
            List<String> alertasEstrategicos,
            List<CenarioEstrategico> cenarios,
            String metodologia,
            Instant geradoEm
    ) {
        public RelatorioJurimetrico {
            indicadores = List.copyOf(indicadores == null ? List.of() : indicadores);
            precedentesRelevantes = JurimetriaSupportUtils.immutableDistinct(precedentesRelevantes);
            alertasEstrategicos = JurimetriaSupportUtils.immutableDistinct(alertasEstrategicos);
            cenarios = List.copyOf(cenarios == null ? List.of() : cenarios);
        }
    }

    public record JurimetriaGeradaEvent(
            Long processoId,
            String numeroUnificado,
            RamoDireito ramo,
            GrauJurisdicao grau,
            NivelRisco nivelRisco,
            double probabilidadeExito,
            double probabilidadeAcordo,
            Instant emitidoEm
    ) {}

    record BaseLocalAnalitica(
            int totalMesmoRamo,
            int totalMesmoRamoNoTribunal,
            int totalMesmoGrau,
            int totalJulgados,
            int totalRecursais,
            int totalComAcordo,
            int totalComTutela,
            double tempoMedioDias,
            double taxaRecurso,
            double taxaAcordo,
            double indiceCongestionamento,
            double taxaEncerramento,
            double indiceUrgencia
    ) {}

    private final ProcessoRepository processoRepository;
    private final ProcessoAnalyticsAggregationService analyticsService;
    private final JurimetriaService jurimetriaService;
    private final JurisprudenciaService jurisprudenciaService;
    private final NationalRulePackEngine rulePackEngine;
    private final NationalPrazoEngine prazoEngine;
    private final AuditLedgerService auditLedgerService;
    private final UiHistoryService uiHistoryService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final JurimetriaAggregationSupport aggregationSupport;
    private final JurimetriaRiskAnalysisSupport riskAnalysisSupport;
    private final JurimetriaNarrativeSupport narrativeSupport;

    public JurimetriaEngine(ProcessoRepository processoRepository,
                            ProcessoAnalyticsAggregationService analyticsService,
                            JurimetriaService jurimetriaService,
                            JurisprudenciaService jurisprudenciaService,
                            NationalRulePackEngine rulePackEngine,
                            NationalPrazoEngine prazoEngine,
                            AuditLedgerService auditLedgerService,
                            UiHistoryService uiHistoryService,
                            CurrentUserService currentUserService,
                            ApplicationEventPublisher eventPublisher,
                            JurimetriaAggregationSupport aggregationSupport,
                            JurimetriaRiskAnalysisSupport riskAnalysisSupport,
                            JurimetriaNarrativeSupport narrativeSupport) {
        this.processoRepository = processoRepository;
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
        this.jurimetriaService = jurimetriaService;
        this.jurisprudenciaService = jurisprudenciaService;
        this.rulePackEngine = rulePackEngine;
        this.prazoEngine = prazoEngine;
        this.auditLedgerService = auditLedgerService;
        this.uiHistoryService = uiHistoryService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
        this.aggregationSupport = aggregationSupport;
        this.riskAnalysisSupport = riskAnalysisSupport;
        this.narrativeSupport = narrativeSupport;
    }

    public RelatorioJurimetrico analisar(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        Processo alvo = carregarCompletoSePossivel(processo);
        RamoDireito ramo = alvo.getRamoDireito() != null ? alvo.getRamoDireito() : RamoDireito.CIVIL;
        GrauJurisdicao grau = resolverGrau(alvo);
        String tribunal = resolverTribunal(alvo);
        TribunalFonte fonteTribunal = resolverFonteTribunal(tribunal, ramo, grau);

        BaseLocalAnalitica baseLocal = aggregationSupport.carregarBaseLocal(analyticsService, ramo, grau, tribunal);
        JurimetriaReport relatorioIA = gerarRelatorioIA(alvo, tribunal);
        NationalRulePackEngine.ResultadoRegras regras = aplicarRegras(alvo, ramo, grau, tribunal);
        List<Precedente> precedentes = buscarPrecedentes(alvo, fonteTribunal, ramo);
        NationalPrazoEngine.PrazoCalculado prazoSensivel = calcularPrazoSensivel(alvo, ramo, grau, tribunal);

        List<IndicadorJurimetrico> indicadores = aggregationSupport.calcularIndicadores(
                alvo,
                ramo,
                grau,
                tribunal,
                baseLocal,
                relatorioIA,
                precedentes,
                regras,
                prazoSensivel
        );
        PerfisDecisional perfil = aggregationSupport.construirPerfilTribunal(tribunal, fonteTribunal, ramo, baseLocal, precedentes);
        RiscoJuridico risco = riskAnalysisSupport.calcularRisco(alvo, ramo, grau, perfil, precedentes, regras, relatorioIA, prazoSensivel, baseLocal);
        List<String> precedentesFormatados = narrativeSupport.formatarPrecedentes(precedentes);
        List<String> alertas = riskAnalysisSupport.gerarAlertasEstrategicos(alvo, ramo, grau, risco, perfil, regras, prazoSensivel, baseLocal, precedentes);
        List<CenarioEstrategico> cenarios = riskAnalysisSupport.construirCenarios(alvo, risco, baseLocal, perfil, prazoSensivel);

        RelatorioJurimetrico relatorio = new RelatorioJurimetrico(
                alvo.getNumeroUnificado(),
                ramo,
                grau,
                tribunal,
                risco,
                indicadores,
                perfil,
                precedentesFormatados,
                alertas,
                cenarios,
                narrativeSupport.construirMetodologia(regras, prazoSensivel, relatorioIA, precedentes.size()),
                Instant.now()
        );

        registrarAuditoria(alvo, relatorio);
        registrarUi(alvo, relatorio);
        publicarEvento(alvo, relatorio);

        log.info("[Jurimetria] Relatório gerado: numero={} ramo={} grau={} risco={} exito={}",
                alvo.getNumeroUnificado(),
                ramo,
                grau,
                risco.nivel(),
                JurimetriaSupportUtils.round(risco.probabilidadeExito()));

        return relatorio;
    }

    public RelatorioJurimetrico analisarPorNumero(String numeroUnificado) {
        Objects.requireNonNull(numeroUnificado, "numeroUnificado");
        Processo processo = processoRepository.findByNumeroUnificado(numeroUnificado)
                .or(() -> processoRepository.findByNumeroProcesso(numeroUnificado))
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + numeroUnificado));
        return analisar(processo);
    }

    private List<Precedente> buscarPrecedentes(Processo processo, TribunalFonte fonteTribunal, RamoDireito ramo) {
        String query = buildPrecedentQuery(processo);
        try {
            Page<Precedente> pagina = jurisprudenciaService.search(
                    fonteTribunal,
                    null,
                    ramo,
                    processo.getRito(),
                    query,
                    0,
                    8
            );
            return pagina == null ? List.of() : pagina.getContent();
        } catch (Exception e) {
            log.warn("[Jurimetria] Falha ao buscar precedentes para processo {}: {}", processo.getNumeroUnificado(), e.getMessage());
            return List.of();
        }
    }

    private NationalRulePackEngine.ResultadoRegras aplicarRegras(Processo processo,
                                                                 RamoDireito ramo,
                                                                 GrauJurisdicao grau,
                                                                 String tribunal) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("valorCausa", processo.getValorCausa());
        extras.put("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        extras.put("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        extras.put("sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
        return rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                ramo,
                grau,
                tribunal,
                extras
        ));
    }

    private NationalPrazoEngine.PrazoCalculado calcularPrazoSensivel(Processo processo,
                                                                     RamoDireito ramo,
                                                                     GrauJurisdicao grau,
                                                                     String tribunal) {
        NationalPrazoEngine.TipoPrazo tipo = switch (grau) {
            case PRIMEIRO_GRAU -> NationalPrazoEngine.TipoPrazo.CONTESTACAO;
            case SEGUNDO_GRAU -> NationalPrazoEngine.TipoPrazo.APELACAO;
            case SUPERIOR -> NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL;
            case CONSTITUCIONAL -> NationalPrazoEngine.TipoPrazo.RECURSO_EXTRAORDINARIO;
        };
        try {
            return prazoEngine.calcularPorRamo(
                    resolverMarcoTemporal(processo).toLocalDate(),
                    tipo,
                    ramo,
                    grau,
                    tribunal
            );
        } catch (Exception e) {
            log.warn("[Jurimetria] Falha ao calcular prazo sensível para processo {}: {}", processo.getNumeroUnificado(), e.getMessage());
            return null;
        }
    }

    private JurimetriaReport gerarRelatorioIA(Processo processo, String tribunal) {
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("ramo", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        filtros.put("grau", processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null ? processo.getJurisdicao().getGrau().name() : null);
        filtros.put("status", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        filtros.put("fase", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        filtros.put("sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
        return jurimetriaService.gerarRelatorio(
                buildTeseReferencia(processo),
                tribunal,
                JurimetriaSupportUtils.processLabel(processo.getClasseProcessual()),
                JurimetriaSupportUtils.processLabel(processo.getAssunto()),
                filtros
        );
    }

    private void registrarAuditoria(Processo processo, RelatorioJurimetrico relatorio) {
        auditLedgerService.appendSafely(
                "JURIMETRIA_RELATORIO_GERADO",
                RESOURCE_TYPE_PROCESSO,
                safeId(processo.getId()),
                hashMaterial(processo.getNumeroUnificado() + "|" + relatorio.risco().nivel().name() + "|" + relatorio.risco().probabilidadeExito()),
                JurimetriaSupportUtils.truncate("ramo=" + relatorio.ramo() + ",grau=" + relatorio.grau() + ",tribunal=" + relatorio.tribunal(), 160)
        );
    }

    private void registrarUi(Processo processo, RelatorioJurimetrico relatorio) {
        Usuario actor = currentUserService.getOrNull();
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.INFO, UiToken.ASSUNTO);
        if (relatorio.risco().nivel() == NivelRisco.DESFAVORAVEL || relatorio.risco().nivel() == NivelRisco.MUITO_DESFAVORAVEL) {
            tokens.add(UiToken.URGENTE);
            tokens.add(UiToken.BLOQUEANTE);
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().name().contains("SIGILO")) {
            tokens.add(UiToken.SIGILOSO);
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            tokens.add(UiToken.RECURSO);
        }
        uiHistoryService.recordInboxEvent(
                "jurimetria:processo:" + safeId(processo.getId()),
                processo.getId(),
                EVENT_TYPE_JURIMETRIA,
                tokens,
                actor != null ? actor.getId() : null,
                actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : null,
                JurimetriaSupportUtils.truncate(
                        "Jurimetria atualizada para " + processo.getNumeroUnificado() + " com nível " + relatorio.risco().nivel().name() + " e exito estimado em " + percentage(relatorio.risco().probabilidadeExito()),
                        240
                )
        );
    }

    private void publicarEvento(Processo processo, RelatorioJurimetrico relatorio) {
        eventPublisher.publishEvent(new JurimetriaGeradaEvent(
                processo.getId(),
                processo.getNumeroUnificado(),
                relatorio.ramo(),
                relatorio.grau(),
                relatorio.risco().nivel(),
                relatorio.risco().probabilidadeExito(),
                relatorio.risco().probabilidadeAcordo(),
                relatorio.geradoEm()
        ));
    }

    private Processo carregarCompletoSePossivel(Processo processo) {
        if (processo.getId() == null) {
            return processo;
        }
        return processoRepository.findProcessoCompletoById(processo.getId()).orElse(processo);
    }

    private TribunalFonte resolverFonteTribunal(String tribunal, RamoDireito ramo, GrauJurisdicao grau) {
        String token = JurimetriaSupportUtils.normalizeToken(tribunal);
        if (token.startsWith("STF") || grau == GrauJurisdicao.CONSTITUCIONAL) {
            return TribunalFonte.STF;
        }
        if (token.startsWith("STJ") || grau == GrauJurisdicao.SUPERIOR && ramo != RamoDireito.TRABALHISTA && ramo != RamoDireito.ELEITORAL && ramo != RamoDireito.MILITAR) {
            return TribunalFonte.STJ;
        }
        if (token.startsWith("TST") || ramo == RamoDireito.TRABALHISTA) {
            return TribunalFonte.TST;
        }
        if (token.startsWith("TSE") || token.startsWith("TRE") || ramo == RamoDireito.ELEITORAL) {
            return token.startsWith("TRE") ? TribunalFonte.TRE : TribunalFonte.TSE;
        }
        if (token.startsWith("STM") || ramo == RamoDireito.MILITAR) {
            return TribunalFonte.STM;
        }
        if (token.startsWith("TRF")) {
            return TribunalFonte.TRF;
        }
        if (token.startsWith("TRT")) {
            return TribunalFonte.TRT;
        }
        if (token.startsWith("TJ")) {
            return TribunalFonte.TJ;
        }
        return TribunalFonte.OUTRO;
    }

    private LocalDateTime resolverMarcoTemporal(Processo processo) {
        if (processo.getDataUltimaMovimentacao() != null) {
            return processo.getDataUltimaMovimentacao();
        }
        if (processo.getDataCriacao() != null) {
            return processo.getDataCriacao();
        }
        if (processo.getDataDistribuicao() != null) {
            return processo.getDataDistribuicao();
        }
        return LocalDateTime.now();
    }

    private String buildTeseReferencia(Processo processo) {
        return JurimetriaSupportUtils.truncate(String.join(" | ", List.of(
                JurimetriaSupportUtils.processLabel(processo.getClasseProcessual()),
                JurimetriaSupportUtils.processLabel(processo.getAssunto()),
                JurimetriaSupportUtils.processLabel(processo.getResumoIA())
        )), 220);
    }

    private String buildPrecedentQuery(Processo processo) {
        List<String> partes = new ArrayList<>();
        JurimetriaSupportUtils.addIfHasText(partes, processo.getClasseProcessual());
        JurimetriaSupportUtils.addIfHasText(partes, processo.getAssunto());
        JurimetriaSupportUtils.addIfHasText(partes, processo.getResumoIA());
        if (processo.getRamoDireito() != null) {
            partes.add(processo.getRamoDireito().getDescricao());
        }
        String query = String.join(" ", partes);
        return JurimetriaSupportUtils.truncate(query.isBlank() ? JurimetriaSupportUtils.processLabel(processo.getNumeroUnificado()) : query, 220);
    }

    private static String safeId(Long id) {
        return id == null ? "0" : String.valueOf(id);
    }

    private static String hashMaterial(String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(String.valueOf(material).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(material));
        }
    }

    private static String percentage(double ratio) {
        return BigDecimal.valueOf(ratio * 100.0).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private static GrauJurisdicao resolverGrau(Processo processo) {
        return processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null
                ? processo.getJurisdicao().getGrau()
                : GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private static String resolverTribunal(Processo processo) {
        if (processo.getJurisdicao() == null) {
            return "DESCONHECIDO";
        }
        return JurimetriaSupportUtils.firstNonBlank(
                processo.getJurisdicao().getCodigo(),
                processo.getJurisdicao().getSigla(),
                processo.getJurisdicao().getNome(),
                "DESCONHECIDO"
        );
    }
}
