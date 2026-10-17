package com.tcc.pjb.backend.core.security.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfessionalInstitutionalAccessGrantServiceTest {

    private final ProfessionalInstitutionalAccessGrantRepository repository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
    private final ProfessionalInstitutionalAccessGrantService service = new ProfessionalInstitutionalAccessGrantService(repository);
    private final Usuario desembargador = new Usuario();
    private final Processo processo = Processo.builder().id(70L).build();

    @BeforeEach
    void setUp() {
        desembargador.setId(5L);
        when(repository.findByUsuario_IdAndAtivoTrueAndProcessoIsNullOrderByIdDesc(5L)).thenReturn(List.of());
    }

    @Test
    void relatoriaDoProcessoVemDaConsultaPorProcessoSemTetoDeGrants() {
        ProfessionalInstitutionalAccessGrant relatoria = grant(ProfessionalAccessGrantType.RELATORIA_PROCESSO);
        relatoria.setProcesso(processo);
        when(repository.findByUsuario_IdAndAtivoTrueAndProcesso_IdOrderByIdDesc(5L, 70L)).thenReturn(List.of(relatoria));

        var resolucao = service.resolveApplicable(desembargador, processo, ProfessionalActorClass.MAGISTRATURA);

        assertThat(resolucao.relatoria()).isTrue();
        verify(repository, never()).findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(anyLong());
    }

    @Test
    void lotacaoNumaUnidadeNaoValeParaProcessoDeOutraUnidade() {
        ProfessionalInstitutionalAccessGrant lotacao = grant(ProfessionalAccessGrantType.LOTACAO_UNIDADE);
        lotacao.setUnidadeJudiciariaCodigo("TJCE-FORTALEZA-1VC");
        when(repository.findByUsuario_IdAndAtivoTrueAndProcessoIsNullOrderByIdDesc(5L)).thenReturn(List.of(lotacao));

        Processo daOutraVara = Processo.builder().id(71L).unidadeJudiciariaCodigo("TJCE-FORTALEZA-2VC").build();
        Processo daMesmaVara = Processo.builder().id(72L).unidadeJudiciariaCodigo("TJCE-FORTALEZA-1VC").build();

        assertThat(service.resolveApplicable(desembargador, daOutraVara, ProfessionalActorClass.MAGISTRATURA).grants()).isEmpty();
        assertThat(service.resolveApplicable(desembargador, daMesmaVara, ProfessionalActorClass.MAGISTRATURA).grants()).containsExactly(lotacao);
    }

    private static ProfessionalInstitutionalAccessGrant grant(ProfessionalAccessGrantType tipo) {
        ProfessionalInstitutionalAccessGrant grant = new ProfessionalInstitutionalAccessGrant();
        grant.setGrantType(tipo);
        grant.setActorClass(ProfessionalActorClass.MAGISTRATURA);
        grant.setAtivo(Boolean.TRUE);
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.APPROVED);
        grant.setInicioVigencia(LocalDateTime.of(2026, 1, 1, 0, 0));
        return grant;
    }
}
