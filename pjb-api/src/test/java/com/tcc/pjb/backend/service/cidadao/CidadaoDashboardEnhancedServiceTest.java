package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.cidadao.CidadaoPosicaoProcessualService.CidadaoPosicaoProcessual;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CidadaoDashboardEnhancedServiceTest {

    private static final String NUMERO = "0001234-56.2026.8.06.0001";
    private static final String CPF = "11122233344";

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);
    private final CidadaoPosicaoProcessualService posicaoService = mock(CidadaoPosicaoProcessualService.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final Processo processo = new Processo();
    private CidadaoDashboardEnhancedService service;

    @BeforeEach
    void setUp() {
        Usuario cidadao = new Usuario();
        cidadao.setId(7L);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setCpf(CPF);
        PerfilDashboardContext context = mock(PerfilDashboardContext.class);
        when(context.usuario()).thenReturn(cidadao);
        when(contextFactory.build()).thenReturn(context);
        service = new CidadaoDashboardEnhancedService(
                contextFactory, mock(PainelServiceCommons.class), processoRepository,
                mock(WorkItemRepository.class), authorizationService, secrecyService, posicaoService);
        processo.setId(42L);
        processo.setNumeroProcesso(NUMERO);
        processo.setTribunal("TJCE");
        when(processoRepository.findProcessoCompletoById(42L)).thenReturn(Optional.of(processo));
        when(secrecyService.effectiveSecrecyForProcesso(42L)).thenReturn(NivelSigilo.SEGREDO_JUSTICA);
        posicao(null, null);
    }

    private Function<Long, Map<String, Object>> leitura(String nome) {
        return switch (nome) {
            case "timeline" -> service::timelineVisualProcesso;
            case "orientacao" -> service::orientacaoAudiencia;
            case "resumo" -> service::exportarResumoProcesso;
            default -> throw new IllegalArgumentException(nome);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void parteDoProcessoLeComGuardaDeParteESigiloEfetivo(String nome) {
        Map<String, Object> out = leitura(nome).apply(42L);

        assertThat(out).containsEntry("tribunal", "TJCE");
        verify(authorizationService).requireReadProcessoAsCidadaoParte(processo);
        verify(authorizationService).requireReadProcessoAtSecrecy(processo, NivelSigilo.SEGREDO_JUSTICA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void cidadaoQueNaoEParteRecebeAcessoNegadoSemConsultarSigilo(String nome) {
        doThrow(new AccessDeniedPjbException("Cidadão não é parte do processo."))
                .when(authorizationService).requireReadProcessoAsCidadaoParte(processo);

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
        verifyNoInteractions(secrecyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void parteSemConfirmacaoExigidaPeloSigiloRecebeAcessoNegado(String nome) {
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(authorizationService).requireReadProcessoAtSecrecy(processo, NivelSigilo.SEGREDO_JUSTICA);

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void usuarioSemPerfilDeCidadaoEBarradoAntesDeCarregarOProcesso(String nome) {
        doThrow(new AccessDeniedPjbException("Acesso negado ao perfil informado."))
                .when(authorizationService).requireRole(any(), eq("ROLE_CIDADAO"));

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
        verify(processoRepository, never()).findProcessoCompletoById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void processoInexistenteRespondeNaoEncontradoSemAvaliarAcesso(String nome) {
        assertThatThrownBy(() -> leitura(nome).apply(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(authorizationService, never()).requireReadProcessoAsCidadaoParte(any());
    }

    private void posicao(TipoPolo polo, TipoRepresentanteProcessual representacao) {
        when(posicaoService.resolver(processo, CPF)).thenReturn(new CidadaoPosicaoProcessual(polo, representacao));
    }

    @Test
    void parteSemAdvogadoNaoRecebeOrientacaoParaProcurarSeuAdvogado() {
        processo.setFaseAtual((FaseProcessual) null);
        posicao(TipoPolo.ATIVO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO);

        Map<String, Object> out = service.timelineVisualProcesso(42L);

        assertThat(out.get("descricaoSimples")).asString()
                .doesNotContain("seu advogado")
                .contains("Você atua sem advogado")
                .contains("Defensoria Pública");
        assertThat(out.get("proximoPasso")).isEqualTo("Aguardar movimentação");
    }

    @Test
    void parteComAdvogadoContinuaOrientadaAAcompanharComOAdvogado() {
        processo.setFaseAtual((FaseProcessual) null);
        posicao(TipoPolo.ATIVO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO);

        assertThat(service.timelineVisualProcesso(42L).get("descricaoSimples")).asString()
                .endsWith("Acompanhe as movimentações com seu advogado.");
    }

    @Test
    void partePelaDefensoriaEOrientadaAAcompanharComADefensoria() {
        processo.setFaseAtual((FaseProcessual) null);
        posicao(TipoPolo.ATIVO, TipoRepresentanteProcessual.DEFENSOR_PUBLICO);

        assertThat(service.timelineVisualProcesso(42L).get("descricaoSimples")).asString()
                .endsWith("Acompanhe as movimentações com a Defensoria Pública.")
                .doesNotContain("advogado");
    }

    @Test
    void representacaoDesconhecidaNaoPresumeAdvogado() {
        processo.setFaseAtual((FaseProcessual) null);

        assertThat(service.timelineVisualProcesso(42L).get("descricaoSimples")).asString()
                .doesNotContain("advogado");
    }

    @Test
    void reuCitadoVeASuaCitacaoENaoADaOutraParte() {
        processo.setFaseAtual(FaseProcessual.CITACAO);
        posicao(TipoPolo.PASSIVO, null);

        assertThat(service.timelineVisualProcesso(42L).get("faseAtual")).isEqualTo("Sua citação");
    }

    @Test
    void reuEmFaseDeRespostaVeOPrazoComoSeu() {
        processo.setFaseAtual(FaseProcessual.RESPOSTA);
        posicao(TipoPolo.PASSIVO, null);

        assertThat(service.timelineVisualProcesso(42L).get("faseAtual")).isEqualTo("Prazo para a sua resposta");
    }

    @Test
    void autorVeACitacaoDaOutraParte() {
        processo.setFaseAtual(FaseProcessual.CITACAO);
        posicao(TipoPolo.ATIVO, null);

        assertThat(service.timelineVisualProcesso(42L).get("faseAtual")).isEqualTo("Citação da outra parte");
    }

    @Test
    void procuracaoSoEPedidaNaAudienciaQuandoHaAdvogado() {
        posicao(TipoPolo.ATIVO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO);
        assertThat((List<?>) service.orientacaoAudiencia(42L).get("documentosNecessarios"))
                .noneMatch(doc -> doc.toString().contains("Procuração"));

        posicao(TipoPolo.ATIVO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO);
        assertThat(service.orientacaoAudiencia(42L).get("documentosNecessarios")).isEqualTo(List.of(
                "Documento de identidade com foto", "CPF", "Comprovante de endereço", "Documentos do processo",
                "Procuração do advogado"));
    }

    @Test
    void resumoMostraACitacaoComoDoReu() {
        processo.setFaseAtual(FaseProcessual.CITACAO);
        posicao(TipoPolo.PASSIVO, null);

        assertThat(service.exportarResumoProcesso(42L)).containsEntry("faseAtual", "Sua citação");
    }

    @Test
    void painelInicialUsaOPoloDoCidadaoEmCadaProcesso() {
        processo.setFaseAtual(FaseProcessual.CITACAO);
        when(processoRepository.findByCidadaoCpf(org.mockito.ArgumentMatchers.eq(CPF), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(processo)));
        when(posicaoService.polo(processo, CPF)).thenReturn(TipoPolo.PASSIVO);

        assertThat(service.bootstrapDashboard().timeline()).singleElement()
                .extracting(CidadaoDashboardEnhancedService.ProcessoResumoCidadao::faseSimples).isEqualTo("Sua citação");
    }

    @Test
    void faseSemTextoProprioUsaADescricaoDaFase() {
        processo.setFaseAtual(FaseProcessual.AUDIENCIA_CUSTODIA);

        assertThat(service.timelineVisualProcesso(42L).get("faseAtual"))
                .isEqualTo(FaseProcessual.AUDIENCIA_CUSTODIA.getDescricao());
    }

    @Test
    void representacaoDesconhecidaMantemAProcuracaoSeHouver() {
        assertThat(service.orientacaoAudiencia(42L).get("documentosNecessarios")).asString()
                .contains("Procuração do advogado, se houver");
    }

    @Test
    void descricaoApresentaAFaseAtualDoProcesso() {
        processo.setFaseAtual(FaseProcessual.CITACAO);
        posicao(TipoPolo.PASSIVO, null);

        assertThat(service.timelineVisualProcesso(42L).get("descricaoSimples")).asString()
                .startsWith("Fase atual do seu processo: Sua citação.");
    }
}
