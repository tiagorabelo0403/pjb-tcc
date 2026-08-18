package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "pjb.dje.enabled=true")
class DjePublicacaoFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DjePublicacaoService djePublicacaoService;

    @Autowired
    private DjePublicacaoRepository djePublicacaoRepository;

    @MockitoBean
    private DjeHttpClient djeHttpClient;

    @Test
    void devePublicarConsolidarENotificar() {
        org.mockito.Mockito.when(djeHttpClient.enviar(org.mockito.ArgumentMatchers.any(com.tcc.pjb.backend.core.dje.domain.DjeEnvioCommand.class)))
                .thenReturn(new com.tcc.pjb.backend.core.dje.domain.DjeEnvioResult("ED123"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("DJE-1")
                .numeroUnificado("DJE-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var result = djePublicacaoService.publicar(processo.getId(), "SENTENCA", "conteudo", "TJCE");
        var entity = djePublicacaoRepository.findById(result.djeId()).orElseThrow();
        entity.setDataPublicacao(LocalDate.now());
        entity.setPrazoComecaEm(LocalDate.now());
        djePublicacaoRepository.save(entity);
        assertThat(djePublicacaoService.consolidarPublicadas(LocalDate.now(), 10)).isEqualTo(1);
        assertThat(djePublicacaoService.notificarPartesPublicadas(LocalDate.now(), 10)).isEqualTo(1);
        assertThat(djePublicacaoRepository.findById(result.djeId()).orElseThrow().isPartesNotificadas()).isTrue();
    }
}
