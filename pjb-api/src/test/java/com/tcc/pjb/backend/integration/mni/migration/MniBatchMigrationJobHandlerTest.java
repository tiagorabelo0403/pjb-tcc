package com.tcc.pjb.backend.integration.mni.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.backfill.service.BackfillRunService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import org.junit.jupiter.api.Test;

class MniBatchMigrationJobHandlerTest {

    private final MniBatchMigrationJobHandler handler = new MniBatchMigrationJobHandler(
            mock(MniMigrationBatchService.class), mock(JobRepository.class), mock(BackfillRunService.class));

    @Test
    void tipoDoJobEhMniBatchMigration() {
        assertThat(handler.type()).isEqualTo(JobType.MNI_BATCH_MIGRATION);
    }

    @Test
    void inputDeserializaBatchSizeAfterIdEUntilId() throws Exception {
        String json = "{\"batchSize\":25,\"afterId\":10,\"untilId\":500}";
        MniBatchMigrationJobHandler.Input input = new ObjectMapper().readValue(json, MniBatchMigrationJobHandler.Input.class);

        assertThat(input.batchSize()).isEqualTo(25);
        assertThat(input.afterId()).isEqualTo(10L);
        assertThat(input.untilId()).isEqualTo(500L);
    }

    @Test
    void inputComCamposAusentesDeserializaComoNulos() throws Exception {
        MniBatchMigrationJobHandler.Input input = new ObjectMapper().readValue("{}", MniBatchMigrationJobHandler.Input.class);

        assertThat(input.batchSize()).isNull();
        assertThat(input.afterId()).isNull();
        assertThat(input.untilId()).isNull();
    }
}
