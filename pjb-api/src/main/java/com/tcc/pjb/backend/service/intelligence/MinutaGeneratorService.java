package com.tcc.pjb.backend.service.intelligence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.templates.PromptTemplate;
import com.tcc.pjb.backend.ai.templates.TemplateRenderer;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageRequest;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageResponse;
import com.tcc.pjb.backend.model.dto.intelligence.MinutaPreviewRequest;
import com.tcc.pjb.backend.model.dto.intelligence.MinutaPreviewResponse;
import com.tcc.pjb.backend.model.dto.intelligence.RitoPlanResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.intelligence.edge.EdgeAiService;
import com.tcc.pjb.backend.service.material.MaterialPackService;
import com.tcc.pjb.backend.service.material.model.MaterialProfile;
import com.tcc.pjb.backend.service.rito.RitoPlanService;

@Service
public class MinutaGeneratorService {

    private final CaseTriageService triageService;
    private final EdgeAiService edgeAiService;
    private final RitoPlanService ritoPlanService;
    private final MaterialPackService materialPackService;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;

    public MinutaGeneratorService(CaseTriageService triageService,
                                  EdgeAiService edgeAiService,
                                  RitoPlanService ritoPlanService,
                                  MaterialPackService materialPackService,
                                  CurrentUserService currentUserService,
                                  AuditLedgerService auditLedgerService) {
        this.triageService = triageService;
        this.edgeAiService = edgeAiService;
        this.ritoPlanService = ritoPlanService;
        this.materialPackService = materialPackService;
        this.currentUserService = currentUserService;
        this.auditLedgerService = auditLedgerService;
    }

    public MinutaPreviewResponse preview(MinutaPreviewRequest req) {
        String previewId = UUID.randomUUID().toString();

        PromptTemplate template = parseTemplate(req.template());

        CaseTriageResponse triage = triageService.triage(new CaseTriageRequest(
                req.resumo(),
                req.materia(),
                req.orgao(),
                "BRASIL",
                null,
                req.uf(),
                req.comarca(),
                req.rito(),
                8
        ));

        RitoPlanResponse plan = ritoPlanService.plan(triage.ritoSugerido() != null ? triage.ritoSugerido().name() : null);
        MaterialProfile material = materialPackService.resolve(triage.ramoSugerido(), triage.ritoSugerido());

        String base = buildDeterministicMinuta(template, triage, req.resumo(), plan, material);

        Optional<String> ai = edgeAiService.tryPredictMinuta(req.resumo());

        String content = ai
                .map(pred -> base + "\n\n---\n\n" + "### Complemento (Edge AI local)\n" + pred)
                .orElse(base);

        Map<String, Object> dbg = new LinkedHashMap<>();
        dbg.put("template", template.name());
        dbg.put("triageId", triage.triageId());
        dbg.put("edgeAiUsed", ai.isPresent());

        try {
            Usuario u = currentUserService.getOrNull();
            String action = actionFor(u);
            String detalhes = "template=" + template.name()
                    + ";ramo=" + (triage.ramoSugerido() != null ? triage.ramoSugerido().name() : "")
                    + ";rito=" + (triage.ritoSugerido() != null ? triage.ritoSugerido().name() : "")
                    + ";edgeAiUsed=" + ai.isPresent();
            auditLedgerService.appendSafely(action, "IA_MINUTA_PREVIEW", previewId, detalhes);
        } catch (Exception ignored) {
        }

        return new MinutaPreviewResponse(previewId, Instant.now(), content, dbg);
    }

    private static PromptTemplate parseTemplate(String raw) {
        if (raw == null || raw.isBlank()) return PromptTemplate.NONE;
        String token = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return PromptTemplate.valueOf(token);
        } catch (Exception ignored) {
            return PromptTemplate.NONE;
        }
    }

    private static String buildDeterministicMinuta(PromptTemplate template,
                                                   CaseTriageResponse triage,
                                                   String resumo,
                                                   RitoPlanResponse plan,
                                                   MaterialProfile material) {
        String estrutura = TemplateRenderer.render(template);

        String enderecamento = triage.jurisdicao() != null && triage.jurisdicao().label() != null
                ? triage.jurisdicao().label()
                : "Juízo competente (a definir)";

        StringBuilder sb = new StringBuilder();
        sb.append("# Minuta (prévia)\n\n");
        sb.append("**Endereçamento sugerido:** ").append(enderecamento).append("\n\n");
        sb.append("**Ramo sugerido:** ").append(triage.ramoSugerido()).append("\n\n");
        sb.append("**Rito sugerido:** ").append(triage.ritoSugerido()).append("\n\n");

        sb.append("---\n\n");
        sb.append("## Estrutura\n").append(estrutura).append("\n\n");

        sb.append("## 1) Fatos (resumo informado)\n");
        sb.append(resumo == null ? "" : resumo.trim()).append("\n\n");

        sb.append("## 2) Fundamentos (roteiro)\n");
        if (triage.jurisdicao() != null) {
            if (triage.jurisdicao().legalBases() != null && !triage.jurisdicao().legalBases().isEmpty()) {
                sb.append("- Bases legais/constitucionais relacionadas: ");
                sb.append(String.join("; ", triage.jurisdicao().legalBases())).append("\n");
            }
            if (triage.jurisdicao().authorities() != null && !triage.jurisdicao().authorities().isEmpty()) {
                sb.append("- Autoridades/órgãos relacionados: ");
                sb.append(String.join(", ", triage.jurisdicao().authorities())).append("\n");
            }
        }
        sb.append("- Enquadrar pedidos e provas conforme o rito sugerido e as particularidades do caso.\n\n");

        if (triage.jurisprudencia() != null && !triage.jurisprudencia().isEmpty()) {
            sb.append("## 3) Jurisprudência (top sugestões)\n");
            int n = 0;
            for (var p : triage.jurisprudencia()) {
                n++;
                sb.append(n).append(") ").append(nullSafe(p.titulo())).append(" — ")
                        .append(nullSafe(p.identificador())).append(" (")
                        .append(nullSafe(p.fonte())).append(")\n");
                if (p.citations() != null && !p.citations().isEmpty()) {
                    sb.append("   - Citações: ").append(String.join(", ", p.citations())).append("\n");
                }
                if (n >= 5) break;
            }
            sb.append("\n");
        }

        if (material != null && ((material.getRequiredDocuments() != null && !material.getRequiredDocuments().isEmpty())
                || (material.getProofChecklist() != null && !material.getProofChecklist().isEmpty()))) {
            sb.append("## 4) Checklist de documentos e provas\n");
            if (material.getRequiredDocuments() != null && !material.getRequiredDocuments().isEmpty()) {
                sb.append("**Documentos típicos (mínimos):**\n");
                int n = 0;
                for (String d : material.getRequiredDocuments()) {
                    if (d == null || d.isBlank()) continue;
                    sb.append("- ").append(d.trim()).append("\n");
                    if (++n >= 10) break;
                }
                sb.append("\n");
            }
            if (material.getProofChecklist() != null && !material.getProofChecklist().isEmpty()) {
                sb.append("**Provas / validações:**\n");
                int n = 0;
                for (String d : material.getProofChecklist()) {
                    if (d == null || d.isBlank()) continue;
                    sb.append("- ").append(d.trim()).append("\n");
                    if (++n >= 10) break;
                }
                sb.append("\n");
            }
            if (material.getWarnings() != null && !material.getWarnings().isEmpty()) {
                sb.append("**Alertas:**\n");
                int n = 0;
                for (String w : material.getWarnings()) {
                    if (w == null || w.isBlank()) continue;
                    sb.append("- ").append(w.trim()).append("\n");
                    if (++n >= 6) break;
                }
                sb.append("\n");
            }
        }

        if (plan != null && plan.stages() != null && !plan.stages().isEmpty()) {
            sb.append("## 5) Workflow do rito (primeiras tarefas)\n");
            var first = plan.stages().get(0);
            sb.append("**Primeira fase:** ").append(nullSafe(first.fase())).append("\n\n");
            if (first.work() != null && !first.work().isEmpty()) {
                int n = 0;
                for (var w : first.work()) {
                    sb.append("- ").append(nullSafe(w.title()));
                    if (w.slaDays() != null) sb.append(" (SLA: ").append(w.slaDays()).append("d)");
                    sb.append("\n");
                    if (++n >= 8) break;
                }
            } else {
                sb.append("- (Sem tarefas parametrizadas nesta fase no pack atual)\n");
            }
            sb.append("\n");
        }

        sb.append("## 6) Pedidos (checklist)\n");
        sb.append("- Pedido principal (mérito)\n");
        sb.append("- Tutela de urgência (se houver fumus/periculum)\n");
        sb.append("- Provas (documental, testemunhal, pericial)\n");
        sb.append("- Gratuidade (se cabível)\n");
        sb.append("- Condenação em custas e honorários (se aplicável)\n\n");

        sb.append("---\n\n");
        sb.append("**Nota:** esta minuta é um rascunho estruturado. A decisão final de estratégia/rito é humana (segurança jurídica).\n");
        return sb.toString();
    }

    private static String nullSafe(String v) {
        if (v == null) return "";
        return v.trim();
    }

    private static String actionFor(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return "IA_MINUTA_PREVIEW";
        TipoUsuario t = u.getTipoUsuario();
        if (t == TipoUsuario.ADVOGADO) return "ADV_IA_MINUTA_PREVIEW";
        if (t == TipoUsuario.CIDADAO) return "CID_IA_MINUTA_PREVIEW";
        if (t == TipoUsuario.MAGISTRADO) return "MAG_IA_MINUTA_PREVIEW";
        if (t == TipoUsuario.SERVIDOR) return "SRV_IA_MINUTA_PREVIEW";
        return "IA_MINUTA_PREVIEW";
    }
}
