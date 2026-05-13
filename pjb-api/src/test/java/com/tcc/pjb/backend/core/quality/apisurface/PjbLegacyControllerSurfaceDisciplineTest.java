package com.tcc.pjb.backend.core.quality.apisurface;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PjbLegacyControllerSurfaceDisciplineTest {

    @Test
    void controllersAtualizadosNaoExibemCoreDomainNaResposta() throws Exception {
        Path root = Path.of("src/main/java/com/tcc/pjb/backend/controller");
        List<String> files = List.of(
                "processual/surface/unificado/ProcessoUnificadoNacionalController.java",
                "processual/surface/unificado/ProcessoOrquestracaoUnificadaController.java",
                "processual/surface/evolution/ProcessoEvolucaoOperacionalController.java",
                "processual/surface/unificado/ProcessoPlataformaNacionalController.java",
                "processual/surface/governance/ProcessoGovernancaVersionadaController.java",
                "processual/surface/hardening/ProcessoFatiasSensivelController.java",
                "processual/surface/hardening/ProcessoFechamentoAvancadoController.java",
                "processual/surface/hardening/ProcessoSigiloInteligenteController.java",
                "processual/govbr/GovBrGovernancaController.java",
                "security/GovBrAuthReadinessController.java",
                "security/GovBrIdentityAssuranceController.java"
        );
        for (String file : files) {
            String source = Files.readString(root.resolve(file));
            assertFalse(source.contains(".core.") && source.contains(".domain."), file);
            assertFalse(source.contains("ResponseEntity<ProcessoUnificadoAggregate>"), file);
            assertFalse(source.contains("ResponseEntity<GovBrAccountEntryGovernanceAggregate>"), file);
        }
    }
}
