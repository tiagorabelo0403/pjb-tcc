package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExpropriationAuctionCycleResolver {

    public ExpropriationAuctionCycleProfile resolve(Processo processo,
                                                    String ato,
                                                    String bem,
                                                    String modalidade,
                                                    int tentativa,
                                                    double valorReferencia) {
        String actType = normalizeActType(ato);
        String assetKind = resolveAssetKind(bem);
        String modeToken = normalize(modalidade);
        int cycleAttempt = Math.max(tentativa, 1);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean highValue = valorReferencia >= 300_000D;
        boolean eletronic = modeToken.contains("ELETRONIC") || modeToken.contains("ONLINE") || actType.equals("HASTA_PUBLICA");

        String cycleMode = resolveCycleMode(actType, assetKind, cycleAttempt, eletronic);
        String pracaMode = cycleAttempt > 1 ? "SEGUNDA_PRACA_COM_REDUCAO_CONTROLADA" : "PRIMEIRA_PRACA_COM_REFERENCIA_INTEGRAL";
        String bidWindowMode = eletronic ? "JANELA_LANCES_ASSINCRONA_COM_FECHAMENTO_PROGRESSIVO" : "JANELA_LANCES_PRESENCIAL_COM_CHAMAMENTO_FORMAL";
        String publicationRefreshMode = highValue || cycleAttempt > 1 ? "REFORCO_PUBLICACAO_MULTICANAL" : "PUBLICACAO_ORDINARIA_CONTROLADA";
        String remicaoMode = actType.equals("ADJUDICACAO") ? "REMIÇÃO_LIMITADA_ATE_HOMOLOGACAO" : "REMIÇÃO_PLENA_ATE_ARREMATACAO_CONTROLADA";
        String parcelamentoMode = assetKind.equals("IMOVEL") || assetKind.equals("VEICULO") ? "PARCELAMENTO_COM_GARANTIA_E_CONTROLE" : "PARCELAMENTO_RESTRITO";
        String antiCollusionDesk = highValue || assetKind.equals("QUOTAS_SOCIAIS") ? "MESA_ANTICOLUSAO_E_LANCES_ESTRUTURADOS" : "MESA_CONTROLE_DE_LANCES";
        String homologationDesk = actType.equals("ADJUDICACAO") ? "MESA_HOMOLOGACAO_ADJUDICATORIA" : "MESA_HOMOLOGACAO_ARREMATACAO";
        String settlementDeadlineMode = cycleAttempt > 1 ? "LIQUIDACAO_IMEDIATA_POS_ARREMATACAO" : "LIQUIDACAO_CONTROLADA_COM_CONFIRMACAO_BANCARIA";
        String queueCode = "LEILAO_CICLO_" + actType + '_' + assetKind + "_T" + cycleAttempt + (sigilo ? "_SIGILO" : "");
        String inboxKey = actType.equals("ADJUDICACAO") ? "inbox.execucao.expropriacao.ciclo.adjudicacao" : eletronic ? "inbox.execucao.expropriacao.ciclo.eletronico" : "inbox.execucao.expropriacao.ciclo.presencial";
        TipoUsuario assignedRole = actType.equals("ADJUDICACAO") ? TipoUsuario.JUIZ : TipoUsuario.LEILOEIRO_JUDICIAL;
        int priority = Math.min(99, 88 + (highValue ? 5 : 0) + (sigilo ? 2 : 0) + (cycleAttempt > 1 ? 2 : 0));
        boolean blocking = true;
        long dueAmount = cycleAttempt > 1 ? 4L : 7L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = actType.equals("ADJUDICACAO") ? "Arts. 876, 877, 878 e 901 do CPC" : "Arts. 879, 886, 887, 888, 892, 895 e 903 do CPC";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (sigilo) {
            warnings.add("Ciclo expropriatório sigiloso exige operadores credenciados, edital filtrado e rastreio reforçado de acesso.");
        }
        if (cycleAttempt > 1) {
            warnings.add("Segunda praça exige revisão do preço mínimo, da publicidade reforçada e das hipóteses de frustração anterior.");
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            warnings.add("Leilão de quotas societárias exige checagem de preferência, restrição estatutária e liquidez efetiva do ativo.");
        }
        reviewChecklist.add("Validar edital, intimações, modo de praça, regras de parcelamento, remição e homologação final.");
        reviewChecklist.add("Conferir integridade dos lances, prevenção de colusão, comissão do leiloeiro e destinação do produto da arrematação.");
        if (actType.equals("HASTA_PUBLICA") || actType.equals("ALIENACAO_JUDICIAL")) {
            reviewChecklist.add("Revisar janela de lances, fechamento do ciclo, adjudicação subsidiária e eventual reabertura por falha operacional.");
        }
        fundamentos.add(baseLegal);
        fundamentos.add("Ciclo expropriatório: " + cycleMode.replace('_', ' '));
        fundamentos.add("Modalidade de praça: " + pracaMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("cycleAttempt", cycleAttempt);
        metadata.put("cycleMode", cycleMode);
        metadata.put("pracaMode", pracaMode);
        metadata.put("bidWindowMode", bidWindowMode);
        metadata.put("publicationRefreshMode", publicationRefreshMode);
        metadata.put("remicaoMode", remicaoMode);
        metadata.put("parcelamentoMode", parcelamentoMode);
        metadata.put("antiCollusionDesk", antiCollusionDesk);
        metadata.put("homologationDesk", homologationDesk);
        metadata.put("settlementDeadlineMode", settlementDeadlineMode);
        metadata.put("eletronic", eletronic);
        metadata.put("sigilo", sigilo);
        metadata.put("highValue", highValue);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExpropriationAuctionCycleProfile(
                actType,
                assetKind,
                cycleMode,
                pracaMode,
                bidWindowMode,
                publicationRefreshMode,
                remicaoMode,
                parcelamentoMode,
                antiCollusionDesk,
                homologationDesk,
                settlementDeadlineMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveCycleMode(String actType, String assetKind, int cycleAttempt, boolean eletronic) {
        if (actType.equals("ADJUDICACAO")) {
            return assetKind.equals("IMOVEL") ? "CICLO_ADJUDICATORIO_COM_REGISTRO_E_ENTREGA" : "CICLO_ADJUDICATORIO_COM_IMPUTACAO_AO_CREDITO";
        }
        if (cycleAttempt > 1) {
            return eletronic ? "SEGUNDA_RODADA_ELETRONICA_COM_REABERTURA_CONTROLADA" : "SEGUNDA_RODADA_PRESENCIAL_COM_REABERTURA_CONTROLADA";
        }
        return eletronic ? "PRIMEIRA_RODADA_ELETRONICA_ESTRUTURADA" : "PRIMEIRA_RODADA_PRESENCIAL_ESTRUTURADA";
    }

    private String normalizeActType(String ato) {
        String token = normalize(ato);
        if (token.contains("ADJUDIC")) {
            return "ADJUDICACAO";
        }
        if (token.contains("HASTA") || token.contains("LEILAO")) {
            return "HASTA_PUBLICA";
        }
        return "ALIENACAO_JUDICIAL";
    }

    private String resolveAssetKind(String bem) {
        String token = normalize(bem);
        if (token.contains("DINHEIRO") || token.contains("CONTA")) {
            return "DINHEIRO";
        }
        if (token.contains("FATURAMENTO") || token.contains("RECEITA")) {
            return "FATURAMENTO";
        }
        if (token.contains("VEICULO") || token.contains("PLACA")) {
            return "VEICULO";
        }
        if (token.contains("QUOTA") || token.contains("SOCIET")) {
            return "QUOTAS_SOCIAIS";
        }
        return token.contains("IMOVEL") || token.contains("MATRICULA") ? "IMOVEL" : "OUTROS_ATIVOS";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }
}
