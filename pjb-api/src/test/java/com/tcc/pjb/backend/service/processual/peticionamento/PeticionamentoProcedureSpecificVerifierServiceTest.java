
package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoProcedureSpecificVerifierServiceTest {

    @Test
    void deveResolverMandadoDeSegurancaEExigirAtoCoatorEProvaPreConstituida() {
        PeticionamentoProcedureSpecificVerifierService service = new PeticionamentoProcedureSpecificVerifierService();

        PeticionamentoProcedureSpecificVerifierService.VerificationReport report = service.analyze(
                new PeticionamentoProcedureSpecificVerifierService.ResolveRequest(
                        "Mandado de segurança contra indeferimento",
                        "PUBLICO",
                        "ESPECIAL",
                        "MANDADO_DE_SEGURANCA",
                        "Direito líquido e certo",
                        "Mandado de segurança",
                        "Controle de legalidade",
                        "FEDERAL",
                        "Petição sobre ato coator e prova documental sem tratar prazo decadencial.",
                        List.of("Fato resumido"),
                        List.of("Concessão da segurança"),
                        List.of("ato_coator.pdf", "documento_oficial.pdf"),
                        true,
                        true,
                        true,
                        true,
                        false,
                        TipoUsuario.ADVOGADO
                )
        );

        assertEquals("MANDADO_DE_SEGURANCA", report.resolvedTrack());
        assertTrue(report.blocking());
        assertTrue(report.finalGates().stream().anyMatch(item -> item.contains("PROTOCOLO_SUBESPECIE_RETIDO")));
    }

    @Test
    void deveResolverFluxoPrevidenciarioBpc() {
        PeticionamentoProcedureSpecificVerifierService service = new PeticionamentoProcedureSpecificVerifierService();

        PeticionamentoProcedureSpecificVerifierService.VerificationReport report = service.analyze(
                new PeticionamentoProcedureSpecificVerifierService.ResolveRequest(
                        "BPC LOAS",
                        "PREVIDENCIARIO",
                        "COMUM",
                        "BENEFICIO_ASSISTENCIAL",
                        "BPC",
                        "LOAS",
                        "Assistencial",
                        "FEDERAL",
                        "Pedido de BPC com CadÚnico, laudo e vulnerabilidade econômica.",
                        List.of("Situação social"),
                        List.of("Concessão do BPC"),
                        List.of("cadunico.pdf", "laudo_medico.pdf"),
                        false,
                        false,
                        true,
                        true,
                        false,
                        TipoUsuario.ADVOGADO
                )
        );

        assertEquals("PREVIDENCIARIO_BPC", report.resolvedTrack());
        assertTrue(report.coveredDocuments().contains("PROVA_SOCIOECONOMICA_MINIMA"));
        assertTrue(report.coveredDocuments().contains("LAUDO_OU_RELATORIO_DE_IMPEDIMENTO"));
    }
}
