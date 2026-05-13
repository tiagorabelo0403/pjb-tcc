package com.tcc.pjb.backend.service.rito.diagnostics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RitoPackCoverageDto {

    private Instant generatedAt;
    private int totalEnumRitos;
    private int totalPackDefinitions;
    private List<String> missingInPack;
    private List<String> extraInPack;
    private Map<String, RitoStageSummary> stageSummaries;
    private List<String> issues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RitoStageSummary {
        private String rito;
        private String title;
        private String ramoSugerido;
        private int stageCount;
        private int totalWorkItems;
        private int requiredPartyRoles;
        private int requiredDocuments;
        private boolean externalParticipation;
        private List<String> fases;
        private boolean hasConhecimento;
        private boolean hasExecucao;
        private boolean hasRecursal;
    }
}
