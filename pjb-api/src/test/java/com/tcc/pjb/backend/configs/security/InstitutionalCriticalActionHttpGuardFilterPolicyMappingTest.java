package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalDocumentSecurityGate;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Cobertura parametrizada de TODAS as 30 policies mapeadas em
 * {@link InstitutionalCriticalActionHttpGuardFilter#resolvePolicy}. Cada cenario constroi uma
 * URL representativa de cada branch do resolvePolicy, dispara o filtro com o gate mockado e
 * verifica que o filtro derivou o {@code operationCode} e o {@code act} corretos — usando
 * ArgumentCaptor via {@code verify(gate).enforce(...)}. Sem Spring context, sem Postgres,
 * sem JWT — teste puro de logica do filtro, milissegundos por caso.
 *
 * <p>Complementa (nao substitui) as ITs end-to-end de familia (InstitutionalRecursalGateIT,
 * InstitutionalMagistraturaGateIT), que provam a ordem de execucao na cadeia real do Spring
 * Security. Aqui a garantia e: se alguem alterar o resolvePolicy sem atualizar os asserts,
 * o cenario correspondente quebra imediatamente — nao ha regressao silenciosa de path.
 */
class InstitutionalCriticalActionHttpGuardFilterPolicyMappingTest {

    static Stream<Arguments> allPolicies() {
        return Stream.of(
                Arguments.of("/api/v1/recursal/processos/42/recurso", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "RECURSAL_UNIFICADO"),
                Arguments.of("/api/v1/mp/manifestacao/17", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_MANIFESTACAO"),
                Arguments.of("/api/v1/mp/parecer/17", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_PARECER"),
                Arguments.of("/api/v1/mp/requisicao/diligencia/9", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MP_REQUISICAO_DILIGENCIA"),
                Arguments.of("/api/v1/extrajudicial/escrituras", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "EXTRAJUDICIAL_LAVRAR_ESCRITURA"),
                Arguments.of("/api/v1/extrajudicial/cartorios/tabelionato-x/vincular-processo/7", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "EXTRAJUDICIAL_VINCULAR_PROCESSO"),
                Arguments.of("/api/v1/psicossocial/processos/42/parecer", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PSICOSSOCIAL_PARECER"),
                Arguments.of("/api/v1/psicossocial/processos/42/relatorio", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PSICOSSOCIAL_RELATORIO"),
                Arguments.of("/api/v1/distribuicao/processual/processos/99/redistribuicao", InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL, "DISTRIBUICAO_REDISCRITICA"),
                Arguments.of("/api/v1/secretaria/especializada/processos/99/redistribuicao", InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL, "SECRETARIA_REDISCRITICA"),
                Arguments.of("/api/v1/procuradoria/operacional/processos/15/parecer", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PROCURADORIA_PARECER"),
                Arguments.of("/api/v1/perito/operacional/processos/33/laudo", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PERITO_LAUDO"),
                Arguments.of("/api/v1/perito/laudos/laudo-uuid/entrega", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "PERITO_ENTREGA_LAUDO"),
                Arguments.of("/api/v1/oficial-justica/processos/44/oficios", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_OFICIO"),
                Arguments.of("/api/v1/oficial-justica/processos/44/oficios/resposta", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_RESPOSTA_OFICIO"),
                Arguments.of("/api/v1/oficial-justica/mandados/mnd-1/certidoes/auto", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_CERTIDAO_AUTO"),
                Arguments.of("/api/v1/oficial-justica/mandados/mnd-1/formalizacao-processual", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_FORMALIZACAO_PROCESSUAL"),
                Arguments.of("/api/v1/oficial-justica/mandados/mnd-1/anexacoes-institucionais", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_ANEXACAO_INSTITUCIONAL"),
                Arguments.of("/api/v1/oficial-justica/mandados/mnd-1/malha-institucional/expedicoes", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "OFICIAL_DISPATCH_INSTITUCIONAL"),
                Arguments.of("/api/v1/oficial-justica/mandados/mnd-1/encerramento-soberano", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "OFICIAL_ENCERRAMENTO_SOBERANO"),
                Arguments.of("/api/v1/delegado/requisicao/diligencia", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "DELEGADO_REQUISICAO_DILIGENCIA"),
                Arguments.of("/api/v1/delegado/diligencias/dil-1/certidoes/auto", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "DELEGADO_CERTIDAO_AUTO"),
                Arguments.of("/api/v1/delegado/diligencias/dil-1/malha-institucional/expedicoes", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_DISPATCH_INSTITUCIONAL"),
                Arguments.of("/api/v1/delegado/diligencias/dil-1/formalizacao-processual", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_FORMALIZACAO_PROCESSUAL"),
                Arguments.of("/api/v1/delegado/diligencias/dil-1/anexacoes-institucionais", InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "DELEGADO_ANEXACAO_INSTITUCIONAL"),
                Arguments.of("/api/v1/juiz/gabinete-decisoes/processos/8/despacho", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_DESPACHO"),
                Arguments.of("/api/v1/juiz/gabinete-decisoes/processos/8/sentenca", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_SENTENCA"),
                Arguments.of("/api/v1/juiz/gabinete-decisoes/processos/8/certidao-tj", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "JUIZ_CERTIDAO_TJ"),
                Arguments.of("/api/v1/magistratura/processos/8/atos", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "MAGISTRATURA_ATO_PROCESSUAL"),
                Arguments.of("/api/v1/processo/transito-julgado/processos/8/certidao", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "TRANSITO_CERTIDAO"),
                Arguments.of("/api/v1/processo/transito-julgado/processos/8/homologacao-expropriacao", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, "TRANSITO_HOMOLOGACAO_EXPROPRIACAO")
        );
    }

    @ParameterizedTest(name = "{2} <- POST {0}")
    @MethodSource("allPolicies")
    void filtroDerivaPolicyCorretaParaCadaPathProtegido(String path, InstitutionalSensitiveAct expectedAct, String expectedOperationCode) throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        when(gateService.enforce(any(), any(), eq(expectedAct), eq(expectedOperationCode), eq(true)))
                .thenReturn(new InstitutionalDocumentSecurityGate(expectedOperationCode, "aff", "nom", null, null, true, true, true, true, true, false, false, List.of(), List.of(), Instant.now()));
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        verify(gateService).enforce(any(), any(), eq(expectedAct), eq(expectedOperationCode), eq(true));
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("operationCode nos headers deve refletir a policy derivada")
                .isEqualTo(expectedOperationCode);
    }

    @ParameterizedTest(name = "GET {0} nao dispara gate")
    @MethodSource("allPolicies")
    void metodoNaoPost_naoDisparaGate(String path, InstitutionalSensitiveAct ignoredAct, String ignoredOperationCode) throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(gateService);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation")).isNull();
    }

    @ParameterizedTest(name = "path livre {0} nao dispara gate")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/api/v1/health",
            "/api/v1/advogado/cockpit/snapshot",
            "/api/v1/mp/painel",
            "/api/v1/defensor/assistidos",
            "/api/v1/oficial-justica/processos/44/oficios/x/y",
            "/api/v1/juiz/gabinete-decisoes/processos/8/nao-mapeado",
            "/api/v1/processo/transito-julgado/processos/8/nao-existe"
    })
    void pathForaDoConjuntoProtegido_naoDisparaGate(String path) throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(gateService);
    }

    @SuppressWarnings("unchecked")
    private InstitutionalCriticalActionHttpGuardFilter filterWith(InstitutionalDocumentSecurityGateApplicationService gateService) {
        ObjectProvider<InstitutionalDocumentSecurityGateApplicationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(gateService);
        return new InstitutionalCriticalActionHttpGuardFilter(provider);
    }
}
