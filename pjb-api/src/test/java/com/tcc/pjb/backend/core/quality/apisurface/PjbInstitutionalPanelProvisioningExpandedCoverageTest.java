package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalHearingRiteGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalOperationalDeskGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelProvisioningReadinessResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalPanelProvisioningExpandedCoverageTest {

    @Test
    void provisioningResponseMustExposeOpinionCalculatorAndHearingGovernance() {
        String response = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalPanelProvisioningReadinessResponse.java"));
        String hearingResponse = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse.java"));
        String deskResponse = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalOperationalDeskGovernanceResponse.java"));
        String support = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/support/NationalCommunicationInstitutionalFacadeSupport.java"));
        List<InstitutionalPanelBlueprintSpec> blueprints = new InstitutionalPanelBlueprintApplicationService().listar(null, null);
        assertTrue(response.contains("boolean opinionFlowReady"));
        assertTrue(response.contains("boolean calculatorReady"));
        assertTrue(response.contains("NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse hearingGovernance"));
        assertTrue(response.contains("NationalCommunicationInstitutionalOperationalDeskGovernanceResponse deskGovernance"));
        assertTrue(hearingResponse.contains("List<NationalCommunicationInstitutionalHearingRiteGovernanceResponse> riteGovernances"));
        assertTrue(deskResponse.contains("List<String> operationalDomains"));
        assertTrue(deskResponse.contains("String unitGroupingKey"));
        assertTrue(deskResponse.contains("String judicialAxis"));
        assertTrue(deskResponse.contains("String unitKind"));
        assertTrue(deskResponse.contains("List<String> deskQueues"));
        assertTrue(deskResponse.contains("List<String> assignmentBoundaries"));
        assertTrue(support.contains("toResponse(item.hearingGovernance())"));
        assertTrue(support.contains("toResponse(item.deskGovernance())"));
        assertTrue(support.contains("item.riteGovernances().stream().map(this::toResponse).toList()"));
        assertTrue(support.contains("item.opinionFlowReady()"));
        assertTrue(support.contains("item.calculatorReady()"));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_SECRETARIA_FLUXO_DIGITAL"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_SECRETARIA_ATOS_E_COMUNICACOES"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_SECRETARIA_CONCLUSOES_E_BAIXA"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("CENTRAL_MANDADOS_OPERACIONAL"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("CEJUSC_GESTAO_OPERACIONAL"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("CONTADORIA_MEMORIA_E_LIQUIDACAO"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_PROTOCOLO_DISTRIBUICAO"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_GABINETE_MAGISTRADO"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_GABINETE_ASSESSORIA"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_UPJ_COORDENACAO"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_SECRETARIA_JUIZADOS"::equals));
        assertTrue(blueprints.stream().map(InstitutionalPanelBlueprintSpec::codigo).anyMatch("FORUM_SEGUNDO_GRAU_SECRETARIA"::equals));
    }
}