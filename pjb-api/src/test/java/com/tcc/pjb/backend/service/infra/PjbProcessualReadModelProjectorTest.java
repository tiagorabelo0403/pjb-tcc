
package com.tcc.pjb.backend.service.infra;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelopeService;
import com.tcc.pjb.backend.service.outbox.OutboxGenericDispatchedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class PjbProcessualReadModelProjectorTest {

    @Test
    void shouldRegisterFreshnessAndInvalidatePublicTimelineCache() {
        PjbProcessualReadModelMeshService meshService = new PjbProcessualReadModelMeshService(new PjbDataSourceRoutingProperties());
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("public_timeline", "timeline_processo", "peticionamento_workspace", "audiencia_agenda");
        cacheManager.getCache("public_timeline").put("123", "valor");
        ProcessoSigiloRlsEnvelopeService envelopeService = mock(ProcessoSigiloRlsEnvelopeService.class);
        when(envelopeService.materialize(123L, "OUTBOX")).thenReturn(new ProcessoSigiloRlsEnvelope(
                123L,
                "processo",
                "SIGILO_N1",
                "SIGILO_N1",
                "FORO-1",
                "TJCE",
                "PROC_SIGILO|TJCE|FORO-1|SIGILO_N1",
                true,
                true,
                false,
                false,
                true,
                java.util.List.of("PROCESSUAL"),
                java.util.List.of(),
                null,
                null
        ));
        PjbProcessoSigiloRlsEntryPointSupport sigiloSupport = new PjbProcessoSigiloRlsEntryPointSupport(
                envelopeService,
                new com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext()
        );
        PjbProcessualReadModelProjector projector = new PjbProcessualReadModelProjector(
                meshService,
                cacheManager,
                new ObjectMapper(),
                new DefaultListableBeanFactory().getBeanProvider(PjbProcessualReadModelPersistenceService.class),
                sigiloSupport
        );

        projector.onOutboxDispatched(new OutboxGenericDispatchedEvent(
                "movimentacao.registrada.v1",
                "processo:123",
                "{\"numero\":\"123\",\"processoId\":123}",
                "{}",
                "Processo",
                "123",
                Instant.now()
        ));

        assertThat(cacheManager.getCache("public_timeline").get("123")).isNull();
        PjbProcessualReadModelMeshService.ProcessualReadModelBlueprint blueprint = meshService.blueprint();
        assertThat(blueprint.domains().stream().filter(domain -> "PROCESSO_TIMELINE_HOT".equals(domain.domain())).findFirst().orElseThrow().freshness()).isNotNull();
    }
}
