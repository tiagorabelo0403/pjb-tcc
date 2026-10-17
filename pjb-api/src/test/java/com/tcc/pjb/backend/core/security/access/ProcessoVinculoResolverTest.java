package com.tcc.pjb.backend.core.security.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalInstitutionalAccessGrantService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalInstitutionalAccessGrantService.GrantResolution;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ConclusaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoVinculoResolverTest {

    private static final Long PROCESSO_ID = 70L;
    private static final String UNIDADE = "TJCE-FORTALEZA-1VC";
    private static final Set<VinculoProcessualDoUsuario> TODOS = EnumSet.allOf(VinculoProcessualDoUsuario.class);

    @Mock private PoloProcessualRepository poloRepository;
    @Mock private LaianeProcuracaoRepository procuracaoRepository;
    @Mock private FuncaoServidorJudiciarioRepository funcaoRepository;
    @Mock private UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    @Mock private ConclusaoProcessualRepository conclusaoRepository;
    @Mock private WorkItemRepository workItemRepository;
    @Mock private PeritoNomeacaoRepository peritoRepository;
    @Mock private OficialJusticaProcessoVinculoService oficialService;
    @Mock private ProcessoRepository processoRepository;
    @Mock private PjbTimeService timeService;

    private final ProfessionalInstitutionalAccessGrantService grantService =
            mock(ProfessionalInstitutionalAccessGrantService.class, invocacao -> GrantResolution.empty());
    private ProcessoVinculoResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ProcessoVinculoResolver(poloRepository, procuracaoRepository, peritoRepository, processoRepository,
                new VinculoInstitucionalResolver(poloRepository, conclusaoRepository, workItemRepository, oficialService),
                new VinculoFuncionalResolver(grantService, funcaoRepository, unidadeRepository, timeService));
    }

    @Test
    void cidadaoComCpfDeAutorEParteSemConsultarOPolo() {
        Usuario cidadao = usuario(1L, TipoUsuario.CIDADAO);
        Processo processo = processo();
        processo.setParteAutoraCpf(cidadao.getCpf());

        assertThat(resolver.vinculo(cidadao, processo, TODOS)).contains(VinculoProcessualDoUsuario.PARTE);
        verifyNoInteractions(poloRepository);
    }

    @Test
    void cidadaoQueAjuizouSozinhoEParte() {
        Usuario cidadao = usuario(1L, TipoUsuario.CIDADAO);
        Processo processo = processo();
        processo.setUsuario(cidadao);

        assertThat(resolver.vinculo(cidadao, processo, TODOS)).contains(VinculoProcessualDoUsuario.PARTE);
    }

    @Test
    void cidadaoQueSoApareceNaLinhaAtivaDoPoloEParte() {
        Usuario cidadao = usuario(1L, TipoUsuario.CIDADAO);
        when(poloRepository.findFirstByProcessoIdAndDocumentoAndAtivoTrueOrderByIdAsc(PROCESSO_ID, cidadao.getCpf()))
                .thenReturn(Optional.of(polo(TipoPolo.PASSIVO, true)));

        assertThat(resolver.vinculo(cidadao, processo(), TODOS)).contains(VinculoProcessualDoUsuario.PARTE);
    }

    @Test
    void cidadaoEstranhoAoProcessoNaoTemVinculo() {
        assertThat(resolver.vinculo(usuario(1L, TipoUsuario.CIDADAO), processo(), TODOS)).isEmpty();
    }

    @Test
    void advogadoQueAjuizouTemVinculoSemConsultarAProcuracaoAtiva() {
        Usuario advogado = usuario(2L, TipoUsuario.ADVOGADO);
        Processo processo = processo();
        processo.setUsuario(advogado);

        assertThat(resolver.vinculo(advogado, processo, TODOS)).contains(VinculoProcessualDoUsuario.ADVOGADO_DO_AJUIZAMENTO);
        verify(procuracaoRepository, never()).existsByAdvogadoIdAndProcessoIdAndStatus(anyLong(), anyLong(), any());
    }

    @Test
    void advogadoQueAjuizouETeveAProcuracaoRevogadaPerdeOVinculo() {
        Usuario advogado = usuario(2L, TipoUsuario.ADVOGADO);
        Processo processo = processo();
        processo.setUsuario(advogado);
        when(procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatusIn(eq(2L), eq(PROCESSO_ID), any()))
                .thenAnswer(invocacao -> invocacao.<Collection<LaianeProcuracaoStatus>>getArgument(2).contains(LaianeProcuracaoStatus.REVOGADA));

        assertThat(resolver.vinculo(advogado, processo, TODOS)).isEmpty();
    }

    @Test
    void advogadoComProcuracaoAtivaTemVinculo() {
        Usuario advogado = usuario(2L, TipoUsuario.ADVOGADO);
        when(procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(2L, PROCESSO_ID, LaianeProcuracaoStatus.ATIVA)).thenReturn(true);

        assertThat(resolver.vinculo(advogado, processo(), TODOS)).contains(VinculoProcessualDoUsuario.ADVOGADO_COM_PROCURACAO);
    }

    @Test
    void advogadoSemProcuracaoNemAjuizamentoNaoTemVinculo() {
        assertThat(resolver.vinculo(usuario(2L, TipoUsuario.ADVOGADO), processo(), TODOS)).isEmpty();
    }

    @Test
    void promotorSoTemVinculoComPoloAtivoDoMinisterioPublicoNaMesmaUf() {
        Usuario promotor = usuario(3L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(poloRepository.findByProcessoIdAndTipoPolo(PROCESSO_ID, TipoPolo.MINISTERIO_PUBLICO)).thenReturn(List.of(polo(TipoPolo.MINISTERIO_PUBLICO, false)));
        assertThat(resolver.vinculo(promotor, processo(), TODOS)).isEmpty();

        when(poloRepository.findByProcessoIdAndTipoPolo(PROCESSO_ID, TipoPolo.MINISTERIO_PUBLICO)).thenReturn(List.of(polo(TipoPolo.MINISTERIO_PUBLICO, true)));
        assertThat(resolver.vinculo(promotor, processo(), TODOS)).contains(VinculoProcessualDoUsuario.ORGAO_NO_POLO);
    }

    @Test
    void promotorEstadualDeOutraUfOuSemUfNaoAproveitaOPoloDoMinisterioPublico() {
        Usuario deOutraUf = usuario(3L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        deOutraUf.setUf("SP");
        Usuario semUf = usuario(4L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        semUf.setUf(null);
        Processo semUfNoProcesso = processo();
        semUfNoProcesso.setUf(null);

        assertThat(resolver.vinculo(deOutraUf, processo(), TODOS)).isEmpty();
        assertThat(resolver.vinculo(semUf, processo(), TODOS)).isEmpty();
        assertThat(resolver.vinculo(usuario(5L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO), semUfNoProcesso, TODOS)).isEmpty();
        verifyNoInteractions(poloRepository);
    }

    @Test
    void orgaosDeAtuacaoNacionalOuRegionalValemPeloPoloSemConferirUf() {
        for (TipoUsuario tipo : List.of(TipoUsuario.PROCURADOR_GERAL_REPUBLICA, TipoUsuario.PROMOTOR_TRABALHISTA,
                TipoUsuario.DEFENSOR_PUBLICO_FEDERAL, TipoUsuario.PROCURADORIA_FEDERAL)) {
            Usuario membro = usuario(6L, tipo);
            membro.setUf("DF");
            TipoPolo polo = tipo.isMinisterioPublico() ? TipoPolo.MINISTERIO_PUBLICO
                    : tipo.isDefensoriaPublica() ? TipoPolo.DEFENSORIA : TipoPolo.PROCURADORIA;
            when(poloRepository.findByProcessoIdAndTipoPolo(PROCESSO_ID, polo)).thenReturn(List.of(polo(polo, true)));

            assertThat(resolver.vinculo(membro, processo(), TODOS)).as(tipo.name()).contains(VinculoProcessualDoUsuario.ORGAO_NO_POLO);
        }
    }

    @Test
    void defensorNaoAproveitaPoloDoMinisterioPublico() {
        Usuario defensor = usuario(10L, TipoUsuario.DEFENSOR_PUBLICO);
        lenient().when(poloRepository.findByProcessoIdAndTipoPolo(PROCESSO_ID, TipoPolo.MINISTERIO_PUBLICO)).thenReturn(List.of(polo(TipoPolo.MINISTERIO_PUBLICO, true)));

        assertThat(resolver.vinculo(defensor, processo(), TODOS)).isEmpty();
    }

    @Test
    void defensorLotadoNaVaraSemEstarNoPoloNaoTemVinculo() {
        Usuario defensor = usuario(10L, TipoUsuario.DEFENSOR_PUBLICO);
        Processo processo = processo();
        ProfessionalInstitutionalAccessGrant lotacao = grant(ProfessionalAccessGrantType.LOTACAO_UNIDADE, UNIDADE, null);
        ProfessionalInstitutionalAccessGrant designacaoNaUnidade = grant(ProfessionalAccessGrantType.DESIGNACAO_PROCESSO, UNIDADE, null);
        when(grantService.resolveApplicable(defensor, processo, ProfessionalActorClass.DEFENSORIA)).thenReturn(designacoes(lotacao, designacaoNaUnidade));

        assertThat(resolver.vinculo(defensor, processo, TODOS)).isEmpty();
    }

    @Test
    void defensorDesignadoParaOProcessoTemVinculo() {
        Usuario defensor = usuario(10L, TipoUsuario.DEFENSOR_PUBLICO);
        Processo processo = processo();
        ProfessionalInstitutionalAccessGrant designacao = grant(ProfessionalAccessGrantType.DESIGNACAO_PROCESSO, null, null);
        designacao.setProcesso(processo);
        when(grantService.resolveApplicable(defensor, processo, ProfessionalActorClass.DEFENSORIA)).thenReturn(designacoes(designacao));

        assertThat(resolver.vinculo(defensor, processo, TODOS)).contains(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);
    }

    @Test
    void servidorComFuncaoVigenteNaUnidadeDoProcessoTemVinculo() {
        Usuario servidor = usuario(4L, TipoUsuario.SERVIDOR);
        relogioNaMadrugadaDeFortaleza();
        unidadeExiste(9L);
        when(funcaoRepository.findByUsuarioIdAndUnidadeIdAndAtivo(4L, 9L, true))
                .thenReturn(List.of(funcao(9L, LocalDate.of(2026, 10, 16), null)));

        assertThat(resolver.vinculo(servidor, processo(), TODOS)).contains(VinculoProcessualDoUsuario.FUNCAO_NA_UNIDADE);
    }

    @Test
    void funcaoVigenteSeMedeNoFusoLegalENaoEmUtc() {
        Usuario servidor = usuario(4L, TipoUsuario.SERVIDOR);
        relogioNaMadrugadaDeFortaleza();
        unidadeExiste(9L);
        when(funcaoRepository.findByUsuarioIdAndUnidadeIdAndAtivo(4L, 9L, true))
                .thenReturn(List.of(funcao(9L, LocalDate.of(2026, 10, 17), null)));

        assertThat(resolver.vinculo(servidor, processo(), TODOS)).isEmpty();
    }

    @Test
    void servidorComFuncaoEncerradaNaUnidadeNaoTemVinculo() {
        Usuario servidor = usuario(4L, TipoUsuario.SERVIDOR);
        relogioNaMadrugadaDeFortaleza();
        unidadeExiste(9L);
        when(funcaoRepository.findByUsuarioIdAndUnidadeIdAndAtivo(4L, 9L, true))
                .thenReturn(List.of(funcao(9L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30))));

        assertThat(resolver.vinculo(servidor, processo(), TODOS)).isEmpty();
    }

    @Test
    void servidorComFuncaoSoEmOutraUnidadeNaoTemVinculo() {
        Usuario servidor = usuario(4L, TipoUsuario.SERVIDOR);
        relogioNaMadrugadaDeFortaleza();
        unidadeExiste(9L);
        when(funcaoRepository.findByUsuarioIdAndUnidadeIdAndAtivo(4L, 9L, true)).thenReturn(List.of());

        assertThat(resolver.vinculo(servidor, processo(), TODOS)).isEmpty();
        verify(funcaoRepository, never()).findByUsuarioIdAndUnidadeIdAndAtivo(eq(4L), eq(10L), eq(true));
    }

    @Test
    void juizComConclusaoPendenteTemVinculo() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        when(conclusaoRepository.existsByProcessoIdAndMagistradoIdAndStatus(PROCESSO_ID, 5L, "PENDENTE")).thenReturn(true);

        assertThat(resolver.vinculo(juiz, processo(), TODOS)).contains(VinculoProcessualDoUsuario.PROCESSO_CONCLUSO);
    }

    @Test
    void juizComAUltimaConclusaoExpiradaEProcessoAindaConclusoTemVinculo() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        Processo processo = processo();
        processo.setStatusProcesso(StatusProcesso.CONCLUSO_JUIZ);
        when(conclusaoRepository.existsUltimaConclusaoExpiradaDoMagistrado(PROCESSO_ID, 5L)).thenReturn(true);

        assertThat(resolver.vinculo(juiz, processo, TODOS)).contains(VinculoProcessualDoUsuario.PROCESSO_CONCLUSO);
    }

    @Test
    void conclusaoExpiradaDeProcessoQueJaSaiuDeConclusoNaoVincula() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        Processo processo = processo();
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);

        assertThat(resolver.vinculo(juiz, processo, TODOS)).isEmpty();
        verify(conclusaoRepository, never()).existsUltimaConclusaoExpiradaDoMagistrado(anyLong(), anyLong());
    }

    @Test
    void juizComTarefaAtribuidaNoProcessoTemVinculo() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                eq(PROCESSO_ID), eq(5L), eq(List.of(TipoUsuario.JUIZ)), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of(new WorkItem()));

        assertThat(resolver.vinculo(juiz, processo(), TODOS)).contains(VinculoProcessualDoUsuario.TAREFA_ATRIBUIDA);
    }

    @Test
    void juizEmSubstituicaoAncoradaNaComarcaTemDesignacaoFormal() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        Processo processo = processo();
        when(grantService.resolveApplicable(juiz, processo, ProfessionalActorClass.MAGISTRATURA))
                .thenReturn(designacoes(grant(ProfessionalAccessGrantType.SUBSTITUICAO, null, "Fortaleza")));

        assertThat(resolver.vinculo(juiz, processo, TODOS)).contains(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);
    }

    @Test
    void juizLotadoNaUnidadeDoProcessoTemDesignacaoFormal() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        Processo processo = processo();
        when(grantService.resolveApplicable(juiz, processo, ProfessionalActorClass.MAGISTRATURA))
                .thenReturn(designacoes(grant(ProfessionalAccessGrantType.LOTACAO_UNIDADE, UNIDADE, null)));

        assertThat(resolver.vinculo(juiz, processo, TODOS)).contains(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);
    }

    @Test
    void designacaoSemAncoraNoProcessoNaUnidadeOuComarcaNaoVincula() {
        Usuario juiz = usuario(5L, TipoUsuario.JUIZ);
        Processo processo = processo();
        when(grantService.resolveApplicable(juiz, processo, ProfessionalActorClass.MAGISTRATURA)).thenReturn(designacoes(
                grant(ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, null, null),
                grant(ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO, null, null),
                grant(ProfessionalAccessGrantType.LOTACAO_UNIDADE, null, "Fortaleza")));

        assertThat(resolver.vinculo(juiz, processo, TODOS)).isEmpty();
    }

    @Test
    void juizSemConclusaoTarefaNemDesignacaoNaoTemVinculo() {
        assertThat(resolver.vinculo(usuario(5L, TipoUsuario.JUIZ), processo(), TODOS)).isEmpty();
    }

    @Test
    void assessorComDelegacaoDeGabineteNaUnidadeTemDesignacaoFormal() {
        Usuario assessor = usuario(6L, TipoUsuario.ASSESSOR_JUDICIAL);
        Processo processo = processo();
        when(grantService.resolveApplicable(assessor, processo, ProfessionalActorClass.APOIO_JUDICIAL))
                .thenReturn(designacoes(grant(ProfessionalAccessGrantType.DELEGACAO_GABINETE, UNIDADE, null)));

        assertThat(resolver.vinculo(assessor, processo, TODOS)).contains(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);
    }

    @Test
    void peritoComNomeacaoAceitaTemVinculo() {
        Usuario perito = usuario(7L, TipoUsuario.PERITO);
        when(peritoRepository.existsByProcessoIdAndPeritoIdAndStatus(PROCESSO_ID, 7L, PeritoNomeacaoStatus.NOMEADO)).thenReturn(false);
        when(peritoRepository.existsByProcessoIdAndPeritoIdAndStatus(PROCESSO_ID, 7L, PeritoNomeacaoStatus.ACEITO)).thenReturn(true);

        assertThat(resolver.vinculo(perito, processo(), TODOS)).contains(VinculoProcessualDoUsuario.PERITO_NOMEADO);
    }

    @Test
    void oficialComMandadoNoProcessoTemVinculo() {
        Usuario oficial = usuario(8L, TipoUsuario.OFICIAL_JUSTICA);
        when(oficialService.possuiVinculoDireto(PROCESSO_ID, 8L, TipoUsuario.OFICIAL_JUSTICA)).thenReturn(true);

        assertThat(resolver.vinculo(oficial, processo(), TODOS)).contains(VinculoProcessualDoUsuario.OFICIAL_COM_MANDADO);
    }

    @Test
    void vinculoForaDosAceitosNaoEConsultado() {
        Usuario perito = usuario(7L, TipoUsuario.PERITO);

        assertThat(resolver.vinculo(perito, processo(), EnumSet.of(VinculoProcessualDoUsuario.PARTE))).isEmpty();
        verifyNoInteractions(peritoRepository);
    }

    @Test
    void administradorNuncaTemVinculoComProcesso() {
        Usuario admin = usuario(9L, TipoUsuario.ADMINISTRADOR);
        Processo processo = processo();
        processo.setUsuario(admin);
        processo.setParteAutoraCpf(admin.getCpf());

        assertThat(resolver.vinculo(admin, processo, TODOS)).isEmpty();
        verifyNoInteractions(procuracaoRepository, funcaoRepository, conclusaoRepository, peritoRepository, oficialService, poloRepository);
    }

    @Test
    void malhaCarregaOProcessoPorIdEUsaAUfGravada() {
        Usuario promotor = usuario(3L, TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        Processo processo = processo();
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processo));
        when(poloRepository.findByProcessoIdAndTipoPolo(PROCESSO_ID, TipoPolo.MINISTERIO_PUBLICO)).thenReturn(List.of(polo(TipoPolo.MINISTERIO_PUBLICO, true)));

        assertThat(resolver.vinculoInstitucional(promotor, PROCESSO_ID)).contains(VinculoProcessualDoUsuario.ORGAO_NO_POLO);

        processo.setUf("PI");
        assertThat(resolver.vinculoInstitucional(promotor, PROCESSO_ID)).isEmpty();
    }

    @Test
    void delegadoComTarefaAtribuidaTemVinculoInstitucional() {
        Usuario delegado = usuario(11L, TipoUsuario.DELEGADO_POLICIA);
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processo()));
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                eq(PROCESSO_ID), eq(11L), eq(List.of(TipoUsuario.DELEGADO_POLICIA)), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of(new WorkItem()));

        assertThat(resolver.vinculoInstitucional(delegado, PROCESSO_ID)).contains(VinculoProcessualDoUsuario.TAREFA_ATRIBUIDA);
    }

    @Test
    void malhaNaoReconheceParteNemAdvogado() {
        Usuario cidadao = usuario(1L, TipoUsuario.CIDADAO);
        Processo processo = processo();
        processo.setParteAutoraCpf(cidadao.getCpf());
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processo));

        assertThat(resolver.vinculoInstitucional(cidadao, PROCESSO_ID)).isEmpty();
        verifyNoInteractions(procuracaoRepository, poloRepository);
    }

    @Test
    void malhaSemProcessoNaoTemVinculo() {
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.empty());

        assertThat(resolver.vinculoInstitucional(usuario(11L, TipoUsuario.DELEGADO_POLICIA), PROCESSO_ID)).isEmpty();
    }

    private void relogioNaMadrugadaDeFortaleza() {
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-10-17T02:00:00Z"));
        when(timeService.legalZone()).thenReturn(ZoneId.of("America/Fortaleza"));
    }

    private void unidadeExiste(Long id) {
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getId()).thenReturn(id);
        when(unidadeRepository.findByCodigo(UNIDADE)).thenReturn(Optional.of(unidade));
    }

    private static GrantResolution designacoes(ProfessionalInstitutionalAccessGrant... grants) {
        return new GrantResolution(List.of(grants), List.of(), false, true, false, false, false, false, false, false, false, false, false, "");
    }

    private static ProfessionalInstitutionalAccessGrant grant(ProfessionalAccessGrantType tipo, String unidade, String comarca) {
        ProfessionalInstitutionalAccessGrant grant = new ProfessionalInstitutionalAccessGrant();
        grant.setGrantType(tipo);
        grant.setUnidadeJudiciariaCodigo(unidade);
        grant.setComarca(comarca);
        grant.setUf("CE");
        return grant;
    }

    private static FuncaoServidorJudiciarioEntity funcao(Long unidadeId, LocalDate inicio, LocalDate fim) {
        FuncaoServidorJudiciarioEntity funcao = new FuncaoServidorJudiciarioEntity(4L, unidadeId, FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, inicio, null, null);
        if (fim != null) {
            funcao.encerrar(fim);
        }
        return funcao;
    }

    private static PoloProcessual polo(TipoPolo tipo, boolean ativo) {
        PoloProcessual polo = new PoloProcessual(PROCESSO_ID, tipo, TipoParte.MINISTERIO_PUBLICO, "Parte", null, null,
                null, null, null, null, null, 1);
        if (!ativo) {
            polo.desativar();
        }
        return polo;
    }

    private static Processo processo() {
        Processo processo = new Processo();
        processo.setId(PROCESSO_ID);
        processo.setUnidadeJudiciariaCodigo(UNIDADE);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        return processo;
    }

    private static Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        usuario.setCpf(String.format("%011d", id));
        usuario.setUf("CE");
        return usuario;
    }
}
