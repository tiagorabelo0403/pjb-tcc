package com.tcc.pjb.backend.core.security.magistratura.lotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MagistradoLotacaoAccessServiceTest {

    private static final String VARA = "TJCE-VARA-01";
    private static final String OUTRA_VARA = "TJCE-VARA-02";
    private static final Long ADMINISTRACAO = 1L;

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
    private final ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AuditLedgerService auditoria = mock(AuditLedgerService.class);
    private final MagistradoLotacaoAccessService service = new MagistradoLotacaoAccessService(
            currentUserService, grantRepository, distribuicaoRepository, unidadeRepository, usuarioRepository, auditoria);

    private final Processo processo = Processo.builder().id(70L).unidadeJudiciariaCodigo(VARA).build();
    private final Usuario juiz = usuario(9L, TipoUsuario.JUIZ);

    @BeforeEach
    void catalogoEAdministracao() {
        noCatalogo(VARA);
        noCatalogo(OUTRA_VARA);
        when(usuarioRepository.findById(ADMINISTRACAO)).thenReturn(Optional.of(usuario(ADMINISTRACAO, TipoUsuario.ADMINISTRADOR)));
    }

    @Test
    void juizComLotacaoNaUnidadeEhAutorizadoEAuditoriaGuardaOFundamento() {
        autenticado(juiz);
        candidatos(grant(41L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null));

        assertThat(service.requireMagistradoLotadoNaUnidadeDoProcesso(processo)).isSameAs(juiz);

        verify(grantRepository).findVigentesNoProcessoOuNaUnidade(eq(ProfessionalActorClass.MAGISTRATURA), any(),
                eq(ProfessionalGrantApprovalStatus.APPROVED), any(), eq(70L), eq(VARA));
        verify(auditoria).appendSafely(eq("MAGISTRADO_LOTACAO_CONCEDIDA"), eq("PROCESSO"), eq("70"), isNull(),
                eq("usuario=9 grant=41 tipo=LOTACAO_UNIDADE aprovador=1 unidade=" + VARA));
    }

    @Test
    void substitutoAuxiliarERelatorDoProcessoTambemSaoAutorizados() {
        autenticado(juiz);
        for (ProfessionalInstitutionalAccessGrant grant : List.of(
                grant(42L, juiz, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null),
                grant(43L, juiz, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, VARA, null),
                grant(44L, juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo))) {
            candidatos(grant);
            assertThat(service.requireMagistradoLotadoNaUnidadeDoProcesso(processo)).isSameAs(juiz);
        }
    }

    @Test
    void perfilDeMagistradoEhExigidoAntesDeConsultarProcessoOuAcessos() {
        autenticado(usuario(9L, TipoUsuario.SERVIDOR));

        assertThatThrownBy(service::requireMagistrado)
                .isInstanceOf(AccessDeniedPjbException.class)
                .hasMessageContaining("ato exige magistrado");
        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class);
        verify(auditoria, times(2)).appendSafely(eq("MAGISTRADO_LOTACAO_NEGADA"), eq("USUARIO"), eq("9"), isNull(), anyString());
        verifyNoInteractions(grantRepository, distribuicaoRepository, unidadeRepository);
    }

    @Test
    void plantaoDelegacaoOutraVaraOutroProcessoOuSoComarcaNaoAutorizam() {
        autenticado(juiz);
        ProfessionalInstitutionalAccessGrant soComarca = grant(45L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, null, null);
        soComarca.setComarca("Fortaleza");
        candidatos(
                grant(46L, juiz, ProfessionalAccessGrantType.PLANTAO, VARA, null),
                grant(47L, juiz, ProfessionalAccessGrantType.DELEGACAO_GABINETE, VARA, null),
                grant(48L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, OUTRA_VARA, null),
                grant(49L, juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, Processo.builder().id(71L).build()),
                soComarca);

        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class)
                .hasMessage("Acesso negado: magistrado sem lotação vigente na unidade do processo");
        verify(auditoria).appendSafely(eq("MAGISTRADO_LOTACAO_NEGADA"), eq("PROCESSO"), eq("70"), isNull(), contains("sem acesso habilitante"));
    }

    @Test
    void acessoPendenteRevogadoFuturoOuVencidoNaoAutoriza() {
        autenticado(juiz);
        ProfessionalInstitutionalAccessGrant pendente = grant(50L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null);
        pendente.setApprovalStatus(ProfessionalGrantApprovalStatus.PENDING);
        ProfessionalInstitutionalAccessGrant revogado = grant(51L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null);
        revogado.setAtivo(false);
        ProfessionalInstitutionalAccessGrant futuro = grant(52L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null);
        futuro.setInicioVigencia(LocalDateTime.now().plusDays(3));
        ProfessionalInstitutionalAccessGrant vencido = grant(53L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null);
        vencido.setFimVigencia(LocalDateTime.now().minusDays(1));
        candidatos(pendente, revogado, futuro, vencido);

        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void acessoAutoaprovadoAprovadoPorColegaOuSemAprovadorNaoAutorizaNemNotifica() {
        autenticado(juiz);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario(20L, TipoUsuario.JUIZ)));
        ProfessionalInstitutionalAccessGrant autoaprovado = grant(54L, juiz, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null);
        autoaprovado.setApprovedByUserId(9L);
        ProfessionalInstitutionalAccessGrant aprovadoPorColega = grant(55L, juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo);
        aprovadoPorColega.setApprovedByUserId(20L);
        ProfessionalInstitutionalAccessGrant semAprovador = grant(56L, juiz, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, VARA, null);
        semAprovador.setApprovedByUserId(null);
        candidatos(autoaprovado, aprovadoPorColega, semAprovador);

        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(processo)).isEmpty();
    }

    @Test
    void conferenciaDeCompetenciaPendenteBloqueiaInclusiveORelatorDoProcesso() {
        autenticado(juiz);
        candidatos(
                grant(57L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null),
                grant(58L, juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo));

        distribuido(StatusDistribuicaoCompetencia.PENDENTE_REVISAO, VARA, null);
        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class)
                .hasMessage("Acesso negado: magistrado sem lotação vigente na unidade do processo");
        verify(auditoria).appendSafely(eq("MAGISTRADO_LOTACAO_NEGADA"), eq("PROCESSO"), eq("70"), isNull(), contains("conferência de competência"));

        distribuido(StatusDistribuicaoCompetencia.APLICADA, VARA, "JUIZADO_EM_UNIDADE_COMUM");
        assertThatThrownBy(() -> service.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(processo)).isEmpty();

        distribuido(StatusDistribuicaoCompetencia.APLICADA, VARA, " ");
        assertThat(service.requireMagistradoLotadoNaUnidadeDoProcesso(processo)).isSameAs(juiz);
    }

    @Test
    void processoRedistribuidoSemNovoRegistroNaoFicaPresoAoRegistroDeOutraUnidade() {
        Usuario impedido = usuario(30L, TipoUsuario.JUIZ);
        Processo redistribuido = Processo.builder().id(70L).unidadeJudiciariaCodigo(OUTRA_VARA).build();
        distribuido(StatusDistribuicaoCompetencia.PENDENTE_REVISAO, VARA, null);
        when(grantRepository.findVigentesNoProcessoOuNaUnidade(any(), any(), any(), any(), eq(70L), eq(OUTRA_VARA)))
                .thenReturn(List.of(grant(59L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, OUTRA_VARA, null)));
        when(grantRepository.findVigentesNoProcessoOuNaUnidade(any(), any(), any(), any(), eq(70L), eq(VARA)))
                .thenReturn(List.of(grant(60L, impedido, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null)));

        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(redistribuido)).containsExactly(juiz);
        assertThat(service.magistradoHabilitadoNoProcesso(30L, redistribuido)).isFalse();
    }

    @Test
    void codigoDeUnidadeForaDoCatalogoSoDeixaHabilitarPorAcessoVinculadoAoProcesso() {
        Processo comCodigoCnj = Processo.builder().id(70L).unidadeJudiciariaCodigo("0001").build();
        Usuario relator = usuario(31L, TipoUsuario.DESEMBARGADOR);
        when(grantRepository.findVigentesNoProcessoOuNaUnidade(any(), any(), any(), any(), eq(70L), isNull())).thenReturn(List.of(
                grant(61L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, "0001", null),
                grant(62L, relator, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, comCodigoCnj)));

        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(comCodigoCnj)).containsExactly(relator);
    }

    @Test
    void unidadeForaDoCatalogoComDistribuicaoPendenteBloqueiaAteOAcessoVinculadoAoProcesso() {
        Processo comCodigoCnj = Processo.builder().id(70L).unidadeJudiciariaCodigo("0001").build();
        distribuido(StatusDistribuicaoCompetencia.PENDENTE_REVISAO, OUTRA_VARA, null);
        candidatos(grant(70L, juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, comCodigoCnj));

        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(comCodigoCnj)).isEmpty();
        assertThat(service.magistradoHabilitadoNoProcesso(9L, comCodigoCnj)).isFalse();
    }

    @Test
    void destinatariosCoincidemComQuemAChecagemAutoriza() {
        Usuario substituto = usuario(10L, TipoUsuario.JUIZ);
        Usuario plantonista = usuario(12L, TipoUsuario.JUIZ);
        Usuario relator = usuario(13L, TipoUsuario.DESEMBARGADOR);
        Usuario autoaprovado = usuario(14L, TipoUsuario.JUIZ);
        ProfessionalInstitutionalAccessGrant grantAutoaprovado = grant(68L, autoaprovado, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null);
        grantAutoaprovado.setApprovedByUserId(14L);
        candidatos(
                grant(63L, juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null),
                grant(64L, substituto, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null),
                grant(65L, plantonista, ProfessionalAccessGrantType.PLANTAO, VARA, null),
                grant(66L, juiz, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, VARA, null),
                grant(67L, relator, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo),
                grantAutoaprovado);

        List<Usuario> destinatarios = service.magistradosLotadosNaUnidadeDoProcesso(processo);

        assertThat(destinatarios).containsExactly(juiz, substituto, relator);
        for (Usuario candidato : List.of(juiz, substituto, plantonista, relator, autoaprovado)) {
            assertThat(service.magistradoHabilitadoNoProcesso(candidato.getId(), processo))
                    .as("usuario %s", candidato.getId())
                    .isEqualTo(destinatarios.contains(candidato));
        }
    }

    @Test
    void magistradoInativoComAcessoValidoNaoRecebeNotificacao() {
        Usuario inativo = usuario(11L, TipoUsuario.JUIZ);
        inativo.setAtivo(false);
        candidatos(grant(69L, inativo, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null));

        assertThat(service.magistradosLotadosNaUnidadeDoProcesso(processo)).isEmpty();
    }

    private void autenticado(Usuario usuario) {
        when(currentUserService.getRequired()).thenReturn(usuario);
    }

    private void candidatos(ProfessionalInstitutionalAccessGrant... grants) {
        when(grantRepository.findVigentesNoProcessoOuNaUnidade(any(), any(), any(), any(), any(), any())).thenReturn(Arrays.asList(grants));
    }

    private void noCatalogo(String codigo) {
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getCodigo()).thenReturn(codigo);
        when(unidadeRepository.findByCodigo(codigo)).thenReturn(Optional.of(unidade));
    }

    private void distribuido(StatusDistribuicaoCompetencia status, String codigoUnidade, String fatoresRevisao) {
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getCodigo()).thenReturn(codigoUnidade);
        ProcessoDistribuicaoCompetencia distribuicao = mock(ProcessoDistribuicaoCompetencia.class);
        when(distribuicao.getStatus()).thenReturn(status);
        when(distribuicao.getUnidade()).thenReturn(unidade);
        when(distribuicao.getFatoresRevisao()).thenReturn(fatoresRevisao);
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(70L)).thenReturn(Optional.of(distribuicao));
    }

    private static ProfessionalInstitutionalAccessGrant grant(Long id, Usuario usuario, ProfessionalAccessGrantType tipo, String unidade, Processo processo) {
        return ProfessionalInstitutionalAccessGrant.builder()
                .id(id)
                .usuario(usuario)
                .processo(processo)
                .actorClass(ProfessionalActorClass.MAGISTRATURA)
                .grantType(tipo)
                .accessBasis(ProfessionalAccessBasis.MAGISTRATURA_COMPETENCIA_TERRITORIAL)
                .unidadeJudiciariaCodigo(unidade)
                .approvalStatus(ProfessionalGrantApprovalStatus.APPROVED)
                .approvedByUserId(ADMINISTRACAO)
                .inicioVigencia(LocalDateTime.now().minusDays(10))
                .build();
    }

    private static Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        usuario.setAtivo(true);
        return usuario;
    }
}
