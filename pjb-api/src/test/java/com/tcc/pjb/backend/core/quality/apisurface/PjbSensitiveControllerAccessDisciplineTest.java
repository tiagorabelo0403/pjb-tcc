package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSensitiveControllerAccessDisciplineTest {

    @Test
    void controllersSensitivosDevemDeclararPreAuthorizeExplicito() throws Exception {
        List<String> arquivos = List.of(
                "src/main/java/com/tcc/pjb/backend/controller/AcordoIntelligenceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/ChatController.java",
                "src/main/java/com/tcc/pjb/backend/controller/intelligence/CaseIntelligenceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/intelligence/TriagemNacionalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/pericia/PeritoNomeacaoController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/movimentacao/MovimentacaoAdjustmentController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/topology/NationalCommunicationInstitutionalTopologyController.java",
                "src/main/java/com/tcc/pjb/backend/controller/JurisprudenciaController.java",
                "src/main/java/com/tcc/pjb/backend/controller/cidadao/CidadaoPerfilController.java",
                "src/main/java/com/tcc/pjb/backend/controller/intelligence/CompetenceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/intelligence/RecursalIntelligenceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AdminSseMetricsController.java",
                "src/main/java/com/tcc/pjb/backend/controller/forum/ForumHabilitacaoController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AtlasAcessoJusticaAdminController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/FederalismoJudicialController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/FederalismoRedistribuicaoController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AdminReplicaRoutingMetricsController.java",
                "src/main/java/com/tcc/pjb/backend/controller/security/TrustedDeviceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/security/SecurityDualApprovalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/security/PanicController.java",
                "src/main/java/com/tcc/pjb/backend/controller/security/SecurityChallengeController.java",
                "src/main/java/com/tcc/pjb/backend/controller/ProcessTwinController.java"
        );
        for (String arquivo : arquivos) {
            Path path = Path.of(arquivo);
            if (!Files.exists(path)) {
                continue;
            }
            String content = Files.readString(path);
            assertTrue(content.contains("@PreAuthorize"), arquivo + " deve declarar @PreAuthorize explicito.");
        }
    }
}
