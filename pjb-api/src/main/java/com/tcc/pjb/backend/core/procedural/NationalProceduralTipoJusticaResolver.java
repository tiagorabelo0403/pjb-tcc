package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralTipoJusticaResolver {

    TipoJustica resolve(Object tipoJusticaInformada,
                        CompetenceResolveResponse competence,
                        ProceduralCanonicalResolver.CanonicalContext canonical,
                        String ritoSugerido,
                        NationalProceduralPartyProfile partyProfile) {
        Objects.requireNonNull(competence);
        Objects.requireNonNull(canonical);
        Objects.requireNonNull(partyProfile);
        return Optional.ofNullable(TipoJustica.fromString(firstNonBlank(text(tipoJusticaInformada), competence.tipoJusticaSugerida(), canonical.ramoJusticaNacional())))
                .orElseGet(() -> inferFromRito(ritoSugerido)
                        .orElseGet(() -> fallbackFromPartyProfile(partyProfile)));
    }

    private Optional<TipoJustica> inferFromRito(String ritoSugerido) {
        String normalized = normalize(ritoSugerido);
        if (isBlank(normalized)) {
            return Optional.empty();
        }
        if (containsAny(normalized, "TRABALH")) {
            return Optional.of(TipoJustica.TRABALHO);
        }
        if (containsAny(normalized, "ELEITORAL")) {
            return Optional.of(TipoJustica.ELEITORAL);
        }
        if (containsAny(normalized, "MILITAR_FEDERAL")) {
            return Optional.of(TipoJustica.MILITAR_FEDERAL);
        }
        if (containsAny(normalized, "MILITAR")) {
            return Optional.of(TipoJustica.MILITAR_ESTADUAL);
        }
        if (containsAny(normalized, "JEF", "PREVIDENCIARIO", "FEDERAL")) {
            return Optional.of(TipoJustica.FEDERAL);
        }
        return Optional.of(TipoJustica.ESTADUAL);
    }

    private TipoJustica fallbackFromPartyProfile(NationalProceduralPartyProfile partyProfile) {
        if (partyProfile.eleitoral()) {
            return TipoJustica.ELEITORAL;
        }
        if (partyProfile.militar()) {
            return TipoJustica.MILITAR_ESTADUAL;
        }
        if (partyProfile.trabalho()) {
            return TipoJustica.TRABALHO;
        }
        if (partyProfile.federal()) {
            return TipoJustica.FEDERAL;
        }
        return TipoJustica.ESTADUAL;
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }

    private static boolean isBlank(String value) {
        return NationalProceduralRoutingSupport.isBlank(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }
}
