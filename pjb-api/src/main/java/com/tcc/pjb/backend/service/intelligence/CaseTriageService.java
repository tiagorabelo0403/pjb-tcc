package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector.SelectedRito;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageRequest;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageResponse;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageResponse.JurisdictionSuggestionDto;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageResponse.PrecedenteRecommendationDto;
import com.tcc.pjb.backend.model.entity.JurisdictionEngine;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationExtractor;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchEngine;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class CaseTriageService {

    private final JurisdictionEngine.Engine jurisdictionEngine;
    private final JurisprudenceSearchEngine jurisprudenceSearchEngine;
    private final CitationExtractor citationExtractor;
    private final CanonicalRitoSelector canonicalRitoSelector;

    public CaseTriageService(JurisdictionEngine.Engine jurisdictionEngine,
                             JurisprudenceSearchEngine jurisprudenceSearchEngine,
                             CitationExtractor citationExtractor,
                             CanonicalRitoSelector canonicalRitoSelector) {
        this.jurisdictionEngine = jurisdictionEngine;
        this.jurisprudenceSearchEngine = jurisprudenceSearchEngine;
        this.citationExtractor = citationExtractor;
        this.canonicalRitoSelector = canonicalRitoSelector;
    }

    @Transactional(readOnly = true)
    public CaseTriageResponse triage(CaseTriageRequest req) {
        Objects.requireNonNull(req, "request é obrigatório");

        String resumo = safe(req.resumo());
        String materia = safe(req.materia());
        String orgao = safe(req.orgao());
        String pais = safe(req.pais());
        String tratado = safe(req.tratado());
        int topK = req.topK() == null ? 8 : Math.max(1, Math.min(req.topK(), 20));

        Map<String, Object> payload = buildCanonicalPayload(req, resumo, materia, orgao);
        RamoDireito ramoHeuristico = inferRamo(materia, resumo);
        SelectedRito selectedRito = canonicalRitoSelector.select(payload, inferRito(req.rito(), ramoHeuristico, materia, resumo), "case_triage_service");
        CanonicalContext canonical = selectedRito.canonicalContext();
        RamoDireito ramo = canonical.ramoDireito() == null ? ramoHeuristico : Objects.requireNonNullElse(RamoDireito.fromString(canonical.ramoDireito()), ramoHeuristico);
        String rito = selectedRito.rito() != null ? selectedRito.rito().name() : null;

        JurisdictionEngine.Rite jRite = mapRite(rito, ramo);
        JurisdictionEngine.Context ctx = new JurisdictionEngine.Context(
                firstNonBlank(materia, ramo != null ? ramo.getDescricao() : null),
                firstNonBlank(canonical.tribunalCodigo(), orgao),
                firstNonBlank(pais, "BRASIL"),
                tratado,
                jRite
        );
        JurisdictionEngine.Result jr = jurisdictionEngine.identifyByContext(ctx);

        JurisdictionSuggestionDto jdto = new JurisdictionSuggestionDto(
                jr.isFound(),
                jr.getConfidence(),
                jr.getReason(),
                jr.getSpec() != null ? jr.getSpec().label : canonical.tribunalNome(),
                jr.getSpec() != null ? jr.getSpec().category : canonical.ramoJusticaNacional(),
                jr.getSpec() != null && jr.getSpec().rite != null ? jr.getSpec().rite.name() : jRite.name(),
                jr.getSpec() != null && jr.getSpec().authorities != null
                        ? jr.getSpec().authorities.stream().map(a -> a.getName()).filter(Objects::nonNull).distinct().toList()
                        : canonical.competenceHints(),
                jr.getSpec() != null && jr.getSpec().legalBases != null
                        ? jr.getSpec().legalBases.stream().map(l -> l.getCitation()).filter(Objects::nonNull).distinct().toList()
                        : List.of()
        );

        List<PrecedenteRecommendationDto> precedentes = recommendPrecedentes(resumo, materia, ramo, rito, topK);

        Map<String, Object> dbg = new LinkedHashMap<>();
        dbg.put("ramoEvidence", ramoEvidence(materia, resumo));
        dbg.put("ritoEvidence", ritoEvidence(req.rito(), materia, resumo));
        dbg.put("jurisdictionDebug", jr.getDebug());
        dbg.put("topK", topK);
        dbg.put("canonicalContext", canonical.toMap());
        dbg.put("ritoSelection", selectedRito.toMap());

        return new CaseTriageResponse(
                UUID.randomUUID().toString(),
                Instant.now(),
                ramo,
                selectedRito.rito(),
                jdto,
                precedentes,
                dbg
        );
    }

    private Map<String, Object> buildCanonicalPayload(CaseTriageRequest req, String resumo, String materia, String orgao) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rito", req.rito());
        payload.put("ramoDireito", materia);
        payload.put("materia", materia);
        payload.put("resumo", resumo);
        payload.put("narrativa", resumo);
        payload.put("classe", req.orgao());
        payload.put("orgao", orgao);
        payload.put("tribunalCodigo", orgao);
        payload.put("pais", req.pais());
        payload.put("tratado", req.tratado());
        return payload;
    }

    private List<PrecedenteRecommendationDto> recommendPrecedentes(String resumo,
                                                                   String materia,
                                                                   RamoDireito ramo,
                                                                   String rito,
                                                                   int topK) {
        String q = firstNonBlank(materia, resumo);
        List<JurisprudenceSearchHit> hits = jurisprudenceSearchEngine.search(q, ramo, rito, Math.max(5, topK));
        if (hits.isEmpty()) {
            return List.of();
        }
        List<PrecedenteRecommendationDto> out = new ArrayList<>(Math.min(topK, hits.size()));
        for (int i = 0; i < hits.size() && out.size() < topK; i++) {
            JurisprudenceSearchHit h = hits.get(i);
            List<String> citations = citationExtractor.extract(h.titulo(), h.tese(), h.ementaResumo()).stream()
                    .map(r -> r.targetRef())
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(12)
                    .toList();
            out.add(new PrecedenteRecommendationDto(
                    h.id(),
                    h.fonte() != null ? h.fonte().name() : null,
                    h.tipo() != null ? h.tipo().name() : null,
                    h.identificador(),
                    h.titulo(),
                    h.tese(),
                    h.score(),
                    citations
            ));
        }
        return out;
    }

    private static String safe(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        return s.isBlank() ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        String x = safe(a);
        if (x != null) {
            return x;
        }
        return safe(b);
    }

    private static String joinNonBlank(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String s = safe(p);
            if (s == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private static RamoDireito inferRamo(String materia, String resumo) {
        RamoDireito byMateria = RamoDireito.fromString(materia);
        if (byMateria != null) {
            return byMateria;
        }
        String text = joinNonBlank(materia, resumo);
        if (text.isBlank()) {
            return RamoDireito.CIVIL;
        }
        String norm = normalize(text);
        if (containsAny(norm, "homicid", "furto", "roubo", "trafic", "flagrante", "inquerito", "denuncia", "cpp", "prisao", "lei maria da penha", "lesao corporal")) {
            return RamoDireito.PENAL;
        }
        if (containsAny(norm, "divorci", "guarda", "alimentos", "uniao estavel", "partilha", "inventario", "sucess")) {
            return RamoDireito.FAMILIA;
        }
        if (containsAny(norm, "clt", "verbas rescisorias", "horas extras", "rescisao", "reclamatoria", "trt", "vara do trabalho")) {
            return RamoDireito.TRABALHISTA;
        }
        if (containsAny(norm, "inss", "bpc", "loas", "aposentador", "auxilio", "beneficio")) {
            return RamoDireito.PREVIDENCIARIO;
        }
        if (containsAny(norm, "icms", "iss", "iptu", "cda", "execucao fiscal", "ctn", "tribut")) {
            return RamoDireito.TRIBUTARIO;
        }
        if (containsAny(norm, "cdc", "consumidor", "produto", "servico", "vicio", "defeito", "cobranca indevida")) {
            return RamoDireito.CONSUMIDOR;
        }
        if (containsAny(norm, "improbidade", "mandado de seguranca", "acao popular", "pad", "servidor publico", "licitacao")) {
            return RamoDireito.ADMINISTRATIVO;
        }
        return RamoDireito.CIVIL;
    }

    private static String inferRito(String ritoRaw, RamoDireito ramo, String materia, String resumo) {
        if (ritoRaw != null && !ritoRaw.isBlank()) {
            var resolved = ProceduralCatalogSupport.resolveRito(ritoRaw, ramo != null ? ramo.name() : materia, materia);
            return resolved != null ? resolved.name() : ritoRaw;
        }
        String norm = normalize(joinNonBlank(materia, resumo));
        if (ramo == null) {
            ramo = RamoDireito.CIVIL;
        }
        String inferred = switch (ramo) {
            case PENAL -> {
                if (containsAny(norm, "tribunal do juri", "juri", "pronuncia")) {
                    yield "TRIBUNAL_JURI";
                }
                if (containsAny(norm, "lei maria da penha")) {
                    yield "PENAL_MARIA_DA_PENHA";
                }
                if (containsAny(norm, "drog", "entorpec", "trafic")) {
                    yield "PENAL_LEI_DROGAS";
                }
                yield "PROCEDIMENTO_PENAL_COMUM";
            }
            case TRABALHISTA -> {
                if (containsAny(norm, "inquerito judicial", "falta grave", "art 853")) {
                    yield "TRABALHISTA_INQUERITO_FALTA_GRAVE";
                }
                if (containsAny(norm, "acao de cumprimento", "art 872", "instrumento coletivo", "convencao coletiva", "acordo coletivo descumprido")) {
                    yield "TRABALHISTA_ACAO_CUMPRIMENTO";
                }
                if (containsAny(norm, "sumario", "alcada", "lei 5 584", "lei 5584")) {
                    yield "TRABALHISTA_SUMARIO_ALCADA";
                }
                if (containsAny(norm, "sumarissimo")) {
                    yield "TRABALHISTA_SUMARISSIMO";
                }
                yield "TRABALHISTA_ORDINARIO";
            }
            case PREVIDENCIARIO -> {
                if (containsAny(norm, "jef", "juizado especial federal")) {
                    yield "PREVIDENCIARIO_JEF";
                }
                if (containsAny(norm, "bpc", "loas")) {
                    yield "PREVIDENCIARIO_BPC_LOAS";
                }
                if (containsAny(norm, "auxilio", "incapacidade")) {
                    yield "PREVIDENCIARIO_AUXILIO_INCAPACIDADE";
                }
                if (containsAny(norm, "aposentador")) {
                    yield "PREVIDENCIARIO_APOSENTADORIA";
                }
                yield "PREVIDENCIARIO_COMUM";
            }
            case TRIBUTARIO -> {
                if (containsAny(norm, "execucao fiscal")) {
                    yield "EXECUCAO_FISCAL";
                }
                if (containsAny(norm, "mandado de seguranca", "ms")) {
                    yield "TRIBUTARIO_MANDADO_SEGURANCA";
                }
                if (containsAny(norm, "repeticao", "indebito")) {
                    yield "TRIBUTARIO_REPETICAO_INDEBITO";
                }
                yield "FAZENDA_PUBLICA_CONHECIMENTO";
            }
            case FAMILIA -> {
                if (containsAny(norm, "alimentos")) {
                    yield "CIVIL_FAMILIA_ALIMENTOS";
                }
                if (containsAny(norm, "inventario", "arrolamento")) {
                    yield "CIVIL_INVENTARIO_ARROLAMENTO";
                }
                if (containsAny(norm, "divorci")) {
                    yield "CIVIL_FAMILIA_DIVORCIO";
                }
                yield "COMUM_ORDINARIO";
            }
            case ADMINISTRATIVO -> {
                if (containsAny(norm, "pad", "processo administrativo disciplinar", "sindicancia", "comissao processante")) {
                    yield "ADMINISTRATIVO_PAD";
                }
                if (containsAny(norm, "concurso publico", "edital", "nomeacao", "posse em cargo publico")) {
                    yield "ADMINISTRATIVO_CONCURSO_PUBLICO";
                }
                if (containsAny(norm, "servidor publico", "reenquadramento funcional", "progressao funcional")) {
                    yield "ADMINISTRATIVO_SERVIDORES";
                }
                yield "FAZENDA_PUBLICA_CONHECIMENTO";
            }
            case INFANCIA_JUVENTUDE -> {
                if (containsAny(norm, "ato infracional", "medida socioeducativa", "apuração de ato infracional", "apuracao de ato infracional", "semiliberdade", "internacao")) {
                    yield "INFANCIA_JUVENTUDE_INFRACIONAL";
                }
                if (containsAny(norm, "adocao", "habilitacao a adocao", "estagio de convivencia")) {
                    yield "INFANCIA_JUVENTUDE_ADOCAO";
                }
                if (containsAny(norm, "guarda", "tutela", "curatela") && containsAny(norm, "menor", "crianca", "adolescente", "eca", "infancia")) {
                    yield "INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR";
                }
                yield "INFANCIA_JUVENTUDE_ECA";
            }
            default -> {
                if (containsAny(norm, "juizado", "jec")) {
                    yield "JUIZADO_ESPECIAL";
                }
                if (containsAny(norm, "execucao")) {
                    yield "EXECUCAO_TITULO_EXTRAJUDICIAL";
                }
                if (containsAny(norm, "mandado de seguranca", "ms")) {
                    yield "ESPECIAL_MANDADO_SEGURANCA";
                }
                yield "COMUM_ORDINARIO";
            }
        };
        var resolved = ProceduralCatalogSupport.resolveRito(inferred, ramo.name(), materia);
        return resolved != null ? resolved.name() : inferred;
    }

    private static JurisdictionEngine.Rite mapRite(String rito, RamoDireito ramo) {
        if (rito == null || rito.isBlank()) {
            return ramo == RamoDireito.PENAL ? JurisdictionEngine.Rite.PENAL : JurisdictionEngine.Rite.COMUM;
        }
        String n = rito.trim().toUpperCase(Locale.ROOT);
        if (n.contains("PENAL") || n.contains("JURI") || n.contains("EXECUCAO_PENAL")) {
            return JurisdictionEngine.Rite.PENAL;
        }
        if (n.contains("TRABALHISTA")) {
            return JurisdictionEngine.Rite.TRABALHISTA;
        }
        if (n.contains("TRIBUT") || n.contains("FAZENDA") || n.contains("EXECUCAO_FISCAL")) {
            return JurisdictionEngine.Rite.TRIBUTARIO;
        }
        if (n.contains("PREVID")) {
            return JurisdictionEngine.Rite.PREVIDENCIARIO;
        }
        if (ramo == RamoDireito.FAMILIA) {
            return JurisdictionEngine.Rite.FAMILIA;
        }
        return JurisdictionEngine.Rite.COMUM;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll(" +", " ")
                .toLowerCase();
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> ramoEvidence(String materia, String resumo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("materia", safe(materia));
        out.put("resumoTokens", normalize(safe(resumo)).split(" ").length);
        return out;
    }

    private static Map<String, Object> ritoEvidence(String rito, String materia, String resumo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ritoInformado", safe(rito));
        out.put("materia", safe(materia));
        out.put("resumoTokens", normalize(safe(resumo)).split(" ").length);
        return out;
    }
}
