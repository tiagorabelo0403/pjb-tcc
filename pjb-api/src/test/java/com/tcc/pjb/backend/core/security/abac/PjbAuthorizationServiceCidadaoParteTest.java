package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.policy.AccessPolicy;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher;
import com.tcc.pjb.backend.core.security.access.ProcessoVinculoResolver;
import com.tcc.pjb.backend.core.security.access.VinculoProcessualDoUsuario;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

class PjbAuthorizationServiceCidadaoParteTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProcessoVinculoResolver vinculoResolver = mock(ProcessoVinculoResolver.class);
    private final Usuario cidadao = new Usuario();
    private final Processo processo = Processo.builder().id(70L).nivelSigilo(NivelSigilo.PUBLICO).build();
    private PjbAuthorizationService service;

    @BeforeEach
    void setUp() {
        cidadao.setId(1L);
        cidadao.setNome("Cidadã");
        cidadao.setEmail("cidada@exemplo.com");
        cidadao.setAtivo(true);
        cidadao.setCpf("11122233344");
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUserService.getOrNull()).thenReturn(cidadao);
        AccessPolicy policy = mock(AccessPolicy.class);
        when(policy.version()).thenReturn("abac-v1.0");
        when(policy.canReadProcesso(any(), any(), any())).thenReturn(AuthzDecision.allow("publico", "abac-v1.0"));
        PolicyRegistry registry = mock(PolicyRegistry.class);
        when(registry.active()).thenReturn(new PolicyRegistry.ActivePolicy(policy, null, "sha"));
        service = new PjbAuthorizationService(currentUserService, registry,
                mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class),
                mock(com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService.class),
                mock(com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService.class, Answers.RETURNS_DEEP_STUBS),
                mock(com.tcc.pjb.backend.platform.security.rbac.RbacGrantedRoleResolver.class),
                mock(com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService.class),
                mock(PjbAuthorizationTrailRegistry.class, Answers.RETURNS_DEEP_STUBS),
                mock(PjbAuthorizationTrailReadModelService.class, Answers.RETURNS_DEEP_STUBS),
                mock(PjbAuthorizationTrailAnalyticsRefreshQueueService.class, Answers.RETURNS_DEEP_STUBS),
                mock(com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor.class, Answers.RETURNS_DEEP_STUBS),
                mock(com.tcc.pjb.backend.core.security.GovBrAssurancePolicy.class),
                mock(com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentScopePolicyService.class),
                mock(com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository.class),
                mock(com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository.class),
                new ProcessoPartyCpfMatcher(),
                vinculoResolver);
    }

    @Test
    void cidadaoQueSoApareceNaLinhaAtivaDoPoloEReconhecidoComoParte() {
        when(vinculoResolver.vinculo(eq(cidadao), eq(processo), eq(EnumSet.of(VinculoProcessualDoUsuario.PARTE))))
                .thenReturn(Optional.of(VinculoProcessualDoUsuario.PARTE));

        assertThatCode(() -> service.requireReadProcessoAsCidadaoParte(processo)).doesNotThrowAnyException();
    }

    @Test
    void cidadaoSemCpfNoProcessoNemLinhaNoPoloENegado() {
        when(vinculoResolver.vinculo(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireReadProcessoAsCidadaoParte(processo))
                .isInstanceOf(AccessDeniedPjbException.class)
                .hasMessageContaining("não é parte");
    }
}
