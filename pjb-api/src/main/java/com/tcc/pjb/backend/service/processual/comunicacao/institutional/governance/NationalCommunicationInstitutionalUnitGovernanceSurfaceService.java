package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

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
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * governança de unidades organizacionais e lotações, plano de identidade de workload,
 * delegações de cobertura entre unidades.
 */
@Service
public class NationalCommunicationInstitutionalUnitGovernanceSurfaceService {

    private final InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService;
    private final InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService;
    private final InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalUnitGovernanceSurfaceService(
            InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService,
            InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService,
            InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.unitGovernanceApplicationService = unitGovernanceApplicationService;
        this.workloadIdentityPlanApplicationService = workloadIdentityPlanApplicationService;
        this.coverageDelegationApplicationService = coverageDelegationApplicationService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse governancaUnidades(String affiliationId) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarUnidade(String affiliationId,
                                                                                      NationalCommunicationInstitutionalManagedUnitUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.registrarUnidade(affiliationId, request));
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarLotacao(String affiliationId,
                                                                                      NationalCommunicationInstitutionalLotationUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.registrarLotacao(affiliationId, request));
    }

    public NationalCommunicationInstitutionalWorkloadIdentityPlanResponse identidadeWorkload(String affiliationId) {
        InstitutionalWorkloadIdentityPlan plan = workloadIdentityPlanApplicationService.avaliar(affiliationId);
        return governanceAssemblerSupport.toResponse(plan);
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse delegacoesCobertura(String affiliationId) {
        return governanceAssemblerSupport.toResponse(coverageDelegationApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse registrarDelegacaoCobertura(String affiliationId,
                                                                                                    NationalCommunicationInstitutionalCoverageDelegationUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(coverageDelegationApplicationService.registrar(affiliationId, request));
    }
}
