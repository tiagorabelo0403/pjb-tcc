package com.tcc.pjb.backend.model.dto.workspace.fila;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFilaAudience;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFilaKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFilaResponse {
    private UUID id;
    private String nome;
    private String descricao;
    private WorkspaceFilaKind kind;
    private boolean sistema;
    private WorkspaceFilaAudience audience;
    private Integer orderIndex;
    private Long count;
    private String descriptor;
    private String operationalMode;
    private String scope;
    private Integer autoRefreshSeconds;
    private String sortHint;
    private String workloadBand;
    private String assistantDesk;
    private String escalationDesk;
    private String coordinationChannel;
    private boolean redistributionEligible;
    private boolean audienceSensitive;
    private List<String> labels;
    private Map<String, Object> metadata;
}
