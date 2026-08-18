package com.tcc.pjb.backend.core.security.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PjbObjectScopeGuardPolicialTest {

    private final SecretariatInstitutionalVisibilityService visibilityService = mock(SecretariatInstitutionalVisibilityService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final BoletimOcorrenciaDigitalRepository boletimRepository = mock(BoletimOcorrenciaDigitalRepository.class);
    private final InqueritoPolicialDigitalRepository inqueritoRepository = mock(InqueritoPolicialDigitalRepository.class);
    private final DelegaciaInstitucionalScopeService delegaciaScopeService = new DelegaciaInstitucionalScopeService(
            unidadeRepository,
            lotacaoRepository
    );
    private final PjbObjectScopeGuard guard = new PjbObjectScopeGuardImpl(
            visibilityService,
            currentUserService,
            auditLedgerService,
            lotacaoRepository,
            delegaciaScopeService,
            boletimRepository,
            inqueritoRepository
    );

    @Test
    void boletimDelegaciaLotadaDiretaPermite() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA);
        BoletimOcorrenciaDigital boletim = boletim(100L, delegacia);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(boletimRepository.findById(100L)).thenReturn(Optional.of(boletim));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));

        assertDoesNotThrow(() -> guard.requireAccess(TipoObjetoProtegido.BOLETIM_OCORRENCIA, 100L, AcaoEscopo.LER));

        verify(unidadeRepository, never()).findAncestorIdsInclusive(anyLong());
    }

    @Test
    void inqueritoDepartamentoAncestralPermite() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao departamento = unidade(20L, TipoUnidadeInstitucional.DEPARTAMENTO_POLICIA);
        UnidadeInstituicao delegacia = unidade(30L, TipoUnidadeInstitucional.DELEGACIA);
        InqueritoPolicialDigital inquerito = inquerito(200L, delegacia, usuario(9L, TipoUsuario.DELEGADO_POLICIA));
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(inqueritoRepository.findById(200L)).thenReturn(Optional.of(inquerito));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(departamento)));
        when(unidadeRepository.findAncestorIdsInclusive(30L)).thenReturn(List.of(30L, 20L));

        assertDoesNotThrow(() -> guard.requireAccess(TipoObjetoProtegido.INQUERITO, 200L, AcaoEscopo.LER));
    }

    @Test
    void boletimOutraDelegaciaNega() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA);
        UnidadeInstituicao outraDelegacia = unidade(11L, TipoUnidadeInstitucional.DELEGACIA);
        BoletimOcorrenciaDigital boletim = boletim(100L, delegacia);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(boletimRepository.findById(100L)).thenReturn(Optional.of(boletim));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(outraDelegacia)));
        when(unidadeRepository.findAncestorIdsInclusive(10L)).thenReturn(List.of(10L));

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard.requireAccess(TipoObjetoProtegido.BOLETIM_OCORRENCIA, 100L, AcaoEscopo.LER));

        assertEquals(MotivoNegacaoEscopo.SEM_ATRIBUICAO, ex.getMotivo());
    }

    @Test
    void inqueritoAutoridadeResponsavelPermiteSemLotacao() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        InqueritoPolicialDigital inquerito = inquerito(200L, null, delegado);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(inqueritoRepository.findById(200L)).thenReturn(Optional.of(inquerito));

        assertDoesNotThrow(() -> guard.requireAccess(TipoObjetoProtegido.INQUERITO, 200L, AcaoEscopo.MOVIMENTAR));

        verify(lotacaoRepository, never()).findAtivasByUsuario(any());
    }

    @Test
    void inqueritoMinisterioPublicoPermiteSemLotacao() {
        Usuario promotor = usuario(2L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        InqueritoPolicialDigital inquerito = inquerito(200L, null, usuario(1L, TipoUsuario.DELEGADO_POLICIA));
        when(currentUserService.getRequired()).thenReturn(promotor);
        when(inqueritoRepository.findById(200L)).thenReturn(Optional.of(inquerito));

        assertDoesNotThrow(() -> guard.requireAccess(TipoObjetoProtegido.INQUERITO, 200L, AcaoEscopo.LER));

        verify(lotacaoRepository, never()).findAtivasByUsuario(any());
    }

    private Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private UnidadeInstituicao unidade(Long id, TipoUnidadeInstitucional tipo) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setNome("Unidade " + id);
        unidade.setTipo(tipo);
        unidade.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);
        unidade.setUf("CE");
        unidade.setComarca("Fortaleza");
        return unidade;
    }

    private LotacaoInstituicao lotacao(UnidadeInstituicao unidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        return lotacao;
    }

    private BoletimOcorrenciaDigital boletim(Long id, UnidadeInstituicao unidade) {
        BoletimOcorrenciaDigital boletim = new BoletimOcorrenciaDigital();
        ReflectionTestUtils.setField(boletim, "id", id);
        boletim.setUnidadeRegistro(unidade);
        return boletim;
    }

    private InqueritoPolicialDigital inquerito(Long id, UnidadeInstituicao unidade, Usuario autoridade) {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        ReflectionTestUtils.setField(inquerito, "id", id);
        inquerito.setUnidadeApuracao(unidade);
        inquerito.setAutoridadeResponsavel(autoridade);
        return inquerito;
    }
}
