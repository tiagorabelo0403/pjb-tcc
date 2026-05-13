package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

@Service
public class ExecucaoCumprimentoEngine {

    private static final Logger log = LoggerFactory.getLogger(ExecucaoCumprimentoEngine.class);
    private static final String RESOURCE_TYPE_PROCESSO = "PROCESSO";
    private static final String RESOURCE_TYPE_EXECUCAO = "EXECUCAO";

    public enum TituloExecutivo {
        SENTENCA_CONDENATORIA,
        ACORDAO,
        SENTENCA_HOMOLOGATORIA_ACORDO,
        CDA_CERTIDAO_DIVIDA_ATIVA,
        CHEQUE_NOTA_PROMISSORIA,
        ESCRITURA_PUBLICA,
        CONTRATO_GARANTIA_REAL,
        CCB_INSTRUMENTO_CREDITO,
        TAC_AJUSTAMENTO_CONDUTA,
        SENTENCA_ARBITRAL,
        ACORDO_EXTRAJUDICIAL_HOMOLOGADO,
        DECISAO_LIMINAR_ASTREINTES,
        TERMO_CEJUSC,
        TITULO_JUDICIAL_PROVISORIO,
        TITULO_EXTRAJUDICIAL_DIGITAL,
        TERMO_TRANSACAO_TRIBUTARIA,
        TERMO_AJUSTE_ADMINISTRATIVO
    }

    public enum MeioExpropriatorio {
        BACENJUD_BLOQUEIO_CONTA,
        BACENJUD_TRANSFERENCIA,
        SISBAJUD_BLOQUEIO_CONTA,
        SISBAJUD_TEIMOSINHA,
        RENAJUD_RESTRICAO_VEICULO,
        INFOJUD_CONSULTA_BENS,
        CCS_PESQUISA_RELACIONAMENTO,
        SNIPER_PATRIMONIAL,
        SERASAJUD_NEGATIVACAO,
        CNIB_INDISPONIBILIDADE_IMOVEL,
        PENHORA_IMOVEL_REGISTRO,
        PENHORA_QUOTA_SOCIAL,
        PENHORA_CREDITO_PRECATORIO,
        PENHORA_FATURAMENTO,
        DESCONTO_FOLHA_ALIMENTOS,
        PROTESTO_DECISAO_JUDICIAL,
        ADJUDICACAO,
        ALIENACAO_JUDICIAL,
        ARREMATACAO_HASTA_PUBLICA,
        EXPROPRIACAO_DIRETA
    }

    public enum StatusMedidaExpropriatoria {
        REQUERIDA,
        DEFERIDA,
        CUMPRIDA,
        PARCIALMENTE_CUMPRIDA,
        INDEFERIDA,
        LEVANTADA,
        CANCELADA
    }

    public record MedidaExpropriatoria(
            UUID medidaId,
            Long processoId,
            MeioExpropriatorio meio,
            StatusMedidaExpropriatoria status,
            BigDecimal valorAlvo,
            BigDecimal valorBloqueado,
            String descricaoBem,
            String sistemaExterno,
            String protocoloExterno,
            Instant requeridaEm,
            Instant deferidaEm,
            Instant cumpridaEm
    ) {
        public MedidaExpropriatoria {
            medidaId = medidaId != null ? medidaId : UUID.randomUUID();
            valorAlvo = scale(valorAlvo);
            valorBloqueado = scale(valorBloqueado);
        }

        public boolean satisfatoria() {
            return valorAlvo != null && valorBloqueado != null && valorBloqueado.compareTo(valorAlvo) >= 0;
        }
    }

    public record ComplianceExecucao(
            boolean admiteExecucao,
            boolean exigeIntimacaoPrevia,
            boolean envolveFazendaPublica,
            boolean naturezaAlimentar,
            boolean exigePriorizacaoHumana,
            boolean recomendaPesquisaPatrimonialAmpliada,
            List<String> travas,
            List<String> alertas,
            List<String> salvaguardas
    ) {
        public ComplianceExecucao {
            travas = immutableDistinct(travas);
            alertas = immutableDistinct(alertas);
            salvaguardas = immutableDistinct(salvaguardas);
        }
    }

    public record MatrizExpropriacao(
            int scoreRecuperabilidade,
            int scorePressaoExecutiva,
            int scoreComplexidade,
            BigDecimal estimativaRecuperacaoInicial,
            List<MeioExpropriatorio> meiosPrioritarios,
            List<MeioExpropriatorio> meiosRestringidos,
            List<String> fatoresPositivos,
            List<String> fatoresRisco
    ) {
        public MatrizExpropriacao {
            estimativaRecuperacaoInicial = scale(estimativaRecuperacaoInicial);
            meiosPrioritarios = List.copyOf(new LinkedHashSet<>(meiosPrioritarios == null ? List.of() : meiosPrioritarios));
            meiosRestringidos = List.copyOf(new LinkedHashSet<>(meiosRestringidos == null ? List.of() : meiosRestringidos));
            fatoresPositivos = immutableDistinct(fatoresPositivos);
            fatoresRisco = immutableDistinct(fatoresRisco);
        }
    }

    public record PlanoExecucao(
            Long processoId,
            TituloExecutivo tipoTitulo,
            BigDecimal valorTotal,
            BigDecimal valorCorrigido,
            BigDecimal juros,
            BigDecimal honorariosExecucao,
            List<MeioExpropriatorio> meiosOrdenados,
            List<String> etapas,
            List<String> alertas,
            String fundamentoLegal,
            boolean exigeIntimarDevedor,
            int prazoRespostaNumero,
            boolean prazoRespostaEmHoras,
            FaseProcessual faseDestino,
            StatusProcesso statusDestino,
            com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.PrazoCalculado prazoCalculado,
            ComplianceExecucao compliance,
            MatrizExpropriacao matriz,
            List<String> checklist,
            List<String> recomendacoesTecnologicas
    ) {
        public PlanoExecucao {
            valorTotal = scale(valorTotal);
            valorCorrigido = scale(valorCorrigido);
            juros = scale(juros);
            honorariosExecucao = scale(honorariosExecucao);
            meiosOrdenados = List.copyOf(new LinkedHashSet<>(meiosOrdenados == null ? List.of() : meiosOrdenados));
            etapas = immutableDistinct(etapas);
            alertas = immutableDistinct(alertas);
            checklist = immutableDistinct(checklist);
            recomendacoesTecnologicas = immutableDistinct(recomendacoesTecnologicas);
        }

        public BigDecimal valorProjetadoTotal() {
            return scale(sum(valorCorrigido, juros, honorariosExecucao));
        }
    }

    public record ResultadoPenhora(
            UUID penhoraId,
            MeioExpropriatorio meio,
            BigDecimal valorPenhorado,
            boolean suficiente,
            int scoreRecuperacao,
            List<String> bensLocalizados,
            List<String> advertencias,
            List<String> provasRecomendadas
    ) {
        public ResultadoPenhora {
            penhoraId = penhoraId != null ? penhoraId : UUID.randomUUID();
            valorPenhorado = scale(valorPenhorado);
            bensLocalizados = immutableDistinct(bensLocalizados);
            advertencias = immutableDistinct(advertencias);
            provasRecomendadas = immutableDistinct(provasRecomendadas);
        }
    }

    public record ResultadoAtivacaoExecucao(
            Long processoId,
            String numeroProcesso,
            boolean sucesso,
            FaseProcessual faseAnterior,
            FaseProcessual faseAtual,
            StatusProcesso statusAnterior,
            StatusProcesso statusAtual,
            PlanoExecucao plano,
            List<String> acoesGeradas,
            Instant atualizadoEm
    ) {
        public ResultadoAtivacaoExecucao {
            acoesGeradas = immutableDistinct(acoesGeradas);
        }
    }

    public record PainelExecucao(
            Long processoId,
            String numeroProcesso,
            BigDecimal exposicaoFinanceira,
            int totalMedidas,
            int medidasSatisfatorias,
            int medidasPendentes,
            boolean prazoCritico,
            List<String> gargalos,
            List<String> oportunidades,
            List<String> proximasAcoes
    ) {
        public PainelExecucao {
            exposicaoFinanceira = scale(exposicaoFinanceira);
            gargalos = immutableDistinct(gargalos);
            oportunidades = immutableDistinct(oportunidades);
            proximasAcoes = immutableDistinct(proximasAcoes);
        }
    }

    public record ExecucaoAtualizadaEvent(
            Long processoId,
            String numeroProcesso,
            TituloExecutivo tituloExecutivo,
            FaseProcessual faseAtual,
            StatusProcesso statusAtual,
            Instant emitidoEm,
            boolean naturezaAlimentar,
            boolean envolveFazendaPublica
    ) {}

    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedger;
    private final ExecucaoCumprimentoPlanningSupport planningSupport;
    private final ExecucaoCumprimentoSimulationSupport simulationSupport;
    private final UiHistoryService uiHistoryService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public ExecucaoCumprimentoEngine(ProcessoRepository processoRepository,
                                     AuditLedgerService auditLedger,
                                     ExecucaoCumprimentoPlanningSupport planningSupport,
                                     ExecucaoCumprimentoSimulationSupport simulationSupport,
                                     UiHistoryService uiHistoryService,
                                     CurrentUserService currentUserService,
                                     ApplicationEventPublisher eventPublisher) {
        this.processoRepository = processoRepository;
        this.auditLedger = auditLedger;
        this.planningSupport = planningSupport;
        this.simulationSupport = simulationSupport;
        this.uiHistoryService = uiHistoryService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public PlanoExecucao montarPlano(Processo processo, TituloExecutivo titulo, BigDecimal valorBase) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(titulo, "titulo");
        return planningSupport.montarPlano(processo, titulo, valorBase);
    }

    @Transactional
    public ResultadoAtivacaoExecucao ativarExecucao(Long processoId, TituloExecutivo titulo, BigDecimal valorBase) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(titulo, "titulo");

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        PlanoExecucao plano = montarPlano(processo, titulo, valorBase);

        FaseProcessual faseAnterior = processo.getFaseAtual();
        StatusProcesso statusAnterior = processo.getStatusProcesso();

        processo.setFaseAtual(plano.faseDestino());
        processo.setStatusProcesso(plano.statusDestino());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);

        String operador = currentUserService.getOrNull() != null && currentUserService.getOrNull().getId() != null
                ? String.valueOf(currentUserService.getOrNull().getId())
                : "system";
        String mensagem = planningSupport.construirMensagemMudancaExecucao(processo, plano, operador);
        uiHistoryService.recordProcessoStatusChange(
                processo,
                statusAnterior,
                processo.getResultadoFinal(),
                processo.getStatusProcesso(),
                processo.getResultadoFinal(),
                mensagem
        );

        auditLedger.appendSafely(
                "EXECUCAO_ATIVADA",
                RESOURCE_TYPE_PROCESSO,
                safeId(processo.getId()),
                hashMaterial(processo.getNumeroUnificado() + "|" + titulo.name() + "|" + plano.valorProjetadoTotal()),
                mensagem
        );

        eventPublisher.publishEvent(new ExecucaoAtualizadaEvent(
                processo.getId(),
                processo.getNumeroUnificado(),
                titulo,
                processo.getFaseAtual(),
                processo.getStatusProcesso(),
                Instant.now(),
                plano.compliance().naturezaAlimentar(),
                plano.compliance().envolveFazendaPublica()
        ));

        List<String> acoesGeradas = new ArrayList<>();
        acoesGeradas.add("Fase atualizada para " + processo.getFaseAtual().name());
        acoesGeradas.add("Status atualizado para " + processo.getStatusProcesso().name());
        if (plano.prazoCalculado() != null) {
            acoesGeradas.add("Prazo processual integrado calculado para " + plano.prazoCalculado().vencimento());
        }
        if (plano.compliance().recomendaPesquisaPatrimonialAmpliada()) {
            acoesGeradas.add("Pesquisa patrimonial ampliada priorizada");
        }
        if (plano.compliance().naturezaAlimentar()) {
            acoesGeradas.add("Rota alimentar com urgência reforçada habilitada");
        }

        log.info("[ExecucaoEngine] Execução ativada: processo={} titulo={} fase={} status={} valor={}",
                processo.getNumeroUnificado(),
                titulo,
                processo.getFaseAtual(),
                processo.getStatusProcesso(),
                plano.valorProjetadoTotal());

        return new ResultadoAtivacaoExecucao(
                processo.getId(),
                processo.getNumeroUnificado(),
                true,
                faseAnterior,
                processo.getFaseAtual(),
                statusAnterior,
                processo.getStatusProcesso(),
                plano,
                acoesGeradas,
                Instant.now()
        );
    }

    public ResultadoPenhora simularPenhora(Long processoId, MeioExpropriatorio meio, BigDecimal valorAlvo) {
        Objects.requireNonNull(meio, "meio");
        Processo processo = processoId == null ? null : processoRepository.findById(processoId).orElse(null);
        ResultadoPenhora resultado = simulationSupport.simularPenhora(processo, processoId, meio, valorAlvo);

        auditLedger.appendSafely(
                "EXECUCAO_SIMULAR_PENHORA",
                RESOURCE_TYPE_EXECUCAO,
                processoId != null ? String.valueOf(processoId) : "SEM_PROCESSO",
                hashMaterial(meio.name() + "|" + scale(valorAlvo != null ? valorAlvo : processo != null ? processo.getValorCausa() : BigDecimal.ZERO) + "|" + resultado.valorPenhorado())
        );

        log.info("[ExecucaoEngine] Simulação de penhora: processoId={} meio={} valorAlvo={} estimativa={}",
                processoId, meio, valorAlvo, resultado.valorPenhorado());
        return resultado;
    }

    public PainelExecucao gerarPainel(Processo processo, PlanoExecucao plano, List<MedidaExpropriatoria> medidas) {
        Objects.requireNonNull(processo, "processo");
        PlanoExecucao planoEfetivo = plano != null ? plano : montarPlano(processo, TituloExecutivo.SENTENCA_CONDENATORIA, processo.getValorCausa());
        return simulationSupport.gerarPainel(processo, planoEfetivo, medidas);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(value);
    }

    private static BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (values == null) {
            return total;
        }
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return scale(total);
    }

    private static int clamp(int score) {
        return score;
    }

    private static List<String> immutableDistinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values == null ? List.of() : values));
    }

    private static String safeId(Long id) {
        return id == null ? "0" : String.valueOf(id);
    }

    private static String hashMaterial(String material) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(String.valueOf(material).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(material));
        }
    }
}
