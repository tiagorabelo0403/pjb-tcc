package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralDistributionResolver {

    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;

    public NationalProceduralDistributionResolver(MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine) {
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
    }

    Optional<NationalProceduralDistributionSuggestion> resolve(NationalProceduralDistributionContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        if (isBlank(context.cidade()) || isBlank(context.uf())) {
            return Optional.empty();
        }
        try {
            Optional<DynamicCompetenceDistributionResponse> distribution = mapaCompetenciaDinamicoEngine.distribuir(
                    new DynamicCompetenceDistributionRequest(
                            firstNonBlank(text(payload.get("nupn")), text(payload.get("numeroProcesso")), text(payload.get("numeroUnificado"))),
                            firstNonBlank(context.canonical().classeTpuCodigo(), text(payload.get("classe")), text(payload.get("classeProcessual"))),
                            firstNonBlank(text(payload.get("assunto")), text(payload.get("objetoProcessual"))),
                            firstNonBlank(context.canonical().ramoDireito(), text(payload.get("ramoDireito")), text(payload.get("materia"))),
                            decimal(payload.get("valorCausa")),
                            context.uf(),
                            context.cidade(),
                            firstNonBlank(text(payload.get("ufReu")), context.uf()),
                            firstNonBlank(text(payload.get("comarcaReu")), context.cidade()),
                            context.juizadoDecision().admiteJuizado(),
                            context.actionProfile().specialProcedure(),
                            firstNonBlank(context.actionProfile().actionNature(), text(payload.get("tipoAcao")), text(payload.get("assunto"))),
                            firstNonBlank(context.tipoJustica().name(), context.competence().tipoJusticaSugerida()),
                            bool(payload.get("casoUrgente")),
                            bool(payload.get("preferenciaDigital")),
                            toLong(payload.get("processoId"))
                    )
            );
            if (distribution.isEmpty()) {
                return Optional.empty();
            }
            DynamicCompetenceDistributionResponse value = distribution.get();
            return Optional.of(new NationalProceduralDistributionSuggestion(
                    value.unidadeCodigo(),
                    value.tribunalCodigo(),
                    value.comarca(),
                    value.uf(),
                    value.tipoVara(),
                    value.scoreFinal(),
                    value.motivacao(),
                    value.alertas(),
                    value.fatoresRevisaoHumana()
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean isBlank(String value) {
        return NationalProceduralRoutingSupport.isBlank(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }

    private static boolean bool(Object value) {
        return NationalProceduralRoutingSupport.bool(value);
    }

    private static BigDecimal decimal(Object value) {
        return NationalProceduralRoutingSupport.decimal(value);
    }
}
