package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure.InstitutionalCoverageDelegationStateRepository;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalCoverageDelegationApplicationServiceTest {

    @Test
    void registerMustMaterializeTemporaryCoverageDelegation() {
        InstitutionalCoverageDelegationStateRepository repository = mock(InstitutionalCoverageDelegationStateRepository.class);
        InstitutionalUnitGovernanceApplicationService unitService = mock(InstitutionalUnitGovernanceApplicationService.class);
        InstitutionalCoverageDelegationApplicationService service = new InstitutionalCoverageDelegationApplicationService(repository, unitService);
        InstitutionalUnitGovernanceSnapshot governance = new InstitutionalUnitGovernanceSnapshot(
                "snap-1",
                "aff-1",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FORUM",
                "CONFIGURADA",
                1,
                1,
                2,
                List.of(new InstitutionalManagedUnitEntry("FORUM-LN", "Fórum", null, "CE", "Limoeiro do Norte", "FORUM-LN:PRINCIPAL", "aff-1|FORUM-LN", "replica-ln", true, true, List.of("FORUM-LN:PRINCIPAL"), List.of("SECRETARIA"), List.of())),
                List.of(
                        new InstitutionalLotationGovernanceEntry("lot-1", null, 10L, "Servidor Origem", "FORUM-LN", "FORUM-LN:PRINCIPAL", "SECRETARIA", "SERVIDOR", "SECRETARIA", "ALTO", true, Instant.now(), null, List.of()),
                        new InstitutionalLotationGovernanceEntry("lot-2", null, 20L, "Servidor Destino", "FORUM-LN", "FORUM-LN:PRINCIPAL", "SECRETARIA", "SERVIDOR", "SECRETARIA", "ALTO", true, Instant.now(), null, List.of())
                ),
                List.of(),
                List.of(),
                Instant.now());
        when(repository.findLatestByAffiliationId("aff-1")).thenReturn(Optional.empty());
        when(unitService.consolidar("aff-1")).thenReturn(governance);
        when(repository.save(any(InstitutionalCoverageDelegationSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionalCoverageDelegationSnapshot snapshot = service.registrar("aff-1", new NationalCommunicationInstitutionalCoverageDelegationUpsertRequest(
                null,
                "lot-1",
                10L,
                null,
                "lot-2",
                20L,
                null,
                "FORUM-LN",
                "FORUM-LN:PRINCIPAL",
                "SECRETARIA",
                "SUBSTITUICAO_TEMPORARIA",
                Instant.now(),
                Instant.now().plusSeconds(86400),
                true,
                false,
                List.of("continuidade_operacional")
        ));

        assertEquals(1, snapshot.totalDelegations());
        assertEquals(1, snapshot.activeDelegations());
        assertTrue(snapshot.delegations().getFirst().active());
        assertEquals("SECRETARIA", snapshot.delegations().getFirst().laneCode());
    }
}
