package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalOperationalProfileSurfaceCoverageTest {

    @Test
    void nominationResponseMustExposeOperationalProfileMaterializedInsidePjb() {
        String dto = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalNominationResponse.java"));
        String facade = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalGovernanceSurfaceFacadeService.java"));
        assertTrue(dto.contains("NationalCommunicationInstitutionalOperationalProfileResponse operationalProfile"));
        assertTrue(facade.contains("stateBundleFacadeService.materializarPerfil") || facade.contains("operationalProfileProjectionApplicationService.materializar(item.affiliationId(), item.nominationId())"));
    }
}