package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.dje.domain.DjeEnvioCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "pjb.dje.enabled=true")
class DjePublicacaoFailureFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DjePublicacaoService djePublicacaoService;

    @Autowired
    private DjePublicacaoRepository djePublicacaoRepository;

    @MockitoBean
    private DjeHttpClient djeHttpClient;

    @Test
    void devePersistirFalhaSemPerderTrilhaDePublicacao() {
        when(djeHttpClient.enviar(any(DjeEnvioCommand.class))).thenThrow(new IllegalStateException("dje indisponivel"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("DJE-FALHA-1")
                .numeroUnificado("0005555-66.2026.8.06.0001")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        var result = djePublicacaoService.publicar(processo.getId(), "DECISAO", "conteudo sujeito a indisponibilidade", "TJCE");

        var entity = djePublicacaoRepository.findById(result.djeId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("FALHA");
        assertThat(entity.getFailureReason()).contains("dje indisponivel");
        assertThat(entity.getConteudoHash()).isNotBlank();
    }
}
