package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalWorkloadIdentityPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalCoverageDelegationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalUnitGovernanceApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalUnitGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitUpsertRequest;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalUnitGovernanceSurfaceServiceTest {

    private final InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService = mock(InstitutionalUnitGovernanceApplicationService.class);
    private final InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService = mock(InstitutionalWorkloadIdentityPlanApplicationService.class);
    private final InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService = mock(InstitutionalCoverageDelegationApplicationService.class);
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport = mock(NationalCommunicationInstitutionalGovernanceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalUnitGovernanceSurfaceService service = new NationalCommunicationInstitutionalUnitGovernanceSurfaceService(
            unitGovernanceApplicationService, workloadIdentityPlanApplicationService,
            coverageDelegationApplicationService, governanceAssemblerSupport);

    @Test
    void governancaUnidadesDelegaComAffiliationId() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalUnitGovernanceResponse.class);
        when(unitGovernanceApplicationService.consolidar("aff-1")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.governancaUnidades("aff-1")).isSameAs(response);
    }

    @Test
    void registrarUnidadeDelegaComAffiliationIdEUpsertRequestIntacto() {
        var request = mock(NationalCommunicationInstitutionalManagedUnitUpsertRequest.class);
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalUnitGovernanceResponse.class);
        when(unitGovernanceApplicationService.registrarUnidade("aff-2", request)).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.registrarUnidade("aff-2", request)).isSameAs(response);
    }

    @Test
    void registrarLotacaoDelegaComAffiliationIdEUpsertRequestIntacto() {
        var request = mock(NationalCommunicationInstitutionalLotationUpsertRequest.class);
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalUnitGovernanceResponse.class);
        when(unitGovernanceApplicationService.registrarLotacao("aff-3", request)).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.registrarLotacao("aff-3", request)).isSameAs(response);
    }

    @Test
    void identidadeWorkloadDelegaComAffiliationId() {
        var plan = mock(InstitutionalWorkloadIdentityPlan.class);
        var response = mock(NationalCommunicationInstitutionalWorkloadIdentityPlanResponse.class);
        when(workloadIdentityPlanApplicationService.avaliar("aff-4")).thenReturn(plan);
        when(governanceAssemblerSupport.toResponse(plan)).thenReturn(response);

        assertThat(service.identidadeWorkload("aff-4")).isSameAs(response);
    }

    @Test
    void delegacoesCoberturaDelegaComAffiliationId() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalCoverageDelegationResponse.class);
        when(coverageDelegationApplicationService.consolidar("aff-5")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.delegacoesCobertura("aff-5")).isSameAs(response);
    }

    @Test
    void registrarDelegacaoCoberturaDelegaComAffiliationIdEUpsertRequestIntacto() {
        var request = mock(NationalCommunicationInstitutionalCoverageDelegationUpsertRequest.class);
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalCoverageDelegationResponse.class);
        when(coverageDelegationApplicationService.registrar("aff-6", request)).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.registrarDelegacaoCobertura("aff-6", request)).isSameAs(response);
    }
}
