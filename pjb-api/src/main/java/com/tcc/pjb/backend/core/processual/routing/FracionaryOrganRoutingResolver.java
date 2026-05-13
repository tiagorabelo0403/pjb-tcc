package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public class FracionaryOrganRoutingResolver {

    private final RecursalCollegiateResolver recursalCollegiateResolver;
    private final CollegiateOrganCatalog collegiateOrganCatalog;
    private final TribunalInternalOrganCatalog tribunalInternalOrganCatalog;

    public FracionaryOrganRoutingResolver(RecursalCollegiateResolver recursalCollegiateResolver,
                                          CollegiateOrganCatalog collegiateOrganCatalog,
                                          TribunalInternalOrganCatalog tribunalInternalOrganCatalog) {
        this.recursalCollegiateResolver = Objects.requireNonNull(recursalCollegiateResolver);
        this.collegiateOrganCatalog = Objects.requireNonNull(collegiateOrganCatalog);
        this.tribunalInternalOrganCatalog = Objects.requireNonNull(tribunalInternalOrganCatalog);
    }

    public FracionaryOrganRoutingProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                                 TipoJustica tipoJustica,
                                                 NationalCompetenceMatrix competencia,
                                                 TerritorialRoutingProfile territorial,
                                                 String specializationAxis) {
        Objects.requireNonNull(command, "command");
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (command.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("tribunalCodigo", competencia != null ? competencia.codigo() : null);
            metadata.put("territorialMode", territorial.mode());
            return new FracionaryOrganRoutingProfile(
                    null,
                    "MONOCRATICO_SINGULAR",
                    "JUIZO_SINGULAR",
                    "JUIZO_ORIGINARIO",
                    null,
                    "SESSAO_NAO_APLICAVEL",
                    "1_MAGISTRADO",
                    null,
                    null,
                    false,
                    List.of(),
                    List.of("Processamento orientado para juízo singular de origem."),
                    List.of(),
                    metadata
            );
        }

        String orgaoFracionario;
        String colegiadoMode;
        String chamberFamily;
        String gabineteMode;
        String admissibilityDesk;
        String sessionMode;
        String panelComposition;
        String allocationOverride;
        String deskProfileOverride;
        boolean virtualSessionEligible = true;

        if (command.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            boolean turma = isTurmaConstitucionalCase(command.rito());
            orgaoFracionario = turma ? "TURMA_STF_" + familyForConstitutional(command.rito(), specializationAxis) : "PLENARIO_STF";
            colegiadoMode = turma ? "TURMA_CONSTITUCIONAL" : "PLENARIO_CONSTITUCIONAL";
            chamberFamily = turma ? "TURMA_STF" : "PLENARIO_STF";
            gabineteMode = turma ? "GABINETE_MINISTRO_PREVENTO" : "ASSESSORIA_PLENARIO";
            admissibilityDesk = turma ? "MESA_ADMISSIBILIDADE_STF_TURMA" : "MESA_ADMISSIBILIDADE_STF_PLENARIO";
            sessionMode = turma ? "SESSAO_VIRTUAL_OU_PRESENCIAL" : "SESSAO_PLENARIA";
            panelComposition = turma ? "5_MINISTROS" : "11_MINISTROS";
            allocationOverride = "DISTRIBUICAO_PARA_ORGAO_FRACIONARIO_CONSTITUCIONAL";
            deskProfileOverride = turma ? "GABINETE_STF_TURMA_" + familyForConstitutional(command.rito(), specializationAxis) : "PLENARIO_STF";
            reviewChecklist.add("Validar competência do STF entre turma e plenário.");
        } else if (command.grau() == GrauJurisdicao.SUPERIOR) {
            SuperiorProfile superior = resolveSuperiorProfile(tipoJustica, command.rito(), specializationAxis);
            orgaoFracionario = superior.orgaoFracionario();
            colegiadoMode = superior.colegiadoMode();
            chamberFamily = superior.chamberFamily();
            gabineteMode = superior.gabineteMode();
            admissibilityDesk = superior.admissibilityDesk();
            sessionMode = superior.sessionMode();
            panelComposition = superior.panelComposition();
            allocationOverride = superior.allocationStrategyOverride();
            deskProfileOverride = superior.deskProfileOverride();
            virtualSessionEligible = superior.virtualSessionEligible();
            reviewChecklist.add("Validar competência entre turma, seção, corte especial, órgão especial ou plenário superior.");
        } else {
            SecondInstanceProfile second = resolveSecondInstanceProfile(tipoJustica, command.rito(), specializationAxis, territorial);
            orgaoFracionario = second.orgaoFracionario();
            colegiadoMode = second.colegiadoMode();
            chamberFamily = second.chamberFamily();
            gabineteMode = second.gabineteMode();
            admissibilityDesk = second.admissibilityDesk();
            sessionMode = second.sessionMode();
            panelComposition = second.panelComposition();
            allocationOverride = second.allocationStrategyOverride();
            deskProfileOverride = second.deskProfileOverride();
            virtualSessionEligible = second.virtualSessionEligible();
            reviewChecklist.add("Conferir câmara/turma/seção competente e prevenção do relator no segundo grau.");
        }

        RecursalCollegiateProfile bridge = recursalCollegiateResolver.resolve(
                command,
                tipoJustica,
                competencia != null ? competencia.codigo() : null,
                territorial,
                specializationAxis,
                orgaoFracionario
        );
        orgaoFracionario = firstNonBlank(bridge.colegiadoNatural(), orgaoFracionario);
        chamberFamily = firstNonBlank(bridge.cluster(), chamberFamily);
        admissibilityDesk = firstNonBlank(bridge.presidencyDesk(), admissibilityDesk);
        colegiadoMode = firstNonBlank(bridge.authorityMode(), colegiadoMode);
        warnings.addAll(bridge.warnings());
        fundamentos.addAll(bridge.fundamentos());
        reviewChecklist.addAll(bridge.reviewChecklist());

        CollegiateOrganCatalogProfile catalog = collegiateOrganCatalog.resolve(
                command,
                tipoJustica,
                competencia != null ? competencia.codigo() : null,
                orgaoFracionario,
                specializationAxis
        );
        chamberFamily = firstNonBlank(catalog.tribunalMacroFamily(), chamberFamily);
        admissibilityDesk = catalog.effectiveSecretariatDesk(admissibilityDesk);
        gabineteMode = catalog.effectiveGabineteCluster(gabineteMode);
        panelComposition = catalog.effectiveCompositionHint(panelComposition);
        sessionMode = catalog.effectiveSessionCadence(sessionMode);
        warnings.addAll(catalog.warnings());
        fundamentos.addAll(catalog.fundamentos());
        reviewChecklist.addAll(catalog.reviewChecklist());

        TribunalInternalOrganProfile internalOrgan = tribunalInternalOrganCatalog.resolve(
                competencia != null ? competencia.codigo() : null,
                tipoJustica,
                command.grau(),
                orgaoFracionario,
                specializationAxis
        );
        orgaoFracionario = internalOrgan.effectiveSpecificOrgan(orgaoFracionario);
        admissibilityDesk = internalOrgan.effectiveSecretariatDesk(admissibilityDesk);
        gabineteMode = internalOrgan.effectiveGabineteDesk(gabineteMode);
        sessionMode = internalOrgan.effectiveSessionChannel(sessionMode);
        chamberFamily = firstNonBlank(internalOrgan.competenceCluster(), chamberFamily);
        panelComposition = firstNonBlank(internalOrgan.quorumHint(), panelComposition);
        warnings.addAll(internalOrgan.warnings());
        fundamentos.addAll(internalOrgan.fundamentos());
        reviewChecklist.addAll(internalOrgan.reviewChecklist());

        if (command.segredoSolicitado()) {
            warnings.add("Órgão fracionário deve receber trilha de sigilo reforçada antes da conclusão.");
            reviewChecklist.add("Verificar segredo de justiça na pauta, mesa e gabinete colegiado.");
        }
        if (command.plantaoJudicial() || command.pedidoLiminar()) {
            fundamentos.add("Fluxo urgente pode deslocar a admissibilidade para desk de urgência, sem afastar o colegiado natural.");
        }
        fundamentos.add("Órgão fracionário sugerido: " + orgaoFracionario + '.');
        fundamentos.add("Composição estimada: " + panelComposition + '.');
        fundamentos.add("Sessão elegível: " + sessionMode + '.');

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", competencia != null ? competencia.codigo() : null);
        metadata.put("tribunalNome", competencia != null ? competencia.nome() : null);
        metadata.put("specializationAxis", specializationAxis);
        metadata.put("territorialLabel", territorial.territorialLabel());
        metadata.put("grau", command.grau().name());
        metadata.put("tipoJustica", tipoJustica != null ? tipoJustica.name() : null);
        metadata.put("bridge", bridge.toMap());
        metadata.put("catalog", catalog.toMap());
        metadata.put("internalOrgan", internalOrgan.toMap());

        return new FracionaryOrganRoutingProfile(
                orgaoFracionario,
                colegiadoMode,
                chamberFamily,
                gabineteMode,
                admissibilityDesk,
                sessionMode,
                panelComposition,
                allocationOverride,
                deskProfileOverride,
                virtualSessionEligible,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private boolean isTurmaConstitucionalCase(RitoProcessual rito) {
        return rito != null && (rito.name().contains("HABEAS_CORPUS")
                || rito.name().contains("RECLAMACAO")
                || rito.name().contains("MANDADO_SEGURANCA"));
    }

    private String familyForConstitutional(RitoProcessual rito, String specializationAxis) {
        if (rito == null) {
            return specializationAxis;
        }
        if (rito.name().contains("HABEAS_CORPUS")) {
            return "HC";
        }
        if (rito.name().contains("MANDADO_SEGURANCA")) {
            return "MS";
        }
        return specializationAxis;
    }

    private SuperiorProfile resolveSuperiorProfile(TipoJustica tipoJustica, RitoProcessual rito, String specializationAxis) {
        if (tipoJustica == TipoJustica.TRABALHO) {
            if (rito == RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO) {
                return new SuperiorProfile("SDC_TST", "SECAO_ESPECIALIZADA", "SDC", "ASSESSORIA_SDC", "MESA_ADMISSIBILIDADE_TST_SDC", "SESSAO_COLEGIADA", "MINISTROS_DA_SECAO", "DISTRIBUICAO_SECAO_ESPECIALIZADA", "SECRETARIA_TST_SDC", false);
            }
            if (rito == RitoProcessual.TRABALHISTA_ACAO_RESCISORIA || rito == RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA) {
                return new SuperiorProfile("SDI2_TST", "SECAO_ESPECIALIZADA", "SDI2", "ASSESSORIA_SDI2", "MESA_ADMISSIBILIDADE_TST_SDI2", "SESSAO_COLEGIADA", "MINISTROS_DA_SECAO", "DISTRIBUICAO_SECAO_ESPECIALIZADA", "SECRETARIA_TST_SDI2", false);
            }
            return new SuperiorProfile("TURMA_TST_" + specializationAxis, "TURMA_SUPERIOR", "TST_TURMA", "GABINETE_MINISTRO_TST", "MESA_ADMISSIBILIDADE_TST", "SESSAO_VIRTUAL_OU_PRESENCIAL", "3_MINISTROS_OU_MAIS", "DISTRIBUICAO_TURMA_SUPERIOR", "GABINETE_TST_" + specializationAxis, true);
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return new SuperiorProfile("PLENARIO_TSE", "PLENARIO_SUPERIOR", "TSE_PLENARIO", "ASSESSORIA_PRESIDENCIA_TSE", "MESA_ADMISSIBILIDADE_TSE", "SESSAO_PLENARIA", "7_MINISTROS", "DISTRIBUICAO_PLENARIO_SUPERIOR", "PLENARIO_TSE", false);
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return new SuperiorProfile("PLENARIO_STM", "PLENARIO_SUPERIOR", "STM_PLENARIO", "ASSESSORIA_STM", "MESA_ADMISSIBILIDADE_STM", "SESSAO_PLENARIA", "MINISTROS_MILITARES_E_CIVIS", "DISTRIBUICAO_PLENARIO_SUPERIOR", "PLENARIO_STM", false);
        }
        if (rito != null && (rito.name().contains("HOMOLOGACAO_SENTENCA_ESTRANGEIRA") || rito.name().contains("CARTA_ROGATORIA"))) {
            return new SuperiorProfile("CORTE_ESPECIAL_STJ", "CORTE_ESPECIAL", "STJ_CORTE_ESPECIAL", "ASSESSORIA_CORTE_ESPECIAL", "MESA_ADMISSIBILIDADE_STJ_CORTE_ESPECIAL", "SESSAO_COLEGIADA", "MINISTROS_DA_CORTE_ESPECIAL", "DISTRIBUICAO_CORTE_ESPECIAL", "CORTE_ESPECIAL_STJ", false);
        }
        return new SuperiorProfile("TURMA_STJ_" + specializationAxis, "TURMA_SUPERIOR", "STJ_TURMA", "GABINETE_MINISTRO_STJ", "MESA_ADMISSIBILIDADE_STJ", "SESSAO_VIRTUAL_OU_PRESENCIAL", "5_MINISTROS", "DISTRIBUICAO_TURMA_SUPERIOR", "GABINETE_STJ_" + specializationAxis, true);
    }

    private SecondInstanceProfile resolveSecondInstanceProfile(TipoJustica tipoJustica,
                                                               RitoProcessual rito,
                                                               String specializationAxis,
                                                               TerritorialRoutingProfile territorial) {
        String anchor = sanitize(firstNonBlank(territorial.subsecaoJudiciaria(), territorial.foro(), territorial.comarca(), territorial.cidade(), "BASE"));
        if (rito == RitoProcessual.JUIZADO_ESPECIAL || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA) {
            return new SecondInstanceProfile("TURMA_RECURSAL_JEC", "TURMA_RECURSAL", "JEC_RECURSAL", "GABINETE_JUIZ_RECURSAL", "MESA_ADMISSIBILIDADE_TURMA_RECURSAL_" + anchor, "SESSAO_COLEGIADA", "3_JUIZES", "DISTRIBUICAO_TURMA_RECURSAL", "TURMA_RECURSAL_JEC", true);
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL || rito == RitoProcessual.PREVIDENCIARIO_JEF) {
            return new SecondInstanceProfile("TURMA_RECURSAL_JEF", "TURMA_RECURSAL_FEDERAL", "JEF_RECURSAL", "GABINETE_JUIZ_RECURSAL_FEDERAL", "MESA_ADMISSIBILIDADE_TURMA_RECURSAL_FEDERAL_" + anchor, "SESSAO_COLEGIADA", "3_JUIZES_FEDERAIS", "DISTRIBUICAO_TURMA_RECURSAL", "TURMA_RECURSAL_JEF", true);
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return new SecondInstanceProfile("TURMA_TRT_" + specializationAxis, "TURMA_REGIONAL", "TRT_TURMA", "GABINETE_DESEMBARGADOR_TRABALHO", "MESA_ADMISSIBILIDADE_TRT_" + anchor, "SESSAO_VIRTUAL_OU_PRESENCIAL", "3_DESEMBARGADORES", "DISTRIBUICAO_TURMA_REGIONAL", "GABINETE_TRT_" + specializationAxis, true);
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return new SecondInstanceProfile("PLENARIO_TRE", "PLENARIO_REGIONAL", "TRE_PLENARIO", "ASSESSORIA_TRE", "MESA_ADMISSIBILIDADE_TRE_" + anchor, "SESSAO_PLENARIA", "COLEGIADO_REGIONAL_ELEITORAL", "DISTRIBUICAO_PLENARIO_REGIONAL", "PLENARIO_TRE", false);
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return new SecondInstanceProfile("CAMARA_MILITAR_" + specializationAxis, "CAMARA_MILITAR", "MILITAR_CAMARA", "GABINETE_DESEMBARGADOR_MILITAR", "MESA_ADMISSIBILIDADE_MILITAR_" + anchor, "SESSAO_COLEGIADA", "3_JULGADORES_OU_MAIS", "DISTRIBUICAO_CAMARA_MILITAR", "GABINETE_MILITAR_" + specializationAxis, false);
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            String family = rito != null && rito.isPrevidenciario() ? "PREVIDENCIARIA" : specializationAxis;
            return new SecondInstanceProfile("TURMA_TRF_" + family, "TURMA_REGIONAL_FEDERAL", "TRF_TURMA", "GABINETE_DESEMBARGADOR_FEDERAL", "MESA_ADMISSIBILIDADE_TRF_" + anchor, "SESSAO_VIRTUAL_OU_PRESENCIAL", "3_DESEMBARGADORES_FEDERAIS", "DISTRIBUICAO_TURMA_REGIONAL", "GABINETE_TRF_" + family, true);
        }
        String family = rito != null && rito.isPenal() ? "CRIMINAL" : specializationAxis;
        return new SecondInstanceProfile("CAMARA_TJ_" + family, "CAMARA_ESTADUAL", "TJ_CAMARA", "GABINETE_DESEMBARGADOR", "MESA_ADMISSIBILIDADE_TJ_" + anchor, "SESSAO_VIRTUAL_OU_PRESENCIAL", "3_DESEMBARGADORES", "DISTRIBUICAO_CAMARA_ESTADUAL", "GABINETE_TJ_" + family, true);
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

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private record SuperiorProfile(
            String orgaoFracionario,
            String colegiadoMode,
            String chamberFamily,
            String gabineteMode,
            String admissibilityDesk,
            String sessionMode,
            String panelComposition,
            String allocationStrategyOverride,
            String deskProfileOverride,
            boolean virtualSessionEligible) {
    }

    private record SecondInstanceProfile(
            String orgaoFracionario,
            String colegiadoMode,
            String chamberFamily,
            String gabineteMode,
            String admissibilityDesk,
            String sessionMode,
            String panelComposition,
            String allocationStrategyOverride,
            String deskProfileOverride,
            boolean virtualSessionEligible) {
    }
}
