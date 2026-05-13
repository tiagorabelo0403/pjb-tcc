package com.tcc.pjb.backend.core.financeiro.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.DepositoRecursalRepository;
import com.tcc.pjb.backend.model.repository.GruJudicialTrabalhistaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorkflowTrabalhistaFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private WorkflowTrabalhistaService workflowTrabalhistaService;
    @Autowired
    private GruJudicialTrabalhistaRepository gruRepository;
    @Autowired
    private DepositoRecursalRepository depositoRepository;

    @Test
    void devePersistirGruEDepositoRecursal() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("TRAB-1")
                .numeroUnificado("TRAB-U-1")
                .tribunal("TRT7")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.TRABALHISTA)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        var gru = workflowTrabalhistaService.gerarGruRecursal(processo.getId(), "PREPARO_RECURSAL", BigDecimal.valueOf(1500));
        var deposito = workflowTrabalhistaService.registrarDepositoRecursal(processo.getId(), "TRT", BigDecimal.valueOf(5000), "HASH-COMP-1");

        assertThat(gruRepository.findById(gru.gruId())).isPresent();
        assertThat(depositoRepository.findById(deposito.depositoId())).isPresent();
        assertThat(processoRepository.findById(processo.getId())).get()
                .extracting(Processo::getStatusProcesso)
                .isEqualTo(StatusProcesso.EXECUCAO_TRABALHISTA);
    }
}
