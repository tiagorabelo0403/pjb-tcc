package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TransitoJulgadoNarrativeSupport {

    private final ExecutionIncidentResolver executionIncidentResolver;
    private final ExecutionEnforcementResolver executionEnforcementResolver;
    private final PatrimonialConstrictionResolver patrimonialConstrictionResolver;
    private final ExternalConstrictionResolver externalConstrictionResolver;
    private final ExpropriationGovernanceResolver expropriationGovernanceResolver;
    private final ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver;
    private final ExternalConstrictionContingencyResolver externalConstrictionContingencyResolver;
    private final ExternalConstrictionReconciliationResolver externalConstrictionReconciliationResolver;
    private final ExpropriationHomologationResolver expropriationHomologationResolver;
    private final ExpropriationSettlementResolver expropriationSettlementResolver;
    private final ExecutionClosureGovernanceResolver executionClosureGovernanceResolver;
    private final ExecutionSatisfactionResolver executionSatisfactionResolver;
    private final TerminalArchiveLinkResolver terminalArchiveLinkResolver;

    public TransitoJulgadoNarrativeSupport(
            ExecutionIncidentResolver executionIncidentResolver,
            ExecutionEnforcementResolver executionEnforcementResolver,
            PatrimonialConstrictionResolver patrimonialConstrictionResolver,
            ExternalConstrictionResolver externalConstrictionResolver,
            ExpropriationGovernanceResolver expropriationGovernanceResolver,
            ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver,
            ExternalConstrictionContingencyResolver externalConstrictionContingencyResolver,
            ExternalConstrictionReconciliationResolver externalConstrictionReconciliationResolver,
            ExpropriationHomologationResolver expropriationHomologationResolver,
            ExpropriationSettlementResolver expropriationSettlementResolver,
            ExecutionClosureGovernanceResolver executionClosureGovernanceResolver,
            ExecutionSatisfactionResolver executionSatisfactionResolver,
            TerminalArchiveLinkResolver terminalArchiveLinkResolver
    ) {
        this.executionIncidentResolver = executionIncidentResolver;
        this.executionEnforcementResolver = executionEnforcementResolver;
        this.patrimonialConstrictionResolver = patrimonialConstrictionResolver;
        this.externalConstrictionResolver = externalConstrictionResolver;
        this.expropriationGovernanceResolver = expropriationGovernanceResolver;
        this.expropriationAuctionCycleResolver = expropriationAuctionCycleResolver;
        this.externalConstrictionContingencyResolver = externalConstrictionContingencyResolver;
        this.externalConstrictionReconciliationResolver = externalConstrictionReconciliationResolver;
        this.expropriationHomologationResolver = expropriationHomologationResolver;
        this.expropriationSettlementResolver = expropriationSettlementResolver;
        this.executionClosureGovernanceResolver = executionClosureGovernanceResolver;
        this.executionSatisfactionResolver = executionSatisfactionResolver;
        this.terminalArchiveLinkResolver = terminalArchiveLinkResolver;
    }
    public String buildIncidentDescription(String fundamentacao,
                                            PostJudgmentOperationalProfile operationalProfile,
                                            ExecutionIncidentProfile incidentProfile) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (fundamentacao != null && !fundamentacao.isBlank()) {
            parts.add(fundamentacao.trim());
        }
        parts.add("Execução: " + operationalProfile.descriptor());
        parts.add("Incidente: " + incidentProfile.descriptor());
        parts.add("Impacto: " + incidentProfile.executionImpact());
        return String.join(" | ", parts);
    }

    public String incidentLabel(String incidentType) {
        return switch (incidentType) {
            case "IMPUGNACAO_CUMPRIMENTO" -> "Impugnação ao Cumprimento";
            case "EMBARGOS_EXECUCAO" -> "Embargos à Execução";
            case "EXCECAO_PRE_EXECUTIVIDADE" -> "Exceção de Pré-executividade";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "Desconsideração da Personalidade";
            case "HABILITACAO_CREDITO" -> "Habilitação de Crédito";
            case "CONCURSO_PREFERENCIAS" -> "Concurso de Preferências";
            default -> "Incidente Executivo";
        };
    }


    public LinkedHashMap<String, Object> buildIncidentMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        for (String incidente : List.of(
                "IMPUGNACAO_CUMPRIMENTO",
                "EMBARGOS_EXECUCAO",
                "EXCECAO_PRE_EXECUTIVIDADE",
                "DESCONSIDERACAO_PERSONALIDADE",
                "HABILITACAO_CREDITO",
                "CONCURSO_PREFERENCIAS")) {
            matrix.put(incidente, executionIncidentResolver.resolve(processo, incidente, null, 0D).toMap());
        }
        return matrix;
    }

    public LinkedHashMap<String, Object> buildActMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        for (String ato : List.of(
                "PENHORA",
                "BLOQUEIO_ATIVOS",
                "AVALIACAO_BENS",
                "ADJUDICACAO",
                "ALIENACAO_JUDICIAL",
                "HASTA_PUBLICA",
                "SATISFACAO_FINAL",
                "EXTINCAO_EXECUTIVA")) {
            matrix.put(ato, executionEnforcementResolver.resolve(processo, ato, null, 0D).toMap());
        }
        return matrix;
    }

    public LinkedHashMap<String, Object> buildPatrimonialMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("DINHEIRO", patrimonialConstrictionResolver.resolve(processo, "PENHORA", "DINHEIRO", null, "SISBAJUD", 0D).toMap());
        matrix.put("FATURAMENTO", patrimonialConstrictionResolver.resolve(processo, "PENHORA", "FATURAMENTO", null, null, 0D).toMap());
        matrix.put("IMOVEL", patrimonialConstrictionResolver.resolve(processo, "PENHORA", "IMOVEL", null, "CNIB", 0D).toMap());
        matrix.put("VEICULO", patrimonialConstrictionResolver.resolve(processo, "PENHORA", "VEICULO", null, "RENAJUD", 0D).toMap());
        matrix.put("QUOTAS_SOCIAIS", patrimonialConstrictionResolver.resolve(processo, "PENHORA", "QUOTAS SOCIETARIAS", null, "REGISTRO EMPRESARIAL INTEGRADO", 0D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildExternalConstrictionMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("SISBAJUD", externalConstrictionResolver.resolve(processo, "PENHORA", "DINHEIRO", "SISBAJUD", 0D).toMap());
        matrix.put("RENAJUD", externalConstrictionResolver.resolve(processo, "PENHORA", "VEICULO", "RENAJUD", 0D).toMap());
        matrix.put("CNIB", externalConstrictionResolver.resolve(processo, "PENHORA", "IMOVEL", "CNIB", 0D).toMap());
        matrix.put("REGISTRO_EMPRESARIAL_INTEGRADO", externalConstrictionResolver.resolve(processo, "PENHORA", "QUOTAS SOCIETARIAS", "REGISTRO EMPRESARIAL INTEGRADO", 0D).toMap());
        matrix.put("OFICIO_ELETRONICO", externalConstrictionResolver.resolve(processo, "PENHORA", "FATURAMENTO", "OFICIO ELETRONICO", 0D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildHomologationMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("ADJUDICACAO_IMOVEL", expropriationHomologationResolver.resolve(processo, "adjudicacao", "imovel", "direta", "Credor exequente", 180000D).toMap());
        matrix.put("ARREMATACAO_VEICULO", expropriationHomologationResolver.resolve(processo, "arrematacao", "veiculo", "eletronica", "Adquirente particular", 65000D).toMap());
        matrix.put("ARREMATACAO_QUOTAS", expropriationHomologationResolver.resolve(processo, "hasta publica", "quotas societarias", "eletronica", "Sociedade adquirente", 420000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildSettlementMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("PRODUTO_INTEGRAL_IMOVEL", expropriationSettlementResolver.resolve(processo, "imovel", "deposito judicial", "hipoteca", "sim", 250000D, 0D, 220000D).toMap());
        matrix.put("PRODUTO_PARCIAL_VEICULO", expropriationSettlementResolver.resolve(processo, "veiculo", "deposito judicial", "trabalhista", "nao", 65000D, 35000D, 100000D).toMap());
        matrix.put("PRODUTO_COM_EXCEDENTE_DINHEIRO", expropriationSettlementResolver.resolve(processo, "dinheiro", "deposito judicial", "ordinaria", "nao", 120000D, 0D, 90000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildClosureGovernanceMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("FECHAMENTO_INTEGRAL", executionClosureGovernanceResolver.resolve(processo, "fechamento integral", "ordinaria", "nao", 100D, 0D, "quitacao integral").toMap());
        matrix.put("FECHAMENTO_PARCIAL", executionClosureGovernanceResolver.resolve(processo, "fechamento parcial", "trabalhista", "sim", 65D, 1500D, "saldo remanescente").toMap());
        matrix.put("FECHAMENTO_FRUSTRADO", executionClosureGovernanceResolver.resolve(processo, "frustrado", "fiscal", "nao", 0D, 2800D, "sem bens localizados").toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildTerminalMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("SATISFACAO_INTEGRAL", executionSatisfactionResolver.resolve(processo, "pagamento integral", 100D, 0D, null).toMap());
        matrix.put("SATISFACAO_PARCIAL", executionSatisfactionResolver.resolve(processo, "satisfacao parcial", 45D, 1000D, null).toMap());
        matrix.put("SUSPENSAO_SEM_BENS", executionSatisfactionResolver.resolve(processo, "suspensao sem bens", 0D, 1000D, null).toMap());
        matrix.put("EXTINCAO_POR_ACORDO_CUMPRIDO", executionSatisfactionResolver.resolve(processo, "acordo cumprido", 100D, 0D, null).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildExpropriationMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("ADJUDICACAO_IMOVEL", expropriationGovernanceResolver.resolve(processo, "adjudicacao", "imovel", "direta", 150000D).toMap());
        matrix.put("ALIENACAO_VEICULO", expropriationGovernanceResolver.resolve(processo, "alienacao judicial", "veiculo", "privada", 80000D).toMap());
        matrix.put("HASTA_PUBLICA_QUOTAS", expropriationGovernanceResolver.resolve(processo, "hasta publica", "quotas societarias", "eletronica", 350000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildAuctionCycleMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("PRIMEIRA_PRACA_IMOVEL", expropriationAuctionCycleResolver.resolve(processo, "hasta publica", "imovel", "eletronica", 1, 250000D).toMap());
        matrix.put("SEGUNDA_PRACA_VEICULO", expropriationAuctionCycleResolver.resolve(processo, "hasta publica", "veiculo", "eletronica", 2, 65000D).toMap());
        matrix.put("ADJUDICACAO_QUOTAS", expropriationAuctionCycleResolver.resolve(processo, "adjudicacao", "quotas societarias", "direta", 1, 410000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildContingencyMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("SISBAJUD_TIMEOUT", externalConstrictionContingencyResolver.resolve(processo, "dinheiro", "sisbajud", "timeout", null, 180000D).toMap());
        matrix.put("RENAJUD_PARTIAL", externalConstrictionContingencyResolver.resolve(processo, "veiculo", "renajud", "partial_success", "PROTO-C1", 65000D).toMap());
        matrix.put("CNIB_REJECTED", externalConstrictionContingencyResolver.resolve(processo, "imovel", "cnib", "rejected", "PROTO-C2", 320000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildReconciliationMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("SISBAJUD_PARTIAL_SUCCESS", externalConstrictionReconciliationResolver.resolve(processo, "dinheiro", "sisbajud", "partial_success", "PROTO-1", 120000D).toMap());
        matrix.put("RENAJUD_PENDING", externalConstrictionReconciliationResolver.resolve(processo, "veiculo", "renajud", "pending", "PROTO-2", 65000D).toMap());
        matrix.put("CNIB_UNAVAILABLE", externalConstrictionReconciliationResolver.resolve(processo, "imovel", "cnib", "unavailable", "PROTO-3", 300000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildArchiveLinkMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("ARQUIVAR_TERMINAL", terminalArchiveLinkResolver.resolve(processo, "arquivar", "baixa_terminal_integral", "baixa definitiva", 100D, 0D).toMap());
        matrix.put("ARQUIVAR_RESERVA", terminalArchiveLinkResolver.resolve(processo, "arquivar", "baixa_parcial_com_saldo", "saldo residual", 60D, 1500D).toMap());
        matrix.put("DESARQUIVAR_REATIVACAO", terminalArchiveLinkResolver.resolve(processo, "desarquivar", "baixa_frustrada", "localizacao de bens", 0D, 1000D).toMap());
        return matrix;
    }

    public String buildAuctionCycleTitle(ExpropriationAuctionCycleProfile profile, Processo processo, int tentativa) {
        return switch (profile.actType()) {
            case "ADJUDICACAO" -> "Ciclo de Adjudicação — T" + tentativa + " — " + processo.getNumeroProcesso();
            case "HASTA_PUBLICA" -> "Ciclo de Hasta Pública — T" + tentativa + " — " + processo.getNumeroProcesso();
            default -> "Ciclo de Alienação Judicial — T" + tentativa + " — " + processo.getNumeroProcesso();
        };
    }

    public String buildAuctionCycleDescription(ExpropriationAuctionCycleProfile profile,
                                                String modalidade,
                                                int tentativa,
                                                double valorReferencia) {
        return "Modalidade: " + firstNonBlank(modalidade, "SEM_MODALIDADE")
                + " | Tentativa: " + tentativa
                + " | Valor referência: R$ " + formatDecimal(valorReferencia)
                + " | Perfil: " + profile.descriptor();
    }

    public String buildContingencyTitle(ExternalConstrictionContingencyProfile profile, Processo processo) {
        return "Contingência de Constrição Externa — " + profile.gatewayCode() + " — " + processo.getNumeroProcesso();
    }

    public String buildContingencyDescription(ExternalConstrictionContingencyProfile profile,
                                               String referenciaExterna,
                                               double valorOperacao) {
        return "Referência externa: " + firstNonBlank(referenciaExterna, "SEM_REFERENCIA")
                + " | Valor operacional: R$ " + formatDecimal(valorOperacao)
                + " | Perfil: " + profile.descriptor();
    }

    public WorkItemType resolveWorkItemTypeForExpropriation(ExpropriationGovernanceProfile profile) {
        if (profile == null) {
            return WorkItemType.EXPEDICAO;
        }
        return switch (profile.actType()) {
            case "ADJUDICACAO" -> WorkItemType.DECISAO;
            case "HASTA_PUBLICA", "ALIENACAO_JUDICIAL" -> WorkItemType.EXPEDICAO;
            default -> WorkItemType.EXPEDICAO;
        };
    }

    public String buildExpropriationTitle(ExpropriationGovernanceProfile profile, Processo processo) {
        return switch (profile.actType()) {
            case "ADJUDICACAO" -> "Governança de Adjudicação — " + processo.getNumeroProcesso();
            case "HASTA_PUBLICA" -> "Governança de Hasta Pública — " + processo.getNumeroProcesso();
            default -> "Governança de Alienação Judicial — " + processo.getNumeroProcesso();
        };
    }

    public String buildExpropriationDescription(ExpropriationGovernanceProfile profile,
                                                 String modalidade,
                                                 double valorReferencia) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (modalidade != null && !modalidade.isBlank()) {
            parts.add("Modalidade: " + modalidade.trim());
        }
        if (valorReferencia > 0D) {
            parts.add("Valor de referência: R$ " + formatDecimal(valorReferencia));
        }
        parts.add("Descriptor: " + profile.descriptor());
        parts.add("Preço mínimo: " + profile.priceFloorMode());
        parts.add("Mesa de fraude: " + profile.fraudReviewDesk());
        return String.join(" | ", parts);
    }

    public String buildReconciliationTitle(ExternalConstrictionReconciliationProfile profile, Processo processo) {
        return "Reconciliação Externa — " + profile.gatewayCode() + " — " + processo.getNumeroProcesso();
    }

    public String buildReconciliationDescription(ExternalConstrictionReconciliationProfile profile,
                                                  String referenciaExterna,
                                                  double valorOperacao) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (referenciaExterna != null && !referenciaExterna.isBlank()) {
            parts.add("Referência: " + referenciaExterna.trim());
        }
        if (valorOperacao > 0D) {
            parts.add("Valor: R$ " + formatDecimal(valorOperacao));
        }
        parts.add("Descriptor: " + profile.descriptor());
        parts.add("Mesa de contingência: " + profile.contingencyDesk());
        parts.add("Mesa de prova: " + profile.proofDesk());
        return String.join(" | ", parts);
    }

    public String buildClosureGovernanceDescription(ExecutionClosureGovernanceProfile profile,
                                                   String motivo,
                                                   double percentualSatisfeito,
                                                   double saldoRemanescente) {
        List<String> parts = new java.util.ArrayList<>();
        parts.add("Motivo: " + firstNonBlank(motivo, "SEM_MOTIVO"));
        parts.add("Percentual satisfeito: " + formatDecimal(percentualSatisfeito) + "%");
        parts.add("Saldo remanescente: R$ " + formatDecimal(saldoRemanescente));
        parts.add("Consistência: " + profile.closureConsistencyStatus());
        parts.add("Arquivo: " + profile.archiveReadiness());
        parts.add("Perfil: " + profile.descriptor());
        return String.join(" | ", parts);
    }

    public String buildArchiveLinkDescription(TerminalArchiveLinkProfile profile,
                                               String motivo,
                                               double percentualSatisfeito,
                                               double saldoRemanescente) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (motivo != null && !motivo.isBlank()) {
            parts.add(motivo.trim());
        }
        parts.add("Descriptor: " + profile.descriptor());
        parts.add("Percentual satisfeito: " + formatDecimal(percentualSatisfeito) + "%");
        parts.add("Saldo remanescente: R$ " + formatDecimal(saldoRemanescente));
        parts.add("Retenção: " + profile.retentionClass());
        return String.join(" | ", parts);
    }

    public FaseProcessual resolveExecutionPhaseForPatrimonial(Processo processo, PatrimonialConstrictionProfile profile) {
        if (profile == null) {
            return processo.getFaseAtual();
        }
        if (profile.assetKind().equals("DINHEIRO") || profile.assetKind().equals("FATURAMENTO")) {
            return FaseProcessual.PENHORA;
        }
        return processo.getFaseAtual() == null ? FaseProcessual.EXECUCAO : processo.getFaseAtual();
    }

    public WorkItemType resolveWorkItemTypeForPatrimonial(PatrimonialConstrictionProfile profile) {
        if (profile == null) {
            return WorkItemType.DILIGENCIA;
        }
        return switch (profile.assetKind()) {
            case "IMOVEL", "VEICULO", "QUOTAS_SOCIAIS" -> WorkItemType.EXPEDICAO;
            default -> WorkItemType.DILIGENCIA;
        };
    }

    public String buildPatrimonialTitle(PatrimonialConstrictionProfile profile, Processo processo) {
        return switch (profile.assetKind()) {
            case "DINHEIRO" -> "Constrição de Dinheiro — " + processo.getNumeroProcesso();
            case "FATURAMENTO" -> "Constrição de Faturamento — " + processo.getNumeroProcesso();
            case "IMOVEL" -> "Constrição de Imóvel — " + processo.getNumeroProcesso();
            case "VEICULO" -> "Constrição de Veículo — " + processo.getNumeroProcesso();
            case "QUOTAS_SOCIAIS" -> "Constrição de Quotas Societárias — " + processo.getNumeroProcesso();
            default -> "Constrição Patrimonial — " + processo.getNumeroProcesso();
        };
    }

    public String buildPatrimonialDescription(String bem,
                                               String detalhe,
                                               String convenio,
                                               PostJudgmentOperationalProfile operationalProfile,
                                               PatrimonialConstrictionProfile patrimonialProfile,
                                               double valorOperacao) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (bem != null && !bem.isBlank()) {
            parts.add("Bem: " + bem.trim());
        }
        if (detalhe != null && !detalhe.isBlank()) {
            parts.add(detalhe.trim());
        }
        if (convenio != null && !convenio.isBlank()) {
            parts.add("Convênio: " + convenio.trim());
        }
        if (valorOperacao > 0D) {
            parts.add("Valor: R$ " + formatDecimal(valorOperacao));
        }
        parts.add("Execução: " + operationalProfile.descriptor());
        parts.add("Constrição: " + patrimonialProfile.descriptor());
        parts.add("Prioridade de satisfação: " + patrimonialProfile.satisfactionPriority());
        return String.join(" | ", parts);
    }

    public String buildExternalTitle(ExternalConstrictionProfile profile, Processo processo) {
        return "Integração de Constrição — " + profile.gatewayCode() + " — " + processo.getNumeroProcesso();
    }

    public String buildExternalDescription(String referenciaExterna,
                                            ExternalConstrictionProfile profile,
                                            double valorOperacao) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (referenciaExterna != null && !referenciaExterna.isBlank()) {
            parts.add("Referência externa: " + referenciaExterna.trim());
        }
        if (valorOperacao > 0D) {
            parts.add("Valor: R$ " + formatDecimal(valorOperacao));
        }
        parts.add("Gateway: " + profile.gatewayCode());
        parts.add("Status alvo: " + profile.statusTarget());
        parts.add("Reconciliação: " + profile.reconciliationMode());
        return String.join(" | ", parts);
    }

    public String buildHomologationTitle(ExpropriationHomologationProfile profile, Processo processo) {
        return "Homologação Final de Expropriação — " + profile.actType() + " — " + processo.getNumeroProcesso();
    }

    public String buildHomologationDescription(ExpropriationHomologationProfile profile,
                                                String adquirente,
                                                double valorArrematacao) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        parts.add("Descriptor: " + profile.descriptor());
        if (adquirente != null && !adquirente.isBlank()) {
            parts.add("Adquirente: " + adquirente.trim());
        }
        if (valorArrematacao > 0D) {
            parts.add("Valor homologado: R$ " + formatDecimal(valorArrematacao));
        }
        parts.add("Transferência: " + profile.titleTransferMode());
        parts.add("Entrega/posse: " + profile.possessionDeliveryMode());
        parts.add("Liberação do depósito: " + profile.depositReleaseMode());
        return String.join(" | ", parts);
    }

    public String buildSettlementTitle(ExpropriationSettlementProfile profile, Processo processo) {
        return "Liquidação do Produto da Expropriação — " + profile.assetKind() + " — " + processo.getNumeroProcesso();
    }

    public String buildSettlementDescription(ExpropriationSettlementProfile profile,
                                              double valorProduto,
                                              double saldoExecutado,
                                              double saldoCredor) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        parts.add("Descriptor: " + profile.descriptor());
        parts.add("Valor do produto: R$ " + formatDecimal(valorProduto));
        parts.add("Saldo executado: R$ " + formatDecimal(saldoExecutado));
        parts.add("Saldo credor: R$ " + formatDecimal(saldoCredor));
        parts.add("Preferência: " + profile.preferenceMode());
        parts.add("Sub-rogação: " + profile.subrogationMode());
        parts.add("Saldo/resultado: " + profile.balanceMode());
        return String.join(" | ", parts);
    }

    public FaseProcessual resolveExecutionPhaseForTerminal(Processo processo, ExecutionSatisfactionProfile profile) {
        if (profile == null) {
            return processo.getFaseAtual();
        }
        if (profile.terminalDisposition().contains("EXTINCAO") || profile.terminalDisposition().contains("BAIXA")) {
            return FaseProcessual.EXECUCAO;
        }
        return processo.getFaseAtual() == null ? FaseProcessual.EXECUCAO : processo.getFaseAtual();
    }

    public WorkItemType resolveWorkItemTypeForTerminal(ExecutionSatisfactionProfile profile) {
        if (profile == null) {
            return WorkItemType.CERTIDAO;
        }
        return profile.terminalDisposition().contains("PARCIAL") ? WorkItemType.CALCULO : WorkItemType.CERTIDAO;
    }

    public WorkItemStatus resolveWorkItemStatusForTerminal(ExecutionSatisfactionProfile profile) {
        if (profile != null && !profile.terminalDisposition().contains("PARCIAL")) {
            return WorkItemStatus.CONCLUIDO;
        }
        return WorkItemStatus.PENDENTE;
    }

    public String buildTerminalTitle(ExecutionSatisfactionProfile profile, Processo processo) {
        return "Satisfação Terminal — " + processo.getNumeroProcesso();
    }

    public String buildTerminalDescription(String fundamento,
                                            ExecutionSatisfactionProfile profile,
                                            double percentualSatisfeito,
                                            double saldoRemanescente) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (fundamento != null && !fundamento.isBlank()) {
            parts.add(fundamento.trim());
        }
        parts.add("Descriptor: " + profile.descriptor());
        parts.add("Percentual satisfeito: " + formatDecimal(percentualSatisfeito) + "%");
        parts.add("Saldo remanescente: R$ " + formatDecimal(saldoRemanescente));
        parts.add("Baixa: " + profile.baixaMode());
        return String.join(" | ", parts);
    }

    public FaseProcessual resolveExecutionPhaseForAct(Processo processo, ExecutionEnforcementProfile profile) {
        if (profile == null) {
            return processo.getFaseAtual();
        }
        if ("EXECUCAO_PENAL".equals(profile.speciesCode())) {
            return FaseProcessual.EXECUCAO;
        }
        if (profile.actType().equals("PENHORA") || profile.actType().equals("BLOQUEIO_ATIVOS")) {
            return FaseProcessual.PENHORA;
        }
        if (profile.speciesCode().startsWith("OBRIGACAO_")) {
            return FaseProcessual.CUMPRIMENTO_SENTENCA;
        }
        return processo.getFaseAtual() == null ? FaseProcessual.EXECUCAO : processo.getFaseAtual();
    }

    public WorkItemType resolveWorkItemTypeForAct(ExecutionEnforcementProfile profile) {
        if (profile == null) {
            return WorkItemType.DILIGENCIA;
        }
        return switch (profile.actType()) {
            case "AVALIACAO_BENS" -> WorkItemType.CALCULO;
            case "SATISFACAO_FINAL", "EXTINCAO_EXECUTIVA" -> WorkItemType.CERTIDAO;
            case "ALIENACAO_JUDICIAL", "HASTA_PUBLICA", "ADJUDICACAO" -> WorkItemType.EXPEDICAO;
            default -> WorkItemType.DILIGENCIA;
        };
    }

    public WorkItemStatus resolveWorkItemStatusForAct(ExecutionEnforcementProfile profile) {
        if (profile != null && ("SATISFACAO_FINAL".equals(profile.actType()) || "EXTINCAO_EXECUTIVA".equals(profile.actType()))) {
            return WorkItemStatus.CONCLUIDO;
        }
        return WorkItemStatus.PENDENTE;
    }

    public String buildActTitle(ExecutionEnforcementProfile profile, Processo processo) {
        return switch (profile.actType()) {
            case "PENHORA" -> "Penhora Executiva — " + processo.getNumeroProcesso();
            case "BLOQUEIO_ATIVOS" -> "Bloqueio de Ativos — " + processo.getNumeroProcesso();
            case "AVALIACAO_BENS" -> "Avaliação de Bens — " + processo.getNumeroProcesso();
            case "ADJUDICACAO" -> "Adjudicação — " + processo.getNumeroProcesso();
            case "ALIENACAO_JUDICIAL" -> "Alienação Judicial — " + processo.getNumeroProcesso();
            case "HASTA_PUBLICA" -> "Hasta Pública — " + processo.getNumeroProcesso();
            case "SATISFACAO_FINAL" -> "Satisfação Final — " + processo.getNumeroProcesso();
            case "EXTINCAO_EXECUTIVA" -> "Extinção Executiva — " + processo.getNumeroProcesso();
            default -> "Ato Executivo — " + processo.getNumeroProcesso();
        };
    }

    public String buildEnforcementDescription(String detalhe,
                                               PostJudgmentOperationalProfile operationalProfile,
                                               ExecutionEnforcementProfile enforcementProfile,
                                               double valorOperacao) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (detalhe != null && !detalhe.isBlank()) {
            parts.add(detalhe.trim());
        }
        if (valorOperacao > 0D) {
            parts.add("Valor: R$ " + formatDecimal(valorOperacao));
        }
        parts.add("Execução: " + operationalProfile.descriptor());
        parts.add("Ato: " + enforcementProfile.descriptor());
        parts.add("Impacto: " + enforcementProfile.executionImpact());
        return String.join(" | ", parts);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
