package com.tcc.pjb.backend.service.secretariat.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class SecretariatOperationalBulkReassignmentServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final SecretariatQueueProjectionService projectionService = mock(SecretariatQueueProjectionService.class);
    private final SecretariatOperationalBulkReassignmentService service =
            new SecretariatOperationalBulkReassignmentService(usuarioRepository, workItemRepository, projectionService);

    private Usuario usuario(Long id, String nome, boolean ativo, String comarca) {
        return Usuario.builder().id(id).nome(nome).tipoUsuario(TipoUsuario.SERVIDOR_FORUM).ativo(ativo).comarca(comarca).build();
    }

    private WorkItem item(Long id, String comarca, Integer prioridade) {
        Processo processo = new Processo();
        processo.setId(100L + id);
        processo.setNumeroProcesso("PROC-" + id);
        return WorkItem.builder().id(id).titulo("Item " + id).assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .comarca(comarca).prioridade(prioridade).processo(processo).build();
    }

    @Test
    void reatribuiCargaParaCandidatoMenosOcupadoNaMesmaComarca() {
        Usuario afastado = usuario(1L, "Servidor Afastado", true, "Fortaleza");
        Usuario candidatoOcupado = usuario(2L, "Candidato Ocupado", true, "Fortaleza");
        Usuario candidatoLivre = usuario(3L, "Candidato Livre", true, "Fortaleza");
        WorkItem itemA = item(10L, "Fortaleza", 3);
        WorkItem itemB = item(11L, "Fortaleza", 3);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(afastado));
        Page<WorkItem> pagina = new PageImpl<>(List.of(itemA, itemB));
        when(workItemRepository.inboxByUser(eq(1L), any())).thenReturn(pagina);
        when(usuarioRepository.findByTipoUsuario(TipoUsuario.SERVIDOR_FORUM)).thenReturn(List.of(afastado, candidatoOcupado, candidatoLivre));
        when(workItemRepository.inboxByUser(eq(2L), any())).thenReturn(new PageImpl<>(List.of(item(50L, "Fortaleza", 3), item(51L, "Fortaleza", 3))));
        when(workItemRepository.inboxByUser(eq(3L), any())).thenReturn(new PageImpl<>(List.of()));

        SecretariatOperationalBulkReassignmentService.BulkReassignmentSnapshot snapshot =
                service.reatribuirCargaPorAfastamento(1L, afastado);

        assertThat(snapshot.totalReatribuido()).isEqualTo(2);
        assertThat(snapshot.semCandidatoDisponivel()).isZero();
        assertThat(snapshot.itens()).extracting("novoResponsavelId").containsOnly(3L);
        verify(workItemRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void naoConsideraCandidatoInativoOuOProprioAfastado() {
        Usuario afastado = usuario(1L, "Servidor Afastado", true, "Fortaleza");
        Usuario candidatoInativo = usuario(2L, "Candidato Inativo", false, "Fortaleza");
        WorkItem itemA = item(10L, "Fortaleza", 3);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(afastado));
        when(workItemRepository.inboxByUser(eq(1L), any())).thenReturn(new PageImpl<>(List.of(itemA)));
        when(usuarioRepository.findByTipoUsuario(TipoUsuario.SERVIDOR_FORUM)).thenReturn(List.of(afastado, candidatoInativo));

        SecretariatOperationalBulkReassignmentService.BulkReassignmentSnapshot snapshot =
                service.reatribuirCargaPorAfastamento(1L, afastado);

        assertThat(snapshot.totalReatribuido()).isZero();
        assertThat(snapshot.semCandidatoDisponivel()).isEqualTo(1);
        verify(workItemRepository, never()).save(any());
    }

    @Test
    void lancaExcecaoQuandoServidorAfastadoNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reatribuirCargaPorAfastamento(99L, usuario(1L, "Ator", true, "Fortaleza")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
