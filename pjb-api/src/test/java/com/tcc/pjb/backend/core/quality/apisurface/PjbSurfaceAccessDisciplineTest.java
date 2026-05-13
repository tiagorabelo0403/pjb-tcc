package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbSurfaceAccessDisciplineTest {

    @Test
    void controllersAtualizadosNaoDevemExporContratosInternosOuMapCru() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of(""));
        var aggregate = service.auditar();
        Set<String> targets = Set.of(
                "src/main/java/com/tcc/pjb/backend/controller/offline/PwaOfflineController.java",
                "src/main/java/com/tcc/pjb/backend/controller/audiencia/AudienciaWebRtcController.java",
                "src/main/java/com/tcc/pjb/backend/controller/juiz/JudicialVoiceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/advogado/LaianePeticaoInicialDraftController.java",
                "src/main/java/com/tcc/pjb/backend/controller/consulta/ProntuarioNacionalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/ui/WcagAaaAuditController.java",
                "src/main/java/com/tcc/pjb/backend/controller/psicossocial/PsicossocialRiscoController.java",
                "src/main/java/com/tcc/pjb/backend/controller/publico/PainelNacionalJusticaController.java",
                "src/main/java/com/tcc/pjb/backend/controller/publico/TribunalPerfilController.java",
                "src/main/java/com/tcc/pjb/backend/controller/publico/AtlasAcessoJusticaController.java",
                "src/main/java/com/tcc/pjb/backend/controller/intelligence/RecursalAttachmentController.java",
                "src/main/java/com/tcc/pjb/backend/controller/extrajudicial/CartorioExtrajudicialEscrituraController.java",
                "src/main/java/com/tcc/pjb/backend/controller/leilao/LeilaoJudicialAnalyticsController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AtlasAcessoJusticaAdminController.java"
        );
        Set<String> forbidden = Set.of(
                "controller.inline.dto",
                "service.nested.exposure",
                "controller.nested.service.contract",
                "controller.raw.map.response",
                "controller.wildcard.response",
                "controller.internal.domain.import",
                "controller.entity.exposure",
                "controller.aggregate.exposure"
        );
        var violations = aggregate.issues().stream()
                .filter(issue -> targets.contains(issue.location()))
                .filter(issue -> forbidden.contains(issue.code()))
                .map(issue -> issue.location() + ':' + issue.code())
                .collect(Collectors.toSet());
        assertTrue(violations.isEmpty(), violations.toString());
    }
}
