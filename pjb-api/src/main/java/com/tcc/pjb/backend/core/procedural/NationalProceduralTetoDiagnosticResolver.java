package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralTetoDiagnosticResolver {

    private final TetoProcessualService tetoProcessualService;

    public NationalProceduralTetoDiagnosticResolver(TetoProcessualService tetoProcessualService) {
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
    }

    TetoProcessualService.DiagnosticoTetoProcessual resolve(NationalProceduralTetoDiagnosticContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.competence());
        Objects.requireNonNull(context.canonical());
        Objects.requireNonNull(context.selectedRito());
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        return tetoProcessualService.diagnosticar(
                decimal(payload.get("valorCausa")),
                TipoJustica.fromString(firstNonBlank(text(payload.get("tipoJustica")), context.competence().tipoJusticaSugerida(), context.canonical().ramoJusticaNacional())),
                RamoDireito.fromString(firstNonBlank(text(payload.get("ramoDireito")), context.canonical().ramoDireito())),
                firstNonBlank(text(payload.get("rito")), context.competence().ritoSugerido(), context.selectedRito().rito() != null ? context.selectedRito().rito().name() : null),
                payload.get("__jurisdicaoEntity") instanceof Jurisdicao jurisdicao ? jurisdicao : null,
                payload.get("__dataReferencia") instanceof LocalDate dataReferencia ? dataReferencia : LocalDate.now()
        );
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
