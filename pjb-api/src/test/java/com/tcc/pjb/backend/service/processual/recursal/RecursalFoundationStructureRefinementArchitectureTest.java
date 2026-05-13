package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.processual.recursal.embargos.EmbargosDeclaracaoFoundationController;
import com.tcc.pjb.backend.controller.processual.recursal.foundation.RecursalFoundationController;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalFoundationResponse;
import com.tcc.pjb.backend.service.processual.recursal.embargos.EmbargosDeclaracaoFoundationService;
import com.tcc.pjb.backend.service.processual.recursal.foundation.RecursalFoundationService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalFoundationStructureRefinementArchitectureTest {

    @Test
    void foundationEEmbargosDevemViverEmSubpacotesDedicados() {
        assertThat(RecursalFoundationService.class.getPackageName()).endsWith(".foundation");
        assertThat(EmbargosDeclaracaoFoundationService.class.getPackageName()).endsWith(".embargos");
        assertThat(RecursalFoundationResponse.class.getPackageName()).endsWith(".foundation");
        assertThat(EmbargosDeclaracaoFoundationResponse.class.getPackageName()).endsWith(".embargos");
        assertThat(RecursalFoundationController.class.getPackageName()).endsWith(".foundation");
        assertThat(EmbargosDeclaracaoFoundationController.class.getPackageName()).endsWith(".embargos");
    }

    @Test
    void rotasRecursaisDevemSerCentralizadas() {
        assertThat(RecursalRoutes.BASE).isEqualTo("/api/v1/processual/recursal");
        assertThat(RecursalRoutes.EMBARGOS_DECLARACAO_PREVIEW).endsWith("/preview");
    }

    @Test
    void recursalNaoDeveEspalharFoundationOuEmbargosNaRaizErrada() throws Exception {
        Path serviceFoundation = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/foundation/RecursalFoundationService.java");
        Path serviceEmbargos = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/embargos/EmbargosDeclaracaoFoundationService.java");
        Path controllerFoundation = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/foundation/RecursalFoundationController.java");
        Path controllerEmbargos = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/embargos/EmbargosDeclaracaoFoundationController.java");

        assertThat(Files.exists(serviceFoundation)).isTrue();
        assertThat(Files.exists(serviceEmbargos)).isTrue();
        assertThat(Files.exists(controllerFoundation)).isTrue();
        assertThat(Files.exists(controllerEmbargos)).isTrue();
    }
}
