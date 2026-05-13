package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RefreshScope
@Component
public class EvidenceSufficiencyPlanner {

    private final AiSufficiencyPlannerProperties props;

    public EvidenceSufficiencyPlanner(AiSufficiencyPlannerProperties props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    public EvidenceSufficiencyPlan plan(AiTelemetryDomain domain,
                                        String capabilityRaw,
                                        ApiVersion version,
                                        IARequest request,
                                        EvidenceQualityReport quality,
                                        EvidenceContradictionReport contradiction) {
        AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
        String cap = CapabilityStrings.canonical(capabilityRaw);
        if (cap == null || cap.isBlank()) cap = "UNKNOWN";
        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        List<String> template = resolveTemplate(d, cap, v);
        List<String> missing = filterByPayloadPresence(template, request);

        if (contradiction != null && contradiction.inconsistencyScore() >= 0.55 && v.isAtLeast(ApiVersion.V3)) {
            List<String> more = new ArrayList<>(missing);
            if (d == AiTelemetryDomain.LEGAL) {
                more.add("Defina recorte temporal (ano) e tribunal/UF do caso para reduzir conflito entre precedentes.");
            } else {
                more.add("Informe período-base e normas aplicáveis (regulatório) para reduzir conflito entre fontes.");
            }
            missing = List.copyOf(more);
        }

        List<String> queryExpansions = new ArrayList<>();
        if (quality != null) {
            if (quality.sourceDiversity() < 2 && v.isAtLeast(ApiVersion.V2)) {
                queryExpansions.addAll(defaultQueryExpansions(d, cap, v));
            }
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("domain", d.tag());
        meta.put("capability", cap);
        meta.put("version", v.name());
        meta.put("templateSize", template.size());
        meta.put("missingSize", missing.size());
        if (contradiction != null) {
            meta.put("inconsistencyScore", contradiction.inconsistencyScore());
            meta.put("contradictionScore", contradiction.contradictionScore());
        }

        return new EvidenceSufficiencyPlan(missing, queryExpansions, meta);
    }

    private List<String> resolveTemplate(AiTelemetryDomain domain, String capability, ApiVersion v) {
        AiSufficiencyPlannerProperties.DomainRules rules = props.getDomains().get(domain.tag());
        if (rules == null) {
            return safeDefaults(domain, capability, v);
        }
        String verKey = (v != null ? v.name() : ApiVersion.latest().name()).toUpperCase(Locale.ROOT);

        List<String> spec = null;
        Map<String, Map<String, List<String>>> cv = rules.getCapabilityVersion();
        if (cv != null) {
            Map<String, List<String>> byVer = cv.get(capability);
            if (byVer != null) {
                spec = byVer.get(verKey);
            }
        }
        if (spec != null && !spec.isEmpty()) return List.copyOf(spec);

        Map<String, List<String>> cd = rules.getCapabilityDefaults();
        if (cd != null) {
            List<String> capDef = cd.get(capability);
            if (capDef != null && !capDef.isEmpty()) return List.copyOf(capDef);
        }

        Map<String, List<String>> vd = rules.getVersionDefaults();
        if (vd != null) {
            List<String> verDef = vd.get(verKey);
            if (verDef != null && !verDef.isEmpty()) return List.copyOf(verDef);
        }

        List<String> base = rules.getDefaultRequests();
        if (base != null && !base.isEmpty()) return List.copyOf(base);

        return safeDefaults(domain, capability, v);
    }

    private static List<String> filterByPayloadPresence(List<String> template, IARequest request) {
        if (template == null || template.isEmpty()) return List.of();
        if (request == null || request.getPayload() == null || request.getPayload().isEmpty()) {
            return List.copyOf(template);
        }
        Set<String> keys = request.getPayload().keySet();
        List<String> out = new ArrayList<>();
        for (String t : template) {
            if (t == null || t.isBlank()) continue;
            if (payloadLikelyContains(keys, t)) continue;
            out.add(t);
        }
        return List.copyOf(out);
    }

    private static boolean payloadLikelyContains(Set<String> keys, String prompt) {
        if (keys == null || keys.isEmpty() || prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        for (String k : keys) {
            if (k == null) continue;
            String kk = k.toLowerCase(Locale.ROOT);
            if (kk.isBlank()) continue;
            if (p.contains(kk)) return true;
            if (kk.contains("idade") && p.contains("idade")) return true;
            if (kk.contains("age") && p.contains("idade")) return true;
            if (kk.contains("sexo") && p.contains("sexo")) return true;
            if (kk.contains("gender") && p.contains("sexo")) return true;
            if (kk.contains("tribunal") && p.contains("tribunal")) return true;
            if (kk.contains("uf") && p.contains("uf")) return true;
            if (kk.contains("valor") && p.contains("valor")) return true;
        }
        return false;
    }

    private static List<String> defaultQueryExpansions(AiTelemetryDomain d, String cap, ApiVersion v) {
        List<String> out = new ArrayList<>();
        if (d == AiTelemetryDomain.LEGAL) {
            out.add("jurisprudência");
            out.add("precedente qualificado");
            if (v.isAtLeast(ApiVersion.V3)) out.add("tese repetitiva");
        } else {
            out.add("regulatório");
            out.add("compliance");
            if (v.isAtLeast(ApiVersion.V3)) out.add("norma atualizada");
        }
        return List.copyOf(out);
    }

    private static List<String> safeDefaults(AiTelemetryDomain domain, String capability, ApiVersion v) {
        List<String> out = new ArrayList<>();
        if (domain == AiTelemetryDomain.LEGAL) {
            out.add("Ramo do direito e assunto/matéria do caso.");
            out.add("Tribunal/UF e estágio processual (inicial, recurso, execução).");
            out.add("Datas relevantes (fato, ciência/intimação, protocolo) para prazos.");
            out.add("Documentos-chave (contrato, notificações, decisões) e pedidos.");
            if (capability != null && capability.contains("PRAZO")) {
                out.add("Tipo de ato/intimação e regra de contagem aplicável (dias úteis/corridos)." );
            }
        } else {
            out.add("Valores envolvidos (custas/receitas/despesas) e moeda.");
            out.add("Período de referência e evento gerador (ex.: protocolo, pagamento, obrigação).");
            out.add("Regras/regulamentos aplicáveis (quando houver) e documentos de suporte.");
            if (v != null && v.isAtLeast(ApiVersion.V3)) {
                out.add("Contexto operacional (volume, recorrência, risco) para estimativa mais precisa.");
            }
        }
        return List.copyOf(out);
    }
}
