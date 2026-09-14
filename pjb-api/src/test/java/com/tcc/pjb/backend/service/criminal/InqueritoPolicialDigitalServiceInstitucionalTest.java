package com.tcc.pjb.backend.service.criminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.scope.AcessoForaDeEscopoException;
import com.tcc.pjb.backend.core.security.scope.DelegaciaInstitucionalScopeService;
import com.tcc.pjb.backend.core.security.scope.PjbObjectScopeGuard;
import com.tcc.pjb.backend.core.security.scope.PjbObjectScopeGuardImpl;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class InqueritoPolicialDigitalServiceInstitucionalTest {

    private final InqueritoPolicialDigitalRepository repository = mock(InqueritoPolicialDigitalRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final BoletimOcorrenciaDigitalRepository boletimRepository = mock(BoletimOcorrenciaDigitalRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final DelegaciaInstitucionalScopeService delegaciaScopeService = new DelegaciaInstitucionalScopeService(
            unidadeRepository,
            lotacaoRepository
    );
    private final PjbObjectScopeGuard scopeGuard = new PjbObjectScopeGuardImpl(
            mock(SecretariatInstitutionalVisibilityService.class),
            currentUserService,
            mock(AuditLedgerService.class),
            lotacaoRepository,
            delegaciaScopeService,
            boletimRepository,
            repository
    );

    private final OfficialDocumentTemplateService officialDocumentTemplateService = mock(OfficialDocumentTemplateService.class);

    private final InqueritoPolicialDigitalService service = new InqueritoPolicialDigitalService(
            repository,
            processoRepository,
            workItemRepository,
            delegaciaScopeService,
            scopeGuard,
            currentUserService,
            officialDocumentTemplateService
    );

    private static HttpServletRequest certRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("PJB_STRONG_AUTH_METHOD")).thenReturn("CERTIFICADO_ICP");
        return request;
    }

    @Test
    void registrarSemLoginPorCertificado_bloqueiaEInformaOQueFalta() {
        when(currentUserService.getRequired()).thenReturn(usuario(1L, TipoUsuario.DELEGADO_POLICIA));
        HttpServletRequest semCertificado = mock(HttpServletRequest.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.registrar(cadastro(10L), semCertificado));

        assertTrue(ex.getMessage().contains("certificado digital"));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarSemUnidadeApuracaoId_naoPersiste() {
        when(currentUserService.getRequired()).thenReturn(usuario(1L, TipoUsuario.DELEGADO_POLICIA));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(cadastro(null), certRequest()));

        verify(repository, never()).save(any());
    }

    @Test
    void registrarSemNumeroDeProcedimento_bloqueiaEInformaOQueFalta() {
        when(currentUserService.getRequired()).thenReturn(usuario(1L, TipoUsuario.DELEGADO_POLICIA));
        InqueritoPolicialDigitalService.InqueritoCadastroRequest semNumero =
                new InqueritoPolicialDigitalService.InqueritoCadastroRequest(
                        null, "INQUERITO_POLICIAL", "Roubo majorado", "Resumo mínimo dos fatos",
                        null, null, null, null, null, 10L, null, null, null,
                        LocalDate.now().plusDays(30), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrar(semNumero, certRequest()));

        assertTrue(ex.getMessage().contains("número do procedimento"));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarSemPrazoDeConclusao_bloqueiaEInformaOQueFalta() {
        when(currentUserService.getRequired()).thenReturn(usuario(1L, TipoUsuario.DELEGADO_POLICIA));
        InqueritoPolicialDigitalService.InqueritoCadastroRequest semPrazo =
                new InqueritoPolicialDigitalService.InqueritoCadastroRequest(
                        "2026.001.INQ.000001", "INQUERITO_POLICIAL", "Roubo majorado", "Resumo mínimo dos fatos",
                        null, null, null, null, null, 10L, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrar(semPrazo, certRequest()));

        assertTrue(ex.getMessage().contains("prazo de conclusão"));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarSemNumeroESemPrazo_listaAmbosNaMensagem() {
        when(currentUserService.getRequired()).thenReturn(usuario(1L, TipoUsuario.DELEGADO_POLICIA));
        InqueritoPolicialDigitalService.InqueritoCadastroRequest semNada =
                new InqueritoPolicialDigitalService.InqueritoCadastroRequest(
                        "  ", "INQUERITO_POLICIAL", "Roubo majorado", "Resumo mínimo dos fatos",
                        null, null, null, null, null, 10L, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrar(semNada, certRequest()));

        assertTrue(ex.getMessage().contains("número do procedimento"));
        assertTrue(ex.getMessage().contains("prazo de conclusão"));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarUnidadeQueNaoEhDelegacia_naoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao forum = unidade(10L, TipoUnidadeInstitucional.FORUM, StatusUnidadeInstitucional.ATIVA);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(forum));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(cadastro(10L), certRequest()));

        verify(repository, never()).save(any());
    }

    @Test
    void registrarDelegaciaSemLotacaoAtiva_naoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.registrar(cadastro(10L), certRequest()));

        verify(repository, never()).save(any());
    }

    @Test
    void registrarDelegaciaSemTerritorio_naoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        delegacia.setUf(null);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(cadastro(10L), certRequest()));

        verify(repository, never()).save(any());
    }

    @Test
    void registrarDelegaciaLotada_persisteVinculoInstitucionalEHerdaTerritorio() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(repository.save(any())).thenAnswer(invocation -> {
            InqueritoPolicialDigital item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 100L);
            return item;
        });

        InqueritoPolicialDigitalService.InqueritoView view = service.registrar(cadastro(10L), certRequest());

        ArgumentCaptor<InqueritoPolicialDigital> captor = ArgumentCaptor.forClass(InqueritoPolicialDigital.class);
        verify(repository).save(captor.capture());
        assertEquals(10L, captor.getValue().getUnidadeApuracao().getId());
        assertEquals("Delegacia de Fortaleza", captor.getValue().getOrgaoApuracao());
        assertEquals("CE", captor.getValue().getUf());
        assertEquals("Fortaleza", captor.getValue().getMunicipio());
        assertEquals(10L, view.unidadeApuracaoId());
    }

    @Test
    void movimentarSegurancaPublicaLotadaNaDelegaciaDoInquerito_permiteSemSerAutoridadeResponsavel() {
        Usuario autoridade = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        Usuario escrivao = usuario(2L, TipoUsuario.ESCRIVAO_POLICIAL);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        InqueritoPolicialDigital inquerito = inquerito(44L, autoridade, delegacia, null);
        when(currentUserService.getRequired()).thenReturn(escrivao);
        when(repository.findById(44L)).thenReturn(Optional.of(inquerito));
        when(lotacaoRepository.findAtivasByUsuario(escrivao)).thenReturn(List.of(lotacao(delegacia)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InqueritoPolicialDigitalService.InqueritoView view = service.movimentar(
                44L,
                new InqueritoPolicialDigitalService.InqueritoMovimentacaoRequest(
                        "RELATADO", null, null, null, "Relatório final", null, false, false)
        );

        assertEquals("RELATADO", view.status());
        verify(repository).save(inquerito);
    }

    @Test
    void movimentarSegurancaPublicaSemLotacaoNaDelegaciaDoInquerito_nega() {
        Usuario autoridade = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        Usuario escrivao = usuario(2L, TipoUsuario.ESCRIVAO_POLICIAL);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        InqueritoPolicialDigital inquerito = inquerito(44L, autoridade, delegacia, null);
        when(currentUserService.getRequired()).thenReturn(escrivao);
        when(repository.findById(44L)).thenReturn(Optional.of(inquerito));
        when(lotacaoRepository.findAtivasByUsuario(escrivao)).thenReturn(List.of());

        assertThrows(AcessoForaDeEscopoException.class, () -> service.movimentar(
                44L,
                new InqueritoPolicialDigitalService.InqueritoMovimentacaoRequest(
                        "RELATADO", null, null, null, "Relatório final", null, false, false)
        ));

        verify(repository, never()).save(any());
    }

    @Test
    void listarMeusSegurancaPublicaUneAutoridadeResponsavelEDelegaciasLotadasSemDuplicar() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = unidade(10L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA);
        InqueritoPolicialDigital porLotacao = inquerito(101L, usuario(7L, TipoUsuario.DELEGADO_POLICIA), delegacia, null);
        InqueritoPolicialDigital duplicado = inquerito(102L, delegado, delegacia, null);
        InqueritoPolicialDigital porAutoridade = inquerito(103L, delegado, null, null);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(repository.findTop100ByUnidadeApuracao_IdInOrderByUpdatedAtDesc(List.of(10L)))
                .thenReturn(List.of(porLotacao, duplicado));
        when(repository.findTop100ByAutoridadeResponsavel_IdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(duplicado, porAutoridade));

        List<InqueritoPolicialDigitalService.InqueritoView> views = service.listarMeus(null);

        assertEquals(List.of(101L, 102L, 103L), views.stream().map(InqueritoPolicialDigitalService.InqueritoView::id).toList());
        assertTrue(views.stream().anyMatch(view -> view.unidadeApuracaoId() == null));
    }

    private InqueritoPolicialDigitalService.InqueritoCadastroRequest cadastro(Long unidadeApuracaoId) {
        return new InqueritoPolicialDigitalService.InqueritoCadastroRequest(
                "2026.001.INQ.000001",
                "INQUERITO_POLICIAL",
                "Roubo majorado",
                "Resumo mínimo dos fatos",
                null,
                null,
                null,
                null,
                null,
                unidadeApuracaoId,
                null,
                null,
                null,
                LocalDate.now().plusDays(30),
                null
        );
    }

    private Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private UnidadeInstituicao unidade(Long id, TipoUnidadeInstitucional tipo, StatusUnidadeInstitucional status) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setNome("Delegacia de Fortaleza");
        unidade.setTipo(tipo);
        unidade.setStatusUnidade(status);
        unidade.setUf("CE");
        unidade.setComarca("Fortaleza");
        return unidade;
    }

    private LotacaoInstituicao lotacao(UnidadeInstituicao unidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        return lotacao;
    }

    private InqueritoPolicialDigital inquerito(Long id,
                                               Usuario autoridade,
                                               UnidadeInstituicao unidade,
                                               Processo processo) {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        ReflectionTestUtils.setField(inquerito, "id", id);
        inquerito.setNumeroProcedimento("IPD-" + id);
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setStatus("INSTAURADO");
        inquerito.setFaseAtual("INVESTIGACAO");
        inquerito.setNaturezaFato("Roubo majorado");
        inquerito.setResumoFatos("Resumo mínimo dos fatos");
        inquerito.setAutoridadeResponsavel(autoridade);
        inquerito.setUnidadeApuracao(unidade);
        inquerito.setProcessoVinculado(processo);
        return inquerito;
    }
}
