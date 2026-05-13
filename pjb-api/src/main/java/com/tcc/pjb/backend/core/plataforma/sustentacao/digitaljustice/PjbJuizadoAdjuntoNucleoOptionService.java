package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PjbJuizadoAdjuntoNucleoOptionService {

    private static final String LANE_NUCLEO = "NUCLEO_JUSTICA_4_0_JUIZADOS_ESPECIAIS_ADJUNTOS";
    private static final String LANE_ORIGEM = "VARA_COMUM_ORIGEM";
    private static final List<String> LEGAL_BASIS = List.of(
            "Portaria TJCE nº 73/2026",
            "Resolução do Tribunal Pleno TJCE nº 13/2024",
            "Orientação Normativa CGJE nº 05/2025",
            "Resolução CNJ nº 385/2021",
            "Resolução CNJ nº 398/2021"
    );

    private final PjbJuizadoAdjuntoNucleoStageCatalog stageCatalog;

    public PjbJuizadoAdjuntoNucleoOptionService() {
        this(new PjbJuizadoAdjuntoNucleoStageCatalog());
    }

    public PjbJuizadoAdjuntoNucleoOptionService(PjbJuizadoAdjuntoNucleoStageCatalog stageCatalog) {
        this.stageCatalog = Objects.requireNonNull(stageCatalog);
    }

    public PjbJuizadoAdjuntoNucleoOptionDecision decide(PjbJuizadoAdjuntoNucleoOptionRequest request) {
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Optional<PjbJuizadoAdjuntoNucleoStage> stage = stageCatalog.stageForCourtUnit(request == null ? null : request.courtUnit());
        if (request == null) {
            reasons.add("REQUERIMENTO_AUSENTE");
            return decision("BLOCKED", LANE_ORIGEM, false, true, false, null, reasons, warnings);
        }
        if (!isTjce(request.tribunalCode())) {
            reasons.add("TRIBUNAL_FORA_DO_PERFIL_TJCE");
        }
        if (!isJuizadoEspecialCivel(request.subjectMatter())) {
            reasons.add("MATERIA_FORA_DOS_JUIZADOS_ESPECIAIS_CIVEIS");
        }
        if (request.hasAutonomousJuizadoUnit()) {
            reasons.add("COMARCA_COM_UNIDADE_AUTONOMA_DE_JUIZADO");
        }
        if (stage.isEmpty()) {
            reasons.add("UNIDADE_NAO_CONTEMPLADA_NO_CRONOGRAMA");
        }
        if (request.protocolDate() == null) {
            reasons.add("DATA_DE_PROTOCOLO_AUSENTE");
        } else if (stage.map(value -> request.protocolDate().isBefore(value.newCaseStartDate())).orElse(false)) {
            reasons.add("PROTOCOLO_ANTERIOR_AO_INICIO_DO_FUNCIONAMENTO");
        }
        if (request.distributionCompleted()) {
            warnings.add("OPCAO_IMUTAVEL_APOS_DISTRIBUICAO");
        }
        if (request.optionOnlyMentionedInInitialPetition() && !request.optionSelectedInPjeRegistration()) {
            reasons.add("MENCAO_NA_PETICAO_INICIAL_NAO_SUBSTITUI_OPCAO_NO_CADASTRO");
        }
        if (!request.optionSelectedInPjeRegistration()) {
            warnings.add("SEM_OPCAO_NO_CADASTRO_PROCESSO_SEGUE_NA_VARA_COMUM");
            warnings.add("NAO_HA_REDISTRIBUICAO_AUTOMATICA_PARA_O_NUCLEO");
        }
        boolean eligible = reasons.isEmpty() && request.optionSelectedInPjeRegistration();
        String status = eligible ? "ROUTE_TO_NUCLEO_4_0" : reasons.isEmpty() ? "STAY_IN_ORIGIN_BY_AUTHOR_CHOICE" : "BLOCKED";
        String targetLane = eligible ? LANE_NUCLEO : LANE_ORIGEM;
        return decision(status, targetLane, eligible, true, request.distributionCompleted(), stage.map(PjbJuizadoAdjuntoNucleoStage::stageCode).orElse(null), reasons, warnings);
    }

    public PjbJuizadoAdjuntoNucleoOptionDecision moradaNovaPreview(LocalDate protocolDate,
                                                                   boolean optionSelectedInPjeRegistration,
                                                                   boolean optionOnlyMentionedInInitialPetition,
                                                                   boolean distributionCompleted) {
        return decide(new PjbJuizadoAdjuntoNucleoOptionRequest(
                "TJCE",
                "Morada Nova",
                "1ª e 2ª Vara Cível da Comarca de Morada Nova",
                "Juizado Especial Cível",
                protocolDate,
                true,
                optionSelectedInPjeRegistration,
                optionOnlyMentionedInInitialPetition,
                distributionCompleted,
                false));
    }

    private boolean isTjce(String tribunalCode) {
        return PjbJuizadoAdjuntoText.normalize(tribunalCode).equals("TJCE");
    }

    private boolean isJuizadoEspecialCivel(String subjectMatter) {
        String normalized = PjbJuizadoAdjuntoText.normalize(subjectMatter);
        return normalized.contains("JUIZADO") && normalized.contains("CIVEL");
    }

    private PjbJuizadoAdjuntoNucleoOptionDecision decision(String status,
                                                           String targetLane,
                                                           boolean eligible,
                                                           boolean authorChoiceRespected,
                                                           boolean immutableAfterDistribution,
                                                           String stageCode,
                                                           List<String> reasons,
                                                           List<String> warnings) {
        return decision(status, targetLane, eligible, authorChoiceRespected, immutableAfterDistribution, stageCode, reasons, warnings, LEGAL_BASIS);
    }

    private PjbJuizadoAdjuntoNucleoOptionDecision decision(String status,
                                                           String targetLane,
                                                           boolean eligible,
                                                           boolean authorChoiceRespected,
                                                           boolean immutableAfterDistribution,
                                                           String stageCode,
                                                           List<String> reasons,
                                                           List<String> warnings,
                                                           List<String> legalBasis) {
        return new PjbJuizadoAdjuntoNucleoOptionDecision(
                status,
                targetLane,
                eligible,
                authorChoiceRespected,
                immutableAfterDistribution,
                stageCode,
                List.copyOf(reasons),
                List.copyOf(warnings),
                legalBasis);
    }
}
