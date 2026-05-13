package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class PjbDigitalJusticeUnitPlanner {

    public PjbDigitalJusticeUnitReadiness assess(PjbDigitalJusticeUnitProfile profile) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        if (profile == null || Objects.toString(profile.unitCode(), "").isBlank()) {
            blockers.add("UNIDADE_NAO_IDENTIFICADA");
        }
        if (profile == null || Objects.toString(profile.subjectMatter(), "").isBlank()) {
            blockers.add("MATERIA_NAO_DEFINIDA");
        }
        if (profile == null || profile.territoryCodes() == null || profile.territoryCodes().isEmpty()) {
            blockers.add("ABRANGENCIA_TERRITORIAL_AUSENTE");
        }
        if (profile == null || !profile.remoteFirst()) {
            recommendations.add("ativar operação remota como padrão do núcleo digital");
        }
        if (profile == null || !profile.digitalHearingEnabled()) {
            recommendations.add("habilitar audiência digital integrada ao calendário e intimações");
        }
        if (profile == null || !profile.specializedStaffAllocated()) {
            blockers.add("EQUIPE_ESPECIALIZADA_NAO_ALOCADA");
        }
        double ratio = occupancy(profile);
        if (ratio >= 1.0d) {
            blockers.add("CAPACIDADE_OPERACIONAL_ESGOTADA");
        } else if (ratio >= 0.82d) {
            recommendations.add("revisar distribuição para evitar saturação do núcleo");
        }
        String status = blockers.isEmpty() ? ratio >= 0.82d ? "READY_WITH_LOAD_WARNING" : "READY" : "BLOCKED";
        return new PjbDigitalJusticeUnitReadiness(status, ratio, blockers.isEmpty(), new ArrayList<>(blockers), new ArrayList<>(recommendations));
    }

    public PjbDigitalJusticeUnitReadiness assessJuizadoAdjunto(PjbJuizadoAdjuntoOperationalProfile profile) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        if (profile == null || Objects.toString(profile.unitCode(), "").isBlank()) {
            blockers.add("NUCLEO_ADJUNTO_NAO_IDENTIFICADO");
        }
        if (profile == null || profile.territoryCodes() == null || profile.territoryCodes().isEmpty()) {
            blockers.add("COMARCAS_DE_ABRANGENCIA_AUSENTES");
        }
        if (profile == null || !profile.pjeEnabled()) {
            blockers.add("PJE_NAO_HABILITADO_COMO_SISTEMA_DE_TRAMITACAO");
        }
        if (profile == null || !profile.protocolOptionAvailable()) {
            blockers.add("OPCAO_NO_CADASTRO_DE_PROTOCOLO_NAO_HABILITADA");
        }
        if (profile == null || !profile.forumDirectorateSupportReady()) {
            recommendations.add("habilitar apoio da diretoria do foro para dúvidas e orientação operacional");
        }
        if (profile == null || !profile.publicGuidancePublished()) {
            recommendations.add("publicar orientação clara sobre opção facultativa no momento do protocolo");
        }
        if (profile == null || !profile.ownSecretariatAvailable()) {
            blockers.add("SECRETARIA_PROPRIA_DO_NUCLEO_NAO_DISPONIVEL");
        }
        if (profile == null || !profile.magistratesDesignated()) {
            blockers.add("MAGISTRADOS_DO_NUCLEO_NAO_DESIGNADOS");
        }
        if (profile == null || !profile.digitalHearingEnabled()) {
            recommendations.add("integrar audiência digital ao fluxo dos juizados especiais adjuntos");
        }
        double ratio = profile == null || profile.monthlyCapacity() <= 0
                ? 1.0d
                : BigDecimal.valueOf((double) Math.max(0, profile.activeCaseLoad()) / profile.monthlyCapacity())
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
        if (ratio >= 1.0d) {
            blockers.add("CAPACIDADE_DO_NUCLEO_ADJUNTO_ESGOTADA");
        } else if (ratio >= 0.82d) {
            recommendations.add("acompanhar saturação antes de ampliar novos ingressos");
        }
        String status = blockers.isEmpty() ? ratio >= 0.82d ? "READY_WITH_LOAD_WARNING" : "READY" : "BLOCKED";
        return new PjbDigitalJusticeUnitReadiness(status, ratio, blockers.isEmpty(), new ArrayList<>(blockers), new ArrayList<>(recommendations));
    }

    private double occupancy(PjbDigitalJusticeUnitProfile profile) {
        if (profile == null || profile.monthlyCapacity() <= 0) {
            return 1.0d;
        }
        return BigDecimal.valueOf((double) Math.max(0, profile.activeCaseLoad()) / profile.monthlyCapacity())
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
