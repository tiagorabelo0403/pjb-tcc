package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PjbSubstituicaoGateSnapshot(NationalCompetenceMatrix tribunal,
                                          int programaScore,
                                          int gateScore,
                                          boolean gateAprovado,
                                          boolean rollbackReversivel,
                                          int tribunalReady,
                                          int productionReady,
                                          int healthySystems,
                                          int blockedSystems,
                                          int cryptoBlocked,
                                          int certificateReady,
                                          List<String> blockers) {

    public boolean blockedFor(PjbSubstituicaoExecucaoAcao acao) {
        return switch (acao) {
            case HOMOLOGAR_TRIBUNAL -> tribunalReady == 0 || blockedSystems > 0 || cryptoBlocked > 0 || gateScore < 68;
            case INICIAR_MIGRACAO_SOMBRA -> !gateAprovado || tribunalReady == 0 || gateScore < 74;
            case SINCRONIZAR_COMUNICACOES_NACIONAIS -> healthySystems == 0 || blockedSystems > 1 || gateScore < 70;
            case CONFIRMAR_CUTOVER -> !gateAprovado || blockedSystems > 0 || cryptoBlocked > 0 || gateScore < 82;
            case ACIONAR_ROLLBACK -> !rollbackReversivel;
        };
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(resultadoBase());
    }

    public Map<String, Object> probeMap() {
        return Map.of(
                "tribunalReady", tribunalReady,
                "productionReady", productionReady,
                "healthySystems", healthySystems,
                "certificateReady", certificateReady
        );
    }

    public Map<String, Object> communicationMap() {
        return Map.of(
                "healthySystems", healthySystems,
                "blockedSystems", blockedSystems,
                "cryptoBlocked", cryptoBlocked,
                "tribunalCodigo", tribunal.codigo()
        );
    }

    public Map<String, Object> cutoverMap() {
        return Map.of(
                "programaScore", programaScore,
                "gateScore", gateScore,
                "gateAprovado", gateAprovado,
                "rollbackReversivel", rollbackReversivel
        );
    }

    public Map<String, Object> rollbackMap() {
        return Map.of(
                "rollbackReversivel", rollbackReversivel,
                "tribunalCodigo", tribunal.codigo(),
                "fallback", tribunal.sistemaJudicialFallback().name(),
                "connectorPreferido", tribunal.connectorPreferido().name()
        );
    }

    public Map<String, Object> resultadoBase() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tribunalCodigo", tribunal.codigo());
        out.put("tribunalNome", tribunal.nome());
        out.put("ramo", tribunal.ramo().name());
        out.put("programaScore", programaScore);
        out.put("gateScore", gateScore);
        out.put("gateAprovado", gateAprovado);
        out.put("rollbackReversivel", rollbackReversivel);
        out.put("tribunalReady", tribunalReady);
        out.put("productionReady", productionReady);
        out.put("healthySystems", healthySystems);
        out.put("blockedSystems", blockedSystems);
        out.put("cryptoBlocked", cryptoBlocked);
        out.put("certificateReady", certificateReady);
        out.put("bloqueadores", blockers);
        return java.util.Collections.unmodifiableMap(out);
    }

    static PjbSubstituicaoGateSnapshot of(String tribunalCodigo,
                                          PjbSubstituicaoNacionalProgramaAggregate programa,
                                          com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService commandCenterService) {
        com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix tribunal = com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.porCodigo(tribunalCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Tribunal não mapeado para substituição nacional: " + tribunalCodigo));
        java.time.Duration horizon = java.time.Duration.ofHours(12);
        com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport report = commandCenterService == null ? null : commandCenterService.tribunalReport(tribunal.codigo(), horizon);
        int tribunalReady = report != null && report.controlPlane() != null ? report.controlPlane().tribunalReadySystems().size() : 0;
        int productionReady = report != null && report.controlPlane() != null ? report.controlPlane().productionReadySystems().size() : 0;
        int healthySystems = report != null && report.observability() != null ? report.observability().healthySystems() : 0;
        int blockedSystems = report != null && report.observability() != null ? report.observability().blockedSystems() : 0;
        int cryptoBlocked = report != null && report.cryptography() != null ? report.cryptography().blockedCount() : 0;
        int certificateReady = report != null && report.cryptography() != null ? report.cryptography().certificateReadyCount() : 0;
        int gateScore = Math.max(0, Math.min(100, (int) Math.round(programa.scoreGeral() * 0.45d
                + tribunalReady * 8d
                + productionReady * 7d
                + healthySystems * 5d
                + certificateReady * 4d
                - blockedSystems * 12d
                - cryptoBlocked * 15d)));
        java.util.LinkedHashSet<String> blockers = new java.util.LinkedHashSet<>();
        if (programa.scoreGeral() < 70) blockers.add("Programa nacional abaixo da faixa mínima");
        if (tribunalReady == 0) blockers.add("Tribunal sem connector tribunal-ready");
        if (productionReady == 0) blockers.add("Tribunal sem connector production-ready");
        if (healthySystems == 0) blockers.add("Sem sistemas saudáveis na janela operacional");
        if (cryptoBlocked > 0) blockers.add("Bloqueio criptográfico ativo");
        boolean gateAprovado = blockers.isEmpty() && gateScore >= 70;
        boolean rollbackReversivel = productionReady > 0 || tribunal.sistemaJudicialFallback() != null;
        return new PjbSubstituicaoGateSnapshot(tribunal, programa.scoreGeral(), gateScore, gateAprovado, rollbackReversivel, tribunalReady, productionReady, healthySystems, blockedSystems, cryptoBlocked, certificateReady, java.util.List.copyOf(blockers));
    }

}
