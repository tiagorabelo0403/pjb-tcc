package com.tcc.pjb.backend.service.recursal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.recursal.routing.RecursalWorkItemPlannerService;
import com.tcc.pjb.backend.service.recursal.routing.WorkItemSpec;

@Service
public class RecursalWorkItemMaterializerService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final RecursalWorkItemPlannerService planner;
    private final PjbTimeService time;

    public RecursalWorkItemMaterializerService(ProcessoRepository processoRepository,
                                               WorkItemRepository workItemRepository,
                                               RecursalWorkItemPlannerService planner,
                                               PjbTimeService time) {
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.planner = planner;
        this.time = time;
    }

    @Transactional
    public void materialize(Long processoId, CanonicalFact fact, RecursalPlan plan) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(fact, "fact é obrigatório");

        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        List<WorkItemSpec> specs = planner.plan(processo, fact, plan);
        if (specs == null || specs.isEmpty()) return;

        Jurisdicao j = processo.getJurisdicao();
        String uf = j != null ? safeTrim(j.getUf()) : null;
        String comarca = j != null ? safeTrim(j.getCidade()) : null;

        for (WorkItemSpec s : specs) {
            if (s == null) continue;
            if (s.title() == null || s.title().isBlank()) continue;

            String templateCode = buildTemplateCode(fact, s);

            if (workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode, WorkItemStatus.CANCELADO).isPresent()) {
                continue;
            }

            String queueCode = safeTrim(s.queueCode());
            String inboxKey = safeTrim(s.inboxKey());

            TipoUsuario role = s.assignedRole() != null ? s.assignedRole() : routeRole(queueCode, s.title());
            int prioridade = s.priority() != null ? s.priority() : routePrioridade(queueCode, s.title());

            WorkItem wi = WorkItem.builder()
                    .processo(processo)
                    .faseOrigem(FaseProcessual.RECURSAL)
                    .templateCode(templateCode)
                    .type(WorkItemType.RECURSO)
                    .titulo(s.title().trim())
                    .descricao(buildDescricao(queueCode, s.description()))
                    .queueCode(queueCode)
                    .inboxKey(inboxKey)
                    .assignedRole(role)
                    .status(WorkItemStatus.PENDENTE)
                    .prioridade(prioridade)
                    .blocking(s.blocking())
                    .dueAt(time.endOfDayLegal(s.dueDate()))
                    .uf(uf)
                    .comarca(comarca)
                    .baseLegal(null)
                    .build();

            workItemRepository.save(wi);
        }
    }

    private static String buildDescricao(String queueCode, String description) {
        String q = safeTrim(queueCode);
        String desc = safeTrim(description);
        if (q == null && desc == null) return null;
        if (q == null) return desc;
        if (desc == null) return "Fila/Localizador: " + q;
        return "Fila/Localizador: " + q + "\n" + desc;
    }

    private static int routePrioridade(String queueCode, String title) {
        String q = normalize(queueCode);
        String t = normalize(title);
        if (q.contains("URGEN") || t.contains("URGEN") || t.contains("LIMINAR")) return 1;
        if (q.contains("GAB") || q.contains("PREV") || t.contains("PREVEN")) return 2;
        if (q.contains("TRIAGEM") || q.contains("DISTRIB")) return 2;
        return 3;
    }

    private static TipoUsuario routeRole(String queueCode, String title) {
        String q = normalize(queueCode);
        String t = normalize(title);
        String v = q + " " + t;

        if (v.contains("MINIST") || v.contains(":STF:") || v.contains(":STJ:") || v.contains("SUPERIOR")) {
            return TipoUsuario.MINISTRO;
        }
        if (v.contains("DESEMB") || v.contains(":2G:") || v.contains("SEGUNDO_GRAU") || v.contains("TURMA")) {
            return TipoUsuario.DESEMBARGADOR;
        }
        if (v.contains("JUIZ") || v.contains(":1G:") || v.contains("PRIMEIRO_GRAU")) {
            return TipoUsuario.JUIZ;
        }
        if (v.contains("MP") || v.contains("MINISTERIO_PUBLICO") || v.contains("PROMOTOR") || v.contains("PROCURADOR")) {
            return TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
        }
        if (v.contains("DEFENSOR") || v.contains("DPU") || v.contains("DPE")) {
            return TipoUsuario.DEFENSOR_PUBLICO;
        }
        if (v.contains("SECRETARIA") || v.contains("TRIAGEM") || v.contains("DISTRIB")) {
            return TipoUsuario.SERVIDOR_FORUM;
        }
        if (v.contains("CONTRARRAZ") || v.contains("PETIC") || v.contains("ADVOG")) {
            return TipoUsuario.ADVOGADO;
        }
        return null;
    }

    private static String buildTemplateCode(CanonicalFact fact, WorkItemSpec s) {
        String dedup = safeTrim(fact == null ? null : fact.dedupKey());
        if (dedup == null) {
            dedup = "NO_DEDUP";
        }
        String q = safeTrim(s.queueCode());
        String title = safeTrim(s.title());
        String h = sha256Hex((q == null ? "" : q) + "|" + (title == null ? "" : title));
        return "PJB_RECURSAL:" + dedup + ":" + (q == null ? "SEM_FILA" : q) + ":" + h.substring(0, 16);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(s));
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isBlank() ? null : v;
    }
}
