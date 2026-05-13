package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardRouter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbOperationalRouteCanonicalizationTest {

    @Test
    void perfilServidorDeveApontarParaSuperficieCanonicaDaSecretaria() {
        PerfilDashboardRouter router = new PerfilDashboardRouter();

        String endpointServidor = router.route(TipoUsuario.SERVIDOR).dashboardEndpoint();
        String endpointServidorForum = router.route(TipoUsuario.SERVIDOR_FORUM).dashboardEndpoint();

        assertEquals(OperationalApiRoutes.secretariatOperationalSnapshot(), endpointServidor);
        assertEquals(OperationalApiRoutes.secretariatOperationalSnapshot(), endpointServidorForum);
    }

    @Test
    void superficieOperacionalNaoDeveConterWorkspaceFantasmaDaSecretariaNemAliasRedundanteDoForum() {
        String forumDeskController = ApiSurfaceTestSupport.read(Path.of(
                "src/main/java/com/tcc/pjb/backend/controller/forum/ForumDeskController.java"));
        String perfilDashboardRouter = ApiSurfaceTestSupport.read(Path.of(
                "src/main/java/com/tcc/pjb/backend/service/dashboard/PerfilDashboardRouter.java"));
        String mainSources = ApiSurfaceTestSupport.read(Path.of(
                "src/main/java/com/tcc/pjb/backend/core/operational/OperationalApiRoutes.java"));

        assertFalse(forumDeskController.contains("@GetMapping(\"/desks\")"));
        assertFalse(perfilDashboardRouter.contains("/api/v1/secretariat/workspace"));
        assertTrue(mainSources.contains("SECRETARIAT_OPERATIONAL_BASE"));
        assertTrue(mainSources.contains("FORUM_BASE"));
    }

    @Test
    void superficiesOperacionaisCentraisDevemUsarClasseCanonicaDeRotas() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumDeskController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/forum/ForumHabilitacaoController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/queue/SecretariatQueueController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/queue/SecretariatSseController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatDossieController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatMinutaJuntadaController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/SecretariatJulgamentoController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/security/SecretariatBreakGlassController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/access/SecretariatProcessoVisibilidadePessoalController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/secretariat/operational/ServidorSecretariaOperacionalController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/pendencia/OperationalPendingDashboardController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/workspace/ProcessualParticipacaoWorkspaceController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao/submission/ProcessualParticipacaoSubmissionController.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/catalog/ForumInstitutionalPanelBlueprintCatalog.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/catalog/MinisterioPublicoInstitutionalPanelBlueprintCatalog.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/catalog/DefensoriaEProcuradoriaInstitutionalPanelBlueprintCatalog.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/catalog/ApoioInstitucionalPanelBlueprintCatalog.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/entry/application/InstitutionalEntryContextApplicationService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/affiliation/application/InstitutionalTrustMatrixApplicationService.java"),
                Path.of("src/main/java/com/tcc/pjb/backend/service/dashboard/PerfilDashboardRouter.java")
        );

        List<String> missing = files.stream()
                .filter(path -> !ApiSurfaceTestSupport.read(path).contains("OperationalApiRoutes"))
                .map(Path::toString)
                .toList();

        assertTrue(missing.isEmpty(), "Arquivos operacionais ainda fora da rota canônica centralizada: " + missing);
    }


    @Test
    void rotasDeParticipacaoAtivaEHabilitacaoEJulgamentoDevemSerReconhecidasComoOperacionais() {
        assertAll(
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.processualParticipacaoWorkspace(1L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.processualParticipacaoProtocolar(1L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.processualParticipacaoSubmissoes(1L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.forumHabilitacoesPendentes())),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.forumHabilitacaoDeferir("42"))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.secretariatJulgamentoProcesso(1L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.secretariatJulgamentoStatus(2L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.secretariatJulgamentoVotos(2L))),
                () -> assertTrue(OperationalApiRoutes.isOperationalPath(
                        OperationalApiRoutes.secretariatJulgamentoAcordao(2L)))
        );
    }
    @Test
    void todasAsBasesOperacionaisRegistradasDevemSerReconhecidasPelaRegraCanonica() {
        assertTrue(OperationalApiRoutes.operationalBases().stream()
                .allMatch(OperationalApiRoutes::isOperationalPath));
    }

}
