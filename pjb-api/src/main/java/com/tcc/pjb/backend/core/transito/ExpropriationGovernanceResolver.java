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
public class ExpropriationGovernanceResolver {

    public ExpropriationGovernanceProfile resolve(Processo processo,
                                                  String ato,
                                                  String bem,
                                                  String modalidade,
                                                  double valorReferencia) {
        String actType = normalizeActType(ato);
        String assetKind = resolveAssetKind(bem);
        String modeToken = normalize(modalidade);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean highValue = valorReferencia >= 250_000D;
        boolean eletronic = modeToken.contains("ELETRONIC") || actType.equals("HASTA_PUBLICA");

        String expropriationMode = resolveExpropriationMode(actType, assetKind, modeToken, eletronic);
        String sessionMode = resolveSessionMode(actType, eletronic);
        String publicationMode = eletronic ? "PUBLICACAO_MULTICANAL_ELETRONICA" : "PUBLICACAO_EDITAL_CONTROLADA";
        String depositaryMode = assetKind.equals("DINHEIRO") ? "LEVANTAMENTO_JUDICIAL_CONTROLADO" : "DEPOSITARIO_JUDICIAL_COM_RASTREIO";
        String leiloeiroMode = resolveLeiloeiroMode(actType, assetKind, eletronic);
        String priceFloorMode = highValue ? "LANCE_MINIMO_COM_DUPLA_REFERENCIA" : assetKind.equals("FATURAMENTO") ? "PISO_POR_PERCENTUAL_PERIODICO" : "LANCE_MINIMO_POR_AVALIACAO";
        String preferenceDesk = assetKind.equals("IMOVEL") || assetKind.equals("QUOTAS_SOCIAIS") ? "MESA_PREFERENCIAS_E_ONUS" : "MESA_PREFERENCIAS_EXECUTIVAS";
        String fraudReviewDesk = highValue || assetKind.equals("QUOTAS_SOCIAIS") ? "MESA_ANTIFRAUDE_PATRIMONIAL" : "MESA_CONTROLE_EXPROPRIACAO";
        String queueCode = "EXPROPRIACAO_" + actType + '_' + assetKind + (sigilo ? "_SIGILO" : "");
        String inboxKey = resolveInboxKey(actType, assetKind, eletronic);
        TipoUsuario assignedRole = actType.equals("ADJUDICACAO") ? TipoUsuario.JUIZ : eletronic ? TipoUsuario.LEILOEIRO_JUDICIAL : TipoUsuario.SERVIDOR_FORUM;
        int priority = Math.min(99, (actType.equals("HASTA_PUBLICA") ? 93 : 90) + (highValue ? 3 : 0) + (sigilo ? 2 : 0));
        boolean blocking = true;
        long dueAmount = actType.equals("HASTA_PUBLICA") ? 10L : actType.equals("ALIENACAO_JUDICIAL") ? 7L : 5L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = resolveBaseLegal(actType, assetKind);
        String settlementMode = actType.equals("ADJUDICACAO") ? "SATISFACAO_POR_IMPUTACAO_DIRETA" : "SATISFACAO_POR_CONVERSAO_EM_DINHEIRO";
        String deliveryMode = assetKind.equals("DINHEIRO") ? "LIBERACAO_FINANCEIRA_CONTROLADA" : "ENTREGA_COM_AUTO_E_REGISTRO";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (sigilo) {
            warnings.add("Expropriação sigilosa exige edital filtrado, operadores credenciados e trilha de divulgação controlada.");
        }
        if (highValue) {
            warnings.add("Expropriação de alto valor marcada para dupla conferência de avaliação, lances e preferência legal.");
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            warnings.add("Ativo societário exige checagem de restrições estatutárias, preferência societária e liquidez real.");
        }
        reviewChecklist.add("Conferir avaliação válida, intimações, ônus sobre o bem, preferência legal e estado da constrição precedente.");
        reviewChecklist.add("Validar preço mínimo, modalidade de venda, arrecadação, comissão e entrega do produto da expropriação.");
        if (actType.equals("HASTA_PUBLICA") || actType.equals("ALIENACAO_JUDICIAL")) {
            reviewChecklist.add("Verificar edital, leiloeiro, janela de lances, publicidade e reconciliação de lances frustrados ou desertos.");
        }
        fundamentos.add(baseLegal);
        fundamentos.add("Modo expropriatório: " + expropriationMode.replace('_', ' '));
        fundamentos.add("Mesa de preferência: " + preferenceDesk.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("expropriationMode", expropriationMode);
        metadata.put("sessionMode", sessionMode);
        metadata.put("publicationMode", publicationMode);
        metadata.put("depositaryMode", depositaryMode);
        metadata.put("leiloeiroMode", leiloeiroMode);
        metadata.put("priceFloorMode", priceFloorMode);
        metadata.put("preferenceDesk", preferenceDesk);
        metadata.put("fraudReviewDesk", fraudReviewDesk);
        metadata.put("settlementMode", settlementMode);
        metadata.put("deliveryMode", deliveryMode);
        metadata.put("sigilo", sigilo);
        metadata.put("highValue", highValue);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExpropriationGovernanceProfile(
                actType,
                assetKind,
                expropriationMode,
                sessionMode,
                publicationMode,
                depositaryMode,
                leiloeiroMode,
                priceFloorMode,
                preferenceDesk,
                fraudReviewDesk,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                settlementMode,
                deliveryMode,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
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

    private String resolveExpropriationMode(String actType, String assetKind, String modeToken, boolean eletronic) {
        if (actType.equals("ADJUDICACAO")) {
            return assetKind.equals("IMOVEL") ? "ADJUDICACAO_DIRETA_COM_REGISTRO_IMOBILIARIO" : "ADJUDICACAO_DIRETA_COM_IMPUTACAO_AO_CREDITO";
        }
        if (actType.equals("HASTA_PUBLICA")) {
            return eletronic ? "HASTA_PUBLICA_ELETRONICA_CONTROLADA" : "HASTA_PUBLICA_PRESENCIAL_CONTROLADA";
        }
        if (assetKind.equals("FATURAMENTO")) {
            return "ALIENACAO_DE_FLUXO_COM_APURACAO_PERIODICA";
        }
        if (modeToken.contains("PRIVADA")) {
            return "ALIENACAO_PRIVADA_ASSISTIDA";
        }
        return "ALIENACAO_JUDICIAL_CONTROLADA";
    }

    private String resolveSessionMode(String actType, boolean eletronic) {
        if (actType.equals("ADJUDICACAO")) {
            return "SESSAO_DECISORIA_INTERNA";
        }
        return eletronic ? "SESSAO_ELETRONICA_DE_LANCES" : "SESSAO_PUBLICA_PRESENCIAL";
    }

    private String resolveLeiloeiroMode(String actType, String assetKind, boolean eletronic) {
        if (actType.equals("ADJUDICACAO")) {
            return "SEM_LEILOEIRO_EXTERNO";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "LEILOEIRO_COM_APOIO_SOCIETARIO";
        }
        return eletronic ? "LEILOEIRO_JUDICIAL_ELETRONICO" : "LEILOEIRO_JUDICIAL_CREDENCIADO";
    }

    private String resolveInboxKey(String actType, String assetKind, boolean eletronic) {
        if (actType.equals("ADJUDICACAO")) {
            return "inbox.execucao.expropriacao.adjudicacao";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "inbox.execucao.expropriacao.societaria";
        }
        return eletronic ? "inbox.execucao.expropriacao.leilao.eletronico" : "inbox.execucao.expropriacao.alienacao";
    }

    private String resolveBaseLegal(String actType, String assetKind) {
        if (actType.equals("ADJUDICACAO")) {
            return assetKind.equals("IMOVEL") ? "Arts. 876, 877, 879 e 901 do CPC" : "Arts. 876, 877 e 878 do CPC";
        }
        if (actType.equals("HASTA_PUBLICA")) {
            return "Arts. 879, 881, 886, 887 e 888 do CPC";
        }
        return "Arts. 879, 880, 885 e 903 do CPC";
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
