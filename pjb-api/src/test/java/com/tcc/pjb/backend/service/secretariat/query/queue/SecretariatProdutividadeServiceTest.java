package com.tcc.pjb.backend.service.secretariat.query.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretariatProdutividadeServiceTest {

    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final SecretariatInstitutionalVisibilityService visibilityService = mock(SecretariatInstitutionalVisibilityService.class);
    private final SecretariatProdutividadeService service = new SecretariatProdutividadeService(workItemRepository, visibilityService);

    private WorkItem concluido(Long servidorId, String nome, Instant createdAt, Instant updatedAt) {
        Usuario servidor = new Usuario();
        servidor.setId(servidorId);
        servidor.setNome(nome);
        WorkItem item = WorkItem.builder()
                .status(WorkItemStatus.CONCLUIDO)
                .assignedUser(servidor)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        return item;
    }

    @Test
    void ranqueiaServidoresPorTotalConcluidoComDuracaoMediaEmHoras() {
        when(visibilityService.requireInboxAccess("inbox.secretaria")).thenReturn("inbox.secretaria");
        Instant agora = Instant.now();
        List<WorkItem> concluidos = List.of(
                concluido(1L, "Ana", agora.minus(10, ChronoUnit.DAYS), agora.minus(10, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS)),
                concluido(1L, "Ana", agora.minus(9, ChronoUnit.DAYS), agora.minus(9, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS)),
                concluido(2L, "Bruno", agora.minus(5, ChronoUnit.DAYS), agora.minus(5, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
        );
        when(workItemRepository.findConcluidosPorInboxAposData(eq("inbox.secretaria"), any(), any())).thenReturn(concluidos);

        SecretariatProdutividadePainelResponse resultado = service.painelPorInbox("inbox.secretaria", 30);

        assertThat(resultado.totalConcluidos()).isEqualTo(3);
        assertThat(resultado.ranking()).hasSize(2);
        assertThat(resultado.ranking().get(0).servidorId()).isEqualTo(1L);
        assertThat(resultado.ranking().get(0).totalConcluidos()).isEqualTo(2);
        assertThat(resultado.ranking().get(0).duracaoMediaHoras()).isEqualTo(3.0);
        assertThat(resultado.ranking().get(1).servidorId()).isEqualTo(2L);
        assertThat(resultado.ranking().get(1).totalConcluidos()).isEqualTo(1);
        assertThat(resultado.ranking().get(1).duracaoMediaHoras()).isEqualTo(1.0);
    }

    @Test
    void retornaPainelVazioQuandoInboxNaoTemConclusaoNoPeriodo() {
        when(visibilityService.requireInboxAccess("inbox.vazia")).thenReturn("inbox.vazia");
        when(workItemRepository.findConcluidosPorInboxAposData(eq("inbox.vazia"), any(), any())).thenReturn(List.of());

        SecretariatProdutividadePainelResponse resultado = service.painelPorInbox("inbox.vazia", 30);

        assertThat(resultado.totalConcluidos()).isZero();
        assertThat(resultado.ranking()).isEmpty();
    }

    @Test
    void ignoraItemSemUsuarioAtribuido() {
        when(visibilityService.requireInboxAccess("inbox.secretaria")).thenReturn("inbox.secretaria");
        WorkItem semUsuario = WorkItem.builder()
                .status(WorkItemStatus.CONCLUIDO)
                .assignedUser(null)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();
        when(workItemRepository.findConcluidosPorInboxAposData(eq("inbox.secretaria"), any(), any())).thenReturn(List.of(semUsuario));

        SecretariatProdutividadePainelResponse resultado = service.painelPorInbox("inbox.secretaria", 30);

        assertThat(resultado.totalConcluidos()).isEqualTo(1);
        assertThat(resultado.ranking()).isEmpty();
    }
}
