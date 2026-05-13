package com.tcc.pjb.backend.model.dto.workspace;

import java.util.List;
import com.tcc.pjb.backend.model.dto.catalog.IdeaDto;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMeResponse {

    private Long userId;
    private String nome;
    private String email;
    private String tipoUsuario;
    
    private String personaKey;
    
    private String displayPerfil;

    
    private String tratamento;
    private String uf;
    private String comarca;

    
    private List<IdeaDto> ideas;

    
    private List<String> quickActions;

    
    private Long inboxCount;
    private List<WorkItemDto> inboxPreview;
}
