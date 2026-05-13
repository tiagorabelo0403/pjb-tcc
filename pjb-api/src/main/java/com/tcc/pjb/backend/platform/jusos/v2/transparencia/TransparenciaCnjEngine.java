package com.tcc.pjb.backend.platform.jusos.v2.transparencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.PrazoCalculado;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.TipoPrazo;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;

@Service
public class TransparenciaCnjEngine {

    private static final String RESOURCE_TYPE = "TRANSPARENCIA_CNJ";
    private static final PageRequest COUNT_PAGE = PageRequest.of(0, 1);
    private static final List<String> UFS_SUDESTE = List.of("SP", "RJ", "MG", "ES");
    private static final List<String> UFS_NORDESTE = List.of("AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE");
    private static final List<RamoDireito> RAMOS_PADRAO_DASHBOARD = List.of(
            RamoDireito.CIVIL,
            RamoDireito.PENAL,
            RamoDireito.TRABALHISTA,
            RamoDireito.PREVIDENCIARIO,
            RamoDireito.FAMILIA,
            RamoDireito.CONSUMIDOR,
            RamoDireito.TRIBUTARIO,
            RamoDireito.INFANCIA_JUVENTUDE,
            RamoDireito.ADMINISTRATIVO,
            RamoDireito.AMBIENTAL,
            RamoDireito.INTERNACIONAL
    );

    public enum MetaCNJ {
        META_1_JULGAR_MAIS_QUE_INGRESSOU("Meta 1 — Julgar mais processos que os distribuídos"),
        META_2_ACERVO_ANTIGO("Meta 2 — Julgar processos mais antigos"),
        META_4_IMPROBIDADE("Meta 4 — Julgar ações de improbidade administrativa"),
        META_6_PRECATORIOS("Meta 6 — Monitoramento de precatórios"),
        META_7_INFANCIA_JUVENTUDE("Meta 7 — Julgar processos de infância e juventude"),
        META_8_EXECUCAO_PENAL("Meta 8 — Julgar execuções penais"),
        META_9_INOVACAO_DADOS("Meta 9 — Governança de dados, transparência e interoperabilidade");

        public final String descricao;

        MetaCNJ(String descricao) {
            this.descricao = descricao;
        }
    }

    public enum StatusConformidadeDataJud {
        CONFORME,
        NAO_CONFORME_CAMPOS_FALTANTES,
        NAO_CONFORME_FORMATO_INVALIDO,
        PENDENTE_ENVIO,
        ENVIADO_AGUARDANDO_CONFIRMACAO,
        ERRO_REJEICAO_CNJ
    }

    public enum PrioridadeGestao {
        NORMAL,
        ATENCAO,
        CRITICA,
        ESTRATEGICA
    }

    public record MetricaProcessual(
            String nome,
            long valor,
            String unidade,
            double percentualMeta,
            boolean dentroMeta,
            MetaCNJ metaCnj,
            String interpretacao,
            PrioridadeGestao prioridade,
            List<String> tags
    ) {
        public boolean critica() {
            return prioridade == PrioridadeGestao.CRITICA || prioridade == PrioridadeGestao.ESTRATEGICA;
        }
    }

    public record SlaProcessual(
            String tribunalCodigo,
            GrauJurisdicao grau,
            RamoDireito ramo,
            long totalProcessos,
            long estimativaForaSla,
            double percentualConformidade,
            long tempoMedioEstimado,
            long tempoMaximoTolerado,
            Instant calculadoEm,
            Instant proximaRevisaoSugerida,
            List<String> alertas,
            String fundamentoOperacional
    ) {}

    public record DistribuicaoRamo(
            RamoDireito ramo,
            long totalEstimado,
            double participacaoPercentual,
            PrioridadeGestao prioridade,
            List<String> alertas
    ) {}

    public record RelatorioConformidadeCnj(
            String tribunalCodigo,
            String competencia,
            List<MetricaProcessual> metricas,
            StatusConformidadeDataJud statusDataJud,
            List<String> camposPendentesDataJud,
            List<String> alertasConformidade,
            List<String> recomendacoes,
            double scoreGeral,
            Instant geradoEm,
            String hashIntegridade,
            List<String> gapsEstruturais
    ) {
        public boolean emRisco() {
            return scoreGeral < 70.0 || statusDataJud != StatusConformidadeDataJud.CONFORME;
        }
    }

    public record DashboardGerencial(
            String tribunal,
            long totalAcervo,
            long processosEncerradosProxy,
            double taxaCongestionamento,
            double taxaResolutividade,
            long acervoUfSudeste,
            long acervoUfNordeste,
            List<String> alertasGestao,
            List<SlaProcessual> slas,
            Instant geradoEm,
            List<DistribuicaoRamo> distribuicaoEstimada,
            double scoreSaudeOperacional,
            List<String> recomendacoesOperacionais,
            String hashIntegridade
    ) {
        public boolean congestionamentoCritico() {
            return taxaCongestionamento >= 0.80;
        }
    }

    public record SnapshotTransparencia(
            String tribunal,
            double scoreSaudeOperacional,
            double scoreConformidade,
            Instant geradoEm,
            String actor,
            String hashIntegridade,
            List<String> destaques
    ) {}

    public record TransparenciaSnapshotEvent(
            String tribunal,
            Instant geradoEm,
            String hashIntegridade,
            double scoreConformidade,
            double scoreSaudeOperacional,
            List<String> alertas
    ) {}

    public record ConfiguracaoTribunal(
            String tribunalCodigo,
            double fatorSla,
            double fatorCongestionamentoCritico,
            Set<MetaCNJ> metasPrioritarias,
            List<String> recomendacoesFixas
    ) {
        public static ConfiguracaoTribunal padrao(String tribunalCodigo) {
            return new ConfiguracaoTribunal(
                    normalizarTribunal(tribunalCodigo),
                    1.0,
                    0.80,
                    EnumSet.of(MetaCNJ.META_1_JULGAR_MAIS_QUE_INGRESSOU, MetaCNJ.META_2_ACERVO_ANTIGO, MetaCNJ.META_9_INOVACAO_DADOS),
                    List.of(
                            "Integrar DataJudEmitter com rotina de conferência mensal.",
                            "Priorizar revisão de TPU e dados mínimos dos processos críticos.",
                            "Usar painel gerencial para mutirão focalizado por ramo e UF."
                    )
            );
        }
    }

    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedger;
    private final CurrentUserService currentUserService;
    private final UiHistoryService uiHistoryService;
    private final NationalRulePackEngine rulePackEngine;
    private final NationalPrazoEngine prazoEngine;
    private final ConfiguracaoDistribuicaoVaraService distribuicaoService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, ConfiguracaoTribunal> configuracoes = new ConcurrentHashMap<>();

    public TransparenciaCnjEngine(
            ProcessoRepository processoRepository,
            AuditLedgerService auditLedger,
            CurrentUserService currentUserService,
            UiHistoryService uiHistoryService,
            NationalRulePackEngine rulePackEngine,
            NationalPrazoEngine prazoEngine,
            ConfiguracaoDistribuicaoVaraService distribuicaoService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.rulePackEngine = Objects.requireNonNull(rulePackEngine, "rulePackEngine");
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.distribuicaoService = Objects.requireNonNull(distribuicaoService, "distribuicaoService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public void registrarConfiguracaoTribunal(ConfiguracaoTribunal configuracao) {
        Objects.requireNonNull(configuracao, "configuracao");
        configuracoes.put(normalizarTribunal(configuracao.tribunalCodigo()), configuracao);
    }

    public RelatorioConformidadeCnj gerarRelatorioConformidade(String tribunalCodigo, String competencia) {
        String tribunal = normalizarTribunal(tribunalCodigo);
        ConfiguracaoTribunal configuracao = configuracao(tribunal);
        ConfiguracaoDistribuicaoVaraService.DashboardDistribuicao dashboardDistribuicao = carregarDashboardDistribuicao(tribunal);
        long totalAcervo = totalAcervoBase(dashboardDistribuicao);
        long julgadosProxy = countByStatus(StatusProcesso.JULGADO);
        long arquivadosProxy = countByStatus(StatusProcesso.ARQUIVADO);
        long baixadosProxy = countByStatus(StatusProcesso.BAIXADO);
        long encerradosProxy = julgadosProxy + arquivadosProxy + baixadosProxy;
        double taxaResolutividade = percentual(encerradosProxy, totalAcervo);
        double taxaCongestionamento = dashboardDistribuicao != null && dashboardDistribuicao.capacidadeTotal() > 0
                ? dashboardDistribuicao.ocupacaoGlobal()
                : totalAcervo > 0 ? round4(1.0 - (double) encerradosProxy / (double) totalAcervo) : 0.0;

        List<MetricaProcessual> metricas = new ArrayList<>();
        metricas.add(new MetricaProcessual(
                "acervo_total",
                totalAcervo,
                "processos",
                1.0,
                true,
                MetaCNJ.META_9_INOVACAO_DADOS,
                "Volume total acompanhado pelo painel nacional.",
                totalAcervo > 100_000 ? PrioridadeGestao.ESTRATEGICA : PrioridadeGestao.ATENCAO,
                List.of("acervo", "capacidade", "governanca")
        ));
        if (dashboardDistribuicao != null && dashboardDistribuicao.capacidadeTotal() > 0) {
            metricas.add(new MetricaProcessual(
                    "capacidade_instalada_real",
                    dashboardDistribuicao.capacidadeTotal(),
                    "processos_capacidade",
                    1.0,
                    true,
                    MetaCNJ.META_9_INOVACAO_DADOS,
                    "Capacidade operacional consolidada por vara com base em dados reais de distribuição.",
                    dashboardDistribuicao.ocupacaoGlobal() >= configuracao.fatorCongestionamentoCritico() ? PrioridadeGestao.ESTRATEGICA : PrioridadeGestao.ATENCAO,
                    List.of("capacidade_real", "distribuicao_vara", "ocupacao")
            ));
        }
        metricas.add(new MetricaProcessual(
                "processos_encerrados_proxy",
                encerradosProxy,
                "processos",
                taxaResolutividade / 100.0,
                taxaResolutividade >= 100.0,
                MetaCNJ.META_1_JULGAR_MAIS_QUE_INGRESSOU,
                taxaResolutividade >= 100.0 ? "Há sinal de absorção do acervo." : "Encerramento proxy abaixo do ideal para redução do estoque.",
                taxaResolutividade < 55.0 ? PrioridadeGestao.CRITICA : PrioridadeGestao.ATENCAO,
                List.of("julgados", "baixados", "encerramento")
        ));
        metricas.add(new MetricaProcessual(
                "taxa_resolutividade_proxy",
                Math.round(taxaResolutividade),
                "%",
                taxaResolutividade / 100.0,
                taxaResolutividade >= 100.0,
                MetaCNJ.META_1_JULGAR_MAIS_QUE_INGRESSOU,
                taxaResolutividade >= 80.0 ? "Ritmo operacional aceitável, mas ainda sujeito a pressão do acervo." : "Produção proxy aquém do estoque atual.",
                taxaResolutividade < 60.0 ? PrioridadeGestao.CRITICA : PrioridadeGestao.ATENCAO,
                List.of("meta1", "resolutividade")
        ));
        metricas.add(new MetricaProcessual(
                "taxa_congestionamento_proxy",
                Math.round(taxaCongestionamento * 100.0),
                "%",
                Math.max(0.0, 1.0 - taxaCongestionamento),
                taxaCongestionamento < configuracao.fatorCongestionamentoCritico(),
                MetaCNJ.META_2_ACERVO_ANTIGO,
                taxaCongestionamento >= configuracao.fatorCongestionamentoCritico() ? "Congestionamento elevado, exigir mutirão focalizado e triagem reforçada." : "Congestionamento sob controle relativo.",
                taxaCongestionamento >= configuracao.fatorCongestionamentoCritico() ? PrioridadeGestao.ESTRATEGICA : PrioridadeGestao.ATENCAO,
                List.of("backlog", "congestionamento")
        ));

        long estimativaInfancia = estimarVolumePorRamo(totalAcervo, RamoDireito.INFANCIA_JUVENTUDE);
        metricas.add(new MetricaProcessual(
                "acervo_infancia_juventude_est",
                estimativaInfancia,
                "processos_estimados",
                1.0,
                true,
                MetaCNJ.META_7_INFANCIA_JUVENTUDE,
                "Estimativa estratégica para priorização de infância e juventude com base em peso histórico e regras do ramo.",
                estimativaInfancia > 0 ? PrioridadeGestao.ATENCAO : PrioridadeGestao.NORMAL,
                List.of("infancia", "prioridade_absoluta")
        ));

        long estimativaExecucaoPenal = estimarVolumePorRamo(totalAcervo, RamoDireito.PENAL);
        metricas.add(new MetricaProcessual(
                "acervo_execucao_penal_est",
                estimativaExecucaoPenal,
                "processos_estimados",
                1.0,
                true,
                MetaCNJ.META_8_EXECUCAO_PENAL,
                "Estimativa para célula penal e monitoramento de execução penal sob ótica gerencial.",
                estimativaExecucaoPenal > 0 ? PrioridadeGestao.ATENCAO : PrioridadeGestao.NORMAL,
                List.of("penal", "execucao_penal")
        ));

        long estimativaImprobidade = estimarVolumeAdministrativo(totalAcervo);
        metricas.add(new MetricaProcessual(
                "improbidade_administrativa_est",
                estimativaImprobidade,
                "processos_estimados",
                1.0,
                true,
                MetaCNJ.META_4_IMPROBIDADE,
                "Estimativa de carteira administrativa com potencial aderência à improbidade e controle sancionatório.",
                estimativaImprobidade > 0 ? PrioridadeGestao.ATENCAO : PrioridadeGestao.NORMAL,
                List.of("improbidade", "administrativo")
        ));

        List<String> camposPendentes = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();
        List<String> gapsEstruturais = new ArrayList<>();

        verificarCamposDataJud(camposPendentes, alertas, gapsEstruturais);
        gerarRecomendacoes(recomendacoes, taxaResolutividade, totalAcervo, configuracao);
        gerarAlertasMeta(metricas, alertas, configuracao);

        double score = calcularScore(metricas, camposPendentes);
        StatusConformidadeDataJud statusDataJud = resolverStatusDataJud(camposPendentes, score);
        String hashIntegridade = hashRelatorio(tribunal, competencia, score, metricas, camposPendentes, alertas);

        RelatorioConformidadeCnj relatorio = new RelatorioConformidadeCnj(
                tribunal,
                competencia != null ? competencia.trim() : "geral",
                Collections.unmodifiableList(metricas),
                statusDataJud,
                Collections.unmodifiableList(camposPendentes),
                Collections.unmodifiableList(alertas),
                Collections.unmodifiableList(recomendacoes),
                score,
                Instant.now(),
                hashIntegridade,
                Collections.unmodifiableList(gapsEstruturais)
        );

        registrarObservabilidadeTransparencia(
                "TRANSPARENCIA_CNJ_RELATORIO",
                tribunal,
                hashIntegridade,
                relatorio.alertasConformidade(),
                relatorio.scoreGeral(),
                null
        );
        return relatorio;
    }

    public DashboardGerencial gerarDashboard(String tribunal) {
        String tribunalNormalizado = normalizarTribunal(tribunal);
        ConfiguracaoTribunal configuracao = configuracao(tribunalNormalizado);
        ConfiguracaoDistribuicaoVaraService.DashboardDistribuicao dashboardDistribuicao = carregarDashboardDistribuicao(tribunalNormalizado);
        long totalAcervo = totalAcervoBase(dashboardDistribuicao);
        long encerrados = countByStatus(StatusProcesso.BAIXADO) + countByStatus(StatusProcesso.ARQUIVADO) + countByStatus(StatusProcesso.JULGADO);
        double congestionamento = dashboardDistribuicao != null && dashboardDistribuicao.capacidadeTotal() > 0
                ? dashboardDistribuicao.ocupacaoGlobal()
                : totalAcervo > 0 ? round4(1.0 - ((double) encerrados / (double) totalAcervo)) : 0.0;
        double resolutividade = totalAcervo > 0 ? round4((double) encerrados / (double) totalAcervo) : 0.0;

        long acervoUfSudeste = somarAcervoPorUfs(UFS_SUDESTE);
        long acervoUfNordeste = somarAcervoPorUfs(UFS_NORDESTE);

        List<String> alertas = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();
        if (congestionamento >= configuracao.fatorCongestionamentoCritico()) {
            alertas.add("Congestionamento crítico — ativar mutirão focalizado por ramo, vara e fila antiga.");
        }
        if (resolutividade < 0.50) {
            alertas.add("Resolutividade proxy abaixo de 50% — revisar triagem cartorária, automação e filtros de admissibilidade.");
        }
        if (acervoUfNordeste > acervoUfSudeste && acervoUfNordeste > 0) {
            alertas.add("Concentração operacional no Nordeste acima do Sudeste dentro do recorte monitorado — recalibrar alocação territorial.");
        }
        if (dashboardDistribuicao != null) {
            if (dashboardDistribuicao.varasCriticas() > 0) {
                alertas.add("Há " + dashboardDistribuicao.varasCriticas() + " varas críticas com ocupação acima do limiar operacional.");
            }
            if (dashboardDistribuicao.varasFechadas() > 0) {
                alertas.add("Há " + dashboardDistribuicao.varasFechadas() + " varas temporariamente fechadas para distribuição.");
            }
            if (dashboardDistribuicao.varasEmMutirao() > 0) {
                recomendacoes.add("Compatibilizar cronograma de mutirão com bloqueios temporários de distribuição para evitar recarga imediata.");
            }
        }
        recomendacoes.addAll(configuracao.recomendacoesFixas());
        recomendacoes.add("Usar score de SLA por ramo para selecionar filas de mutirão com maior retorno operacional.");
        recomendacoes.add("Priorizar revisão de cadastros TPU nos processos mais antigos e sigilosos.");
        recomendacoes.add("Cruzar fila antiga com conciliação, execução e colegiado para acelerar baixa qualificada.");

        List<SlaProcessual> slas = new ArrayList<>();
        for (RamoDireito ramo : RAMOS_PADRAO_DASHBOARD) {
            slas.add(calcularSla(tribunalNormalizado, GrauJurisdicao.PRIMEIRO_GRAU, ramo, totalAcervo));
        }

        List<DistribuicaoRamo> distribuicao = new ArrayList<>();
        for (RamoDireito ramo : RAMOS_PADRAO_DASHBOARD) {
            long totalEstimado = estimarVolumePorRamo(totalAcervo, ramo);
            double participacao = totalAcervo > 0 ? round2((double) totalEstimado / (double) totalAcervo * 100.0) : 0.0;
            List<String> alertasRamo = new ArrayList<>();
            if (ramo.geraSigiloAutomatico()) {
                alertasRamo.add("Ramo com exigência reforçada de sigilo e governança de acesso.");
            }
            if (ramo.exigeAtuacaoMP()) {
                alertasRamo.add("Ramo exige atenção à atuação ministerial e atos obrigatórios.");
            }
            PrioridadeGestao prioridade = participacao >= 18.0 ? PrioridadeGestao.ESTRATEGICA
                    : participacao >= 10.0 ? PrioridadeGestao.ATENCAO
                    : PrioridadeGestao.NORMAL;
            distribuicao.add(new DistribuicaoRamo(ramo, totalEstimado, participacao, prioridade, Collections.unmodifiableList(alertasRamo)));
        }

        double scoreSaude = calcularSaudeOperacional(congestionamento, resolutividade, slas);
        String hashIntegridade = hashDashboard(tribunalNormalizado, totalAcervo, encerrados, congestionamento, resolutividade, slas, distribuicao);

        DashboardGerencial dashboard = new DashboardGerencial(
                tribunalNormalizado,
                totalAcervo,
                encerrados,
                congestionamento,
                resolutividade,
                acervoUfSudeste,
                acervoUfNordeste,
                Collections.unmodifiableList(alertas),
                Collections.unmodifiableList(slas),
                Instant.now(),
                Collections.unmodifiableList(distribuicao),
                scoreSaude,
                Collections.unmodifiableList(recomendacoes),
                hashIntegridade
        );

        registrarObservabilidadeTransparencia(
                "TRANSPARENCIA_CNJ_DASHBOARD",
                tribunalNormalizado,
                hashIntegridade,
                dashboard.alertasGestao(),
                null,
                dashboard.scoreSaudeOperacional()
        );
        return dashboard;
    }

    public SlaProcessual calcularSla(String tribunal, GrauJurisdicao grau, RamoDireito ramo, long totalAcervo) {
        String tribunalNormalizado = normalizarTribunal(tribunal);
        ConfiguracaoTribunal configuracao = configuracao(tribunalNormalizado);
        long totalRamo = estimarVolumePorRamo(totalAcervo, ramo);
        long slaMaximo = Math.max(30L, Math.round(resolverSlaMaximo(ramo, grau) * configuracao.fatorSla()));
        long foraSla = Math.round(totalRamo * percentualForaSlaEstimado(ramo));
        double conformidade = totalRamo > 0
                ? round2(((double) (totalRamo - foraSla) / (double) totalRamo) * 100.0)
                : 100.0;

        List<String> alertas = new ArrayList<>();
        if (conformidade < 70.0) {
            alertas.add("Conformidade de SLA baixa — priorizar células de triagem e saneamento processual.");
        }
        if (ramo != null && ramo.geraSigiloAutomatico()) {
            alertas.add("Ramo com sigilo automático exige governança reforçada e fila especializada.");
        }
        NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                null,
                "monitoramento_transparencia",
                ramo,
                grau,
                tribunalNormalizado,
                Map.of("painel", "transparencia", "totalEstimado", totalRamo)
        ));
        if (regras.alertas() != null && !regras.alertas().isEmpty()) {
            regras.alertas().stream().limit(2).forEach(alertas::add);
        }

        PrazoCalculado controle = prazoEngine.calcular(
                LocalDate.now(),
                TipoPrazo.PRAZO_GENERICO,
                ramo,
                grau,
                tribunalNormalizado
        );
        Instant revisao = controle.vencimento().atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        String fundamentoOperacional = "SLA estimado por ramo/grau com calibragem operacional, regras nacionais e janela de revisão automática.";

        return new SlaProcessual(
                tribunalNormalizado,
                grau,
                ramo,
                totalRamo,
                foraSla,
                conformidade,
                Math.max(1L, slaMaximo / 2L),
                slaMaximo,
                Instant.now(),
                revisao,
                Collections.unmodifiableList(dedup(alertas)),
                fundamentoOperacional
        );
    }

    public SnapshotTransparencia registrarSnapshotGerencial(String tribunal, String competencia) {
        DashboardGerencial dashboard = gerarDashboard(tribunal);
        RelatorioConformidadeCnj relatorio = gerarRelatorioConformidade(tribunal, competencia);
        Usuario actor = currentUserService.getOrNull();
        List<String> destaques = new ArrayList<>();
        destaques.add("Score operacional: " + round2(dashboard.scoreSaudeOperacional()));
        destaques.add("Score conformidade CNJ/DataJud: " + round2(relatorio.scoreGeral()));
        if (!dashboard.alertasGestao().isEmpty()) {
            dashboard.alertasGestao().stream().limit(3).forEach(destaques::add);
        }
        String hash = sha256Hex(dashboard.hashIntegridade() + "|" + relatorio.hashIntegridade());
        SnapshotTransparencia snapshot = new SnapshotTransparencia(
                normalizarTribunal(tribunal),
                dashboard.scoreSaudeOperacional(),
                relatorio.scoreGeral(),
                Instant.now(),
                actor != null ? actor.getNome() : "SISTEMA",
                hash,
                Collections.unmodifiableList(destaques)
        );
        registrarObservabilidadeTransparencia(
                "TRANSPARENCIA_CNJ_SNAPSHOT",
                snapshot.tribunal(),
                snapshot.hashIntegridade(),
                snapshot.destaques(),
                snapshot.scoreConformidade(),
                snapshot.scoreSaudeOperacional()
        );
        return snapshot;
    }

    private void registrarObservabilidadeTransparencia(
            String action,
            String tribunal,
            String payloadHash,
            List<String> mensagens,
            Double scoreConformidade,
            Double scoreSaude
    ) {
        String normalizedTribunal = normalizarTribunal(tribunal);
        String resourceId = normalizedTribunal + ":" + Instant.now().toEpochMilli();
        auditLedger.appendSafely(action, RESOURCE_TYPE, resourceId, payloadHash);

        List<UiToken> tokens = new ArrayList<>();
        if (mensagens != null && !mensagens.isEmpty()) {
            tokens.add(UiToken.INFO);
            if (mensagens.size() >= 3) {
                tokens.add(UiToken.ATRASADO);
            }
        } else {
            tokens.add(UiToken.NEUTRO);
        }
        if ((scoreConformidade != null && scoreConformidade < 70.0) || (scoreSaude != null && scoreSaude < 70.0)) {
            tokens.add(UiToken.URGENTE);
        }

        String message = buildUiMessage(normalizedTribunal, mensagens, scoreConformidade, scoreSaude);
        Usuario actor = currentUserService.getOrNull();
        uiHistoryService.recordInboxEvent(
                "transparencia:" + normalizedTribunal.toLowerCase(Locale.ROOT),
                null,
                "TRANSPARENCIA_CNJ_UPDATE",
                EnumSet.copyOf(new LinkedHashSet<>(tokens)),
                actor != null ? actor.getId() : null,
                actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "SISTEMA",
                message
        );

        eventPublisher.publishEvent(new TransparenciaSnapshotEvent(
                normalizedTribunal,
                Instant.now(),
                payloadHash,
                scoreConformidade != null ? scoreConformidade : 0.0,
                scoreSaude != null ? scoreSaude : 0.0,
                mensagens == null ? List.of() : List.copyOf(mensagens)
        ));
    }

    private String buildUiMessage(String tribunal, List<String> mensagens, Double scoreConformidade, Double scoreSaude) {
        StringBuilder sb = new StringBuilder("Painel CNJ/DataJud atualizado para ").append(tribunal).append('.');
        if (scoreConformidade != null) {
            sb.append(" Conformidade: ").append(round2(scoreConformidade)).append(".");
        }
        if (scoreSaude != null) {
            sb.append(" Saúde operacional: ").append(round2(scoreSaude)).append(".");
        }
        if (mensagens != null && !mensagens.isEmpty()) {
            sb.append(' ').append(mensagens.get(0));
        }
        return sb.toString();
    }

    private long countByStatus(StatusProcesso status) {
        return processoRepository.searchCidadao(null, null, null, status, COUNT_PAGE).getTotalElements();
    }

    private long somarAcervoPorUfs(List<String> ufs) {
        long total = 0L;
        for (String uf : ufs) {
            total += processoRepository.findForMagistradoDashboard(uf, null, COUNT_PAGE).getTotalElements();
        }
        return total;
    }

    private ConfiguracaoDistribuicaoVaraService.DashboardDistribuicao carregarDashboardDistribuicao(String tribunalCodigo) {
        try {
            return distribuicaoService.dashboard(tribunalCodigo);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long totalAcervoBase(ConfiguracaoDistribuicaoVaraService.DashboardDistribuicao dashboardDistribuicao) {
        if (dashboardDistribuicao != null && dashboardDistribuicao.processosAtivosTotais() > 0) {
            return dashboardDistribuicao.processosAtivosTotais();
        }
        return processoRepository.count();
    }

    private ConfiguracaoTribunal configuracao(String tribunal) {
        String key = normalizarTribunal(tribunal);
        return configuracoes.computeIfAbsent(key, ConfiguracaoTribunal::padrao);
    }

    private long resolverSlaMaximo(RamoDireito ramo, GrauJurisdicao grau) {
        long base = 730L;
        if (ramo != null) {
            base = switch (ramo) {
                case INFANCIA_JUVENTUDE -> 180L;
                case PENAL -> 365L;
                case TRABALHISTA -> 365L;
                case PREVIDENCIARIO -> 540L;
                case CONSUMIDOR -> 365L;
                case FAMILIA -> 365L;
                case AMBIENTAL -> 730L;
                case TRIBUTARIO -> 1095L;
                case ADMINISTRATIVO -> 820L;
                case CONSTITUCIONAL -> 1825L;
                case AGRARIO -> 760L;
                case EMPRESARIAL -> 540L;
                case ELEITORAL -> 210L;
                case MILITAR -> 460L;
                case CIVIL -> 540L;
                case INTERNACIONAL -> 1460L;
                default -> switch (ramo.verticalPrincipal()) {
                    case "TRABALHISTA" -> 365L;
                    case "PENAL" -> 365L;
                    case "ELEITORAL" -> 210L;
                    case "FAZENDA" -> 820L;
                    case "DIFUSO" -> 730L;
                    case "CIVEL" -> 540L;
                    default -> 730L;
                };
            };
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            base = Math.round(base * 1.30d);
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            base = Math.round(base * 2.0d);
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            base = Math.round(base * 2.5d);
        }
        return base;
    }

    private long estimarVolumePorRamo(long totalAcervo, RamoDireito ramo) {
        return Math.max(0L, Math.round(totalAcervo * pesoHistoricoRamo(ramo)));
    }

    private long estimarVolumeAdministrativo(long totalAcervo) {
        return Math.max(0L, Math.round(totalAcervo * (pesoHistoricoRamo(RamoDireito.ADMINISTRATIVO) * 0.55d)));
    }

    private double pesoHistoricoRamo(RamoDireito ramo) {
        if (ramo == null) {
            return 0.10d;
        }
        return switch (ramo) {
            case CIVIL, PROCESSUAL_CIVIL, CONTRATUAL, RESPONSABILIDADE_CIVIL, IMOBILIARIO,
                    BANCARIO, REGISTRAL_NOTARIAL, ARBITRAGEM_MEDIACAO, DIGITAL_PROTECAO_DADOS,
                    SAUDE_SUPLEMENTAR -> 0.24d;
            case TRABALHISTA, PROCESSUAL_TRABALHISTA, ACIDENTARIO -> 0.18d;
            case PENAL, PROCESSUAL_PENAL, EXECUCAO_PENAL -> 0.17d;
            case PREVIDENCIARIO -> 0.11d;
            case FAMILIA, SUCESSOES -> 0.08d;
            case CONSUMIDOR -> 0.07d;
            case TRIBUTARIO, EXECUCAO_FISCAL -> 0.05d;
            case ADMINISTRATIVO, LICITACOES_CONTRATOS, IMPROBIDADE_ADMINISTRATIVA,
                    SERVIDOR_PUBLICO, REGULATORIO, ADUANEIRO -> 0.04d;
            case INFANCIA_JUVENTUDE, EMPRESARIAL, FALIMENTAR_RECUPERACIONAL -> 0.03d;
            case AMBIENTAL, URBANISTICO, CIVIL_PUBLICA_COLETIVO, CONSTITUCIONAL,
                    ELEITORAL, PROCESSUAL_ELEITORAL, MILITAR, AGRARIO, MINERARIO, ENERGETICO -> 0.02d;
            case INTERNACIONAL -> 0.01d;
            default -> switch (ramo.verticalPrincipal()) {
                case "TRABALHISTA" -> 0.18d;
                case "PENAL" -> 0.17d;
                case "ELEITORAL" -> 0.02d;
                case "FAZENDA" -> 0.04d;
                case "DIFUSO" -> 0.02d;
                case "CIVEL" -> 0.24d;
                default -> 0.10d;
            };
        };
    }

    private double percentualForaSlaEstimado(RamoDireito ramo) {
        if (ramo == null) {
            return 0.25d;
        }
        return switch (ramo) {
            case TRIBUTARIO -> 0.55d;
            case AMBIENTAL -> 0.45d;
            case CONSTITUCIONAL -> 0.60d;
            case PREVIDENCIARIO -> 0.35d;
            case ADMINISTRATIVO -> 0.34d;
            case PENAL -> 0.32d;
            case FAMILIA -> 0.24d;
            case INFANCIA_JUVENTUDE -> 0.22d;
            case TRABALHISTA -> 0.21d;
            case ELEITORAL -> 0.28d;
            case INTERNACIONAL -> 0.38d;
            default -> 0.25d;
        };
    }

    private void verificarCamposDataJud(List<String> campos, List<String> alertas, List<String> gapsEstruturais) {
        campos.add("classeTPU — obrigatório");
        campos.add("assuntoTPU — obrigatório");
        campos.add("grauJurisdicao — obrigatório");
        campos.add("dataDistribuicao — obrigatório");
        alertas.add("DataJud exige consistência de TPU, grau e distribuição para envio confiável.");
        alertas.add("Campos críticos devem ser saneados antes do fechamento mensal do lote.");
        gapsEstruturais.add("ProcessoRepository ainda não expõe contagem por ramo/status com granularidade nativa.");
        gapsEstruturais.add("Métricas por meta CNJ dependem de consultas dedicadas para acervo antigo, improbidade e execução penal.");
        gapsEstruturais.add("Falta consolidar scheduler de reconciliação DataJud com confirmação de recebimento externo.");
    }

    private void gerarRecomendacoes(List<String> recs, double taxaResolutividade, long acervo, ConfiguracaoTribunal configuracao) {
        if (taxaResolutividade < 100.0d) {
            recs.add("Ampliar pauta de conciliação e filtros de admissibilidade nos ramos autocompositivos.");
            recs.add("Executar mutirão direcionado ao acervo antigo com foco em baixa qualificada.");
        }
        if (acervo > 50_000L) {
            recs.add("Criar célula de governança do acervo com recorte territorial e priorização por fila crítica.");
        }
        recs.addAll(configuracao.recomendacoesFixas());
        recs.add("Adicionar consultas agregadas por ramo, status e tempo de tramitação ao ProcessoRepository.");
        recs.add("Automatizar reconciliação do lote DataJud com trilha de rejeição e reenvio assistido.");
        recs.add("Acompanhar SLAs por ramo com revisão quinzenal do painel e metas de saneamento.");
    }

    private void gerarAlertasMeta(List<MetricaProcessual> metricas, List<String> alertas, ConfiguracaoTribunal configuracao) {
        for (MetricaProcessual metrica : metricas) {
            if (metrica.critica()) {
                alertas.add("Prioridade " + metrica.prioridade().name() + " em " + metrica.nome() + ": " + metrica.interpretacao());
            }
            if (configuracao.metasPrioritarias().contains(metrica.metaCnj()) && !metrica.dentroMeta()) {
                alertas.add("Meta prioritária em risco: " + metrica.metaCnj().descricao + ".");
            }
        }
    }

    private StatusConformidadeDataJud resolverStatusDataJud(List<String> camposPendentes, double score) {
        if (camposPendentes.isEmpty() && score >= 90.0d) {
            return StatusConformidadeDataJud.CONFORME;
        }
        if (!camposPendentes.isEmpty()) {
            return StatusConformidadeDataJud.NAO_CONFORME_CAMPOS_FALTANTES;
        }
        if (score < 60.0d) {
            return StatusConformidadeDataJud.PENDENTE_ENVIO;
        }
        return StatusConformidadeDataJud.ENVIADO_AGUARDANDO_CONFIRMACAO;
    }

    private double calcularScore(List<MetricaProcessual> metricas, List<String> camposPendentes) {
        if (metricas.isEmpty()) {
            return 0.0d;
        }
        double media = metricas.stream()
                .mapToDouble(m -> m.dentroMeta() ? 1.0d : Math.max(0.0d, Math.min(1.0d, m.percentualMeta())))
                .average()
                .orElse(0.0d) * 100.0d;
        double penalidade = Math.min(30.0d, camposPendentes.size() * 4.0d);
        return round2(Math.max(0.0d, media - penalidade));
    }

    private double calcularSaudeOperacional(double congestionamento, double resolutividade, List<SlaProcessual> slas) {
        double scoreBase = 100.0d;
        scoreBase -= congestionamento * 45.0d;
        scoreBase += resolutividade * 25.0d;
        double mediaSla = slas.stream().mapToDouble(SlaProcessual::percentualConformidade).average().orElse(70.0d);
        scoreBase = (scoreBase + mediaSla) / 2.0d;
        return round2(Math.max(0.0d, Math.min(100.0d, scoreBase)));
    }

    private static List<String> dedup(List<String> itens) {
        return new ArrayList<>(new LinkedHashSet<>(itens));
    }

    private static String hashRelatorio(
            String tribunal,
            String competencia,
            double score,
            List<MetricaProcessual> metricas,
            List<String> camposPendentes,
            List<String> alertas
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(tribunal).append('|').append(competencia).append('|').append(round2(score));
        for (MetricaProcessual metrica : metricas) {
            sb.append('|').append(metrica.nome()).append('=').append(metrica.valor());
        }
        for (String campo : camposPendentes) {
            sb.append('|').append(campo);
        }
        for (String alerta : alertas) {
            sb.append('|').append(alerta);
        }
        return sha256Hex(sb.toString());
    }

    private static String hashDashboard(
            String tribunal,
            long totalAcervo,
            long encerrados,
            double congestionamento,
            double resolutividade,
            List<SlaProcessual> slas,
            List<DistribuicaoRamo> distribuicao
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(tribunal)
                .append('|').append(totalAcervo)
                .append('|').append(encerrados)
                .append('|').append(round4(congestionamento))
                .append('|').append(round4(resolutividade));
        for (SlaProcessual sla : slas) {
            sb.append('|').append(sla.ramo()).append(':').append(sla.percentualConformidade());
        }
        for (DistribuicaoRamo item : distribuicao) {
            sb.append('|').append(item.ramo()).append(':').append(item.totalEstimado());
        }
        return sha256Hex(sb.toString());
    }

    private static double percentual(long parte, long total) {
        if (total <= 0L) {
            return 0.0d;
        }
        return round2(((double) parte / (double) total) * 100.0d);
    }

    private static String normalizarTribunal(String tribunal) {
        if (tribunal == null || tribunal.isBlank()) {
            return "NACIONAL";
        }
        return tribunal.trim().toUpperCase(Locale.ROOT);
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.valueOf(input).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(input).hashCode());
        }
    }
}
