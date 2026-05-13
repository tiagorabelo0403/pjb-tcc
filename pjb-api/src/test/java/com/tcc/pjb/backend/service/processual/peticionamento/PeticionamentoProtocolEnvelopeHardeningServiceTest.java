
package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoProtocolEnvelopeHardeningServiceTest {

    @Test
    void deveGerarHashDeterministicoComMesmoContexto() {
        PeticionamentoProtocolEnvelopeHardeningService service = new PeticionamentoProtocolEnvelopeHardeningService();

        LaianeProtocolPackageDto protocolPackage = LaianeProtocolPackageDto.builder()
                .title("Pacote previdenciário")
                .integrityHash("hash-protocolo")
                .status("READY")
                .externalProtocolRef("TRF5-123")
                .build();

        PeticionamentoProtocolEnvelopeHardeningService.ResolveRequest request =
                new PeticionamentoProtocolEnvelopeHardeningService.ResolveRequest(
                        "sessao-123",
                        99L,
                        "Benefício previdenciário",
                        "PREVIDENCIARIO",
                        "COMUM",
                        "BENEFICIO_POR_INCAPACIDADE",
                        "FEDERAL",
                        TipoUsuario.PROCURADORIA_FEDERAL,
                        "PROCURACAO",
                        "PUBLICO",
                        true,
                        true,
                        true,
                        "payload-hash",
                        List.of("peticao.pdf", "cnis.pdf", "laudo.pdf"),
                        "PETICIONAMENTO_BATCH_LEITURA_GUARDADA_V2",
                        List.of(),
                        "VERIFICADOR_PREVIDENCIARIO_BENEFICIO_V3",
                        "PREVIDENCIARIO_BENEFICIO",
                        List.of(),
                        protocolPackage
                );

        PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport first = service.harden(request);
        PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport second = service.harden(request);

        assertEquals(first.deterministicHash(), second.deterministicHash());
        assertTrue(first.finalGates().contains("PACOTE_PROTOCOLO_BASE_OK"));
        assertTrue(first.finalGates().contains("VERIFICADOR_SUBESPECIE_FINAL_OK"));
    }

    @Test
    void deveReterEnvelopeQuandoHaGateBloqueante() {
        PeticionamentoProtocolEnvelopeHardeningService service = new PeticionamentoProtocolEnvelopeHardeningService();

        PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport report = service.harden(
                new PeticionamentoProtocolEnvelopeHardeningService.ResolveRequest(
                        "sessao-456",
                        null,
                        "Mandado de segurança",
                        "PUBLICO",
                        "ESPECIAL",
                        "MANDADO_DE_SEGURANCA",
                        "FEDERAL",
                        TipoUsuario.ADVOGADO,
                        "PROCURACAO",
                        "SEGREDO_JUSTICA",
                        false,
                        true,
                        false,
                        "payload-hash-2",
                        List.of("peticao.pdf"),
                        "PETICIONAMENTO_BATCH_LEITURA_GUARDADA_V2",
                        List.of("A trilha documental não contém instrumento formal de representação exigido para este peticionamento."),
                        "VERIFICADOR_MANDADO_SEGURANCA_V3",
                        "MANDADO_DE_SEGURANCA",
                        List.of("Mandado de segurança sem prova pré-constituída suficiente."),
                        null
                )
        );

        assertTrue(report.blocking());
        assertTrue(report.finalGates().contains("REPRESENTACAO_VALIDADA_PENDENTE"));
        assertTrue(report.finalGates().contains("PACOTE_PROTOCOLO_BASE_PENDENTE"));
    }
}
