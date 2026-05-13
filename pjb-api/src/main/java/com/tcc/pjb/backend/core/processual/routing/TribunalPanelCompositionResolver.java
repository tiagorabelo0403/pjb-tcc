package com.tcc.pjb.backend.core.processual.routing;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TribunalPanelCompositionResolver {

    public TribunalPanelCompositionProfile resolve(String tribunalCodigo,
                                                   TipoJustica tipoJustica,
                                                   GrauJurisdicao grau,
                                                   String specificOrgan,
                                                   String sessionChannel,
                                                   String quorumHint,
                                                   String specializationAxis) {
        String court = normalize(tribunalCodigo, "TRIBUNAL");
        String organ = normalize(specificOrgan, "ORGAO_INTERNO");
        String axis = normalize(specializationAxis, "GERAL");
        boolean superior = grau == GrauJurisdicao.SUPERIOR || court.startsWith("ST");
        boolean plenary = organ.contains("PLENARIO") || organ.contains("PLENO") || organ.contains("ORGAO_ESPECIAL");
        boolean section = organ.contains("SECAO") || organ.contains("SDI") || organ.contains("SDC");
        boolean chamber = organ.contains("CAMARA") || organ.contains("TURMA");
        boolean penal = axis.contains("PENAL") || axis.contains("MILITAR");

        String panelCompositionLabel = plenary
                ? superior ? "PAINEL_PLENARIO_SUPERIOR" : "PAINEL_PLENARIO_REGIMENTAL"
                : section ? "PAINEL_SECAO_TEMATICA"
                : chamber ? "PAINEL_CAMARA_TURMA" : "PAINEL_COLEGIADO_BASE";
        String relatoriaMode = superior
                ? "RELATORIA_MINISTRO_COM_ASSESSORIA"
                : plenary ? "RELATORIA_ORGAO_ESPECIAL"
                : penal ? "RELATORIA_COM_REVISAO_REFORCADA"
                : "RELATORIA_DESEMPATE_PADRAO";
        String reviewFlow = plenary
                ? "REVISAO_PUBLICACAO_PLENARIA"
                : section ? "REVISAO_SECAO_TEMATICA"
                : chamber ? "REVISAO_CAMARA_TURMA"
                : "REVISAO_COLEGIADA_PADRAO";
        String voteCollectionMode = superior
                ? "COLETA_VOTO_GABINETE_MINISTRO"
                : plenary ? "COLETA_VOTO_COLEGIADO_AMPLIADO"
                : penal ? "COLETA_VOTO_COM_REVISOR"
                : "COLETA_VOTO_RELATOR_E_VOGAIS";
        String sustentacaoWindow = penal
                ? "SUSTENTACAO_PRIORIDADE_PENAL"
                : tipoJustica == TipoJustica.ELEITORAL
                ? "SUSTENTACAO_JANELA_ELEITORAL"
                : superior ? "SUSTENTACAO_JANELA_SUPERIOR"
                : "SUSTENTACAO_JANELA_REGIMENTAL";
        String publicationSequence = plenary
                ? "PUBLICACAO_PAUTA_PLENARIO>REVISAO_ACORDAO>DIARIO"
                : section ? "PUBLICACAO_PAUTA_SECAO>REVISAO_VOTO>DIARIO"
                : "PUBLICACAO_PAUTA_CAMARA>REVISAO_ACORDAO>DIARIO";
        String clerkCluster = superior
                ? "CLERK_CLUSTER_SUPERIOR"
                : tipoJustica == TipoJustica.TRABALHO
                ? "CLERK_CLUSTER_TRABALHISTA"
                : tipoJustica == TipoJustica.ELEITORAL
                ? "CLERK_CLUSTER_ELEITORAL"
                : tipoJustica == TipoJustica.MILITAR_FEDERAL || tipoJustica == TipoJustica.MILITAR_ESTADUAL
                ? "CLERK_CLUSTER_MILITAR"
                : "CLERK_CLUSTER_COLEGIADO";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(panelCompositionLabel);
        labels.add(relatoriaMode);
        labels.add(voteCollectionMode);
        labels.add(clerkCluster);
        if (sessionChannel != null && !sessionChannel.isBlank()) {
            labels.add(sessionChannel.trim());
        }
        if (quorumHint != null && !quorumHint.isBlank()) {
            labels.add(quorumHint.trim());
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", court);
        metadata.put("specificOrgan", organ);
        metadata.put("axis", axis);
        metadata.put("superior", superior);
        metadata.put("plenary", plenary);
        metadata.put("section", section);
        metadata.put("chamber", chamber);
        metadata.put("descriptor", panelCompositionLabel + ':' + voteCollectionMode + ':' + publicationSequence);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new TribunalPanelCompositionProfile(
                panelCompositionLabel,
                relatoriaMode,
                reviewFlow,
                voteCollectionMode,
                sustentacaoWindow,
                publicationSequence,
                clerkCluster,
                List.copyOf(labels),
                metadata
        );
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
