package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.recursal.workspace.MeshBundle;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RecursalProjectionAssemblerTest {

    private final RecursalProjectionAssembler assembler = new RecursalProjectionAssembler(
            Mockito.mock(DocumentoProcessualRepository.class),
            Mockito.mock(DocumentoPaginaRepository.class)
    );

    @Test
    void deveInferirCorteSuperiorQuandoNaoHouverPlanoNemAdmissibilidade() {
        Processo processo = Processo.builder().id(88L).numeroProcesso("0001111-22.2026.8.06.0001").build();
        processo.setTribunalCodigoRoteado("TJCE");

        assertThat(assembler.resolveTargetInstanceHint(LegalAppealType.RE, MeshBundle.empty())).isEqualTo(InstanceLevel.EXTRAORDINARY);
        assertThat(assembler.resolveTargetCourtHint(processo, MeshBundle.empty(), InstanceLevel.EXTRAORDINARY)).isEqualTo("STF");
        assertThat(assembler.resolveTargetCourtHint(processo, MeshBundle.empty(), InstanceLevel.SUPERIOR)).isEqualTo("STJ");
    }

    @Test
    void deveEnriquecerWorkspaceEstrategiaComSigiloSemPerderRotas() {
        LinkedHashMap<String, Object> strategy = assembler.buildStrategy(null, LegalAppealType.APELACAO, true, false);
        LinkedHashMap<String, Object> workspace = assembler.buildWorkspaceProjection(55L, LegalAppealType.APELACAO, null);
        Map<String, Object> sigilo = Map.of(
                "status", "REVIEW_REQUIRED",
                "nivelRecomendado", "SEGREDO_DE_JUSTICA",
                "protocolSubmissionMode", "STRONG_CREDENTIAL",
                "certificateOrStrongCredentialRequired", true,
                "workspaceLeituraModo", "RESTRITO",
                "stepUpAcessoRecurso", true
        );

        assembler.enrichStrategyWithSigilo(strategy, sigilo);
        assembler.enrichWorkspaceWithSigilo(workspace, 55L, sigilo);

        assertThat(strategy)
                .containsEntry("sigiloStatus", "REVIEW_REQUIRED")
                .containsEntry("nivelSigiloRecursal", "SEGREDO_DE_JUSTICA")
                .containsEntry("protocolSubmissionMode", "STRONG_CREDENTIAL");
        assertThat(workspace)
                .containsEntry("workspaceLeituraModo", "RESTRITO")
                .containsEntry("nivelSigiloRecursal", "SEGREDO_DE_JUSTICA");
        assertThat(workspace.get("sigiloInteligente")).isEqualTo("/api/v1/processual/unificado/55/sigilo-inteligente");
        assertThat(workspace.get("sigiloNotificacoes")).isEqualTo("/api/v1/processual/unificado/55/sigilo-notificacoes");
    }
}
