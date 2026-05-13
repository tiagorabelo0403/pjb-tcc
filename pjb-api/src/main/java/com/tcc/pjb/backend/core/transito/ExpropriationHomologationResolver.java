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
public class ExpropriationHomologationResolver {

    public ExpropriationHomologationProfile resolve(Processo processo,
                                                    String ato,
                                                    String bem,
                                                    String modalidade,
                                                    String adquirente,
                                                    double valorArrematacao) {
        String actType = normalizeActType(ato);
        String assetKind = resolveAssetKind(bem);
        String modeToken = normalize(modalidade);
        String acquirerToken = normalize(adquirente);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean highValue = valorArrematacao >= 350_000D;
        boolean institutionalAcquirer = acquirerToken.contains("BANCO") || acquirerToken.contains("FUNDO") || acquirerToken.contains("SOCIEDADE");

        String homologationMode = resolveHomologationMode(actType, assetKind, modeToken, institutionalAcquirer);
        String adjudicationMode = actType.equals("ADJUDICACAO") ? "ADJUDICACAO_COM_IMPUTACAO_AO_CREDITO_E_VERIFICACAO_DE_PREFERENCIA" : "NAO_APLICAVEL";
        String arrematacaoMode = actType.equals("ADJUDICACAO") ? "NAO_APLICAVEL" : modeToken.contains("ELETRON") ? "ARREMATACAO_ELETRONICA_COM_HOMOLOGACAO_CONDICIONADA" : "ARREMATACAO_PRESENCIAL_COM_HOMOLOGACAO_CONTROLADA";
        String titleTransferMode = assetKind.equals("IMOVEL") ? "TRANSFERENCIA_REGISTRAL_COM_CARTORIO_E_INTIMACOES" : assetKind.equals("VEICULO") ? "TRANSFERENCIA_REGISTRAL_COM_ORGAO_DE_TRANSITO" : "TRANSFERENCIA_PATRIMONIAL_CONTROLADA";
        String possessionDeliveryMode = assetKind.equals("IMOVEL") ? "IMISSAO_NA_POSSE_COM_MANDADO_ESPECIFICO" : assetKind.equals("VEICULO") ? "ENTREGA_COM_RESTRICAO_REGISTRAL_E_BAIXA_DE_ONUS" : "ENTREGA_CONTROLADA_COM_TERMO_DE_RECEBIMENTO";
        String depositReleaseMode = highValue || institutionalAcquirer ? "LIBERACAO_DEPOSITO_APOS_DUPLA_CONFERENCIA_E_PREFERENCIAS" : "LIBERACAO_DEPOSITO_APOS_HOMOLOGACAO_E_CERTIFICACAO";
        String fraudReviewDesk = highValue || sigilo ? "MESA_HOMOLOGACAO_ANTIFRAUDE_E_INTEGRIDADE" : "MESA_HOMOLOGACAO_EXECUTIVA";
        String preferenceReviewDesk = assetKind.equals("QUOTAS_SOCIAIS") || assetKind.equals("IMOVEL") ? "MESA_PREFERENCIA_E_SUBROGACAO_PATRIMONIAL" : "MESA_PREFERENCIA_ORDINARIA";
        String queueCode = "EXPROPRIACAO_HOMOLOGACAO_" + actType + '_' + assetKind + (sigilo ? "_SIGILO" : "");
        String inboxKey = actType.equals("ADJUDICACAO") ? "inbox.execucao.expropriacao.homologacao.adjudicacao" : "inbox.execucao.expropriacao.homologacao.arrematacao";
        TipoUsuario assignedRole = TipoUsuario.JUIZ;
        int priority = Math.min(99, 90 + (highValue ? 5 : 0) + (sigilo ? 3 : 0));
        boolean blocking = true;
        long dueAmount = highValue ? 3L : 2L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = actType.equals("ADJUDICACAO") ? "Arts. 876, 877, 878, 901 e 903 do CPC" : "Arts. 892, 903, 904, 905 e 908 do CPC";
        String settlementTriggerMode = "TRIGGER_LIQUIDACAO_PRODUTO_EXPROPRIACAO";
        String closureHint = actType.equals("ADJUDICACAO") ? "HOMOLOGACAO_COM_IMPUTACAO_CREDITO_E_ANALISE_DE_SALDO" : "HOMOLOGACAO_COM_LIBERACAO_PRODUTO_E_RATEIO_CONTROLADO";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (sigilo) {
            warnings.add("Homologação sigilosa exige restrição de edital, integridade de acesso e dupla checagem da cadeia de custódia dos atos expropriatórios.");
        }
        if (institutionalAcquirer) {
            warnings.add("Adquirente institucional exige revisão reforçada de integridade concorrencial, origem do pagamento e prevenção de fraude à execução.");
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            warnings.add("Expropriação de quotas societárias exige revisão de preferência legal/estatutária, liquidez e efeitos societários da transferência.");
        }
        reviewChecklist.add("Conferir lance ou imputação ao crédito, depósito, edital, nulidades, preferência, sub-rogação e liberação do produto.");
        reviewChecklist.add("Validar título de transferência, posse, intimações finais, baixa de ônus e consolidação da arrematação ou adjudicação.");
        reviewChecklist.add("Checar produto líquido, comissão do leiloeiro, custas, saldo remanescente e credores preferenciais.");
        fundamentos.add(baseLegal);
        fundamentos.add("Homologação final: " + homologationMode.replace('_', ' '));
        fundamentos.add("Entrega e transferência: " + titleTransferMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("modeToken", modeToken);
        metadata.put("adquirente", adquirente);
        metadata.put("institutionalAcquirer", institutionalAcquirer);
        metadata.put("highValue", highValue);
        metadata.put("sigilo", sigilo);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExpropriationHomologationProfile(
                actType,
                assetKind,
                homologationMode,
                adjudicationMode,
                arrematacaoMode,
                titleTransferMode,
                possessionDeliveryMode,
                depositReleaseMode,
                fraudReviewDesk,
                preferenceReviewDesk,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                settlementTriggerMode,
                closureHint,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveHomologationMode(String actType, String assetKind, String modeToken, boolean institutionalAcquirer) {
        if (actType.equals("ADJUDICACAO")) {
            return assetKind.equals("IMOVEL") ? "HOMOLOGACAO_ADJUDICACAO_IMOVEL_COM_REGISTRO" : "HOMOLOGACAO_ADJUDICACAO_COM_IMPUTACAO_AO_CREDITO";
        }
        if (institutionalAcquirer) {
            return "HOMOLOGACAO_ARREMATACAO_COM_REVIEW_REFORCADO_DE_INTEGRIDADE";
        }
        return modeToken.contains("ELETRON") ? "HOMOLOGACAO_ARREMATACAO_ELETRONICA_COM_FECHAMENTO_FORMAL" : "HOMOLOGACAO_ARREMATACAO_PRESENCIAL_COM_FECHAMENTO_FORMAL";
    }

    private String normalizeActType(String ato) {
        String token = normalize(ato);
        if (token.contains("ADJUDIC")) {
            return "ADJUDICACAO";
        }
        if (token.contains("ARREMAT")) {
            return "ARREMATACAO";
        }
        if (token.contains("HASTA") || token.contains("LEILAO")) {
            return "ARREMATACAO";
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
