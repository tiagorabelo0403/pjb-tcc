package com.tcc.pjb.backend.core.servidor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FuncaoServidorEncerramentoServiceTest {

    private FuncaoServidorApplicationService funcaoServidorApplicationService;
    private LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService;
    private AuditLedgerService auditLedgerService;
    private FuncaoServidorEncerramentoService service;

    @BeforeEach
    void setUp() {
        funcaoServidorApplicationService = mock(FuncaoServidorApplicationService.class);
        lotacaoInstituicaoMaterializationService = mock(LotacaoInstituicaoMaterializationService.class);
        auditLedgerService = mock(AuditLedgerService.class);
        service = new FuncaoServidorEncerramentoService(funcaoServidorApplicationService,
                lotacaoInstituicaoMaterializationService, auditLedgerService);
    }

    @Test
    void ultimaFuncaoAtivaNaUnidadeEncerraLotacao() {
        LocalDate dataFim = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL,
                dataFim.minusDays(30), 1L, "Portaria 1");
        when(funcaoServidorApplicationService.encerrar(99L, dataFim, 1L)).thenReturn(entidade);
        when(funcaoServidorApplicationService.funcoesAtivas(10L, 5L)).thenReturn(List.of());

        var resultado = service.encerrarComLotacao(99L, dataFim, 1L);

        assertThat(resultado).isSameAs(entidade);
        verify(lotacaoInstituicaoMaterializationService).encerrarLotacaoSeAtiva(10L, 5L, dataFim);
        verify(auditLedgerService).appendSafely("FUNCAO_SERVIDOR_ENCERRADA", "FUNCAO_SERVIDOR_JUDICIARIO",
                String.valueOf(entidade.getId()));
    }

    @Test
    void outraFuncaoAindaAtivaNaUnidadeNaoEncerraLotacao() {
        LocalDate dataFim = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.CALCULISTA_JUDICIAL,
                dataFim.minusDays(30), 1L, "Portaria 2");
        var outraFuncaoAtiva = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO,
                dataFim.minusDays(60), 1L, "Portaria 0");
        when(funcaoServidorApplicationService.encerrar(99L, dataFim, 1L)).thenReturn(entidade);
        when(funcaoServidorApplicationService.funcoesAtivas(10L, 5L)).thenReturn(List.of(outraFuncaoAtiva));

        var resultado = service.encerrarComLotacao(99L, dataFim, 1L);

        assertThat(resultado).isSameAs(entidade);
        verify(lotacaoInstituicaoMaterializationService, never()).encerrarLotacaoSeAtiva(any(), any(), any());
    }

    @Test
    void falhaAoEncerrarLotacaoNaoImpedeRetornoDaEntidade() {
        LocalDate dataFim = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.TECNICO_JUDICIARIO,
                dataFim.minusDays(10), 1L, "Portaria 3");
        when(funcaoServidorApplicationService.encerrar(99L, dataFim, 1L)).thenReturn(entidade);
        when(funcaoServidorApplicationService.funcoesAtivas(10L, 5L)).thenReturn(List.of());
        doThrow(new RuntimeException("falha simulada"))
                .when(lotacaoInstituicaoMaterializationService).encerrarLotacaoSeAtiva(10L, 5L, dataFim);

        var resultado = service.encerrarComLotacao(99L, dataFim, 1L);

        assertThat(resultado).isSameAs(entidade);
    }
}
