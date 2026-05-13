package com.tcc.pjb.backend.service.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelMaterializationTrail;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelProjection;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelMaterializationTrailRepository;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelProjectionRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PjbProcessualReadModelPersistenceServiceTest {

    @Test
    void shouldMaterializeWithoutNullExplosionAndIncrementVersion() {
        ProcessualReadModelProjectionRepository projectionRepository = Mockito.mock(ProcessualReadModelProjectionRepository.class);
        ProcessualReadModelMaterializationTrailRepository trailRepository = Mockito.mock(ProcessualReadModelMaterializationTrailRepository.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelProjection existing = new ProcessualReadModelProjection();
        existing.setDomain("PROCESSO_TIMELINE_HOT");
        existing.setMaterializationKey("PROCESSO_TIMELINE_HOT|123");
        existing.setProjectionVersion(2L);
        when(projectionRepository.findByDomainIgnoreCaseAndMaterializationKeyIgnoreCase("PROCESSO_TIMELINE_HOT", "PROCESSO_TIMELINE_HOT|123")).thenReturn(Optional.of(existing));
        when(projectionRepository.save(any(ProcessualReadModelProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trailRepository.save(any(ProcessualReadModelMaterializationTrail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PjbProcessualReadModelPersistenceService service = new PjbProcessualReadModelPersistenceService(projectionRepository, trailRepository, properties, new ObjectMapper());
        PjbProcessualReadModelPersistenceService.MaterializationResult result = service.materialize(
                "processo_timeline_hot",
                "movimentacao.registrada.v1",
                "Processo",
                "123",
                "OUTBOX",
                Map.of("numero", "123", "tribunalCode", "TJCE")
        );

        assertThat(result.version()).isEqualTo(3L);
        assertThat(result.tribunalCode()).isEqualTo("TJCE");
        assertThat(result.status()).isEqualTo("MATERIALIZED");
    }

    @Test
    void shouldReturnDisabledWhenPersistenceIsOff() {
        ProcessualReadModelProjectionRepository projectionRepository = Mockito.mock(ProcessualReadModelProjectionRepository.class);
        ProcessualReadModelMaterializationTrailRepository trailRepository = Mockito.mock(ProcessualReadModelMaterializationTrailRepository.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getProcessualReadModels().setPersistenceEnabled(false);

        PjbProcessualReadModelPersistenceService service = new PjbProcessualReadModelPersistenceService(projectionRepository, trailRepository, properties, new ObjectMapper());
        PjbProcessualReadModelPersistenceService.MaterializationResult result = service.materialize("x", null, null, null, null, null);

        assertThat(result.status()).isEqualTo("DISABLED");
        assertThat(result.materializationKey()).isNull();
    }
}
