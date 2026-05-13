package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.entity.ui.UsuarioCalculoExperiencePreference;
import com.tcc.pjb.backend.repository.ui.UsuarioCalculoExperiencePreferenceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class CalculoJudicialExperiencePreferenceServiceTest {

    private final CalculoJudicialFrontendContractService contractService = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());
    private final UsuarioCalculoExperiencePreferenceRepository repository = mock(UsuarioCalculoExperiencePreferenceRepository.class);
    private final CalculoJudicialExperiencePreferenceService service = new CalculoJudicialExperiencePreferenceService(repository, contractService);

    @Test
    void deveCairNoManualParaPerfilTecnicoSemPreferenciaPersistida() {
        var response = service.resolve(null, CalculoJudicialSolicitantePerfil.ADVOGADO);

        assertThat(response.resolvedExperienceMode()).isEqualTo("manual_tradicional");
        assertThat(response.source()).isEqualTo("PROFILE_DEFAULT");
        assertThat(response.selector()).containsEntry("savePreferenceRoute", "/api/v1/processual/calculos/experiencia/preferencia");
    }

    @Test
    void deveAplicarPreferenciaGlobalPorDominioQuandoExistir() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("advogado@pjb", "n/a");
        UsuarioCalculoExperiencePreference pref = new UsuarioCalculoExperiencePreference("advogado@pjb", null, "TRABALHISTA_CLT", "assistido_com_ia", "USER", null, null, null, null, null, null, false);
        when(repository.findAllByPrincipalKeyAndEquipeAtivaIdIsNullOrderByUpdatedAtDescIdDesc("advogado@pjb")).thenReturn(List.of(pref));

        var response = service.resolve(auth, CalculoJudicialSolicitantePerfil.ADVOGADO, "TRABALHISTA_CLT");

        assertThat(response.resolvedExperienceMode()).isEqualTo("assistido_com_ia");
        assertThat(response.domainCode()).isEqualTo("TRABALHISTA_CLT");
        assertThat(response.source()).isEqualTo("USER_DOMAIN_PREFERENCE");
        assertThat(response.domainScoped()).isTrue();
    }

    @Test
    void deveAplicarPoliticaInstitucionalContextualPorClasseETipoDeCausa() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("advogado@pjb", "n/a");
        UsuarioCalculoExperiencePreference policy = new UsuarioCalculoExperiencePreference("__TEAM_POLICY__", 99L, "TRABALHISTA_CLT", "manual_tradicional", "TEAM_POLICY_CONTEXT", "TRABALHISTA", "RECLAMACAO_TRABALHISTA", "VERBAS_RESCISORIAS", "CONTENCIOSO_MASSIFICADO", "TRT7", "PJE", true);
        when(repository.findAllByPrincipalKeyAndEquipeAtivaIdOrderByUpdatedAtDescIdDesc("advogado@pjb", 99L)).thenReturn(List.of());
        when(repository.findAllByPrincipalKeyAndEquipeAtivaIdIsNullOrderByUpdatedAtDescIdDesc("advogado@pjb")).thenReturn(List.of());
        when(repository.findAllByPrincipalKeyAndEquipeAtivaIdOrderByUpdatedAtDescIdDesc("__TEAM_POLICY__", 99L)).thenReturn(List.of(policy));

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader(com.tcc.pjb.backend.configs.EquipeSwitchInterceptor.HEADER_EQUIPE_ID, "99");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            var response = service.resolve(auth, CalculoJudicialSolicitantePerfil.ADVOGADO, "TRABALHISTA_CLT", new CalculoJudicialExperienceContext("trabalhista", "reclamacao trabalhista", "verbas rescisorias", "contencioso massificado", "trt7", "pje"));
            assertThat(response.resolvedExperienceMode()).isEqualTo("manual_tradicional");
            assertThat(response.source()).isEqualTo("TEAM_DOMAIN_POLICY");
            assertThat(response.institutionalPolicyApplied()).isTrue();
            assertThat(response.policyContext()).containsEntry("classeProcessual", "RECLAMACAO_TRABALHISTA");
            assertThat(response.policyContext()).containsEntry("tribunal", "TRT7");
            assertThat(response.policyContext()).containsEntry("sistemaOrigem", "PJE");
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void deveForcarModoManualParaContextoLegadoOuMassificadoSemPreferenciaPersistida() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("advogado@pjb", "n/a");

        var response = service.resolve(auth, CalculoJudicialSolicitantePerfil.CIDADAO, "FAZENDA_TRIBUTARIO", new CalculoJudicialExperienceContext(null, null, null, "contadoria", "TJSP", "eproc legado"));

        assertThat(response.resolvedExperienceMode()).isEqualTo("manual_tradicional");
        assertThat(response.source()).isEqualTo("PROFILE_DEFAULT");
        assertThat(response.policyContext()).containsEntry("tribunal", "TJSP");
        assertThat(response.policyContext()).containsEntry("sistemaOrigem", "EPROC_LEGADO");
    }

}
