package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MagistraturaJudicialActRelatoriaFormalizationSupportTest {

    @Mock private WorkItemRepository workItemRepository;
    @Mock private InstitutionalActorRoutingService institutionalActorRoutingService;
    @Mock private RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;
    @Mock private CaseContinuityDecisionGateService caseContinuityDecisionGateService;
    @Mock private DecisionSafetyService decisionSafetyService;
    @Mock private PainelServiceCommons commons;

    private MagistraturaJudicialActRelatoriaFormalizationSupport support;

    @BeforeEach
    void setUp() {
        support = new MagistraturaJudicialActRelatoriaFormalizationSupport(
                workItemRepository,
                institutionalActorRoutingService,
                recursalQualifiedDocumentMaterializerService,
                caseContinuityDecisionGateService,
                decisionSafetyService,
                commons,
                new MagistraturaJudicialActProjectionSupport()
        );
    }

    @Test
    void registrarDespachoRelatoriaDevePersistirWorkItemEFormalizarDocumento() {
        Processo processo = Processo.builder()
                .id(55L)
                .numeroProcesso("0004321-10.2026.8.06.0001")
                .faseAtual(FaseProcessual.RECURSAL)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");

        when(institutionalActorRoutingService.colegiado(55L, "DESPACHO_RELATORIA"))
                .thenReturn(new InstitutionalActorRoutingService.InstitutionalRoute("Q1", "INBOX1", TipoUsuario.ASSESSOR_DESEMBARGADOR, "DESPACHO_RELATORIA", "topologia-1", "rationale", Map.of()));
        when(recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(eq(55L), any(), any(), any(), any(), any(), eq("DESPACHO_RELATORIA"), eq(null)))
                .thenReturn(Map.of("documentoId", 999L));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = support.registrarDespachoRelatoria(processo, usuario, "Determine-se a vista.", "CPC");

        verify(caseContinuityDecisionGateService).requireAllowed(55L, com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction.ASSINAR_DESPACHO);
        verify(decisionSafetyService).requireSafeDecisionContext(processo, usuario, "DESPACHO_RELATOR", "Determine-se a vista.", "CPC");
        verify(workItemRepository).save(any(WorkItem.class));
        verify(commons).publishUserHistory(eq(usuario), eq("DESEMBARGADOR"), eq("DESPACHO_RELATORIA_ASSINADO"), any(), eq(processo), eq(55L));
        assertThat(response).containsEntry("status", "DESPACHO_RELATORIA_REGISTRADO");
        assertThat(response).containsKey("documentoFormalAssinado");
    }
}
