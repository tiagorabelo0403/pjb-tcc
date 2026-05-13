package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralCompetenceRequestFactory {

    CompetenceResolveRequest create(Map<String, Object> payload,
                                    ProceduralCanonicalResolver.CanonicalContext canonical,
                                    NationalProceduralPartyProfile partyProfile) {
        Objects.requireNonNull(canonical);
        Objects.requireNonNull(partyProfile);
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        return new CompetenceResolveRequest(
                NationalProceduralRoutingSupport.buildCorpus(safePayload),
                firstNonBlank(text(safePayload.get("assunto")), canonical.classeTpuNome()),
                firstNonBlank(text(safePayload.get("classe")), text(safePayload.get("classeProcessual")), canonical.classeTpuCodigo()),
                firstNonBlank(text(safePayload.get("materia")), text(safePayload.get("ramoDireito")), canonical.ramoDireito()),
                firstNonBlank(text(safePayload.get("ufAutor")), text(safePayload.get("ufReu"))),
                firstNonBlank(text(safePayload.get("comarcaAutor")), text(safePayload.get("cidadeAutor")), text(safePayload.get("comarcaReu")), text(safePayload.get("cidadeReu"))),
                decimal(safePayload.get("valorCausa")),
                partyProfile.federal(),
                partyProfile.autarquiaFederal(),
                partyProfile.empresaPublicaFederal(),
                involvesState(canonical, partyProfile),
                partyProfile.municipal(),
                partyProfile.trabalho(),
                partyProfile.eleitoral(),
                partyProfile.militar()
        );
    }

    private static boolean involvesState(ProceduralCanonicalResolver.CanonicalContext canonical,
                                         NationalProceduralPartyProfile partyProfile) {
        if (partyProfile.state()) {
            return true;
        }
        if (partyProfile.publicParty() && partyProfile.municipal()) {
            return true;
        }
        if (partyProfile.tags() != null && partyProfile.tags().stream().anyMatch(tag -> "PARTE_ESTADUAL".equals(tag) || "ENTE_PUBLICO".equals(tag))) {
            return true;
        }
        String tipoJustica = canonical.ramoJusticaNacional();
        String tribunal = canonical.tribunalCodigo();
        return partyProfile.publicParty()
                && ((tipoJustica != null && tipoJustica.toUpperCase(java.util.Locale.ROOT).contains("ESTADUAL"))
                || (tribunal != null && tribunal.toUpperCase(java.util.Locale.ROOT).startsWith("TJ")));
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }

    private static BigDecimal decimal(Object value) {
        return NationalProceduralRoutingSupport.decimal(value);
    }
}
