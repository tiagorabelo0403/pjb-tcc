package com.tcc.pjb.backend.core.security.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionalAccessGrantResolutionTest {

    @Test
    void defensorWithFormalDesignationReceivesConfidentialInstitutionalScope() {
        LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
        SigiloAccessService sigiloAccessService = mock(SigiloAccessService.class);
        ProfessionalInstitutionalAccessGrantService grantService = mock(ProfessionalInstitutionalAccessGrantService.class);

        ProfessionalInstitutionalAccessGrantService.GrantResolution resolution = new ProfessionalInstitutionalAccessGrantService.GrantResolution(
                List.of(),
                List.of("Designação processual • Designação institucional formal"),
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "CE / Morada Nova / TJCE"
        );
        when(grantService.resolveApplicable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(resolution);

        ProfessionalProcessAccessVectorService service = new ProfessionalProcessAccessVectorService(procuracaoRepository, sigiloAccessService, grantService);

        Usuario usuario = new Usuario();
        usuario.setId(51L);
        usuario.setNome("Defensor");
        usuario.setEmail("defensor@pjb.br");
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO);
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");
        usuario.setAtivo(true);

        Processo processo = Processo.builder().id(19L).uf("CE").comarca("Morada Nova").nivelSigilo(NivelSigilo.SIGILO_N2).build();

        ProfessionalProcessAccessVector vector = service.resolve(usuario, processo);

        assertThat(vector.allowed()).isTrue();
        assertThat(vector.primaryBasis()).isEqualTo(ProfessionalAccessBasis.DEFENSORIA_DESIGNACAO_FORMAL);
        assertThat(vector.allowedScopes()).contains(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION, ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED);
        assertThat(vector.capabilities()).contains(ProfessionalCapability.VIEW_CONFIDENTIAL_CASE, ProfessionalCapability.USE_AI_ASSIST);
    }
}
