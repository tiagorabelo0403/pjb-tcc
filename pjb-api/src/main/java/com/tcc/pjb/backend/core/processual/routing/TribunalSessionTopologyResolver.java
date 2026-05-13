package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TribunalSessionTopologyResolver {

    public TribunalSessionTopologyProfile resolve(String tribunalCodigo,
                                                  String specificOrgan,
                                                  String competenceCluster,
                                                  String specializationAxis,
                                                  String chamberLabel,
                                                  String sessionChannel) {
        String court = normalize(tribunalCodigo);
        String organ = normalize(specificOrgan);
        String cluster = normalize(competenceCluster);
        String axis = normalize(specializationAxis);
        String chamber = normalize(chamberLabel);

        String sessionBlock;
        String publicationFlow;
        String internalReviewDesk;
        String panelSizeHint;
        String cadenceHint;
        String sessionSecretariatDesk;
        boolean virtualSessionEligible;

        if (court.startsWith("STF")) {
            sessionBlock = organ.contains("PLENARIO") ? "PLENARIO_VIRTUAL_STF" : "TURMA_VIRTUAL_STF";
            publicationFlow = organ.contains("PLENARIO") ? "PUBLICACAO_PRESIDENCIA_STF" : "PUBLICACAO_SECRETARIA_TURMA_STF";
            internalReviewDesk = axis.contains("CONSTITUCIONAL") ? "REVISAO_CONTROLE_CONCENTRADO_STF" : "REVISAO_PAUTA_STF";
            panelSizeHint = organ.contains("PLENARIO") ? "11_MINISTROS" : "5_MINISTROS";
            cadenceHint = "SESSAO_SEMANAL_COORDENADA";
            sessionSecretariatDesk = organ.contains("PLENARIO") ? "SECRETARIA_PLENARIO_STF" : "SECRETARIA_TURMA_STF";
            virtualSessionEligible = !organ.contains("PLENARIO") || !axis.contains("PENAL");
        } else if (court.startsWith("STJ")) {
            sessionBlock = organ.contains("CORTE_ESPECIAL") ? "CORTE_ESPECIAL_HIBRIDA_STJ" : organ.contains("SECAO") ? "SECAO_DIGITAL_STJ" : "TURMA_DIGITAL_STJ";
            publicationFlow = organ.contains("CORTE_ESPECIAL") ? "PUBLICACAO_CORTE_ESPECIAL_STJ" : "PUBLICACAO_SECRETARIA_SECAO_TURMA_STJ";
            internalReviewDesk = cluster.contains("PUBLICO") ? "REVISAO_PUBLICO_STJ" : cluster.contains("PRIVADO") ? "REVISAO_PRIVADO_STJ" : "REVISAO_PENAL_STJ";
            panelSizeHint = organ.contains("CORTE_ESPECIAL") ? "15_MINISTROS" : organ.contains("SECAO") ? "10_MINISTROS" : "5_MINISTROS";
            cadenceHint = cluster.contains("PENAL") ? "PAUTA_PENAL_INTENSIVA" : "PAUTA_TEMATICA_PROGRAMADA";
            sessionSecretariatDesk = organ.contains("CORTE_ESPECIAL") ? "SECRETARIA_CORTE_ESPECIAL_STJ" : "SECRETARIA_TURMA_SECAO_STJ";
            virtualSessionEligible = !organ.contains("CORTE_ESPECIAL");
        } else if (court.startsWith("TRF")) {
            sessionBlock = organ.contains("SECAO") ? "SECAO_JULGADORA_TRF" : "TURMA_JULGADORA_TRF";
            publicationFlow = cluster.contains("PENAL") ? "PUBLICACAO_CRIMINAL_TRF" : cluster.contains("PREVIDENCIARIO") ? "PUBLICACAO_PREVIDENCIARIA_TRF" : "PUBLICACAO_CIVEL_PUBLICA_TRF";
            internalReviewDesk = cluster.contains("PREVIDENCIARIO") ? "REVISAO_PREVIDENCIARIA_TRF" : cluster.contains("TRIBUTARIO") ? "REVISAO_TRIBUTARIA_TRF" : "REVISAO_GERAL_TRF";
            panelSizeHint = organ.contains("SECAO") ? "COLEGIADO_AMPLIADO_TRF" : "3_DESEMBARGADORES_FEDERAIS";
            cadenceHint = axis.contains("PENAL") ? "PAUTA_PENAL_PERIODICA" : "SESSAO_COLEGIADA_CONTINUA";
            sessionSecretariatDesk = cluster.contains("PREVIDENCIARIO") ? "SECRETARIA_PREVIDENCIARIA_TRF" : "SECRETARIA_TURMA_TRF";
            virtualSessionEligible = !axis.contains("PENAL");
        } else if (court.startsWith("TRT")) {
            sessionBlock = organ.contains("SECAO") ? "SECAO_ESPECIALIZADA_TRT" : "TURMA_TRABALHISTA_TRT";
            publicationFlow = axis.contains("COLETIVO") ? "PUBLICACAO_COLETIVA_TRT" : "PUBLICACAO_TURMA_TRT";
            internalReviewDesk = axis.contains("EXECUCAO") ? "REVISAO_EXECUCAO_TRT" : "REVISAO_RECURSAL_TRT";
            panelSizeHint = organ.contains("SECAO") ? "COLEGIADO_SECAO_TRT" : "3_DESEMBARGADORES_TRABALHISTAS";
            cadenceHint = "PAUTA_RECURSAL_TRABALHISTA";
            sessionSecretariatDesk = organ.contains("SECAO") ? "SECRETARIA_SECAO_TRT" : "SECRETARIA_TURMA_TRT";
            virtualSessionEligible = true;
        } else if (court.startsWith("TRE") || court.startsWith("TSE")) {
            sessionBlock = court.startsWith("TSE") ? "PLENARIO_ELEITORAL_SUPERIOR" : "PLENARIO_ELEITORAL_REGIONAL";
            publicationFlow = court.startsWith("TSE") ? "PUBLICACAO_PLENARIA_TSE" : "PUBLICACAO_PLENARIA_TRE";
            internalReviewDesk = organ.contains("CORREGEDORIA") ? "REVISAO_CORREGEDORIA_ELEITORAL" : "REVISAO_PLENARIA_ELEITORAL";
            panelSizeHint = court.startsWith("TSE") ? "7_MINISTROS" : "COLEGIADO_REGIONAL_ELEITORAL";
            cadenceHint = "SESSAO_ELEITORAL_PROGRAMADA";
            sessionSecretariatDesk = court.startsWith("TSE") ? "SECRETARIA_JUDICIARIA_TSE" : "SECRETARIA_JUDICIARIA_TRE";
            virtualSessionEligible = false;
        } else if (court.startsWith("STM") || court.startsWith("TJM")) {
            sessionBlock = organ.contains("PLENARIO") || organ.contains("PLENO") ? "PLENARIO_MILITAR" : "CAMARA_CONSELHO_MILITAR";
            publicationFlow = organ.contains("PLENARIO") || organ.contains("PLENO") ? "PUBLICACAO_PLENARIA_MILITAR" : "PUBLICACAO_COLEGIADO_MILITAR";
            internalReviewDesk = cluster.contains("MILITAR") ? "REVISAO_MILITAR" : "REVISAO_DISCIPLINAR_MILITAR";
            panelSizeHint = organ.contains("PLENARIO") || organ.contains("PLENO") ? "PLENO_MILITAR" : "COLEGIADO_MILITAR";
            cadenceHint = "SESSAO_MILITAR_PROGRAMADA";
            sessionSecretariatDesk = court.startsWith("STM") ? "SECRETARIA_JUDICIARIA_STM" : "SECRETARIA_TJM";
            virtualSessionEligible = false;
        } else if (court.startsWith("TST")) {
            sessionBlock = organ.contains("SDI") || organ.contains("SDC") ? "SECAO_TST" : "TURMA_TST";
            publicationFlow = organ.contains("SDI") || organ.contains("SDC") ? "PUBLICACAO_SECAO_TST" : "PUBLICACAO_TURMA_TST";
            internalReviewDesk = chamber.contains("DISSIDIOS") ? "REVISAO_DISSIDIOS_TST" : "REVISAO_TURMA_TST";
            panelSizeHint = organ.contains("SDI") || organ.contains("SDC") ? "SECAO_TST" : "3_MINISTROS_OU_MAIS";
            cadenceHint = "PAUTA_TST_COORDENADA";
            sessionSecretariatDesk = organ.contains("SDI") || organ.contains("SDC") ? "SECRETARIA_SECAO_TST" : "SECRETARIA_TURMA_TST";
            virtualSessionEligible = true;
        } else {
            sessionBlock = organ.contains("CAMARA") ? "CAMARA_TJ" : organ.contains("PLENARIO") ? "PLENARIO_TJ" : "COLEGIADO_TJ";
            publicationFlow = chamber.contains("CRIMINAL") ? "PUBLICACAO_CRIMINAL_TJ" : chamber.contains("FAZENDA") ? "PUBLICACAO_FAZENDA_TJ" : chamber.contains("FAMILIA") ? "PUBLICACAO_FAMILIA_TJ" : "PUBLICACAO_CAMARA_TJ";
            internalReviewDesk = chamber.contains("CRIMINAL") ? "REVISAO_CAMARA_CRIMINAL" : chamber.contains("FAZENDA") ? "REVISAO_CAMARA_FAZENDA" : "REVISAO_CAMARA_GERAL";
            panelSizeHint = organ.contains("PLENARIO") ? "PLENO_TJ" : "3_DESEMBARGADORES";
            cadenceHint = axis.contains("JURI") ? "PAUTA_JURI_RECURSAL" : "SESSAO_CAMARA_PROGRAMADA";
            sessionSecretariatDesk = organ.contains("PLENARIO") ? "SECRETARIA_PLENO_TJ" : "SECRETARIA_CAMARA_TJ";
            virtualSessionEligible = !chamber.contains("CRIMINAL");
        }

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(firstNonBlank(court, "TRIBUNAL"));
        labels.add(sessionBlock);
        labels.add(publicationFlow);
        labels.add(cadenceHint);
        if (virtualSessionEligible) {
            labels.add("VIRTUAL_ELIGIBLE");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", court);
        metadata.put("specificOrgan", organ);
        metadata.put("competenceCluster", cluster);
        metadata.put("specializationAxis", axis);
        metadata.put("chamberLabel", chamber);
        metadata.put("descriptor", firstNonBlank(court, "TRIBUNAL") + ':' + sessionBlock + ':' + publicationFlow);

        return new TribunalSessionTopologyProfile(
                sessionBlock,
                publicationFlow,
                internalReviewDesk,
                panelSizeHint,
                cadenceHint,
                sessionSecretariatDesk,
                virtualSessionEligible,
                List.copyOf(labels),
                metadata
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String firstNonBlank(String... values) {
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
