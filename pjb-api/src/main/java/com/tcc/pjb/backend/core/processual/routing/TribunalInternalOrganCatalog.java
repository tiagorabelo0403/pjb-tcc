package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class TribunalInternalOrganCatalog {

    private final TribunalChamberSpecializationResolver chamberSpecializationResolver;
    private final TribunalSessionTopologyResolver sessionTopologyResolver;
    private final TribunalSpecificOrganResolver specificOrganResolver;
    private final TribunalPanelCompositionResolver panelCompositionResolver;
    private final TribunalDeliberationCycleResolver deliberationCycleResolver;

    public TribunalInternalOrganCatalog(TribunalChamberSpecializationResolver chamberSpecializationResolver,
                                        TribunalSessionTopologyResolver sessionTopologyResolver,
                                        TribunalSpecificOrganResolver specificOrganResolver,
                                        TribunalPanelCompositionResolver panelCompositionResolver,
                                        TribunalDeliberationCycleResolver deliberationCycleResolver) {
        this.chamberSpecializationResolver = chamberSpecializationResolver;
        this.sessionTopologyResolver = sessionTopologyResolver;
        this.specificOrganResolver = specificOrganResolver;
        this.panelCompositionResolver = panelCompositionResolver;
        this.deliberationCycleResolver = deliberationCycleResolver;
    }

    public TribunalInternalOrganProfile resolve(String tribunalCodigo,
                                                TipoJustica tipoJustica,
                                                GrauJurisdicao grau,
                                                String orgaoFracionario,
                                                String specializationAxis) {
        String court = normalize(tribunalCodigo);
        String organ = normalize(orgaoFracionario);
        String axis = normalize(specializationAxis);
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", court);
        metadata.put("grau", grau != null ? grau.name() : null);
        metadata.put("specializationAxis", axis);

        TribunalChamberSpecializationProfile chamber = chamberSpecializationResolver.resolve(court, tipoJustica, grau, axis, organ);

        String macroOrgan;
        String specificOrgan;
        String competenceCluster;
        String secretariatDesk;
        String gabineteDesk;
        String sessionChannel;
        String admissibilityPath;
        String preventionBucket;
        String quorumHint;

        if (court.startsWith("STJ")) {
            macroOrgan = organ.contains("CORTE_ESPECIAL") ? "CORTE_ESPECIAL_STJ" : organ.contains("SECAO") ? "SECAO_STJ" : "TURMA_STJ";
            String section = stjSection(axis);
            specificOrgan = organ.contains("CORTE_ESPECIAL") ? "CORTE_ESPECIAL_STJ" : section + '_' + (organ.contains("TURMA") ? "TURMA" : "COLEGIADO");
            competenceCluster = section;
            secretariatDesk = "SECRETARIA_" + specificOrgan;
            gabineteDesk = "GABINETE_MINISTRO_" + section;
            sessionChannel = organ.contains("CORTE_ESPECIAL") ? "SESSAO_CORTE_ESPECIAL" : "SESSAO_SECAO_TURMA_STJ";
            admissibilityPath = "PRESIDENCIA_STJ>SECRETARIA_" + section + ">" + firstNonBlank(organ, "TURMA_STJ");
            preventionBucket = "PREV_STJ_" + section;
            quorumHint = organ.contains("CORTE_ESPECIAL") ? "MAIORIA_CORTE_ESPECIAL" : "COLEGIADO_DE_5_MINISTROS";
            fundamentos.add("Catálogo interno do STJ separou seção material e trilha de gabinete/secretaria para o órgão fracionário natural.");
            reviewChecklist.add("Conferir se o recurso é da competência da seção temática do STJ ou da Corte Especial.");
        } else if (court.startsWith("STF")) {
            macroOrgan = organ.contains("PLENARIO") ? "PLENARIO_STF" : "TURMA_STF";
            specificOrgan = organ.contains("PLENARIO") ? "PLENARIO_STF" : axis.startsWith("CONSTITUCIONAL") ? "TURMA_STF_CONTROLE" : "TURMA_STF_PROCESSUAL";
            competenceCluster = organ.contains("PLENARIO") ? "PLENARIO" : "TURMA";
            secretariatDesk = organ.contains("PLENARIO") ? "SECRETARIA_PLENARIO_STF" : "SECRETARIA_TURMA_STF";
            gabineteDesk = organ.contains("PLENARIO") ? "ASSESSORIA_PLENARIO_STF" : "GABINETE_MINISTRO_STF";
            sessionChannel = organ.contains("PLENARIO") ? "SESSAO_PLENARIA_STF" : "SESSAO_TURMA_STF";
            admissibilityPath = "PROTOCOLO_STF>" + secretariatDesk + '>' + firstNonBlank(organ, specificOrgan);
            preventionBucket = "PREV_STF_" + competenceCluster;
            quorumHint = organ.contains("PLENARIO") ? "11_MINISTROS" : "5_MINISTROS";
            fundamentos.add("Catálogo do STF diferenciou plenário e turma por cluster de competência constitucional/processual.");
        } else if (court.startsWith("TST")) {
            macroOrgan = organ.contains("SDC") ? "SDC_TST" : organ.contains("SDI2") ? "SDI2_TST" : organ.contains("SDI1") ? "SDI1_TST" : "TURMA_TST";
            specificOrgan = macroOrgan;
            competenceCluster = macroOrgan.replace("_TST", "");
            secretariatDesk = "SECRETARIA_" + macroOrgan;
            gabineteDesk = macroOrgan.startsWith("TURMA") ? "GABINETE_MINISTRO_TST" : "ASSESSORIA_" + macroOrgan;
            sessionChannel = macroOrgan.startsWith("TURMA") ? "SESSAO_TURMA_TST" : "SESSAO_SECAO_TST";
            admissibilityPath = "PRESIDENCIA_TST>" + secretariatDesk + '>' + macroOrgan;
            preventionBucket = "PREV_TST_" + competenceCluster;
            quorumHint = macroOrgan.startsWith("TURMA") ? "3_MINISTROS_OU_MAIS" : "MINISTROS_DA_SECAO";
            fundamentos.add("Catálogo do TST distribuiu o órgão interno entre Turmas, SDI e SDC conforme o eixo recursal e mandamental.");
        } else if (court.startsWith("TSE")) {
            macroOrgan = "PLENARIO_TSE";
            specificOrgan = organ.contains("CORREGEDORIA") ? "CORREGEDORIA_GERAL_ELEITORAL" : "PLENARIO_TSE";
            competenceCluster = "ELEITORAL_SUPERIOR";
            secretariatDesk = "SECRETARIA_JUDICIARIA_TSE";
            gabineteDesk = specificOrgan.startsWith("CORREGEDORIA") ? "GABINETE_CORREGEDORIA_TSE" : "GABINETE_MINISTRO_TSE";
            sessionChannel = "SESSAO_PLENARIA_TSE";
            admissibilityPath = "PROTOCOLO_TSE>SECRETARIA_JUDICIARIA_TSE>PLENARIO_TSE";
            preventionBucket = "PREV_TSE_PLENARIO";
            quorumHint = "7_MINISTROS";
            fundamentos.add("Catálogo do TSE centralizou a competência colegiada no Plenário, com desvio para corregedoria quando o órgão fracionário indicar correição.");
        } else if (court.startsWith("STM") || (tipoJustica == TipoJustica.MILITAR_FEDERAL && court != null && !court.isBlank())) {
            macroOrgan = organ.contains("PLENARIO") ? "PLENARIO_STM" : "CONSELHO_JUSTICA_MILITAR";
            specificOrgan = macroOrgan;
            competenceCluster = "MILITAR_FEDERAL";
            secretariatDesk = "SECRETARIA_JUDICIARIA_STM";
            gabineteDesk = organ.contains("PLENARIO") ? "GABINETE_MINISTRO_STM" : "SECRETARIA_CONSELHO_MILITAR";
            sessionChannel = organ.contains("PLENARIO") ? "SESSAO_PLENARIA_STM" : "SESSAO_CONSELHO_JMU";
            admissibilityPath = "PROTOCOLO_STM>SECRETARIA_JUDICIARIA_STM>" + specificOrgan;
            preventionBucket = "PREV_STM_" + competenceCluster;
            quorumHint = organ.contains("PLENARIO") ? "COLEGIADO_STM" : "CONSELHO_MILITAR";
            fundamentos.add("Catálogo militar distinguiu conselho/auditoria e plenário do STM para não nivelar órgão singular e colegiado.");
        } else if (court.startsWith("TRF")) {
            String family = federalFamily(axis);
            macroOrgan = organ.contains("TURMA") ? "TURMA_TRF" : organ.contains("SECAO") ? "SECAO_TRF" : "COLEGIADO_TRF";
            specificOrgan = macroOrgan + '_' + family;
            competenceCluster = family;
            secretariatDesk = "SECRETARIA_" + macroOrgan + '_' + family;
            gabineteDesk = "GABINETE_DESEMBARGADOR_TRF_" + family;
            sessionChannel = organ.contains("SECAO") ? "SESSAO_SECAO_TRF" : "SESSAO_TURMA_TRF";
            admissibilityPath = "NUCLEO_RECURSAL_TRF>" + secretariatDesk + '>' + specificOrgan;
            preventionBucket = "PREV_TRF_" + family;
            quorumHint = "3_DESEMBARGADORES_FEDERAIS";
            fundamentos.add("Catálogo interno do TRF separou clusters previdenciário, tributário, penal e público-regulatório para a formação colegiada.");
            reviewChecklist.add("Validar se a matéria atrai turma ou seção do TRF conforme a composição temática local.");
        } else if (court.startsWith("TRT")) {
            String family = trabalhoFamily(axis);
            macroOrgan = organ.contains("SECAO") ? "SECAO_TRT" : "TURMA_TRT";
            specificOrgan = macroOrgan + '_' + family;
            competenceCluster = family;
            secretariatDesk = "SECRETARIA_" + specificOrgan;
            gabineteDesk = "GABINETE_DESEMBARGADOR_TRT_" + family;
            sessionChannel = macroOrgan.contains("SECAO") ? "SESSAO_SECAO_TRT" : "SESSAO_TURMA_TRT";
            admissibilityPath = "NUCLEO_RECURSAL_TRT>" + secretariatDesk + '>' + specificOrgan;
            preventionBucket = "PREV_TRT_" + family;
            quorumHint = "3_DESEMBARGADORES_TRABALHISTAS";
            fundamentos.add("Catálogo interno do TRT repartiu a malha entre turmas e seções por família material trabalhista.");
        } else if (court.startsWith("TRE")) {
            macroOrgan = organ.contains("PLENARIO") ? "PLENARIO_TRE" : "COLEGIADO_TRE";
            specificOrgan = organ.contains("CORREGEDORIA") ? "CORREGEDORIA_REGIONAL_ELEITORAL" : macroOrgan;
            competenceCluster = "ELEITORAL_REGIONAL";
            secretariatDesk = "SECRETARIA_JUDICIARIA_TRE";
            gabineteDesk = specificOrgan.startsWith("CORREGEDORIA") ? "GABINETE_CORREGEDORIA_TRE" : "GABINETE_JUIZ_ELEITORAL_RECURSAL";
            sessionChannel = "SESSAO_PLENARIA_TRE";
            admissibilityPath = "PROTOCOLO_TRE>SECRETARIA_JUDICIARIA_TRE>" + specificOrgan;
            preventionBucket = "PREV_TRE_PLENARIO";
            quorumHint = "COLEGIADO_REGIONAL_ELEITORAL";
            fundamentos.add("Catálogo do TRE concentrou a malha colegiada regional no Plenário com via própria de corregedoria.");
        } else if (court.startsWith("TJM")) {
            macroOrgan = organ.contains("CAMARA") ? "CAMARA_MILITAR_ESTADUAL" : "PLENO_TJM";
            specificOrgan = macroOrgan;
            competenceCluster = "MILITAR_ESTADUAL";
            secretariatDesk = "SECRETARIA_" + macroOrgan;
            gabineteDesk = macroOrgan.startsWith("CAMARA") ? "GABINETE_JUIZ_MILITAR_2G" : "ASSESSORIA_PLENO_TJM";
            sessionChannel = macroOrgan.startsWith("CAMARA") ? "SESSAO_CAMARA_MILITAR" : "SESSAO_PLENO_TJM";
            admissibilityPath = "PROTOCOLO_TJM>" + secretariatDesk + '>' + specificOrgan;
            preventionBucket = "PREV_TJM_" + competenceCluster;
            quorumHint = macroOrgan.startsWith("CAMARA") ? "3_JULGADORES_OU_MAIS" : "PLENO_TJM";
            fundamentos.add("Catálogo do TJM separou plenário e câmaras/auditorias para tratamento do militar estadual.");
        } else {
            String family = estadualFamily(axis, organ);
            macroOrgan = organ.contains("ORGAO_ESPECIAL") ? "ORGAO_ESPECIAL_TJ" : organ.contains("PLENARIO") ? "PLENO_TJ" : "CAMARA_TJ";
            specificOrgan = macroOrgan + '_' + family;
            competenceCluster = family;
            secretariatDesk = "SECRETARIA_" + specificOrgan;
            gabineteDesk = macroOrgan.startsWith("CAMARA") ? "GABINETE_DESEMBARGADOR_" + family : "ASSESSORIA_" + macroOrgan;
            sessionChannel = macroOrgan.startsWith("CAMARA") ? "SESSAO_CAMARA_TJ" : "SESSAO_" + macroOrgan;
            admissibilityPath = "NUCLEO_RECURSAL_TJ>" + secretariatDesk + '>' + specificOrgan;
            preventionBucket = "PREV_TJ_" + family;
            quorumHint = macroOrgan.startsWith("CAMARA") ? "3_DESEMBARGADORES" : "COLEGIADO_AMPLIADO";
            fundamentos.add("Catálogo do TJ distribuiu a malha interna entre câmara, órgão especial e plenário conforme o eixo material.");
        }

        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            warnings.add("Catálogo interno colegiado foi invocado em primeiro grau e retornou organograma de apoio, não órgão julgador final.");
            reviewChecklist.add("Conferir se o caso permanece em juízo singular antes de acionar colegiado interno.");
        }

        specificOrgan = chamber.effectiveChamberLabel(specificOrgan);
        gabineteDesk = chamber.effectiveRelatoriaDesk(gabineteDesk);
        secretariatDesk = chamber.effectiveAdvisoryDesk(secretariatDesk);
        sessionChannel = chamber.effectiveSessionRoom(sessionChannel);
        preventionBucket = chamber.effectivePreventionClass(preventionBucket);
        admissibilityPath = firstNonBlank(admissibilityPath, chamber.effectiveDistributionPool(null));
        TribunalSpecificOrganProfile specific = specificOrganResolver.resolve(court, tipoJustica, grau, specificOrgan, axis);
        specificOrgan = specific.effectiveOrganAlias(specificOrgan);
        TribunalSessionTopologyProfile topology = sessionTopologyResolver.resolve(court, specificOrgan, competenceCluster, axis, specificOrgan, sessionChannel);
        TribunalPanelCompositionProfile panelComposition = panelCompositionResolver.resolve(court, tipoJustica, grau, specificOrgan, sessionChannel, quorumHint, axis);
        TribunalDeliberationCycleProfile deliberationCycle = deliberationCycleResolver.resolve(court, tipoJustica, grau, specificOrgan, axis, sessionChannel, quorumHint);
        secretariatDesk = firstNonBlank(topology.sessionSecretariatDesk(), specific.publicationDesk(), secretariatDesk);
        sessionChannel = firstNonBlank(topology.sessionBlock(), sessionChannel);
        quorumHint = firstNonBlank(topology.panelSizeHint(), panelComposition.panelCompositionLabel(), quorumHint);
        metadata.put("chamber", chamber.toMap());
        metadata.put("specificOrganProfile", specific.toMap());
        metadata.put("sessionTopology", topology.toMap());
        metadata.put("panelComposition", panelComposition.toMap());
        metadata.put("deliberationCycle", deliberationCycle.toMap());
        fundamentos.add("Especialização interna sugerida: " + specificOrgan + '.');
        fundamentos.add("Topologia específica do tribunal: " + specific.topologyDescriptor() + '.');
        fundamentos.add("Topologia de sessão sugerida: " + topology.descriptor() + '.');
        fundamentos.add("Composição do painel sugerida: " + panelComposition.descriptor() + '.');
        fundamentos.add("Ciclo deliberativo sugerido: " + deliberationCycle.descriptor() + '.');
        reviewChecklist.add("Conferir prevenção por classe temática e pool de distribuição interno do tribunal.");
        reviewChecklist.add("Conferir publicação e revisão do órgão interno segundo a família específica do tribunal.");
        reviewChecklist.add("Conferir fluxo de votação, sustentação oral e cluster de assessoria do painel colegiado.");

        return new TribunalInternalOrganProfile(
                macroOrgan,
                specificOrgan,
                competenceCluster,
                secretariatDesk,
                gabineteDesk,
                sessionChannel,
                admissibilityPath,
                preventionBucket,
                quorumHint,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private static String stjSection(String axis) {
        if (axis.contains("PENAL") || axis.contains("MILITAR")) {
            return "TERCEIRA_SECAO_STJ";
        }
        if (axis.contains("TRIBUTARIO") || axis.contains("PREVIDENCIARIO") || axis.contains("ADMINISTRATIVO") || axis.contains("PUBLICO")) {
            return "PRIMEIRA_SECAO_STJ";
        }
        return "SEGUNDA_SECAO_STJ";
    }

    private static String federalFamily(String axis) {
        if (axis.contains("PREVIDENCIARIO")) {
            return "PREVIDENCIARIA";
        }
        if (axis.contains("TRIBUTARIO") || axis.contains("FAZENDA")) {
            return "TRIBUTARIA";
        }
        if (axis.contains("PENAL") || axis.contains("MILITAR")) {
            return "PENAL";
        }
        if (axis.contains("AMBIENTAL") || axis.contains("AGRARIA") || axis.contains("EMPRESARIAL")) {
            return "ESPECIALIZADA";
        }
        return "PUBLICO_CIVEL";
    }

    private static String trabalhoFamily(String axis) {
        if (axis.contains("COLETIVO")) {
            return "COLETIVO";
        }
        if (axis.contains("MANDADO") || axis.contains("RESCISORIA")) {
            return "ORIGINARIO";
        }
        return "ORDINARIO";
    }

    private static String estadualFamily(String axis, String organ) {
        if (organ.contains("ORGAO_ESPECIAL")) {
            return "ORGAO_ESPECIAL";
        }
        if (axis.contains("CRIMINAL") || axis.contains("JURI") || axis.contains("VIOLENCIA_DOMESTICA")) {
            return "CRIMINAL";
        }
        if (axis.contains("FAMILIA") || axis.contains("SUCESSOES") || axis.contains("INFANCIA")) {
            return "FAMILIA_PUBLICO_PESSOAL";
        }
        if (axis.contains("FAZENDA") || axis.contains("TRIBUTARIO") || axis.contains("PREVIDENCIARIO")) {
            return "FAZENDA_PUBLICA";
        }
        if (axis.contains("EMPRESARIAL") || axis.contains("AMBIENTAL") || axis.contains("AGRARIA")) {
            return "ESPECIALIZADA";
        }
        return "CIVEL";
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "BASE";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
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
