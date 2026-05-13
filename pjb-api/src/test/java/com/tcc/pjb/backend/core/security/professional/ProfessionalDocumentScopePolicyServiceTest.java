package com.tcc.pjb.backend.core.security.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfessionalDocumentScopePolicyServiceTest {

    @Test
    void professionalWithoutMandateCanReadPublicDecisionButNotRestrictedEvidence() {
        LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
        SigiloAccessService sigiloAccessService = mock(SigiloAccessService.class);
        ProfessionalInstitutionalAccessGrantService grantService = mock(ProfessionalInstitutionalAccessGrantService.class);
        when(procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(10L, 44L, LaianeProcuracaoStatus.ATIVA)).thenReturn(false);
        when(grantService.resolveApplicable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ProfessionalInstitutionalAccessGrantService.GrantResolution.empty());
        ProfessionalProcessAccessVectorService accessVectorService = new ProfessionalProcessAccessVectorService(procuracaoRepository, sigiloAccessService, grantService);
        ProfessionalDocumentScopePolicyService service = new ProfessionalDocumentScopePolicyService(accessVectorService);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Advogada");
        usuario.setEmail("adv@pjb.br");
        usuario.setOab("CE12345");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);
        Processo processo = Processo.builder().id(44L).uf("CE").comarca("Morada Nova").nivelSigilo(NivelSigilo.PUBLICO).build();

        DocumentoProcessual decisao = DocumentoProcessual.builder().id(UUID.randomUUID()).titulo("Decisão interlocutória").build();
        DocumentoProcessual laudo = DocumentoProcessual.builder().id(UUID.randomUUID()).titulo("Laudo pericial médico sigiloso").nivelSigilo(NivelSigilo.SIGILO_N2).build();

        assertThat(service.decide(usuario, processo, decisao).allowed()).isTrue();
        assertThat(service.decide(usuario, processo, laudo).allowed()).isFalse();
    }
}
