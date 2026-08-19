package com.tcc.pjb.backend.service.secretariat.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecretariatOperationalRedistributionServiceTest {

    private final SecretariatOperationalAssignmentService assignmentService = mock(SecretariatOperationalAssignmentService.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SecretariatQueueProjectionService projectionService = mock(SecretariatQueueProjectionService.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final SecretariatOperationalRedistributionService service =
            new SecretariatOperationalRedistributionService(assignmentService, workItemRepository, usuarioRepository, projectionService, authorizationService);

    private Processo processo() {
        return Processo.builder().id(10L).numeroProcesso("0001234-56.2026.8.06.0001").uf("CE").comarca("Fortaleza").build();
    }

    private Usuario usuario(Long id, String nome, TipoUsuario tipoUsuario) {
        return Usuario.builder().id(id).nome(nome).tipoUsuario(tipoUsuario).build();
    }

    private WorkItem item(Long id, Usuario assignedUser) {
        return WorkItem.builder().id(id).titulo("Item " + id).assignedUser(assignedUser).build();
    }

    private SecretariatOperationalRoutingProfile routing() {
        return new SecretariatOperationalRoutingProfile(
                "ROTA-SECRETARIA",
                "ESTADUAL",
                "TJCE",
                "1G",
                "COMUM",
                "FAZENDA",
                "EXECUCAO",
                "SECRETARIA_FAZENDA",
                "RCV",
                "INBOX_RCV",
                "SAN",
                "INBOX_SAN",
                "AUD",
                "INBOX_AUD",
                "EXEC",
                "INBOX_EXEC",
                "AUD-01",
                "/TJCE/QUX/SECRETARIA",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(6),
                30,
                true,
                true,
                true,
                true,
                List.of(),
                List.of(),
                new SecretariatSpecializationResolver().resolve(
                        "TJCE",
                        "PRIMEIRO_GRAU",
                        "JUSTICA_ESTADUAL",
                        "FAZENDA",
                        "SECRETARIA_FAZENDA",
                        "INBOX_RCV",
                        "SAN",
                        "AUD",
                        "EXEC",
                        "/TJCE/QUX/SECRETARIA",
                        Map.of("laneAxis", "FAZENDA", "forumAxis", "FORO_COMUM", "unitDescriptor", "1a Vara da Fazenda")
                ),
                JudicialScaleProfile.VARA_1G,
                Map.of()
        );
    }

    private void stubAssignment(Processo processo, SecretariatOperationalRoutingProfile routing, Usuario current, Usuario outro, WorkItem item) {
        SecretariatOperationalAssignmentService.CandidateLoad candidatoAtual =
                new SecretariatOperationalAssignmentService.CandidateLoad(current, current.getId(), current.getNome(), "CELL", 0, 0, 0, 100, List.of());
        SecretariatOperationalAssignmentService.CandidateLoad candidatoOutro =
                new SecretariatOperationalAssignmentService.CandidateLoad(outro, outro.getId(), outro.getNome(), "CELL", 0, 0, 0, 200, List.of());
        SecretariatOperationalAssignmentService.AssignmentSnapshot assignmentSnapshot =
                new SecretariatOperationalAssignmentService.AssignmentSnapshot("RECEBIMENTO", "CELL", List.of(item.getId()),
                        null, null, List.of(candidatoAtual, candidatoOutro), List.of(), Map.of());
        when(assignmentService.avaliar(eq(processo), eq(routing), eq("RECEBIMENTO"))).thenReturn(assignmentSnapshot);
        when(workItemRepository.findAllById(eq(List.of(item.getId())))).thenReturn(List.of(item));
    }

    @Test
    void servidorComFuncaoDistribuirAtivaRedistribuiComSucesso() {
        Processo processo = processo();
        SecretariatOperationalRoutingProfile routing = routing();
        Usuario atual = usuario(1L, "Servidor Atual", TipoUsuario.SERVIDOR_FORUM);
        Usuario outro = usuario(2L, "Servidor Recomendado", TipoUsuario.SERVIDOR_FORUM);
        Usuario actor = usuario(5L, "Diretor de Secretaria", TipoUsuario.SERVIDOR_FORUM);
        WorkItem item = item(10L, atual);
        stubAssignment(processo, routing, atual, outro, item);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outro));

        SecretariatOperationalRedistributionService.RedistributionSnapshot resultado =
                service.redistribuir(processo, actor, routing, "RECEBIMENTO");

        assertThat(resultado.current()).isNotNull();
        assertThat(resultado.current().getId()).isEqualTo(2L);
        verify(authorizationService).requireFuncaoServidorCapability(eq(processo), eq(AcaoProcessualServidor.DISTRIBUIR));
        verify(workItemRepository).saveAll(List.of(item));
        verify(projectionService).upsert(eq(item), anyInt(), any());
    }

    @Test
    void servidorSemFuncaoDistribuirAtivaLancaAccessDenied() {
        Processo processo = processo();
        SecretariatOperationalRoutingProfile routing = routing();
        Usuario actor = usuario(5L, "Diretor de Secretaria", TipoUsuario.SERVIDOR_FORUM);
        doThrow(new AccessDeniedPjbException("Acesso negado à ação processual do servidor"))
                .when(authorizationService).requireFuncaoServidorCapability(eq(processo), eq(AcaoProcessualServidor.DISTRIBUIR));

        assertThrows(AccessDeniedPjbException.class, () -> service.redistribuir(processo, actor, routing, "RECEBIMENTO"));

        verify(assignmentService, never()).avaliar(any(), any(), any());
        verify(workItemRepository, never()).saveAll(any());
    }

    @Test
    void magistradoSemFuncaoServidorRedistribuiSemSerBloqueadoPelaNovaChecagem() {
        Processo processo = processo();
        SecretariatOperationalRoutingProfile routing = routing();
        Usuario atual = usuario(1L, "Servidor Atual", TipoUsuario.SERVIDOR_FORUM);
        Usuario outro = usuario(2L, "Servidor Recomendado", TipoUsuario.SERVIDOR_FORUM);
        Usuario actor = usuario(6L, "Juiz Titular", TipoUsuario.JUIZ);
        WorkItem item = item(10L, atual);
        stubAssignment(processo, routing, atual, outro, item);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outro));

        SecretariatOperationalRedistributionService.RedistributionSnapshot resultado =
                service.redistribuir(processo, actor, routing, "RECEBIMENTO");

        assertThat(resultado.current()).isNotNull();
        assertThat(resultado.current().getId()).isEqualTo(2L);
        verify(authorizationService, never()).requireFuncaoServidorCapability(any(), any());
        verify(workItemRepository).saveAll(List.of(item));
    }
}
