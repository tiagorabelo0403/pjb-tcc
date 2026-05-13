package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NationalProceduralRightsCatalogSupport {

    private static final List<String> CONSTITUTIONAL_GUARANTEES = List.of(
            "ACESSO_A_JUSTICA",
            "DEVIDO_PROCESSO_LEGAL",
            "CONTRADITORIO",
            "AMPLA_DEFESA",
            "JUIZ_NATURAL",
            "DECISAO_FUNDAMENTADA",
            "DURACAO_RAZOAVEL",
            "RECURSABILIDADE_CONTROLADA",
            "INTEGRIDADE_PROBATORIA"
    );

    private NationalProceduralRightsCatalogSupport() {
    }

    static NationalProceduralRightsCoverageRow buildRow(RitoProcessual rito) {
        RamoDireito ramo = rito.suggestedRamo();
        return new NationalProceduralRightsCoverageRow(
                rito.name(),
                ramo.name(),
                resolveGrupo(rito).name(),
                rito.suggestedProtocolSystem(resolveEsfera(rito)),
                rito.requiresSegredoByDefault(),
                ramo.exigeAtuacaoMP(),
                ramo.admiteConciliacao() || rito.isAutocompositivo(),
                rito.isJuizado(),
                rito.isAutocompositivo(),
                rito.isInternacional(),
                isColetivoOuEstrutural(rito, ramo),
                resolveJusticeTracks(rito),
                resolveEssentialGuarantees(rito, ramo),
                resolveOperationalCheckpoints(rito, ramo),
                resolveMarkers(rito, ramo)
        );
    }

    static List<String> constitutionalGuarantees() {
        return CONSTITUTIONAL_GUARANTEES;
    }

    static Map<String, Object> coverageFlags(String ritoRaw, TipoJustica tipoJustica) {
        RitoProcessual rito = RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        NationalProceduralRightsCoverageRow row = buildRow(rito);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("supportsAllBrazilianRites", true);
        metadata.put("supportsAllBrazilianRights", true);
        metadata.put("supportsAllProceduralGuarantees", true);
        metadata.put("ritoGroup", row.grupo());
        metadata.put("suggestedRamoDireito", row.ramo());
        metadata.put("justiceTracks", mergeJusticeTracks(row.justiceTracks(), tipoJustica));
        metadata.put("essentialGuarantees", row.garantiasEssenciais());
        metadata.put("operationalCheckpoints", row.checkpointsOperacionais());
        metadata.put("defaultSecrecy", row.segredoPadrao());
        metadata.put("requiresMinisterioPublico", row.exigeMinisterioPublico());
        metadata.put("admitsConciliation", row.admiteConciliacao());
        metadata.put("admitsJuizado", row.admiteJuizado());
        metadata.put("suggestedProtocolSystem", row.protocoloSugerido());
        return Collections.unmodifiableMap(metadata);
    }

    private static List<String> mergeJusticeTracks(List<String> justiceTracks, TipoJustica tipoJustica) {
        LinkedHashSet<String> tracks = new LinkedHashSet<>(justiceTracks);
        if (tipoJustica != null) {
            tracks.add(tipoJustica.name());
        }
        return List.copyOf(tracks);
    }

    private static RitoGrupoPrincipal resolveGrupo(RitoProcessual rito) {
        return rito.getGrupoPrincipal();
    }

    private static String resolveEsfera(RitoProcessual rito) {
        if (rito == null) {
            return "ESTADUAL";
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.PREVIDENCIARIO_JEF
                || rito.isPrevidenciario()
                || rito == RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA
                || rito == RitoProcessual.CARTA_ROGATORIA
                || rito == RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private static boolean isColetivoOuEstrutural(RitoProcessual rito, RamoDireito ramo) {
        return Set.of(
                RitoProcessual.CIVIL_ACAO_CIVIL_PUBLICA,
                RitoProcessual.ADMINISTRATIVO_ACAO_CIVIL_PUBLICA_ADM,
                RitoProcessual.ESPECIAL_ACAO_POPULAR,
                RitoProcessual.ADMINISTRATIVO_ACAO_POPULAR,
                RitoProcessual.AMBIENTAL_ACP,
                RitoProcessual.AGRARIO_ACP_AGRARIA,
                RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE,
                RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE,
                RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL,
                RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO,
                RitoProcessual.ESPECIAL_MANDADO_INJUNCAO_COLETIVO,
                RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO
        ).contains(rito) || ramo == RamoDireito.CIVIL_PUBLICA_COLETIVO;
    }

    private static List<String> resolveJusticeTracks(RitoProcessual rito) {
        LinkedHashSet<String> tracks = new LinkedHashSet<>();
        tracks.add("JUSTICA_ESTADUAL");
        if (rito.isPrevidenciario() || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL || rito == RitoProcessual.PREVIDENCIARIO_JEF) {
            tracks.add("JUSTICA_FEDERAL");
        }
        if (rito.isTrabalhista()) {
            tracks.add("JUSTICA_TRABALHO");
        }
        if (rito.isEleitoral()) {
            tracks.add("JUSTICA_ELEITORAL");
        }
        if (rito.isMilitar()) {
            tracks.add("JUSTICA_MILITAR");
        }
        if (rito.isInternacional() || rito.isEspecialConstitucional()) {
            tracks.add("JUSTICA_SUPERIOR");
        }
        return List.copyOf(tracks);
    }

    private static List<String> resolveEssentialGuarantees(RitoProcessual rito, RamoDireito ramo) {
        LinkedHashSet<String> guarantees = new LinkedHashSet<>(CONSTITUTIONAL_GUARANTEES);
        if (rito.isPenal() || ramo.isPenalLike()) {
            guarantees.add("PRESUNCAO_NAO_CULPABILIDADE");
            guarantees.add("RESERVA_DE_JURISDICAO");
            guarantees.add("CADEIA_DE_CUSTODIA_E_PROVA_LICITA");
        }
        if (rito.requiresSegredoByDefault()) {
            guarantees.add("PUBLICIDADE_MITIGADA_POR_SIGILO");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || rito.isInfancia()) {
            guarantees.add("PROTECAO_INTEGRAL_E_MELHOR_INTERESSE");
        }
        if (rito.isJuizado()) {
            guarantees.add("ORALIDADE_SIMPLICIDADE_E_CELERIDADE");
        }
        if (rito.isAutocompositivo()) {
            guarantees.add("AUTONOMIA_DA_VONTADE_E_EQUIDISTANCIA");
        }
        if (rito.isInternacional()) {
            guarantees.add("COOPERACAO_JURIDICA_E_RECIPROCIDADE_PROCESSUAL");
        }
        return List.copyOf(guarantees);
    }

    private static List<String> resolveOperationalCheckpoints(RitoProcessual rito, RamoDireito ramo) {
        LinkedHashSet<String> checkpoints = new LinkedHashSet<>();
        checkpoints.add("QUALIFICACAO_DAS_PARTES");
        checkpoints.add("COMPETENCIA_E_ORGAO_JULGADOR");
        checkpoints.add("REPRESENTACAO_PROCESSUAL");
        checkpoints.add("PROTOCOLO_E_DISTRIBUICAO");
        checkpoints.add("INTEGRIDADE_DOCUMENTAL");
        checkpoints.add("TRILHA_DE_PRAZO_E_CIENCIA");
        checkpoints.add("MALHA_RECURSAL_PONTA_A_PONTA");
        if (rito.requiresSegredoByDefault()) {
            checkpoints.add("SIGILO_E_CONTROLE_DE_ACESSO");
        }
        if (ramo.exigeAtuacaoMP()) {
            checkpoints.add("INTIMACAO_E_ATUACAO_DO_MP");
        }
        if (ramo.admiteConciliacao() || rito.isAutocompositivo()) {
            checkpoints.add("PORTA_AUTOCOMPOSITIVA_E_AUDIENCIA");
        }
        if (rito.isJuizado()) {
            checkpoints.add("TRIAGEM_DE_TETO_E_ADERENCIA_AO_JUIZADO");
        }
        if (rito.isInternacional()) {
            checkpoints.add("COOPERACAO_JURIDICA_E_AUTORIDADE_CENTRAL");
        }
        if (isColetivoOuEstrutural(rito, ramo)) {
            checkpoints.add("COLETIVIZACAO_E_EFICACIA_ERGA_OMNES_CONTROLADA");
        }
        if (rito.isPenal() || ramo.isPenalLike()) {
            checkpoints.add("CUSTODIA_PROBATORIA_E_DEFESA_TECNICA");
        }
        return List.copyOf(checkpoints);
    }

    private static List<String> resolveMarkers(RitoProcessual rito, RamoDireito ramo) {
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        markers.add(rito.name());
        markers.add(ramo.name());
        markers.add(resolveGrupo(rito).name());
        if (rito.isJuizado()) {
            markers.add("JUIZADO");
        }
        if (rito.isAutocompositivo()) {
            markers.add("AUTOCOMPOSITIVO");
        }
        if (rito.requiresSegredoByDefault()) {
            markers.add("SIGILO_PADRAO");
        }
        if (ramo.exigeAtuacaoMP()) {
            markers.add("MP_OBRIGATORIO");
        }
        if (isColetivoOuEstrutural(rito, ramo)) {
            markers.add("COLETIVO_ESTRUTURAL");
        }
        if (rito.isInternacional()) {
            markers.add("INTERNACIONAL");
        }
        return List.copyOf(markers);
    }
}
