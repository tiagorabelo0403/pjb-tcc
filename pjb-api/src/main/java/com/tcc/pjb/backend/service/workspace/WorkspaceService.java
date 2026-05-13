package com.tcc.pjb.backend.service.workspace;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.persona.PersonaKey;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.catalog.IdeaDto;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.dto.workspace.WorkspaceMeResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.catalog.IdeaCatalogService;
import com.tcc.pjb.backend.service.workitem.WorkItemService;

@Service
public class WorkspaceService {

    private final CurrentUserService currentUserService;
    private final UserPersonaService personaService;
    private final IdeaCatalogService ideaCatalogService;
    private final WorkItemService workItemService;

    public WorkspaceService(CurrentUserService currentUserService,
                            UserPersonaService personaService,
                            IdeaCatalogService ideaCatalogService,
                            WorkItemService workItemService) {
        this.currentUserService = currentUserService;
        this.personaService = personaService;
        this.ideaCatalogService = ideaCatalogService;
        this.workItemService = workItemService;
    }

    public WorkspaceMeResponse me() {
        Usuario u = currentUserService.getRequired();
        UserPersona persona = personaService.getRequiredPersona();
        String role = u.getTipoUsuario() != null ? u.getTipoUsuario().name() : "USER";

        
        List<IdeaDto> ideas = ideaCatalogService.getByRole(persona.personaKey().name());
        if (ideas == null || ideas.isEmpty()) {
            ideas = ideaCatalogService.getByRole(role);
        }

        boolean assessorDelegado = persona.personaKey() == PersonaKey.ASSESSOR;
        boolean juizAuxiliar = persona.personaKey() == PersonaKey.JUIZ_AUXILIAR;

        List<String> quick = juizAuxiliar ? List.of(
                "Analisar processos do gabinete (auxílio)",
                "Minutar votos/decisões para revisão do ministro",
                "Gerar síntese de precedentes",
                "Checar pendências de revisão do ministro"
        ) : assessorDelegado ? List.of(
                "Ver fila do gabinete (delegado)",
                "Minutar atos (escopo DCP)",
                "Revisar minutas em lote",
                "Checar pendências de assinatura do magistrado"
        ) : switch (role) {
            case "JUIZ", "DESEMBARGADOR", "MINISTRO" -> List.of(
                    "Ver fila do gabinete (docket)",
                    "Abrir urgências (saúde / prazo fatal)",
                    "Revisar minutas em lote",
                    "Agenda de audiências do dia"
            );
            case "ADVOGADO" -> List.of(
                    "Central de intimações",
                    "Prazos da semana",
                    "Modelos/peças favoritas",
                    "Solicitar incidente OAB/UF"
            );
            case "SERVIDOR_FORUM", "SERVIDOR" -> List.of(
                    "Fila de tarefas do cartório",
                    "Expedir mandados/intimações",
                    "Pendências por rito",
                    "Gargalos (SLA)"
            );
            case "OFICIAL_JUSTICA", "OFICIAL_JUSTICA_AVALIADOR" -> List.of(
                    "Roteiro do dia",
                    "Registrar diligência",
                    "Pendências devolvidas",
                    "Relatório semanal"
            );
            case "DELEGADO_POLICIA", "DELEGADO_POLICIA_FEDERAL", "AGENTE_POLICIAL", "ESCRIVAO_POLICIAL" -> List.of(
                    "Casos em andamento",
                    "Cadeia de custódia",
                    "Remessas ao MP",
                    "Relatórios"
            );
            case "MEMBRO_MINISTERIO_PUBLICO" -> List.of(
                    "Manifestação pendente",
                    "Prazos",
                    "Audiências",
                    "Casos prioritários"
            );
            case "CONCILIADOR_CEJUSC", "MEDIADOR", "ARBITRO" -> List.of(
                    "Agenda de sessões",
                    "Pautas pendentes",
                    "Termos a registrar",
                    "Pendências de comparecimento"
            );
            case "CIDADAO" -> List.of(
                    "Meus processos",
                    "Linha do tempo",
                    "Documentos pendentes",
                    "Notificações"
            );
            default -> List.of("Inbox", "Processos", "Prazos", "Documentos");
        };

        Page<WorkItemDto> inbox = workItemService.inbox(0, 5);

        return WorkspaceMeResponse.builder()
                .userId(u.getId())
                .nome(u.getNome())
                .email(u.getEmail())
                .tipoUsuario(role)
                .personaKey(persona.personaKey().name())
                .displayPerfil(persona.displayPerfil())
                .tratamento(persona.tratamento())
                .uf(u.getUf())
                .comarca(u.getComarca())
                .ideas(ideas)
                .quickActions(quick)
                .inboxCount(inbox.getTotalElements())
                .inboxPreview(inbox.getContent())
                .build();
    }
}
