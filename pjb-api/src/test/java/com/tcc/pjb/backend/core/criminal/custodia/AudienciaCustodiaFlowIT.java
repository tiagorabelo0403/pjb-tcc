package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;

class AudienciaCustodiaFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private AudienciaCustodiaService audienciaCustodiaService;
    @Autowired
    private AudienciaCustodiaRepository audRepository;

    @MockitoBean
    private BnmpConsultaService bnmpConsultaService;

    @MockitoBean
    private com.tcc.pjb.backend.core.security.CurrentUserService currentUserService;

    @Test
    void deveRegistrarPrisaoEConcluirAudiencia() {
        org.mockito.Mockito.when(currentUserService.currentUserIdOrZero()).thenReturn(77L);
        org.mockito.Mockito.when(bnmpConsultaService.consultarMandadoAtivo(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult(false, null));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CUST-1")
                .numeroUnificado("CUST-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var prisao = audienciaCustodiaService.registrarPrisao(new RegistrarPrisaoCommand(processo.getId(), "Custodiado", "12345678901", Instant.now()));
        var resultado = audienciaCustodiaService.concluirAudiencia(new ConcluirAudienciaCommand(prisao.custodiaId(), "LIBERDADE_PROVISORIA", List.of("COMPARECIMENTO_PERIODICO")));
        assertThat(resultado.statusProcesso()).isEqualTo(StatusProcesso.LIBERDADE_PROVISORIA.name());
        assertThat(audRepository.findById(prisao.custodiaId())).get().extracting(v -> v.getStatus()).isEqualTo("REALIZADA");
    }
}
