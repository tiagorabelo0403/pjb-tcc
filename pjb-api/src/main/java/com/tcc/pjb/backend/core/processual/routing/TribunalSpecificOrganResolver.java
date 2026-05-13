package com.tcc.pjb.backend.core.processual.routing;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TribunalSpecificOrganResolver {

    public TribunalSpecificOrganProfile resolve(String tribunalCodigo,
                                                TipoJustica tipoJustica,
                                                GrauJurisdicao grau,
                                                String organLabel,
                                                String specializationAxis) {
        String court = normalize(firstNonBlank(tribunalCodigo, "BASE"));
        String organ = normalize(firstNonBlank(organLabel, "ORGAO"));
        String axis = normalize(firstNonBlank(specializationAxis, "GENERAL"));

        String tribunalFamily;
        String organAlias;
        String publicationDesk;
        String publicationQueue;
        String reviewDesk;
        String internalRouteKey;
        String topologyDescriptor;

        if (court.startsWith("STF")) {
            tribunalFamily = "STF";
            organAlias = organ.contains("TURMA") ? "TURMA_STF_" + turmaConstitucional(axis) : "PLENARIO_STF";
            publicationDesk = organAlias.startsWith("PLENARIO") ? "PUBLICACAO_PLENARIO_STF" : "PUBLICACAO_TURMA_STF";
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = organAlias.startsWith("PLENARIO") ? "REVISAO_CONSTITUCIONAL_PLENARIO" : "REVISAO_CONSTITUCIONAL_TURMA";
            internalRouteKey = "STF:" + organAlias;
            topologyDescriptor = organAlias.startsWith("PLENARIO") ? "PLENARIO_SUPREMO:PUBLICACAO_IMEDIATA" : "TURMA_SUPREMA:PUBLICACAO_GABINETE";
        } else if (court.startsWith("STJ")) {
            tribunalFamily = "STJ";
            organAlias = organ.contains("CORTE_ESPECIAL") ? "CORTE_ESPECIAL_STJ"
                    : organ.contains("SECAO") ? stjSecao(axis)
                    : "TURMA_STJ_" + axis;
            publicationDesk = organAlias.contains("CORTE_ESPECIAL") ? "PUBLICACAO_CORTE_ESPECIAL_STJ"
                    : organAlias.contains("SECAO") ? "PUBLICACAO_SECAO_STJ"
                    : "PUBLICACAO_TURMA_STJ";
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = organAlias.contains("PENAL") ? "REVISAO_PENAL_STJ" : "REVISAO_MATERIAL_STJ";
            internalRouteKey = "STJ:" + organAlias;
            topologyDescriptor = organAlias + ":PUBLICACAO_MINISTRO_RELATOR";
        } else if (court.startsWith("TRF")) {
            tribunalFamily = court;
            organAlias = organ.contains("SECAO") ? court + "_SECAO_" + axis : court + "_TURMA_" + axis;
            publicationDesk = "PUBLICACAO_" + court + '_' + (organ.contains("SECAO") ? "SECAO" : "TURMA");
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = "REVISAO_RELATORIA_" + court;
            internalRouteKey = court + ":" + axis + ':' + organ;
            topologyDescriptor = court + ":" + (organ.contains("SECAO") ? "SECAO_TEMATICA" : "TURMA_TEMATICA");
        } else if (court.startsWith("TRT")) {
            tribunalFamily = court;
            organAlias = organ.contains("SECAO") ? court + "_SECAO_" + axis : court + "_TURMA_" + axis;
            publicationDesk = "PUBLICACAO_" + court + '_' + (organ.contains("SECAO") ? "SECAO" : "TURMA");
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = tipoJustica == TipoJustica.TRABALHO ? "REVISAO_TRABALHISTA_" + court : "REVISAO_" + court;
            internalRouteKey = court + ":" + axis + ':' + organ;
            topologyDescriptor = court + ":SESSAO_COLEGIADA_TRABALHISTA";
        } else if (court.startsWith("TRE")) {
            tribunalFamily = court;
            organAlias = organ.contains("CORREGEDORIA") ? court + "_CORREGEDORIA" : court + "_PLENARIO";
            publicationDesk = "PUBLICACAO_" + organAlias;
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = "REVISAO_ELEITORAL_" + court;
            internalRouteKey = court + ":ELEITORAL:" + organAlias;
            topologyDescriptor = court + ":PLENARIO_ELEITORAL";
        } else if (court.startsWith("TJM") || court.startsWith("STM")) {
            tribunalFamily = court.startsWith("STM") ? "STM" : court;
            organAlias = organ.contains("PLENO") ? tribunalFamily + "_PLENO" : tribunalFamily + "_CAMARA_" + axis;
            publicationDesk = "PUBLICACAO_" + tribunalFamily;
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = "REVISAO_MILITAR_" + tribunalFamily;
            internalRouteKey = tribunalFamily + ":MILITAR:" + organAlias;
            topologyDescriptor = tribunalFamily + ":COLEGIADO_MILITAR";
        } else {
            tribunalFamily = court.startsWith("TJ") ? court : (grau == GrauJurisdicao.PRIMEIRO_GRAU ? "JUIZO_SINGULAR" : "TJ");
            organAlias = organ.contains("ORGAO_ESPECIAL") ? tribunalFamily + "_ORGAO_ESPECIAL"
                    : organ.contains("PLENO") || organ.contains("PLENARIO") ? tribunalFamily + "_PLENO"
                    : tribunalFamily + "_CAMARA_" + axis;
            publicationDesk = organAlias.contains("ORGAO_ESPECIAL") ? "PUBLICACAO_ORGAO_ESPECIAL_" + tribunalFamily
                    : organAlias.contains("PLENO") ? "PUBLICACAO_PLENO_" + tribunalFamily
                    : "PUBLICACAO_CAMARA_" + tribunalFamily;
            publicationQueue = organAlias + "_PUB_QUEUE";
            reviewDesk = axis.contains("CRIMINAL") ? "REVISAO_CRIMINAL_" + tribunalFamily : "REVISAO_CIVEL_" + tribunalFamily;
            internalRouteKey = tribunalFamily + ':' + axis + ':' + organ;
            topologyDescriptor = organAlias.contains("PLENO") ? tribunalFamily + ":PLENO" : tribunalFamily + ":CAMARA_ESPECIALIZADA";
        }

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(tribunalFamily);
        labels.add(organAlias);
        labels.add(topologyDescriptor);
        if (grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            labels.add("CORTE_SUPERIOR_TOPOLOGY");
        }
        if (tipoJustica != null) {
            labels.add(tipoJustica.name());
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", court);
        metadata.put("tipoJustica", tipoJustica == null ? null : tipoJustica.name());
        metadata.put("grau", grau == null ? null : grau.name());
        metadata.put("specializationAxis", axis);
        metadata.put("descriptor", internalRouteKey + ':' + publicationDesk);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new TribunalSpecificOrganProfile(
                tribunalFamily,
                organAlias,
                publicationDesk,
                publicationQueue,
                reviewDesk,
                internalRouteKey,
                topologyDescriptor,
                List.copyOf(labels),
                metadata
        );
    }

    private static String stjSecao(String axis) {
        if (axis.contains("PENAL") || axis.contains("MILITAR")) {
            return "TERCEIRA_SECAO_STJ";
        }
        if (axis.contains("TRIBUTARIO") || axis.contains("PREVIDENCIARIO") || axis.contains("PUBLICO") || axis.contains("FAZENDA")) {
            return "PRIMEIRA_SECAO_STJ";
        }
        return "SEGUNDA_SECAO_STJ";
    }

    private static String turmaConstitucional(String axis) {
        if (axis.contains("PENAL") || axis.contains("HC")) {
            return "PENAL";
        }
        return "CIVEL";
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

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? "BASE" : normalized;
    }
}
