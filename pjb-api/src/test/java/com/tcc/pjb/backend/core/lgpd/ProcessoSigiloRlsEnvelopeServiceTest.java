package com.tcc.pjb.backend.core.lgpd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProcessoSigiloRlsEnvelopeServiceTest {

    private final ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
    private final ProcessoSigiloRlsEnvelopeService service = new ProcessoSigiloRlsEnvelopeService(processoRepository, new DataClassificationCatalog());

    @Test
    void deveMaterializarEnvelopeSigilosoComStepUpEscopoETrilhaQualificada() {
        when(processoRepository.findById(9L)).thenReturn(Optional.of(Processo.builder()
                .id(9L)
                .tribunal("TJCE")
                .unidadeJudiciariaCodigo("UNID-44")
                .nivelSigilo(NivelSigilo.SIGILO_N3)
                .build()));

        ProcessoSigiloRlsEnvelope envelope = service.materialize(9L, "GET");

        assertThat(envelope.tableName()).isEqualTo("tb_processo");
        assertThat(envelope.requiredSigiloClearance()).isEqualTo("SIGILO_N3");
        assertThat(envelope.requiredTribunalCode()).isEqualTo("TJCE");
        assertThat(envelope.requiredUnitCode()).isEqualTo("UNID-44");
        assertThat(envelope.rlsScopeKey()).isEqualTo("PROC_SIGILO|TJCE|UNID-44|SIGILO_N3");
        assertThat(envelope.requiresStepUp()).isTrue();
        assertThat(envelope.requiresQualifiedCertificate()).isTrue();
        assertThat(envelope.readOnlyRecommended()).isTrue();
        assertThat(envelope.classificationCategories()).contains("DADOS_JUDICIAIS", "DADOS_SENSIVEIS");
        assertThat(envelope.findings()).isEmpty();
    }

    @Test
    void deveApontarAchadosQuandoProcessoRestritoNaoTrazEnvelopeInstitucionalMinimo() {
        when(processoRepository.findById(33L)).thenReturn(Optional.of(Processo.builder()
                .id(33L)
                .nivelSigilo(NivelSigilo.SEGREDO_ESTADO)
                .build()));

        ProcessoSigiloRlsEnvelope envelope = service.materialize(33L, "POST");

        assertThat(envelope.requiredSigiloClearance()).isEqualTo("SEGREDO_ESTADO");
        assertThat(envelope.requiresStepUp()).isTrue();
        assertThat(envelope.requiresQualifiedCertificate()).isTrue();
        assertThat(envelope.readOnlyRecommended()).isTrue();
        assertThat(envelope.findings())
                .contains("PROCESSO_SIGILOSO_SEM_TRIBUNAL_MATERIALIZADO",
                        "PROCESSO_SIGILOSO_SEM_UNIDADE_MATERIALIZADA",
                        "SCOPE_RLS_SIGILOSO_INCOMPLETO",
                        "FLOW_REQUER_TRILHA_INSTITUCIONAL_MAXIMA");
    }
}
