package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public final class PjbContextualPanelPolicy {

    public PjbContextualPanelCapabilitySet capabilities(PjbRitoContext context) {
        PjbRitoContext safe = context == null ? PjbRitoContext.from(PjbDigitalCoreRitoKind.DESCONHECIDO, true) : context;
        List<String> visible = new ArrayList<>(List.of("VISUALIZAR_CAPA", "VISUALIZAR_TIMELINE", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS"));
        List<String> hidden = new ArrayList<>();
        List<String> validations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        RitoProcessual rito = safe.canonicalRito();
        RitoGrupoPrincipal grupo = rito != null ? rito.getGrupoPrincipal() : RitoGrupoPrincipal.CIVIL;

        switch (grupo) {
            case JUIZADO -> {
                visible.addAll(List.of("DESIGNAR_CONCILIACAO", "EXPEDIR_CITACAO_SIMPLIFICADA", "PROPOR_ACORDO_EXPRESSO"));
                hidden.addAll(List.of("CITACAO_POR_EDITAL_PADRAO", "PROVA_PERICIAL_COMPLEXA_SEM_TRIAGEM", "GUIA_CUSTAS_PRIMEIRO_GRAU"));
                validations.add("VALIDAR_COMPATIBILIDADE_SUMARISSIMA_ANTES_DA_DISTRIBUICAO");
            }
            case PENAL -> {
                visible.addAll(List.of("AUDIENCIA_INSTRUCAO_JULGAMENTO", "REGISTRO_JUDICIAL_PENAL", "TUTELA_CAUTELAR_PENAL", "DESIGNAR_AUDIENCIA_PENAL"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO"));
                validations.add("VALIDAR_COMPETENCIA_PENAL_ANTES_DA_DISTRIBUICAO");
            }
            case TRABALHISTA -> {
                visible.addAll(List.of("AUDIENCIA_TRABALHISTA", "DESIGNAR_CONCILIACAO_TRABALHISTA", "CALCULO_LIQUIDACAO_TRABALHISTA"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU"));
                validations.add("VALIDAR_COMPETENCIA_TRABALHISTA");
            }
            case ELEITORAL -> {
                visible.addAll(List.of("REGISTRO_CANDIDATURA", "ATOS_ELEITORAIS", "DIREITO_DE_RESPOSTA_ELEITORAL"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO"));
                validations.add("VALIDAR_PERIODO_ELEITORAL");
            }
            case MILITAR -> {
                visible.addAll(List.of("ATOS_MILITARES", "CONSELHO_DE_JUSTICA_MILITAR", "IPM_REGISTRO"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU"));
                validations.add("VALIDAR_HIERARQUIA_MILITAR");
            }
            case EXECUCAO_FISCAL -> {
                visible.addAll(List.of("EMISSAO_CERTIDAO_FISCAL", "PENHORA_FISCAL", "EXCECAO_DE_EXECUTIVIDADE"));
                validations.add("VALIDAR_CDA_ANTES_DA_DISTRIBUICAO");
            }
            case EXECUCAO_PENAL -> {
                visible.addAll(List.of("ATOS_EXECUCAO_PENAL", "PROGRESSO_DE_REGIME", "PARECER_CONSELHOS_PENITENCIARIOS"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU"));
            }
            case PREVIDENCIARIO -> {
                boolean jef = rito != null && rito.isJuizado();
                if (jef) {
                    visible.addAll(List.of("DESIGNAR_CONCILIACAO", "EXPEDIR_CITACAO_SIMPLIFICADA", "PROTOCOLO_JEF"));
                    hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU"));
                } else {
                    visible.addAll(List.of("ATOS_PREVIDENCIARIOS", "CALCULO_BENEFICIO", "PERICIA_MEDICA_PREVIDENCIARIA"));
                }
            }
            case FALENCIA_RECUPERACAO -> {
                visible.addAll(List.of("ASSEMBLEIA_CREDORES", "PLANO_RECUPERACAO", "HABILITACAO_CREDITO", "ARRECADACAO_BENS"));
                validations.add("VALIDAR_COMPETENCIA_EMPRESARIAL");
            }
            case FAMILIA -> {
                visible.addAll(List.of("MEDIACAO_FAMILIAR", "TUTELA_CRIANCA_ADOLESCENTE", "ACORDOS_FAMILIA"));
                hidden.addAll(List.of("CITACAO_POR_EDITAL_PADRAO"));
            }
            default -> {
                visible.addAll(List.of("ATOS_ORDINARIOS", "SANEAMENTO", "PROVA_PERICIAL", "CUSTAS_INICIAIS"));
            }
        }

        if (rito != null) {
            if (rito.isInfancia()) {
                visible.addAll(List.of("AUDIENCIA_PROTECAO_INTEGRAL", "REGISTRO_INFRACIONAL", "MEDIDA_SOCIOEDUCATIVA"));
                hidden.addAll(List.of("CITACAO_POR_EDITAL_PADRAO", "GUIA_CUSTAS_PRIMEIRO_GRAU"));
            }
            if (rito.isAmbiental()) {
                visible.addAll(List.of("MEDIDA_CAUTELAR_AMBIENTAL", "ACP_AMBIENTAL", "PERICIA_AMBIENTAL"));
            }
            if (rito.isAgrario()) {
                visible.addAll(List.of("VISTORIA_AGRARIA", "MEDIACAO_AGRARIA", "REGISTRO_FUNDIARIO"));
            }
            if (rito.isAutocompositivo()) {
                visible.addAll(List.of("SESSAO_MEDIACAO", "SESSAO_CONCILIACAO", "ACORDO_EXTRAJUDICIAL"));
                hidden.addAll(List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "CITACAO_POR_EDITAL_PADRAO"));
            }
            if (rito.requiresSegredoByDefault()) {
                warnings.add("PROCESSO_REQUER_SIGILO_PADRAO");
            }
        }

        if (safe.humanReviewRequired()) {
            warnings.add("EXIGE_REVISAO_HUMANA_DO_RITO_CONTEXT");
        }
        if (!safe.allowsEdital()) {
            hidden.add("CITACAO_EDITALICIA_SEM_DECISAO_ESPECIFICA");
        }

        return new PjbContextualPanelCapabilitySet(
                safe.labelPolicy(),
                safe.prazoPolicy(),
                safe.labelPolicy(),
                List.copyOf(visible),
                List.copyOf(hidden),
                List.copyOf(validations),
                List.copyOf(warnings));
    }
}
