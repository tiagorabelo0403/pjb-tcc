package com.tcc.pjb.backend.service.procuradoria.queue;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvNaturezaCredito;
import com.tcc.pjb.backend.model.entity.Processo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PrecatorioRpvQueuePlanner {

    public QueuePlan plan(QueuePlanCommand command) {
        Processo processo = command.processo();
        String modalidade = normalize(command.modalidade());
        String prioridade = normalize(command.prioridadePagamento());
        String entidade = normalize(command.entidadeDevedoraCodigo(), deriveEntidadeCode(processo));
        LocalDate apresentacao = command.dataApresentacao() == null ? command.dataCalculo() : command.dataApresentacao();
        String filaPrincipal = resolveFilaPrincipal(modalidade, prioridade, command.naturezaCredito());
        List<String> filasOperacionais = buildFilasOperacionais(command, entidade, filaPrincipal);
        List<String> etapas = buildEtapas(command, modalidade);
        String regraOrdenacao = buildRegraOrdenacao(command, modalidade);
        String chaveOrdenacao = buildChaveOrdenacao(command, entidade, apresentacao);
        String regimePagamento = command.regimeEspecial()
                ? "REGIME_ESPECIAL_CRONOLOGIA_E_REDUCAO_ESTOQUE"
                : "REGIME_ORDINARIO";
        LocalDate pagamentoEstimado = "RPV".equals(modalidade)
                ? command.dataCalculo().plusMonths(2)
                : LocalDate.of(apresentacao.getYear() + 1, 12, 31);
        QueueGovernance governance = new QueueGovernance(
                true,
                true,
                true,
                true,
                true,
                command.regimeEspecial(),
                true,
                command.regimeEspecial() && command.acordoDiretoHabilitado()
        );
        return new QueuePlan(
                buildRequisicaoPagamentoId(command, entidade, apresentacao),
                regimePagamento,
                filaPrincipal,
                List.copyOf(filasOperacionais),
                List.copyOf(buildEstagios(command, entidade, filaPrincipal)),
                List.copyOf(etapas),
                regraOrdenacao,
                chaveOrdenacao,
                apresentacao,
                pagamentoEstimado,
                command.superpreferencia(),
                command.acordoDiretoHabilitado(),
                command.regimeEspecial(),
                governance
        );
    }

    private List<String> buildFilasOperacionais(QueuePlanCommand command, String entidade, String filaPrincipal) {
        List<String> filas = new ArrayList<>();
        filas.add("PRECATORIO_ANALISE_REQUISITORIO");
        filas.add("PRECATORIO_VALIDACAO_FORMAL");
        filas.add("PRECATORIO_CLASSIFICACAO_NATUREZA");
        if ("RPV".equals(command.modalidade())) {
            filas.add(filaPrincipal);
            filas.add("RPV_CONTROLE_PRAZO_2M");
            filas.add("RPV_CONFERENCIA_DEPOSITO");
            filas.add("RPV_ALVARA_E_LIBERACAO");
            return filas;
        }
        filas.add(filaPrincipal);
        filas.add("PRECATORIO_LISTA_ORDEM_" + entidade);
        if (command.regimeEspecial()) {
            filas.add("PRECATORIO_CRONOLOGIA_REGIME_ESPECIAL_50");
        }
        if (command.superpreferencia()) {
            filas.add("PRECATORIO_SUPERPREFERENCIA_CONTROLE");
        }
        if (command.regimeEspecial() && command.acordoDiretoHabilitado()) {
            filas.add("PRECATORIO_ACORDO_DIRETO_" + entidade);
        }
        filas.add("PRECATORIO_RESERVA_ORCAMENTARIA");
        filas.add("PRECATORIO_MONITORAMENTO_SEQUESTRO");
        filas.add("PRECATORIO_ALVARA_E_LIBERACAO");
        return filas;
    }


    private List<QueueStage> buildEstagios(QueuePlanCommand command, String entidade, String filaPrincipal) {
        if ("RPV".equals(command.modalidade())) {
            return List.of(
                    new QueueStage(
                            "REQUISITORIO",
                            "Recepção e conferência",
                            List.of("PRECATORIO_ANALISE_REQUISITORIO", "PRECATORIO_VALIDACAO_FORMAL"),
                            "Conferir integridade formal da requisição de pequeno valor antes da execução direta."
                    ),
                    new QueueStage(
                            "PROCESSAMENTO",
                            "Execução financeira",
                            List.of(filaPrincipal, "RPV_CONTROLE_PRAZO_2M", "RPV_CONFERENCIA_DEPOSITO"),
                            "Acompanhar o aporte financeiro dentro da janela operacional de pagamento."
                    ),
                    new QueueStage(
                            "LIBERACAO",
                            "Liberação final",
                            List.of("RPV_ALVARA_E_LIBERACAO"),
                            "Converter depósito confirmado em liberação efetiva ao credor."
                    )
            );
        }
        ArrayList<QueueStage> stages = new ArrayList<>();
        stages.add(new QueueStage(
                "REQUISITORIO",
                "Recepção e classificação",
                List.of("PRECATORIO_ANALISE_REQUISITORIO", "PRECATORIO_VALIDACAO_FORMAL", "PRECATORIO_CLASSIFICACAO_NATUREZA"),
                "Validar o ofício requisitório e enquadrar corretamente natureza, prioridade e regime."
        ));
        ArrayList<String> ordenacao = new ArrayList<>(List.of(filaPrincipal, "PRECATORIO_LISTA_ORDEM_" + entidade));
        if (command.superpreferencia()) {
            ordenacao.add("PRECATORIO_SUPERPREFERENCIA_CONTROLE");
        }
        stages.add(new QueueStage(
                "ORDENACAO",
                "Ordem cronológica",
                List.copyOf(ordenacao),
                "Posicionar o crédito na lista cronológica adequada sem perder o rastro de prioridade."
        ));
        ArrayList<String> orcamento = new ArrayList<>();
        if (command.regimeEspecial()) {
            orcamento.add("PRECATORIO_CRONOLOGIA_REGIME_ESPECIAL_50");
        }
        if (command.regimeEspecial() && command.acordoDiretoHabilitado()) {
            orcamento.add("PRECATORIO_ACORDO_DIRETO_" + entidade);
        }
        orcamento.add("PRECATORIO_RESERVA_ORCAMENTARIA");
        stages.add(new QueueStage(
                "ORCAMENTO",
                "Reserva e aporte",
                List.copyOf(orcamento),
                "Reservar recursos do exercício e acompanhar aportes do ente devedor."
        ));
        stages.add(new QueueStage(
                "LIBERACAO",
                "Controle e liberação",
                List.of("PRECATORIO_MONITORAMENTO_SEQUESTRO", "PRECATORIO_ALVARA_E_LIBERACAO"),
                "Vigiar risco de frustração do pagamento e liberar após confirmação do aporte."
        ));
        return List.copyOf(stages);
    }

    private List<String> buildEtapas(QueuePlanCommand command, String modalidade) {
        if ("RPV".equals(modalidade)) {
            return List.of(
                    "LIQUIDAR_VALOR_EXECUTORIO",
                    "EXPEDIR_RPV",
                    "MONITORAR_PRAZO_CONSTITUCIONAL",
                    "CONFIRMAR_APORTE",
                    "LIBERAR_ALVARA"
            );
        }
        ArrayList<String> etapas = new ArrayList<>(List.of(
                "LIQUIDAR_VALOR_EXECUTORIO",
                "EXPEDIR_OFICIO_PRECATORIO",
                "REGISTRAR_APRESENTACAO_NO_TRIBUNAL",
                "CLASSIFICAR_LISTA_CRONOLOGICA"
        ));
        if (command.superpreferencia()) {
            etapas.add("PROCESSAR_PARCELA_SUPERPREFERENCIAL");
        }
        if (command.regimeEspecial()) {
            etapas.add("ALOCAR_RECURSOS_REGIME_ESPECIAL");
        }
        if (command.regimeEspecial() && command.acordoDiretoHabilitado()) {
            etapas.add("HABILITAR_ACORDO_DIRETO");
        }
        etapas.add("CONFIRMAR_APORTE");
        etapas.add("LIBERAR_ALVARA");
        return List.copyOf(etapas);
    }

    private String buildRegraOrdenacao(QueuePlanCommand command, String modalidade) {
        if ("RPV".equals(modalidade)) {
            return "requisicao > prazo constitucional de 2 meses > confirmacao de deposito > liberacao";
        }
        StringBuilder regra = new StringBuilder("entidade devedora > natureza do credito > superpreferencia > momento de apresentacao > menor valor > maior idade");
        if (command.regimeEspecial()) {
            regra.append(" > alocacao por cronologia e reducao de estoque");
        }
        if (command.acordoDiretoHabilitado()) {
            regra.append(" > trilha facultativa de acordo direto quando regulamentada");
        }
        return regra.toString();
    }

    private String buildChaveOrdenacao(QueuePlanCommand command, String entidade, LocalDate apresentacao) {
        String natureza = command.naturezaCredito() == null ? "COMUM" : command.naturezaCredito().name();
        String superpreferencia = command.superpreferencia() ? "SP" : "OR";
        String valor = command.totalAtualizado().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String idade = command.idadeBeneficiario() == null ? "000" : String.format(Locale.ROOT, "%03d", command.idadeBeneficiario());
        return entidade + '|' + natureza + '|' + superpreferencia + '|' + apresentacao + '|' + valor + '|' + idade;
    }

    private String buildRequisicaoPagamentoId(QueuePlanCommand command, String entidade, LocalDate apresentacao) {
        String processoId = command.processo().getId() == null ? "SEMID" : command.processo().getId().toString();
        return entidade + '-' + apresentacao.format(DateTimeFormatter.BASIC_ISO_DATE) + '-' + processoId;
    }

    private String resolveFilaPrincipal(String modalidade, String prioridade, PrecatorioRpvNaturezaCredito naturezaCredito) {
        if ("RPV".equals(modalidade)) {
            return "RPV_EXECUCAO_DIRETA";
        }
        if ("SUPERPREFERENCIAL_ALIMENTAR".equals(prioridade)) {
            return "PRECATORIO_SUPERPREFERENCIAL_ALIMENTAR";
        }
        if (naturezaCredito == PrecatorioRpvNaturezaCredito.ALIMENTAR) {
            return "PRECATORIO_ALIMENTAR_CRONOLOGICO";
        }
        if (naturezaCredito == PrecatorioRpvNaturezaCredito.TRIBUTARIO) {
            return "PRECATORIO_TRIBUTARIO_SELIC";
        }
        return "PRECATORIO_COMUM_CRONOLOGICO";
    }

    private String deriveEntidadeCode(Processo processo) {
        String tribunal = processo == null ? null : normalize(processo.getTribunal());
        String uf = processo == null ? null : normalize(processo.getUf());
        if (tribunal != null) {
            return tribunal.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        }
        if (uf != null) {
            return "ENTE_" + uf.toUpperCase(Locale.ROOT);
        }
        return "ENTE_NAO_IDENTIFICADO";
    }

    private String normalize(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed.toUpperCase(Locale.ROOT);
                }
            }
        }
        return null;
    }

    public record QueuePlanCommand(
            Processo processo,
            String modalidade,
            String prioridadePagamento,
            PrecatorioRpvNaturezaCredito naturezaCredito,
            BigDecimal totalAtualizado,
            LocalDate dataCalculo,
            LocalDate dataApresentacao,
            Integer idadeBeneficiario,
            String entidadeDevedoraCodigo,
            boolean superpreferencia,
            boolean regimeEspecial,
            boolean acordoDiretoHabilitado
    ) {
    }

    public record QueuePlan(
            String requisicaoPagamentoId,
            String regimePagamento,
            String filaPrincipal,
            List<String> filasOperacionais,
            List<QueueStage> estagios,
            List<String> etapasOperacionais,
            String criterioOrdenacao,
            String chaveOrdenacao,
            LocalDate dataApresentacaoConsiderada,
            LocalDate pagamentoEstimadoAte,
            boolean superpreferencia,
            boolean acordoDiretoElegivel,
            boolean regimeEspecial,
            QueueGovernance governanca
    ) {
    }

    public record QueueStage(
            String stageCode,
            String stageTitle,
            List<String> filas,
            String objetivo
    ) {
    }

    public record QueueGovernance(
            boolean publicarListaCronologicaAnonimizada,
            boolean publicarPagamentosDoExercicio,
            boolean desempatarPorMenorValor,
            boolean desempatarPorMaiorIdade,
            boolean preservarPosicaoOriginalAposParcelaSuperpreferencial,
            boolean reservarFaixaCronologicaEmRegimeEspecial,
            boolean monitorarSequestroPorBurlaOuNaoAlocacao,
            boolean habilitarFilaDeAcordoDireto
    ) {
    }
}
