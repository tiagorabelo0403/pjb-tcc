package com.tcc.pjb.backend.service.competencia;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CompetenceResolverService {

    private static final Set<String> TOKENS_TRABALHO = Set.of(
            "trabalh", "clt", "verbas", "rescis", "vinculo", "empregado", "empregador",
            "horas", "extra", "fgts", "insalubr", "periculos", "dano_moral_trabalh"
    );

    private static final Set<String> TOKENS_ELEITORAL = Set.of(
            "eleic", "candid", "registro", "propaganda", "tse", "tre", "urna", "voto"
    );

    private static final Set<String> TOKENS_MILITAR = Set.of(
            "militar", "cpm", "cjm", "justica_militar", "exercito", "aeronautica", "marinha"
    );

    private static final Set<String> TOKENS_PENAL = Set.of(
            "cpp", "pris", "flagrante", "inquer", "denuncia", "habeas", "custodia",
            "lesao", "amea", "furto", "roubo", "homicid", "tce", "juri", "pronuncia"
    );

    private static final Set<String> TOKENS_FAZENDA = Set.of(
            "fazenda", "municip", "estado", "tribut", "execucao_fiscal", "iptu", "icms", "iss",
            "procuradoria"
    );

    private static final Set<String> TOKENS_FEDERAL = Set.of(
            "uniao", "inss", "ibama", "caixa", "cef", "fgts", "receita_federal", "trf",
            "autarquia", "empresa_publica_federal", "aneel", "anvisa", "ans", "antt", "anac"
    );

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    public CompetenceResolverService(ProceduralCanonicalResolver proceduralCanonicalResolver,
                                     CanonicalSanityGate canonicalSanityGate) {
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
    }

    public CompetenceResolveResponse resolve(CompetenceResolveRequest req) {
        String requestId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String joined = joinSignals(req);
        Set<String> tokens = TextTokenUtils.tokens(joined);
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(buildCanonicalPayload(req, joined));
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);

        List<String> reasons = new ArrayList<>();
        List<String> legal = new ArrayList<>();
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("tokens", new ArrayList<>(tokens));
        debug.put("canonicalContext", canonicalContext.toMap());
        debug.put("sanityGate", gateResult.toMap());
        debug.put("sanityStatus", gateResult.overallStatus());

        Score score = new Score();
        applyExplicitFlags(req, score, reasons, legal);
        applyGateSignals(gateResult, reasons, debug);
        applyCanonicalSignals(canonicalContext, score, reasons, legal, debug);
        applyTextualSignals(req, tokens, score, reasons, legal, debug);

        if (score.isEmpty()) {
            score.add(TipoJustica.ESTADUAL, 0.55d);
            reasons.add("Sem sinais fortes de competência especializada: padrão é Justiça Estadual");
        }

        boolean penal = overlap(tokens, TOKENS_PENAL) >= 0.18d || isPenalCanonical(canonicalContext);
        boolean fazenda = isTrue(req.envolveEstado())
                || isTrue(req.envolveMunicipio())
                || overlap(tokens, TOKENS_FAZENDA) >= 0.18d
                || isFazendaCanonical(canonicalContext);
        debug.put("penalHeuristic", penal);
        debug.put("fazendaHeuristic", fazenda);
        debug.put("valorCausa", req.valorCausa());

        TipoJustica top = score.top();
        double confidence = strengthenConfidence(score.topScore(), canonicalContext);
        if (gateResult.hasBlockingIssues()) {
            confidence = Math.max(0.10d, confidence - 0.20d);
        }
        String rito = resolveRito(top, penal, fazenda, req.valorCausa(), canonicalContext, reasons, legal);

        if (top == TipoJustica.MILITAR_ESTADUAL || top == TipoJustica.MILITAR_FEDERAL) {
            reasons.add("Justiça Militar exige detalhamento da força vinculada e do enquadramento material do fato");
        }

        reasons = dedupe(reasons);
        legal = dedupe(legal);
        debug.put("topCandidates", score.topN(6));
        debug.put("tribunalMatrix", NationalCompetenceMatrix.porCodigo(canonicalContext.tribunalCodigo()).map(NationalCompetenceMatrix::toMap).orElse(Map.of()));

        return new CompetenceResolveResponse(
                requestId,
                now,
                top != null ? top.name() : null,
                rito,
                round2(confidence),
                reasons,
                legal,
                safeCopy(debug)
        );
    }

    private void applyGateSignals(GateResult gateResult,
                                  List<String> reasons,
                                  Map<String, Object> debug) {
        if (gateResult == null) {
            return;
        }
        if (gateResult.hasBlockingIssues()) {
            reasons.add("Gate canônico detectou inconsistências estruturais: " + String.join(", ", gateResult.statusCodes()));
        }
        debug.put("sanityBlocking", gateResult.hasBlockingIssues());
    }

    private void applyExplicitFlags(CompetenceResolveRequest req,
                                    Score score,
                                    List<String> reasons,
                                    List<String> legal) {
        if (isTrue(req.envolveEleitoral())) {
            score.add(TipoJustica.ELEITORAL, 0.95d);
            reasons.add("Flag eleitoral informada com aderência à Justiça Eleitoral");
            legal.add("CF/88, art. 121");
        }
        if (isTrue(req.envolveMilitar())) {
            score.add(TipoJustica.MILITAR_ESTADUAL, 0.82d);
            score.add(TipoJustica.MILITAR_FEDERAL, 0.82d);
            reasons.add("Flag militar informada com aderência à Justiça Militar");
            legal.add("CF/88, arts. 124 e 125, §4º");
        }
        if (isTrue(req.envolveRelacaoTrabalho())) {
            score.add(TipoJustica.TRABALHO, 0.95d);
            reasons.add("Flag de relação de trabalho informada");
            legal.add("CF/88, art. 114");
        }
        boolean federalParty = isTrue(req.envolveUniao()) || isTrue(req.envolveAutarquiaFederal()) || isTrue(req.envolveEmpresaPublicaFederal());
        if (federalParty) {
            score.add(TipoJustica.FEDERAL, 0.92d);
            reasons.add("Parte federal informada com aderência à Justiça Federal");
            legal.add("CF/88, art. 109, I");
        }
    }

    private void applyCanonicalSignals(CanonicalContext canonicalContext,
                                       Score score,
                                       List<String> reasons,
                                       List<String> legal,
                                       Map<String, Object> debug) {
        TipoJustica canonicalTipoJustica = mapCanonicalTipoJustica(canonicalContext);
        if (canonicalTipoJustica != null) {
            score.add(canonicalTipoJustica, 0.97d);
            reasons.add("Contexto procedural canônico consolidou ramo da Justiça e tribunal preferencial");
            legal.add(legalBaseFor(canonicalTipoJustica));
            debug.put("canonicalTipoJustica", canonicalTipoJustica.name());
        }
        if (canonicalContext.tribunalCodigo() != null) {
            NationalCompetenceMatrix.porCodigo(canonicalContext.tribunalCodigo()).ifPresent(matrix -> {
                TipoJustica byMatrix = mapRamoJusticaToTipoJustica(matrix.ramoJusticaNacional());
                if (byMatrix != null) {
                    score.add(byMatrix, 0.96d);
                    reasons.add("Matriz nacional de competência confirmou tribunal compatível com o caso");
                    legal.add(legalBaseFor(byMatrix));
                }
            });
        }
        if (canonicalContext.rito() != null) {
            TipoJustica byRito = mapRitoToTipoJustica(canonicalContext.rito().name());
            if (byRito != null) {
                score.add(byRito, 0.90d);
                reasons.add("Rito canônico reforçou especialização material e orgânica");
                legal.add(legalBaseFor(byRito));
            }
        }
    }

    private void applyTextualSignals(CompetenceResolveRequest req,
                                     Set<String> tokens,
                                     Score score,
                                     List<String> reasons,
                                     List<String> legal,
                                     Map<String, Object> debug) {
        double trabalhoScore = overlap(tokens, TOKENS_TRABALHO);
        if (trabalhoScore >= 0.18d) {
            score.add(TipoJustica.TRABALHO, clamp(0.55d + trabalhoScore));
            reasons.add("Sinais de relação de trabalho detectados no texto");
            legal.add("CF/88, art. 114");
        }
        double eleitoralScore = overlap(tokens, TOKENS_ELEITORAL);
        if (eleitoralScore >= 0.18d) {
            score.add(TipoJustica.ELEITORAL, clamp(0.55d + eleitoralScore));
            reasons.add("Sinais eleitorais detectados no texto");
            legal.add("CF/88, art. 121");
        }
        double militarScore = overlap(tokens, TOKENS_MILITAR);
        if (militarScore >= 0.18d) {
            score.add(TipoJustica.MILITAR_ESTADUAL, clamp(0.50d + militarScore));
            score.add(TipoJustica.MILITAR_FEDERAL, clamp(0.50d + militarScore));
            reasons.add("Sinais militares detectados no texto");
            legal.add("CF/88, arts. 124 e 125, §4º");
        }
        double federalTextScore = overlap(tokens, TOKENS_FEDERAL);
        if (federalTextScore >= 0.18d) {
            score.add(TipoJustica.FEDERAL, clamp(0.55d + federalTextScore));
            reasons.add("Sinais de ente ou órgão federal detectados no texto");
            legal.add("CF/88, art. 109");
        }
        if (req.valorCausa() != null) {
            debug.put("valorBaixoJuizado", req.valorCausa().compareTo(new BigDecimal("30000")) <= 0);
        }
    }

    private static String resolveRito(TipoJustica tj,
                                              boolean penal,
                                              boolean fazenda,
                                              BigDecimal valorCausa,
                                              CanonicalContext canonicalContext,
                                              List<String> reasons,
                                              List<String> legal) {
        if (canonicalContext.rito() != null) {
            return canonicalContext.rito().name();
        }
        if (tj == null) {
            return "COMUM_ORDINARIO";
        }
        if (tj == TipoJustica.TRABALHO) {
            return "TRABALHISTA_ORDINARIO";
        }
        if (tj == TipoJustica.ELEITORAL) {
            return "ELEITORAL";
        }
        if (tj == TipoJustica.MILITAR_ESTADUAL || tj == TipoJustica.MILITAR_FEDERAL) {
            return "MILITAR";
        }
        if (penal) {
            legal.add("CPP");
            return "PROCEDIMENTO_PENAL_COMUM";
        }
        if (valorCausa != null && valorCausa.signum() >= 0 && valorCausa.compareTo(new BigDecimal("30000")) <= 0) {
            if (tj == TipoJustica.FEDERAL) {
                reasons.add("Valor reduzido compatível com fluxo de juizado federal");
                legal.add("Lei 10.259/2001");
                return "JUIZADO_ESPECIAL_FEDERAL";
            }
            if (fazenda) {
                reasons.add("Valor reduzido com presença fazendária compatível com juizado da Fazenda Pública");
                legal.add("Lei 12.153/2009");
                return "JUIZADO_ESPECIAL_FAZENDA_PUBLICA";
            }
            if (tj == TipoJustica.ESTADUAL) {
                reasons.add("Valor reduzido compatível com juizado cível");
                legal.add("Lei 9.099/1995");
                return "JUIZADO_ESPECIAL_CIVEL";
            }
        }
        if (fazenda && tj == TipoJustica.ESTADUAL) {
            legal.add("CPC/2015 e Lei 6.830/1980");
            return "FAZENDA_PUBLICA_CONHECIMENTO";
        }
        return "COMUM_ORDINARIO";
    }

    private Map<String, Object> buildCanonicalPayload(CompetenceResolveRequest req, String joined) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("textoCaso", req.textoCaso());
        payload.put("assunto", req.assunto());
        payload.put("classeTpu", req.classeProcessual());
        payload.put("classeProcessual", req.classeProcessual());
        payload.put("materia", req.materia());
        payload.put("ramoDireito", req.materia());
        payload.put("uf", req.uf());
        payload.put("comarca", req.comarca());
        payload.put("textoComposto", joined);
        if (isTrue(req.envolveEleitoral())) {
            payload.put("competencia", "ELEITORAL");
            payload.put("rito", "ELEITORAL");
        } else if (isTrue(req.envolveMilitar())) {
            payload.put("competencia", "MILITAR");
            payload.put("rito", "MILITAR");
        } else if (isTrue(req.envolveRelacaoTrabalho())) {
            payload.put("competencia", "TRABALHO");
            payload.put("rito", "TRABALHISTA_ORDINARIO");
        }
        return payload;
    }

    private static TipoJustica mapCanonicalTipoJustica(CanonicalContext canonicalContext) {
        if (canonicalContext == null) {
            return null;
        }
        if (canonicalContext.tribunalCodigo() != null) {
            TipoJustica byMatrix = NationalCompetenceMatrix.porCodigo(canonicalContext.tribunalCodigo())
                    .map(matrix -> mapRamoJusticaToTipoJustica(matrix.ramoJusticaNacional()))
                    .orElse(null);
            if (byMatrix != null) {
                return byMatrix;
            }
        }
        String ramo = canonicalContext.ramoJusticaNacional();
        if (ramo == null || ramo.isBlank()) {
            return mapRitoToTipoJustica(canonicalContext.rito() != null ? canonicalContext.rito().name() : null);
        }
        try {
            return switch (ramo.trim().toUpperCase(Locale.ROOT)) {
                case "ELEITORAL", "ELEITORAL_SUPERIOR" -> TipoJustica.ELEITORAL;
                case "TRABALHO", "TRABALHO_SUPERIOR" -> TipoJustica.TRABALHO;
                case "FEDERAL" -> TipoJustica.FEDERAL;
                case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
                case "MILITAR_FEDERAL", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
                case "SUPERIOR", "SUPERIOR_STF" -> TipoJustica.SUPERIOR;
                default -> TipoJustica.ESTADUAL;
            };
        } catch (Exception ignored) {
            return mapRitoToTipoJustica(canonicalContext.rito() != null ? canonicalContext.rito().name() : null);
        }
    }

    private static TipoJustica mapRamoJusticaToTipoJustica(NationalCompetenceMatrix.RamoJusticaNacional ramo) {
        if (ramo == null) {
            return null;
        }
        return switch (ramo) {
            case ELEITORAL, ELEITORAL_SUPERIOR -> TipoJustica.ELEITORAL;
            case TRABALHO, TRABALHO_SUPERIOR -> TipoJustica.TRABALHO;
            case FEDERAL -> TipoJustica.FEDERAL;
            case MILITAR_ESTADUAL -> TipoJustica.MILITAR_ESTADUAL;
            case MILITAR_SUPERIOR -> TipoJustica.MILITAR_FEDERAL;
            case SUPERIOR, SUPERIOR_STF -> TipoJustica.SUPERIOR;
            default -> TipoJustica.ESTADUAL;
        };
    }

    private static TipoJustica mapRitoToTipoJustica(String rito) {
        if (rito == null || rito.isBlank()) {
            return null;
        }
        String normalized = rito.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("ELEITORAL")) {
            return TipoJustica.ELEITORAL;
        }
        if (normalized.contains("TRABALH")) {
            return TipoJustica.TRABALHO;
        }
        if (normalized.contains("MILITAR")) {
            return TipoJustica.MILITAR_ESTADUAL;
        }
        if (normalized.contains("PREVID") || normalized.equals("JUIZADO_ESPECIAL_FEDERAL")) {
            return TipoJustica.FEDERAL;
        }
        if (normalized.contains("FAZENDA") || normalized.contains("TRIBUT") || normalized.equals("EXECUCAO_FISCAL")) {
            return TipoJustica.ESTADUAL;
        }
        return null;
    }

    private static boolean isPenalCanonical(CanonicalContext canonicalContext) {
        String classe = canonicalContext == null ? null : canonicalContext.classeTpuNome();
        String joined = ((classe == null ? "" : classe) + ' ' + (canonicalContext == null || canonicalContext.ramoDireito() == null ? "" : canonicalContext.ramoDireito())).toUpperCase(Locale.ROOT);
        return joined.contains("PENAL") || joined.contains("CRIM") || joined.contains("JURI") || joined.contains("HABEAS");
    }

    private static boolean isFazendaCanonical(CanonicalContext canonicalContext) {
        String ramo = canonicalContext == null ? null : canonicalContext.ramoDireito();
        String classe = canonicalContext == null ? null : canonicalContext.classeTpuNome();
        String joined = ((ramo == null ? "" : ramo) + ' ' + (classe == null ? "" : classe)).toUpperCase(Locale.ROOT);
        return joined.contains("TRIBUT") || joined.contains("FAZENDA") || joined.contains("FISCAL") || joined.contains("ADMINISTRAT");
    }

    private static String legalBaseFor(TipoJustica tipoJustica) {
        if (tipoJustica == null) {
            return "CF/88";
        }
        return switch (tipoJustica) {
            case ELEITORAL -> "CF/88, art. 121";
            case TRABALHO -> "CF/88, art. 114";
            case FEDERAL -> "CF/88, art. 109";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "CF/88, arts. 124 e 125, §4º";
            case SUPERIOR -> "CF/88, arts. 102 a 105";
            default -> "CF/88, art. 125";
        };
    }

    private static double strengthenConfidence(double score, CanonicalContext canonicalContext) {
        double value = score;
        if (canonicalContext != null && canonicalContext.tribunalCodigo() != null) {
            value = Math.max(value, 0.96d);
        }
        if (canonicalContext != null && canonicalContext.classeTpuCodigo() != null) {
            value = Math.max(value, 0.94d);
        }
        return Math.min(0.99d, value);
    }

    private static String joinSignals(CompetenceResolveRequest req) {
        StringBuilder sb = new StringBuilder(512);
        append(sb, req.textoCaso());
        append(sb, req.assunto());
        append(sb, req.classeProcessual());
        append(sb, req.materia());
        if (isTrue(req.envolveUniao())) sb.append(" uniao ");
        if (isTrue(req.envolveAutarquiaFederal())) sb.append(" autarquia_federal ");
        if (isTrue(req.envolveEmpresaPublicaFederal())) sb.append(" empresa_publica_federal ");
        if (isTrue(req.envolveRelacaoTrabalho())) sb.append(" trabalho clt ");
        if (isTrue(req.envolveEleitoral())) sb.append(" eleitoral ");
        if (isTrue(req.envolveMilitar())) sb.append(" militar ");
        if (isTrue(req.envolveEstado())) sb.append(" estado fazenda ");
        if (isTrue(req.envolveMunicipio())) sb.append(" municipio fazenda ");
        return sb.toString();
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append(' ').append(value).append(' ');
    }

    private static boolean isTrue(Boolean value) {
        return value != null && value;
    }

    private static double overlap(Set<String> docTokens, Set<String> anchors) {
        if (docTokens == null || docTokens.isEmpty() || anchors == null || anchors.isEmpty()) {
            return 0.0d;
        }
        int hit = 0;
        for (String anchor : anchors) {
            for (String token : docTokens) {
                if (token.startsWith(anchor) || anchor.startsWith(token)) {
                    hit++;
                    break;
                }
            }
        }
        return (double) hit / (double) anchors.size();
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(0.98d, value));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }


    private static Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private static List<String> dedupe(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : input) {
            if (item == null) {
                continue;
            }
            String normalized = item.trim();
            if (!normalized.isBlank()) {
                set.add(normalized);
            }
        }
        return List.copyOf(set);
    }

    private static final class Score {
        private final EnumMap<TipoJustica, Double> map = new EnumMap<>(TipoJustica.class);

        void add(TipoJustica tipoJustica, double score) {
            if (tipoJustica == null) {
                return;
            }
            double normalized = Math.max(0.0d, Math.min(1.0d, score));
            map.merge(tipoJustica, normalized, Math::max);
        }

        boolean isEmpty() {
            return map.isEmpty();
        }

        TipoJustica top() {
            return map.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        double topScore() {
            return map.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0d);
        }

        List<Map<String, Object>> topN(int n) {
            return map.entrySet().stream()
                    .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                    .limit(Math.max(1, n))
                    .map(entry -> Map.<String, Object>of("tipoJustica", entry.getKey().name(), "score", round2(entry.getValue())))
                    .toList();
        }
    }
}
