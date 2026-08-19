package com.tcc.pjb.backend.core.servidor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.StatusFuncaoServidorSolicitacao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.FuncaoServidorSolicitacaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FuncaoServidorSolicitacaoServiceTest {

    private FuncaoServidorSolicitacaoRepository solicitacaoRepository;
    private FuncaoServidorJudiciarioRepository funcaoServidorJudiciarioRepository;
    private FuncaoServidorDesignacaoService designacaoService;
    private UsuarioRepository usuarioRepository;
    private AuditLedgerService auditLedgerService;
    private FuncaoServidorSolicitacaoService service;

    @BeforeEach
    void setUp() {
        solicitacaoRepository = mock(FuncaoServidorSolicitacaoRepository.class);
        funcaoServidorJudiciarioRepository = mock(FuncaoServidorJudiciarioRepository.class);
        designacaoService = mock(FuncaoServidorDesignacaoService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        auditLedgerService = mock(AuditLedgerService.class);
        service = new FuncaoServidorSolicitacaoService(solicitacaoRepository, funcaoServidorJudiciarioRepository,
                designacaoService, usuarioRepository, auditLedgerService);
        when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void solicitarCriaEmPendenteEAuditaCriacao() {
        var criada = service.solicitar(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, "Motivo");

        assertThat(criada.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.PENDENTE);
        assertThat(criada.getSolicitanteId()).isEqualTo(10L);
        verify(auditLedgerService).appendSafely("FUNCAO_SERVIDOR_SOLICITACAO_CRIADA", "FUNCAO_SERVIDOR_SOLICITACAO", "null");
    }

    @Test
    void aprovarPorAdministradorSempreAutorizadoEMaterializaDesignacao() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario admin = usuarioComTipo(99L, TipoUsuario.ADMINISTRADOR);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(designacaoService.designarComLotacao(eqLong(10L), eqLong(5L), org.mockito.ArgumentMatchers.eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL),
                any(), eqLong(99L), any())).thenReturn(mock(FuncaoServidorJudiciarioEntity.class));

        var resultado = service.aprovar(1L, 99L);

        assertThat(resultado.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.APROVADA);
        verify(designacaoService).designarComLotacao(eqLong(10L), eqLong(5L), org.mockito.ArgumentMatchers.eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL),
                any(), eqLong(99L), any());
    }

    @Test
    void aprovarPorDiretorDaMesmaUnidadeAutorizado() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario diretor = usuarioComTipo(77L, TipoUsuario.SERVIDOR_FORUM);
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(diretor));
        when(funcaoServidorJudiciarioRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                77L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true))
                .thenReturn(Optional.of(mock(FuncaoServidorJudiciarioEntity.class)));

        var resultado = service.aprovar(1L, 77L);

        assertThat(resultado.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.APROVADA);
    }

    @Test
    void aprovarPorDiretorDeOutraUnidadeNegado() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario diretorOutraUnidade = usuarioComTipo(77L, TipoUsuario.SERVIDOR_FORUM);
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(diretorOutraUnidade));
        when(funcaoServidorJudiciarioRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                77L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprovar(1L, 77L)).isInstanceOf(SecurityException.class);
    }

    @Test
    void aprovarPorServidorSemFuncaoNenhumaNegado() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario servidorComum = usuarioComTipo(50L, TipoUsuario.SERVIDOR_FORUM);
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(servidorComum));
        when(funcaoServidorJudiciarioRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                50L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprovar(1L, 50L)).isInstanceOf(SecurityException.class);
    }

    @Test
    void rejeitarPorQuemNaoPodeDecidirNegado() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario semFuncao = usuarioComTipo(50L, TipoUsuario.SERVIDOR_FORUM);
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(semFuncao));
        when(funcaoServidorJudiciarioRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                50L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rejeitar(1L, 50L, "motivo")).isInstanceOf(SecurityException.class);
    }

    @Test
    void aprovarSolicitacaoJaDecididaConvertePraRecursoJaExistenteException() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        solicitacao.aprovar(1L);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario admin = usuarioComTipo(99L, TipoUsuario.ADMINISTRADOR);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.aprovar(1L, 99L)).isInstanceOf(RecursoJaExistenteException.class);
    }

    @Test
    void rejeitarSolicitacaoJaDecididaConvertePraRecursoJaExistenteException() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        solicitacao.aprovar(1L);
        setId(solicitacao, 1L);
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.of(solicitacao));
        Usuario admin = usuarioComTipo(99L, TipoUsuario.ADMINISTRADOR);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.rejeitar(1L, 99L, "motivo")).isInstanceOf(RecursoJaExistenteException.class);
    }

    private Usuario usuarioComTipo(Long id, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTipoUsuario(tipo);
        return u;
    }

    private Long eqLong(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private void setId(FuncaoServidorSolicitacao solicitacao, Long id) {
        try {
            var field = FuncaoServidorSolicitacao.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(solicitacao, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
