package com.tcc.pjb.backend.service.competencia;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InferenciaCompetenciaDistribuicaoService {

    static final ZoneId ZONA_LEGAL = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal SALARIOS_MINIMOS_ALCADA_JUIZADO_FEDERAL =
            RitoProcessual.JUIZADO_ESPECIAL_FEDERAL.salariosMinimosDaAlcada();

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CompetenceResolverService competenceResolverService;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    @Inject
    public InferenciaCompetenciaDistribuicaoService(ProceduralCanonicalResolver proceduralCanonicalResolver,
                                                    CompetenceResolverService competenceResolverService,
                                                    SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.competenceResolverService = Objects.requireNonNull(competenceResolverService);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    MapaCompetenciaDinamicoEngine.DynamicRequest enriquecer(MapaCompetenciaDinamicoEngine.DynamicRequest request) {
        if (request == null) {
            return null;
        }
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.ofEntries(
                Map.entry("classeTpu", Objects.toString(request.classeTpu(), "")),
                Map.entry("assunto", Objects.toString(request.assuntoTpu(), "")),
                Map.entry("ramoDireito", request.ramoDireito() != null ? request.ramoDireito().name() : ""),
                Map.entry("materia", Objects.toString(request.materiaPrincipal(), "")),
                Map.entry("competencia", request.tipoJustica() != null ? request.tipoJustica().name() : ""),
                Map.entry("uf", Objects.toString(request.ufReu() != null ? request.ufReu() : request.ufAutor(), "")),
                Map.entry("comarca", Objects.toString(request.comarcaReu() != null ? request.comarcaReu() : request.comarcaAutor(), ""))
        ));
        TipoJustica canonicalTipoJustica = parseTipoJustica(canonicalContext.ramoJusticaNacional());
        RamoDireito canonicalRamo = parseRamo(canonicalContext.ramoDireito());
        RitoProcessual ritoDaInferencia = request.ritoInformado() != null ? request.ritoInformado() : canonicalContext.rito();
        if (request.tipoJustica() != null && request.ramoDireito() != null) {
            return comInferenciaFederal(request, ritoDaInferencia, canonicalRamo != null ? canonicalRamo : request.ramoDireito());
        }
        if (canonicalTipoJustica != null || canonicalRamo != null) {
            return comInferenciaFederal(new MapaCompetenciaDinamicoEngine.DynamicRequest(
                    request.nupn(),
                    request.classeTpu(),
                    request.assuntoTpu(),
                    canonicalRamo != null ? canonicalRamo : request.ramoDireito(),
                    request.valorCausa(),
                    request.ufAutor(),
                    request.comarcaAutor(),
                    request.ufReu(),
                    request.comarcaReu(),
                    request.requerJuizadoEspecial(),
                    request.requerVaraEspecializada(),
                    request.materiaPrincipal(),
                    canonicalTipoJustica != null ? canonicalTipoJustica : request.tipoJustica(),
                    request.casoUrgente(),
                    request.preferenciaDigital(),
                    request.processoId(),
                    request.naturezaJuizado(),
                    request.conferenciaFederal(),
                    request.ritoInformado()
            ), ritoDaInferencia, canonicalRamo != null ? canonicalRamo : request.ramoDireito());
        }
        var resolucao = competenceResolverService.resolve(new CompetenceResolveRequest(
                null,
                request.assuntoTpu(),
                request.classeTpu(),
                request.materiaPrincipal(),
                request.ufReu() != null ? request.ufReu() : request.ufAutor(),
                request.comarcaReu() != null ? request.comarcaReu() : request.comarcaAutor(),
                request.valorCausa(),
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false
        ));
        TipoJustica inferida = parseTipoJustica(resolucao.tipoJusticaSugerida());
        return comInferenciaFederal(new MapaCompetenciaDinamicoEngine.DynamicRequest(
                request.nupn(),
                request.classeTpu(),
                request.assuntoTpu(),
                request.ramoDireito(),
                request.valorCausa(),
                request.ufAutor(),
                request.comarcaAutor(),
                request.ufReu(),
                request.comarcaReu(),
                request.requerJuizadoEspecial(),
                request.requerVaraEspecializada(),
                request.materiaPrincipal(),
                request.tipoJustica() != null ? request.tipoJustica() : inferida,
                request.casoUrgente(),
                request.preferenciaDigital(),
                request.processoId(),
                request.naturezaJuizado(),
                request.conferenciaFederal(),
                request.ritoInformado()
        ), ritoDaInferencia, request.ramoDireito());
    }

    private MapaCompetenciaDinamicoEngine.DynamicRequest comInferenciaFederal(MapaCompetenciaDinamicoEngine.DynamicRequest request, RitoProcessual ritoDaInferencia, RamoDireito ramo) {
        ConferenciaFederal conferencia = request.conferenciaFederal() != null
                ? request.conferenciaFederal()
                : conferenciaFederal(ritoDaInferencia, request.tipoJustica(), request.valorCausa(), hoje());
        NaturezaJuizado natureza = request.naturezaJuizado() != null
                ? request.naturezaJuizado()
                : naturezaJuizado(ritoDaInferencia, ramo, request.tipoJustica(), conferencia);
        return request.comNaturezaJuizado(natureza).comConferenciaFederal(conferencia);
    }

    ConferenciaFederal conferenciaFederal(RitoProcessual rito, TipoJustica tipoJustica,
                                                  BigDecimal valorCausa, LocalDate dataReferencia) {
        boolean acidentario = rito == RitoProcessual.PREVIDENCIARIO_ACIDENTARIO;
        if (rito != null && !acidentario && !rito.cabeNoJuizadoEspecialFederal()) {
            return ConferenciaFederal.NENHUMA;
        }
        if (tipoJustica == null) {
            return null;
        }
        if (tipoJustica != TipoJustica.FEDERAL) {
            return ConferenciaFederal.NENHUMA;
        }
        if (acidentario) {
            return ConferenciaFederal.ACIDENTARIO_DA_JUSTICA_ESTADUAL;
        }
        if (valorCausa == null || valorCausa.signum() <= 0) {
            return ConferenciaFederal.VALOR_DA_CAUSA_AUSENTE;
        }
        return dentroDaAlcadaDoJuizadoFederal(valorCausa, dataReferencia) ? ConferenciaFederal.ALCADA_DO_JEF : ConferenciaFederal.NENHUMA;
    }

    private boolean dentroDaAlcadaDoJuizadoFederal(BigDecimal valorCausa, LocalDate dataReferencia) {
        return valorCausa.compareTo(salarioMinimoNacionalService.multiplicar(SALARIOS_MINIMOS_ALCADA_JUIZADO_FEDERAL, dataReferencia)) <= 0;
    }

    static LocalDate hoje() {
        return LocalDate.now(ZONA_LEGAL);
    }

    static NaturezaJuizado naturezaJuizado(RitoProcessual rito, RamoDireito ramo, TipoJustica tipoJustica,
                                                   ConferenciaFederal conferencia) {
        NaturezaJuizado porRito = naturezaJuizado(rito);
        if (porRito == NaturezaJuizado.CIVEL && tipoJustica == TipoJustica.FEDERAL) {
            return NaturezaJuizado.FEDERAL;
        }
        if (porRito != null) {
            return porRito;
        }
        boolean materiaPrevidenciaria = (rito != null && rito.isPrevidenciario()) || ramo == RamoDireito.PREVIDENCIARIO;
        return materiaPrevidenciaria && tipoJustica == TipoJustica.FEDERAL && conferencia == ConferenciaFederal.ALCADA_DO_JEF
                ? NaturezaJuizado.FEDERAL
                : null;
    }

    private static NaturezaJuizado naturezaJuizado(RitoProcessual rito) {
        if (rito == null) {
            return null;
        }
        return switch (rito) {
            case JUIZADO_ESPECIAL_CRIMINAL, JUIZADO_ESPECIAL_FEDERAL_CRIMINAL, PROCEDIMENTO_PENAL_SUMARISSIMO -> NaturezaJuizado.CRIMINAL;
            case JUIZADO_ESPECIAL_FAZENDA_PUBLICA -> NaturezaJuizado.FAZENDA_PUBLICA;
            case JUIZADO_ESPECIAL_FEDERAL, PREVIDENCIARIO_JEF -> NaturezaJuizado.FEDERAL;
            default -> rito.isJuizado() ? NaturezaJuizado.CIVEL : null;
        };
    }

    static TipoJustica parseTipoJustica(String value) {
        String normalized = EnumText.normalizeToken(value);
        if (normalized.isEmpty()) {
            return null;
        }
        return switch (normalized) {
            case "ELEITORAL", "ELEITORAL_SUPERIOR" -> TipoJustica.ELEITORAL;
            case "TRABALHO", "TRABALHO_SUPERIOR" -> TipoJustica.TRABALHO;
            case "FEDERAL" -> TipoJustica.FEDERAL;
            case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
            case "MILITAR_FEDERAL", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
            case "SUPERIOR", "SUPERIOR_STF" -> TipoJustica.SUPERIOR;
            default -> {
                try {
                    yield TipoJustica.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }

    static RamoDireito parseRamo(String value) {
        String normalized = EnumText.normalizeToken(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return RamoDireito.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
