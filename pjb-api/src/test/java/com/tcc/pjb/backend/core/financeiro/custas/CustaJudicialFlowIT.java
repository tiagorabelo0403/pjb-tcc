package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.PixResult;

class CustaJudicialFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private CustaJudicialService custaJudicialService;
    @Autowired
    private CustaJudicialRepository custaJudicialRepository;

    @MockitoBean
    private GruCodigoBarrasGenerator gruCodigoBarrasGenerator;
    @MockitoBean
    private PixPayloadGenerator pixPayloadGenerator;
    @MockitoBean
    private CustaIsencaoPolicy custaIsencaoPolicy;

    @Test
    void deveGerarCustaPendente() {
        org.mockito.Mockito.when(custaIsencaoPolicy.verificar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult(false, null));
        org.mockito.Mockito.when(gruCodigoBarrasGenerator.gerar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GruResult("001", "linha", "barra", "nosso"));
        org.mockito.Mockito.when(pixPayloadGenerator.gerar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PixResult("payload", "txid"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CUSTA-1")
                .numeroUnificado("CUSTA-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var result = custaJudicialService.gerarCustas(processo.getId(), com.tcc.pjb.backend.core.financeiro.custas.domain.TipoCusta.CUSTAS_INICIAIS, BigDecimal.valueOf(100));
        assertThat(result.isento()).isFalse();
        assertThat(custaJudicialRepository.findById(result.custaId())).isPresent();
    }
}
