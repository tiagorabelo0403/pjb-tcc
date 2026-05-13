package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralPreflightPayloadFactoryTest {

    private final NationalProceduralPreflightPayloadFactory factory = new NationalProceduralPreflightPayloadFactory();

    @Test
    void mustProjectSignatureAndProcuracaoIntoPreflightPayload() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("classe", "Procedimento Comum");
        payload.put("provas", "Procuração; Contrato");
        payload.put("assinadoDigitalmente", true);
        payload.put("tipoAcao", "ação inicial");

        Map<String, Object> out = factory.buildPreflightPayload(
                payload,
                null,
                "COMUM_ORDINARIO",
                new ProceduralCanonicalResolver.CanonicalContext(),
                "TJCE",
                "Fortaleza",
                "CE",
                null,
                "1ª Vara Cível",
                new TribunalProtocolRoutingService.RoutingDecision()
        );

        assertEquals(true, out.get("assinadoDigitalmente"));
        assertEquals(true, out.get("possuiProcuracao"));
        assertTrue(((List<?>) out.get("documentosPresentes")).contains("PROCURACAO"));
        assertTrue(((List<?>) out.get("documentosPresentes")).contains("ASSINATURA_DIGITAL"));
    }
}
