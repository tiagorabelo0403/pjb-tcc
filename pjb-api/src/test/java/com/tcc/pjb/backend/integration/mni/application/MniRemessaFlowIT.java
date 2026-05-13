package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaRequest;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "pjb.mni.enabled=true")
class MniRemessaFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private MniRemessaService mniRemessaService;

    @Autowired
    private MniRemessaRepository mniRemessaRepository;

    @MockitoBean
    private MniHttpClient mniHttpClient;

    @Test
    void deveEnviarEReprocessarFluxoMni() {
        org.mockito.Mockito.when(mniHttpClient.enviarAutos(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse("PROTO-IT"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("MNI-1")
                .numeroUnificado("MNI-U-1")
                .tribunal("TJCE")
                .classeProcessual("CIVEL")
                .assunto("teste")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var result = mniRemessaService.enviar(new MniRemessaRequest(processo.getId(), "TJSP", "DECLINIO"));
        assertThat(result.success()).isTrue();
        var remessa = mniRemessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(processo.getId(), "TJSP", "DECLINIO").orElseThrow();
        remessa.setStatus(com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa.FAILED);
        remessa.setProximoRetryEm(Instant.now().minusSeconds(1));
        mniRemessaRepository.save(remessa);
        assertThat(mniRemessaService.reprocessarPendentes()).isGreaterThanOrEqualTo(1);
    }
}
