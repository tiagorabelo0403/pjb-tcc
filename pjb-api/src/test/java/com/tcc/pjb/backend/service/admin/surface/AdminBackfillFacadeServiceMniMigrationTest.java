package com.tcc.pjb.backend.service.admin.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRun;
import com.tcc.pjb.backend.core.backfill.service.BackfillRunService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.mni.migration.MniMigrationBatchItem;
import com.tcc.pjb.backend.integration.mni.migration.MniMigrationBatchService;
import com.tcc.pjb.backend.integration.mni.migration.MniMigrationItemStatus;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillMniMigrationRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationEnqueueRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationItemRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminBackfillFacadeServiceMniMigrationTest {

    private JobCommandService jobCommandService;
    private JobRepository jobRepository;
    private BackfillRunService backfillRunService;
    private MniMigrationBatchService mniMigrationBatchService;
    private AdminBackfillFacadeService facade;

    @BeforeEach
    void setUp() {
        jobCommandService = mock(JobCommandService.class);
        jobRepository = mock(JobRepository.class);
        backfillRunService = mock(BackfillRunService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        mniMigrationBatchService = mock(MniMigrationBatchService.class);
        when(currentUserService.currentUserIdOrZero()).thenReturn(7L);
        facade = new AdminBackfillFacadeService(jobCommandService, jobRepository, backfillRunService,
                currentUserService, new ObjectMapper(), mniMigrationBatchService);
    }

    @Test
    void enqueueMniMigrationItensDelegaCadaItemAoServico() {
        when(mniMigrationBatchService.enfileirar("TJCE", "CARTA_PRECATORIA", "<mni-1/>")).thenReturn(1L);
        when(mniMigrationBatchService.enfileirar("TJSP", "COOPERACAO", "<mni-2/>")).thenReturn(2L);
        var request = new AdminMniMigrationEnqueueRequest(List.of(
                new AdminMniMigrationItemRequest("TJCE", "CARTA_PRECATORIA", "<mni-1/>"),
                new AdminMniMigrationItemRequest("TJSP", "COOPERACAO", "<mni-2/>")));

        var response = facade.enqueueMniMigrationItens(request);

        assertThat(response.itemIds()).containsExactly(1L, 2L);
    }

    @Test
    void kickoffMniMigrationCriaJobERegistraBackfillRun() {
        UUID jobId = UUID.randomUUID();
        when(jobCommandService.createIdempotent(eq(JobType.MNI_BATCH_MIGRATION), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new JobCommandService.JobCreateResult(jobId, false, false));

        var response = facade.kickoffMniMigration(new AdminBackfillMniMigrationRequest(20, 0L, null, null, null, null), null, "req-1");

        assertThat(response.jobId()).isEqualTo(jobId);
        verify(backfillRunService).upsertKickoff(eq(jobId), eq("MNI_BATCH_MIGRATION"), any(), any(), eq(20), eq(false), eq(0L), isNull());
    }

    @Test
    void mniMigrationStatusRetornaVazioQuandoNaoHaBackfillRun() {
        UUID jobId = UUID.randomUUID();
        when(backfillRunService.findById(jobId)).thenReturn(Optional.empty());

        assertThat(facade.mniMigrationStatus(jobId, null)).isEmpty();
    }

    @Test
    void mniMigrationStatusMontaRespostaAPartirDoBackfillRunEDoJob() {
        UUID jobId = UUID.randomUUID();
        BackfillRun run = new BackfillRun(jobId, "MNI_BATCH_MIGRATION", "mni:migracao:lote", "7", 20, false, 0L, null);
        run.addBatch(5, 4, 1, 5);
        when(backfillRunService.findById(jobId)).thenReturn(Optional.of(run));
        Job job = mock(Job.class);
        when(job.getStatus()).thenReturn(null);
        when(job.getProgressCurrent()).thenReturn(5L);
        when(job.getProgressTotal()).thenReturn(10L);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        var status = facade.mniMigrationStatus(jobId, null);

        assertThat(status).isPresent();
        assertThat(status.get().processed()).isEqualTo(5L);
        assertThat(status.get().updated()).isEqualTo(4L);
        assertThat(status.get().duplicates()).isEqualTo(1L);
    }

    @Test
    void mniMigrationFalhasMapeiaItensParaDto() {
        MniMigrationBatchItem falha = MniMigrationBatchItem.builder()
                .id(3L).tribunalOrigem("TJCE").motivo("CARTA_PRECATORIA")
                .status(MniMigrationItemStatus.FALHOU).erro("xml invalido").build();
        when(mniMigrationBatchService.listarFalhas()).thenReturn(List.of(falha));

        var falhas = facade.mniMigrationFalhas();

        assertThat(falhas).hasSize(1);
        assertThat(falhas.get(0).id()).isEqualTo(3L);
        assertThat(falhas.get(0).erro()).isEqualTo("xml invalido");
    }
}
