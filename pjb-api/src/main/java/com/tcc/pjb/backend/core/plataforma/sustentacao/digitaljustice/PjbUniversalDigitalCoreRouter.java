package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PjbUniversalDigitalCoreRouter {

    public PjbUniversalDigitalCoreDecision route(PjbUniversalDigitalCoreRequest request) {
        if (request == null) {
            PjbRitoContext context = PjbRitoContext.from(PjbDigitalCoreRitoKind.DESCONHECIDO, true);
            return new PjbUniversalDigitalCoreDecision(
                    RitoProcessual.COMUM_ORDINARIO,
                    PjbDigitalCoreRitoKind.DESCONHECIDO,
                    "TRIAGEM_MANUAL",
                    "RITO>UNIDADE>COMARCA>TRIBUNAL",
                    false,
                    true,
                    false,
                    context,
                    List.of("REQUERIMENTO_AUSENTE"),
                    List.of());
        }

        RitoProcessual canonicalRito = RitoProcessual.tryParse(request.classeProcessual())
                .or(() -> RitoProcessual.tryParse(request.assuntoPrincipal()))
                .orElse(null);

        PjbDigitalCoreRitoKind kind;
        if (canonicalRito != null) {
            kind = PjbDigitalCoreRitoKind.fromRitoProcessual(canonicalRito);
        } else {
            kind = PjbDigitalCoreRitoKind.resolve(request.classeProcessual(), request.assuntoPrincipal(), request.valorCausa());
            canonicalRito = kind.toCanonicalRito();
        }

        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean humanReview = false;

        if (blank(request.tribunalCode())) {
            reasons.add("TRIBUNAL_NAO_INFORMADO");
            humanReview = true;
        }
        if (blank(request.comarcaCode())) {
            reasons.add("COMARCA_NAO_INFORMADA");
            humanReview = true;
        }
        if (kind == PjbDigitalCoreRitoKind.DESCONHECIDO) {
            reasons.add("RITO_NAO_CLASSIFICADO");
            humanReview = true;
        }
        if (request.exigePericiaTecnicaComplexa() && canonicalRito.isJuizado()) {
            warnings.add("PERICIA_TECNICA_COMPLEXA_PODE_EXIGIR_RITO_ORDINARIO");
            humanReview = true;
        }
        if (request.parteDemandadaOposicaoTempestiva()) {
            warnings.add("OPOSICAO_TEMPESTIVA_AO_JUIZO_100_DIGITAL_EXIGE_REAVALIACAO_DO_NUCLEO");
            humanReview = true;
        }
        if (request.distribuicaoConcluida()) {
            warnings.add("RITO_CONTEXT_IMUTAVEL_SEM_DECISAO_DE_DECLINIO_OU_REDISTRIBUICAO");
        }

        boolean routeToDigitalCore = request.juizoDigitalDisponivel()
                && request.parteAutoraAceitaTramitacaoDigital()
                && !request.parteDemandadaOposicaoTempestiva()
                && kind.ritoDigitalPreferencial()
                && reasons.isEmpty();

        String routingAxis = routeToDigitalCore ? "NUCLEO_DIGITAL_UNIVERSAL" : "DISTRIBUICAO_ORDINARIA_CONTEXTUAL";
        String targetHierarchy = hierarchy(canonicalRito, kind, request);
        PjbRitoContext context = PjbRitoContext.from(canonicalRito, humanReview);

        return new PjbUniversalDigitalCoreDecision(
                canonicalRito,
                kind,
                routingAxis,
                targetHierarchy,
                routeToDigitalCore,
                humanReview,
                true,
                context,
                List.copyOf(reasons),
                List.copyOf(warnings));
    }

    private String hierarchy(RitoProcessual canonicalRito, PjbDigitalCoreRitoKind kind, PjbUniversalDigitalCoreRequest request) {
        String ritoLabel = canonicalRito != null ? normalize(canonicalRito.name()) : normalize(kind.name());
        return ritoLabel
                + ">" + normalize(Objects.toString(request.varaReferencia(), "UNIDADE_NAO_DEFINIDA"))
                + ">" + normalize(Objects.toString(request.comarcaCode(), "COMARCA_NAO_DEFINIDA"))
                + ">" + normalize(Objects.toString(request.tribunalCode(), "TRIBUNAL_NAO_DEFINIDO"));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
