package com.tcc.pjb.backend.core.servidor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FuncaoServidorDesignacaoServiceTest {

    private FuncaoServidorApplicationService funcaoServidorApplicationService;
    private LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService;
    private AuditLedgerService auditLedgerService;
    private FuncaoServidorDesignacaoService service;

    @BeforeEach
    void setUp() {
        funcaoServidorApplicationService = mock(FuncaoServidorApplicationService.class);
        lotacaoInstituicaoMaterializationService = mock(LotacaoInstituicaoMaterializationService.class);
        auditLedgerService = mock(AuditLedgerService.class);
        service = new FuncaoServidorDesignacaoService(funcaoServidorApplicationService,
                lotacaoInstituicaoMaterializationService, auditLedgerService);
    }

    @Test
    void designaComSucessoDelegaMaterializacaoEAuditaDesignacao() {
        LocalDate dataInicio = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL,
                dataInicio, 1L, "Portaria 1");
        when(funcaoServidorApplicationService.designar(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL,
                dataInicio, 1L, "Portaria 1")).thenReturn(entidade);

        var resultado = service.designarComLotacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL,
                dataInicio, 1L, "Portaria 1");

        assertThat(resultado).isSameAs(entidade);
        verify(lotacaoInstituicaoMaterializationService)
                .materializarLotacaoSePonteExistir(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, dataInicio);
        verify(auditLedgerService).appendSafely("FUNCAO_SERVIDOR_DESIGNADA", "FUNCAO_SERVIDOR_JUDICIARIO",
                String.valueOf(entidade.getId()));
    }

    @Test
    void designaComFalhaNaMaterializacaoNaoImpedeRetornoDaEntidade() {
        LocalDate dataInicio = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.CHEFE_CARTORIO,
                dataInicio, 1L, "Portaria 4");
        when(funcaoServidorApplicationService.designar(10L, 5L, FuncaoServidorJudiciario.CHEFE_CARTORIO,
                dataInicio, 1L, "Portaria 4")).thenReturn(entidade);
        doThrow(new RuntimeException("falha simulada")).when(lotacaoInstituicaoMaterializationService)
                .materializarLotacaoSePonteExistir(10L, 5L, FuncaoServidorJudiciario.CHEFE_CARTORIO, dataInicio);

        var resultado = service.designarComLotacao(10L, 5L, FuncaoServidorJudiciario.CHEFE_CARTORIO,
                dataInicio, 1L, "Portaria 4");

        assertThat(resultado).isSameAs(entidade);
    }

    @Test
    void designaLancaRecursoJaExistenteQuandoApplicationServiceDetectaConflito() {
        LocalDate dataInicio = LocalDate.now();
        when(funcaoServidorApplicationService.designar(10L, 5L, FuncaoServidorJudiciario.OFICIAL_MAIOR,
                dataInicio, 1L, "Portaria 5"))
                .thenThrow(new DataIntegrityViolationException(
                        "Já existe função ativa para o usuário na unidade: OFICIAL_MAIOR"));

        assertThatThrownBy(() -> service.designarComLotacao(10L, 5L, FuncaoServidorJudiciario.OFICIAL_MAIOR,
                dataInicio, 1L, "Portaria 5"))
                .isInstanceOf(RecursoJaExistenteException.class);

        verifyNoInteractions(lotacaoInstituicaoMaterializationService);
        verify(auditLedgerService, never()).appendSafely(any(), any(), any());
    }

    @Test
    void falhaNoSaveDaMaterializacaoNaoRevertaEntidadePrincipalJaDesignada() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepo = mock(UnidadeJudiciariaCompetenciaRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        LotacaoInstituicaoRepository lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        var materializationService = new LotacaoInstituicaoMaterializationService(unidadeRepo, usuarioRepo, lotacaoRepo);
        var designacaoService = new FuncaoServidorDesignacaoService(funcaoServidorApplicationService,
                materializationService, auditLedgerService);

        UnidadeInstituicao unidadeInstituicao = new UnidadeInstituicao();
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getUnidadeInstituicao()).thenReturn(unidadeInstituicao);
        when(unidadeRepo.findById(5L)).thenReturn(Optional.of(unidade));
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(usuarioRepo.findById(10L)).thenReturn(Optional.of(usuario));
        when(lotacaoRepo.findFirstByUsuarioAndUnidadeOrderByInicioDesc(usuario, unidadeInstituicao))
                .thenReturn(Optional.empty());
        when(lotacaoRepo.save(any())).thenThrow(new RuntimeException("falha de save no proxy transacional participante"));

        LocalDate dataInicio = LocalDate.now();
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA,
                dataInicio, 1L, "Portaria 6");
        when(funcaoServidorApplicationService.designar(10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA,
                dataInicio, 1L, "Portaria 6")).thenReturn(entidade);

        var resultado = designacaoService.designarComLotacao(10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA,
                dataInicio, 1L, "Portaria 6");

        assertThat(resultado).isSameAs(entidade);
    }
}
