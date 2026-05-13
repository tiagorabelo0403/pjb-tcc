package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpExecutionTranscript(
        String transcriptId,
        String planId,
        String captureMode,
        boolean replayReady,
        boolean approvalLinked,
        List<String> pinnedServerIds,
        List<String> pinnedSkillIds,
        List<String> pinnedToolExampleIds,
        List<String> checkpoints,
        List<String> riskFlags,
        List<String> adaptationHints
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("transcriptId", transcriptId);
        out.put("planId", planId);
        out.put("captureMode", captureMode);
        out.put("replayReady", replayReady);
        out.put("approvalLinked", approvalLinked);
        out.put("pinnedServerIds", pinnedServerIds == null ? List.of() : List.copyOf(pinnedServerIds));
        out.put("pinnedSkillIds", pinnedSkillIds == null ? List.of() : List.copyOf(pinnedSkillIds));
        out.put("pinnedToolExampleIds", pinnedToolExampleIds == null ? List.of() : List.copyOf(pinnedToolExampleIds));
        out.put("checkpoints", checkpoints == null ? List.of() : List.copyOf(checkpoints));
        out.put("riskFlags", riskFlags == null ? List.of() : List.copyOf(riskFlags));
        out.put("adaptationHints", adaptationHints == null ? List.of() : List.copyOf(adaptationHints));
        return Collections.unmodifiableMap(out);
    }
}
