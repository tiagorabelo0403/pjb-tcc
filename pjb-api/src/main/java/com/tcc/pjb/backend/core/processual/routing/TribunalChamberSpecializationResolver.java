package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class TribunalChamberSpecializationResolver {

    public TribunalChamberSpecializationProfile resolve(String tribunalCodigo,
                                                       TipoJustica tipoJustica,
                                                       GrauJurisdicao grau,
                                                       String specializationAxis,
                                                       String orgaoFracionario) {
        String court = normalize(tribunalCodigo);
        String axis = normalize(specializationAxis);
        String organ = normalize(orgaoFracionario);

        String chamberLabel = resolveChamberLabel(court, axis, organ, tipoJustica, grau);
        String relatoriaDesk = "RELATORIA_" + chamberLabel;
        String advisoryDesk = organ.contains("PLENARIO")
                ? "ASSESSORIA_PLENARIA_" + court
                : organ.contains("SECAO")
                ? "ASSESSORIA_SECAO_" + court
                : "ASSESSORIA_CAMARA_" + court;
        String preventionClass = chamberLabel.contains("PENAL") ? "PREV_PENAL"
                : chamberLabel.contains("FAZENDA") ? "PREV_PUBLICO"
                : chamberLabel.contains("FAMILIA") ? "PREV_FAMILIA"
                : chamberLabel.contains("PREVIDENCIARIO") ? "PREV_PREVIDENCIARIO"
                : chamberLabel.contains("TRABALHO") ? "PREV_TRABALHISTA"
                : "PREV_GERAL";
        String distributionPool = court + '_' + firstNonBlank(chamberLabel, "COLEGIADO") + '_' + preventionClass;
        String sessionRoom = organ.contains("PLENARIO") ? "SALA_PLENARIA_" + court
                : organ.contains("SECAO") ? "SALA_SECAO_" + court
                : "SALA_TURMA_" + court;
        String specializationDepth = organ.contains("PLENARIO") ? "AMPLA"
                : axis.contains("PENAL") || axis.contains("TRIBUTARIO") || axis.contains("PREVIDENCIARIO") ? "SETORIAL"
                : "TEMATICA";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(court);
        labels.add(firstNonBlank(chamberLabel, "COLEGIADO"));
        labels.add(preventionClass);
        labels.add(specializationDepth);
        if (grau != null) {
            labels.add(grau.name());
        }
        if (tipoJustica != null) {
            labels.add(tipoJustica.name());
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", court);
        metadata.put("specializationAxis", axis);
        metadata.put("orgaoFracionario", organ);
        metadata.put("tipoJustica", tipoJustica == null ? null : tipoJustica.name());
        metadata.put("grau", grau == null ? null : grau.name());
        metadata.put("descriptor", chamberLabel + ':' + relatoriaDesk + ':' + sessionRoom);

        return new TribunalChamberSpecializationProfile(
                chamberLabel,
                relatoriaDesk,
                advisoryDesk,
                preventionClass,
                distributionPool,
                sessionRoom,
                specializationDepth,
                List.copyOf(labels),
                metadata
        );
    }

    private static String resolveChamberLabel(String court,
                                              String axis,
                                              String organ,
                                              TipoJustica tipoJustica,
                                              GrauJurisdicao grau) {
        if (court.startsWith("STJ")) {
            if (axis.contains("PENAL")) return "TURMA_PENAL_STJ";
            if (axis.contains("PUBLICO") || axis.contains("TRIBUTARIO") || axis.contains("AMBIENTAL")) return "SECAO_PUBLICO_STJ";
            return "SECAO_PRIVADO_STJ";
        }
        if (court.startsWith("STF")) {
            return organ.contains("PLENARIO") ? "PLENARIO_CONSTITUCIONAL_STF" : "TURMA_CONSTITUCIONAL_STF";
        }
        if (court.startsWith("TRF")) {
            if (axis.contains("PREVIDENCIARIO")) return "TURMA_PREVIDENCIARIA_TRF";
            if (axis.contains("TRIBUTARIO") || axis.contains("FAZENDA") || axis.contains("REGULATORIO")) return "TURMA_PUBLICA_TRF";
            if (axis.contains("PENAL")) return "TURMA_PENAL_TRF";
            return "TURMA_CIVEL_TRF";
        }
        if (court.startsWith("TRT") || tipoJustica == TipoJustica.TRABALHO) {
            if (axis.contains("COLETIVO") || organ.contains("SECAO")) return "SECAO_COLETIVA_TRT";
            if (axis.contains("EXECUCAO")) return "TURMA_EXECUCAO_TRT";
            return "TURMA_TRABALHO_TRT";
        }
        if (court.startsWith("TRE") || tipoJustica == TipoJustica.ELEITORAL) {
            return organ.contains("CORREGEDORIA") ? "CORREGEDORIA_ELEITORAL" : "PLENARIO_ELEITORAL";
        }
        if (court.startsWith("TJM") || tipoJustica == TipoJustica.MILITAR_ESTADUAL) {
            return organ.contains("PLENO") ? "PLENO_MILITAR_ESTADUAL" : "CAMARA_MILITAR_ESTADUAL";
        }
        if (court.startsWith("STM") || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return organ.contains("PLENARIO") ? "PLENARIO_MILITAR_FEDERAL" : "CONSELHO_MILITAR_FEDERAL";
        }
        if (court.startsWith("TST")) {
            return organ.contains("SDI") ? "SECAO_DISSIDIOS_TST" : "TURMA_RECURSAL_TST";
        }
        if (court.startsWith("TSE")) {
            return "PLENARIO_ELEITORAL_SUPERIOR";
        }
        if (court.startsWith("TJ") || grau == GrauJurisdicao.SEGUNDO_GRAU) {
            if (axis.contains("FAMILIA") || axis.contains("SUCESSOES")) return "CAMARA_FAMILIA_TJ";
            if (axis.contains("FAZENDA") || axis.contains("TRIBUTARIO") || axis.contains("PUBLICO")) return "CAMARA_FAZENDA_TJ";
            if (axis.contains("EMPRESARIAL")) return "CAMARA_EMPRESARIAL_TJ";
            if (axis.contains("PENAL") || axis.contains("JURI")) return "CAMARA_CRIMINAL_TJ";
            return "CAMARA_CIVEL_TJ";
        }
        return "COLEGIADO_GERAL";
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
