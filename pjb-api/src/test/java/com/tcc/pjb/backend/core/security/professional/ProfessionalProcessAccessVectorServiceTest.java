package com.tcc.pjb.backend.core.security.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import org.junit.jupiter.api.Test;

class ProfessionalProcessAccessVectorServiceTest {

    @Test
    void lawyerWithoutMandateCanReadPublicCaseWithQualifiedPublicBasis() {
        LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
        SigiloAccessService sigiloAccessService = mock(SigiloAccessService.class);
        ProfessionalInstitutionalAccessGrantService grantService = mock(ProfessionalInstitutionalAccessGrantService.class);
        when(procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(10L, 44L, LaianeProcuracaoStatus.ATIVA)).thenReturn(false);
        when(grantService.resolveApplicable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ProfessionalInstitutionalAccessGrantService.GrantResolution.empty());

        ProfessionalProcessAccessVectorService service = new ProfessionalProcessAccessVectorService(procuracaoRepository, sigiloAccessService, grantService);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Advogada");
        usuario.setEmail("adv@pjb.br");
        usuario.setOab("CE12345");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);

        Processo processo = Processo.builder().id(44L).uf("CE").comarca("Morada Nova").nivelSigilo(NivelSigilo.PUBLICO).build();

        ProfessionalProcessAccessVector vector = service.resolve(usuario, processo);

        assertThat(vector.allowed()).isTrue();
        assertThat(vector.primaryBasis()).isEqualTo(ProfessionalAccessBasis.PUBLICO_QUALIFICADO_ADVOCACIA);
        assertThat(vector.allowedScopes()).contains(ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW);
        assertThat(vector.allowedScopes()).doesNotContain(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY);
    }

    @Test
    void lawyerWithoutMandateCannotReadConfidentialCase() {
        LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
        SigiloAccessService sigiloAccessService = mock(SigiloAccessService.class);
        ProfessionalInstitutionalAccessGrantService grantService = mock(ProfessionalInstitutionalAccessGrantService.class);
        when(procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(10L, 88L, LaianeProcuracaoStatus.ATIVA)).thenReturn(false);
        when(grantService.resolveApplicable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ProfessionalInstitutionalAccessGrantService.GrantResolution.empty());

        ProfessionalProcessAccessVectorService service = new ProfessionalProcessAccessVectorService(procuracaoRepository, sigiloAccessService, grantService);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Advogada");
        usuario.setEmail("adv@pjb.br");
        usuario.setOab("CE12345");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);

        Processo processo = Processo.builder().id(88L).uf("CE").comarca("Morada Nova").nivelSigilo(NivelSigilo.SIGILO_N2).build();

        ProfessionalProcessAccessVector vector = service.resolve(usuario, processo);

        assertThat(vector.allowed()).isFalse();
        assertThat(vector.reason()).contains("Autos sigilosos");
    }

    @Test
    void magistrateInSameTerritoryReceivesConfidentialCapabilities() {
        LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
        SigiloAccessService sigiloAccessService = mock(SigiloAccessService.class);
        ProfessionalInstitutionalAccessGrantService grantService = mock(ProfessionalInstitutionalAccessGrantService.class);
        when(grantService.resolveApplicable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ProfessionalInstitutionalAccessGrantService.GrantResolution.empty());
        ProfessionalProcessAccessVectorService service = new ProfessionalProcessAccessVectorService(procuracaoRepository, sigiloAccessService, grantService);

        Usuario usuario = new Usuario();
        usuario.setId(91L);
        usuario.setNome("Juiz");
        usuario.setEmail("juiz@pjb.br");
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");
        usuario.setAtivo(true);

        Processo processo = Processo.builder().id(77L).uf("CE").comarca("Morada Nova").nivelSigilo(NivelSigilo.SIGILO_N2).build();

        ProfessionalProcessAccessVector vector = service.resolve(usuario, processo);

        assertThat(vector.allowed()).isTrue();
        assertThat(vector.primaryBasis()).isEqualTo(ProfessionalAccessBasis.MAGISTRATURA_COMPETENCIA_TERRITORIAL);
        assertThat(vector.capabilities()).contains(ProfessionalCapability.VIEW_CONFIDENTIAL_CASE, ProfessionalCapability.SIGN_JUDICIAL_ACT);
        assertThat(vector.allowedScopes()).contains(ProfessionalDocumentVisibilityScope.COURT_INTERNAL, ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
    }
}
