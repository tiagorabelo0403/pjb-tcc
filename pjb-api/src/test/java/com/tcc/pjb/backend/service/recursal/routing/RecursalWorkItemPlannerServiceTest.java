package com.tcc.pjb.backend.service.recursal.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.AppealFiledPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.AutuationPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.ProceedingUpsert;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecursalWorkItemPlannerServiceTest {

    private CaseFileRepository caseFileRepository;
    private CaseProceedingRepository proceedingRepository;
    private RecursalCabinetAllocator cabinetAllocator;
    private RecursalRoutingProperties properties;
    private RecursalWorkItemPlannerService service;

    @BeforeEach
    void setUp() {
        caseFileRepository = mock(CaseFileRepository.class);
        proceedingRepository = mock(CaseProceedingRepository.class);
        cabinetAllocator = mock(RecursalCabinetAllocator.class);
        properties = mock(RecursalRoutingProperties.class);
        service = new RecursalWorkItemPlannerService(caseFileRepository, proceedingRepository, cabinetAllocator, properties);

        when(caseFileRepository.findByRootProcessoId(10L)).thenReturn(Optional.of(CaseFile.builder().id(1000L).build()));
        when(proceedingRepository.findFirstByLinkedProcessoId(10L)).thenReturn(Optional.empty());
        when(properties.isEmitOriginSecretaryWorkItemOnAppealFiled()).thenReturn(true);
        when(properties.isEmitTargetTriageWorkItemOnAppealFiled()).thenReturn(true);
        when(properties.resolveOriginSecretaryLane(any(), any())).thenReturn("SECRETARIA_RECURSAL");
        when(properties.resolvePriority(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(2);
        when(properties.resolveDueDays(anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(3);
    }

    @Test
    void devePlanejarEscalonamentoParaTurmaRecursalQuandoRecursoInominadoInterposto() {
        Processo processo = processoJuizado();
        AppealFiledPayload payload = new AppealFiledPayload(
                LegalAppealType.RECURSO_INOMINADO,
                "PROTO-1",
                "ADVOGADO",
                InstanceLevel.SECOND_INSTANCE,
                "TJSP",
                false,
                "recurso inominado"
        );
        CanonicalFact fact = new CanonicalFact(
                null,
                RecursalFactType.APPEAL_FILED,
                LegalIntegrationSystem.MANUAL,
                "fact-ri-10",
                processo.getNumeroUnificado(),
                payload,
                Instant.parse("2026-04-06T12:00:00Z")
        );
        RecursalPlan plan = RecursalPlan.builder()
                .addProceeding(new ProceedingUpsert(
                        "shadow-2g",
                        true,
                        CaseProceedingStatus.PREDICTED,
                        InstanceLevel.SECOND_INSTANCE,
                        "TJSP",
                        "",
                        null,
                        NivelSigilo.PUBLICO,
                        LegalIntegrationSystem.MANUAL
                ))
                .build();

        List<WorkItemSpec> specs = service.plan(processo, fact, plan);

        assertThat(specs)
                .extracting(WorkItemSpec::queueCode)
                .contains("REC:1G:TJSP:SECRETARIA_RECURSAL", "REC:2G:TJSP:TRIAGEM", "REC:2G:TJSP:TURMA_RECURSAL");
        assertThat(specs)
                .extracting(WorkItemSpec::title)
                .anyMatch(title -> title != null && title.contains("triagem/distribuição recursal"))
                .anyMatch(title -> title != null && title.contains("turma recursal"));
    }

    @Test
    void devePlanejarDistribuicaoGabineteEAssessoriaQuandoAutuadoNoTribunalSuperior() {
        Processo processo = processoComum();
        AutuationPayload payload = new AutuationPayload("5000001-12.2026.4.04.0000", InstanceLevel.SUPERIOR, "STJ", "SECRETARIA_DISTRIBUICAO_STJ");
        CanonicalFact fact = new CanonicalFact(
                null,
                RecursalFactType.AUTUATED_IN_TARGET,
                LegalIntegrationSystem.MANUAL,
                "fact-resp-10",
                processo.getNumeroUnificado(),
                payload,
                Instant.parse("2026-04-06T12:10:00Z")
        );
        when(cabinetAllocator.allocate("STJ", 1000L, "STJ", "5000001-12.2026.4.04.0000"))
                .thenReturn(new RecursalCabinetAllocator.CabinetSlot("STJ", "STJ", 2, 7, 32));

        List<WorkItemSpec> specs = service.plan(processo, fact, RecursalPlan.builder().build());

        assertThat(specs)
                .extracting(WorkItemSpec::queueCode)
                .contains(
                        "REC:STJ:STJ:DISTRIBUICAO",
                        "REC:STJ:STJ:GAB_PREV:BAND_2:SLOT_7",
                        "REC:STJ:STJ:ASSESSORIA_GABINETE"
                );
        assertThat(specs)
                .extracting(WorkItemSpec::assignedRole)
                .containsSequence(
                        TipoUsuario.SERVIDOR_FORUM,
                        TipoUsuario.MINISTRO,
                        TipoUsuario.ASSESSOR_MINISTRO
                );
    }

    private Processo processoJuizado() {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setSigla("TJSP");
        return Processo.builder()
                .id(10L)
                .numeroUnificado("1000001-12.2026.8.26.0001")
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL)
                .nivelSigilo(NivelSigilo.PUBLICO)
                .jurisdicao(jurisdicao)
                .build();
    }

    private Processo processoComum() {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setSigla("TJCE");
        return Processo.builder()
                .id(10L)
                .numeroUnificado("1000001-12.2026.8.06.0001")
                .faseAtual(FaseProcessual.RECURSAL)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .nivelSigilo(NivelSigilo.PUBLICO)
                .jurisdicao(jurisdicao)
                .build();
    }
}
