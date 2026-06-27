package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void canalTipado_quandoDocumentosTipadosPresente_usaExclusivamenteLista_fuzzyDesativado() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("provas", "Contrato de trabalho");
        payload.put("documentosTipados", List.of("PETICAO_INICIAL", "PROCURACAO", "CTPS", "CALCULO_INICIAL"));

        Map<String, Object> out = factory.buildPreflightPayload(
                payload,
                null,
                "TRABALHISTA_ORDINARIO",
                new ProceduralCanonicalResolver.CanonicalContext(),
                "TRT2",
                "São Paulo",
                "SP",
                null,
                "1ª Vara do Trabalho",
                new TribunalProtocolRoutingService.RoutingDecision()
        );

        @SuppressWarnings("unchecked")
        List<String> docs = (List<String>) out.get("documentosPresentes");
        assertEquals(List.of("PETICAO_INICIAL", "PROCURACAO", "CTPS", "CALCULO_INICIAL"), docs,
                "Canal tipado deve retornar exclusivamente a lista de TipoDocumento — sem tokens de provas, sem fuzzy boost");
    }

    @Test
    void canalLegado_quandoDocumentosTipadosAusente_comportamentoAntigoPreservado() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("provas", "Contrato de trabalho. Notificação extrajudicial.");

        Map<String, Object> out = factory.buildPreflightPayload(
                payload,
                null,
                "COMUM_ORDINARIO",
                new ProceduralCanonicalResolver.CanonicalContext(),
                "TJCE",
                "Fortaleza",
                "CE",
                null,
                "2ª Vara Cível",
                new TribunalProtocolRoutingService.RoutingDecision()
        );

        @SuppressWarnings("unchecked")
        List<String> docs = (List<String>) out.get("documentosPresentes");
        assertFalse(docs.contains("PETICAO_INICIAL"),
                "Sem canal tipado, enum name PETICAO_INICIAL não deve aparecer via tokenização de string");
        assertFalse(docs.contains("PROCURACAO"),
                "Sem canal tipado, PROCURACAO só aparece se string provas contiver 'PROCURACAO' — não é o caso aqui");
    }
}
