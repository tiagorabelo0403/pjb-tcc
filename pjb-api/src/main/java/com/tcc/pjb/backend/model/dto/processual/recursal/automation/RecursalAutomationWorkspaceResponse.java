package com.tcc.pjb.backend.model.dto.processual.recursal.automation;

import java.util.List;

public record RecursalAutomationWorkspaceResponse(
        String rotaPrioritaria,
        String nomenclaturaAtiva,
        List<String> verbosOperacionais,
        boolean poderRecorrerBloqueado,
        String motivoBloqueioPoderRecorrer,
        List<RecursalAutomationWorkspaceTrackView> trilhas) {
}
