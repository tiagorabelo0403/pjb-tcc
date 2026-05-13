package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "pjb.datajud.feed.enabled=true")
class DataJudFeedFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DataJudFeedService dataJudFeedService;

    @Autowired
    private DataJudFeedCheckpointRepository checkpointRepository;

    @MockitoBean
    private DataJudFeedHttpClient dataJudFeedHttpClient;

    @Test
    void deveAtualizarCheckpointPorTribunal() {
        org.mockito.Mockito.doNothing().when(dataJudFeedHttpClient).bulkIndex(org.mockito.ArgumentMatchers.anyList());
        processoRepository.save(Processo.builder()
                .numeroProcesso("DJ-1")
                .numeroUnificado("DJ-U-1")
                .tribunal("TJCE")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var enviados = dataJudFeedService.runIncremental("TJCE");
        assertThat(enviados.totalSent()).isGreaterThanOrEqualTo(1);
        assertThat(checkpointRepository.findByTribunalCodigo("TJCE")).isPresent();
    }
}
