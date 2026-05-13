package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalExpeditionBatchService {

    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService projectionService;
    private final OfficialDocumentTemplateService officialDocumentTemplateService;

    public SecretariatOperationalExpeditionBatchService(WorkItemRepository workItemRepository,
                                                        SecretariatQueueProjectionService projectionService,
                                                        OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
    }

    @Transactional(readOnly = true)
    public ExpeditionBatchSnapshot avaliar(Processo processo,
                                           SecretariatOperationalRoutingProfile routing,
                                           SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                           SecretariatOperationalActLineService.ActLineSnapshot actLine,
                                           String lote) {
        String batchCode = normalizeBatch(lote);
        List<ExpeditionTemplate> templates = templatesFor(processo, routing, checklist, actLine, batchCode);
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Expedição cartorária em lote é governada por templates operacionais vinculados à secretaria, rito e fase.");
        fundamentos.add("Fila de execução: " + routing.executionQueueCode() + ".");
        fundamentos.add("Inbox institucional: " + routing.executionInboxKey() + ".");
        fundamentos.add("Lote selecionado: " + batchCode + ".");
        if (routing.secrecyAware()) {
            fundamentos.add("Templates sigilosos preservam trilha segregada e prioridade reforçada.");
        }
        if (!checklist.blockers().isEmpty()) {
            fundamentos.add("Há bloqueios operacionais que podem segurar templates marcados como bloqueantes.");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("templateCount", templates.size());
        metrics.put("blockingTemplateCount", templates.stream().filter(ExpeditionTemplate::blocking).count());
        metrics.put("batchCode", batchCode);
        metrics.put("secretariatCode", routing.secretariatCode());
        metrics.put("executionQueueCode", routing.executionQueueCode());
        metrics.put("executionInboxKey", routing.executionInboxKey());
        return new ExpeditionBatchSnapshot(batchCode, List.copyOf(templates), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    @Transactional
    public ExpeditionBatchExecution materializar(Processo processo,
                                                 Usuario actor,
                                                 SecretariatOperationalRoutingProfile routing,
                                                 SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                                 SecretariatOperationalActLineService.ActLineSnapshot actLine,
                                                 String lote) {
        ExpeditionBatchSnapshot snapshot = avaliar(processo, routing, checklist, actLine, lote);
        Instant now = Instant.now();
        List<Long> generatedIds = new ArrayList<>();
        List<Map<String, Object>> documentosFormaisAssinados = new ArrayList<>();
        for (ExpeditionTemplate template : snapshot.templates()) {
            String templateCode = "SECRETARIA:EXPEDICAO:" + snapshot.batchCode() + ':' + routing.routeKey() + ':' + processo.getId() + ':' + template.code();
            WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode)
                    .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
            item.setFaseOrigem(processo.getFaseAtual());
            item.setType(template.workItemType());
            item.setTitulo(template.label() + " — " + routing.secretariatCode());
            item.setDescricao(buildDescription(actor, processo, routing, template, snapshot.batchCode()));
            item.setQueueCode(routing.executionQueueCode());
            item.setInboxKey(routing.executionInboxKey());
            item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
            item.setStatus(WorkItemStatus.PENDENTE);
            item.setPrioridade(template.priority());
            item.setBlocking(template.blocking());
            item.setUf(processo.getUf());
            item.setComarca(processo.getComarca());
            item.setBaseLegal("Template de expedição governada pela secretaria " + routing.secretariatCode() + " no lote " + snapshot.batchCode());
            item.setDueAt(now.plusSeconds(template.dueHours() * 3600L));
            WorkItem saved = workItemRepository.save(item);
            projectionService.upsert(saved, 70 + Math.max(0, (6 - Math.max(1, template.priority())) * 10),
                    List.of("EXPEDICAO_LOTE", snapshot.batchCode(), routing.secretariatCode(), template.code()));
            generatedIds.add(saved.getId());
            renderFormalDocument(processo, actor, routing, snapshot.batchCode(), template, saved)
                    .ifPresent(documentosFormaisAssinados::add);
        }
        return new ExpeditionBatchExecution(snapshot, List.copyOf(generatedIds), List.copyOf(documentosFormaisAssinados));
    }

    private java.util.Optional<Map<String, Object>> renderFormalDocument(Processo processo,
                                                                         Usuario actor,
                                                                         SecretariatOperationalRoutingProfile routing,
                                                                         String batchCode,
                                                                         ExpeditionTemplate template,
                                                                         WorkItem workItem) {
        TemplateDocumentoOficial templateDocumento = mapTemplateDocumento(template);
        if (templateDocumento == null) {
            return java.util.Optional.empty();
        }
        OfficialDocumentTemplateRenderResponse render = officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                templateDocumento,
                template.label() + " — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                buildTemplateVariables(processo, actor, routing, batchCode, template, workItem, templateDocumento),
                Boolean.TRUE,
                Boolean.TRUE
        ));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("workItemId", workItem.getId());
        out.put("templateCode", template.code());
        out.put("templateDocumentoOficial", templateDocumento.name());
        out.put("tituloDocumento", render.tituloDocumento());
        out.put("documentoId", render.documentoId());
        out.put("hashSha256", render.hashSha256());
        out.put("assinaturaQualificada", render.assinaturaQualificada());
        out.put("validacaoSoberana", render.validacaoSoberana());
        out.put("selado", render.selado());
        return java.util.Optional.of(Collections.unmodifiableMap(out));
    }

    private TemplateDocumentoOficial mapTemplateDocumento(ExpeditionTemplate template) {
        return switch (template.code()) {
            case "CERTIDAO_SANEAMENTO", "CONTROLE_SIGILO", "JUNTADA_COMPROVANTES" -> TemplateDocumentoOficial.CERTIDAO;
            case "INTIMACAO_PARTES", "CIENCIA_MP", "PAUTA_CONCILIACAO" -> TemplateDocumentoOficial.INTIMACAO_FORMAL;
            case "OFICIO_ENTE_PUBLICO", "REQUISICAO_ESCOLTA", "RESOLUCAO_BLOQUEIOS" -> TemplateDocumentoOficial.OFICIO;
            default -> null;
        };
    }

    private Map<String, String> buildTemplateVariables(Processo processo,
                                                       Usuario actor,
                                                       SecretariatOperationalRoutingProfile routing,
                                                       String batchCode,
                                                       ExpeditionTemplate template,
                                                       WorkItem workItem,
                                                       TemplateDocumentoOficial templateDocumento) {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        String processoNumero = firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero());
        switch (templateDocumento) {
            case CERTIDAO -> {
                vars.put("fatoCertificado", template.label() + " do lote " + batchCode + " materializado para o processo " + processoNumero + '.');
                vars.put("responsavelCertificacao", firstNonBlank(actor == null ? null : actor.getNome(), routing.secretariatCode(), "UNIDADE_JUDICIAL"));
                vars.put("templateOperacional", template.code());
                vars.put("workItemId", String.valueOf(workItem.getId()));
                vars.put("filaExecucao", routing.executionQueueCode());
            }
            case INTIMACAO_FORMAL -> {
                vars.put("destinatario", resolveFormalRecipient(template));
                vars.put("conteudoIntimacao", template.label() + " na fila " + routing.executionQueueCode() + " para o processo " + processoNumero + '.');
                vars.put("prazoResposta", workItem.getDueAt() == null ? "CONFORME_AGENDA_INSTITUCIONAL" : workItem.getDueAt().toString());
                vars.put("workItemId", String.valueOf(workItem.getId()));
                vars.put("batchCode", batchCode);
            }
            case OFICIO -> {
                vars.put("destinatario", resolveFormalRecipient(template));
                vars.put("objeto", template.label() + " no processo " + processoNumero + '.');
                vars.put("fundamentoLegal", "Expedição cartorária governada pela secretaria " + routing.secretariatCode() + " no lote " + batchCode + '.');
                vars.put("workItemId", String.valueOf(workItem.getId()));
                vars.put("filaExecucao", routing.executionQueueCode());
            }
            default -> {
            }
        }
        return Map.copyOf(vars);
    }

    private String resolveFormalRecipient(ExpeditionTemplate template) {
        return switch (template.code()) {
            case "CIENCIA_MP" -> "MINISTERIO_PUBLICO";
            case "OFICIO_ENTE_PUBLICO" -> "ENTE_PUBLICO_COMPETENTE";
            case "REQUISICAO_ESCOLTA" -> "ORGAO_DE_SEGURANCA_RESPONSAVEL";
            case "PAUTA_CONCILIACAO", "INTIMACAO_PARTES" -> "PARTES_E_REPRESENTANTES";
            default -> "DESTINATARIO_INSTITUCIONAL";
        };
    }

    private List<ExpeditionTemplate> templatesFor(Processo processo,
                                                  SecretariatOperationalRoutingProfile routing,
                                                  SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                                  SecretariatOperationalActLineService.ActLineSnapshot actLine,
                                                  String batchCode) {
        List<ExpeditionTemplate> out = new ArrayList<>();
        out.add(template("CERTIDAO_SANEAMENTO", "Certidão de saneamento e conferência", WorkItemType.CERTIDAO, 2, true, 6));
        out.add(template("INTIMACAO_PARTES", "Intimação das partes e ciência de secretaria", WorkItemType.INTIMACAO, 2, false, 12));
        out.add(template("JUNTADA_COMPROVANTES", "Juntada e consolidação de comprovantes operacionais", WorkItemType.JUNTADA, 3, false, 18));
        if (routing.conciliationPreferred()) {
            out.add(template("PAUTA_CONCILIACAO", "Expedição de pauta conciliatória e confirmação de agenda", WorkItemType.EXPEDICAO, 2, false, 8));
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().isPenalLike()) {
            out.add(template("REQUISICAO_ESCOLTA", "Requisição de escolta, custódia ou apoio de segurança", WorkItemType.EXPEDICAO, 1, true, 4));
            out.add(template("CIENCIA_MP", "Intimação institucional do Ministério Público", WorkItemType.INTIMACAO, 1, false, 6));
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().isFazendaLike()) {
            out.add(template("OFICIO_ENTE_PUBLICO", "Expedição de ofício para ente público e resposta administrativa", WorkItemType.EXPEDICAO, 2, false, 24));
        }
        if (routing.secrecyAware() || processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            out.add(template("CONTROLE_SIGILO", "Controle de circulação documental sigilosa e trilha de acesso", WorkItemType.CERTIDAO, 1, true, 4));
        }
        if (actLine.acts().stream().anyMatch(item -> "AUDIENCIA".equals(item.stage()))) {
            out.add(template("PASTA_AUDIENCIA", "Preparação de pasta de audiência, presença e recursos", WorkItemType.EXPEDICAO, 2, false, 6));
        }
        if (!checklist.blockers().isEmpty()) {
            out.add(template("RESOLUCAO_BLOQUEIOS", "Expedição para remover bloqueios operacionais do checklist", WorkItemType.EXPEDICAO, 1, true, 4));
        }
        if ("MINIMO".equals(batchCode)) {
            return out.stream().filter(ExpeditionTemplate::blocking).toList();
        }
        if ("AUDIENCIA".equals(batchCode)) {
            return out.stream().filter(template -> template.code().contains("PAUTA") || template.code().contains("AUDIENCIA") || template.code().contains("PRESENCA")).toList();
        }
        return List.copyOf(out);
    }

    private ExpeditionTemplate template(String code,
                                        String label,
                                        WorkItemType type,
                                        int priority,
                                        boolean blocking,
                                        int dueHours) {
        return new ExpeditionTemplate(code, label, type, priority, blocking, dueHours);
    }

    private String buildDescription(Usuario actor,
                                    Processo processo,
                                    SecretariatOperationalRoutingProfile routing,
                                    ExpeditionTemplate template,
                                    String batchCode) {
        List<String> lines = new ArrayList<>();
        lines.add("Lote: " + batchCode);
        lines.add("Template: " + template.code());
        lines.add("Ator: " + actor.getNome() + " (#" + actor.getId() + ")");
        lines.add("Secretaria: " + routing.secretariatCode());
        lines.add("Fila de execução: " + routing.executionQueueCode());
        lines.add("Inbox institucional: " + routing.executionInboxKey());
        lines.add("Processo: " + processo.getId() + " — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        lines.add("Descrição operacional: " + template.label());
        return String.join("\n", lines);
    }

    private String normalizeBatch(String lote) {
        if (lote == null || lote.isBlank()) {
            return "PADRAO";
        }
        String normalized = lote.trim().toUpperCase(Locale.ROOT).replace('Ç', 'C');
        if (normalized.startsWith("MIN")) {
            return "MINIMO";
        }
        if (normalized.startsWith("AUD")) {
            return "AUDIENCIA";
        }
        return "PADRAO";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record ExpeditionBatchSnapshot(
            String batchCode,
            List<ExpeditionTemplate> templates,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record ExpeditionBatchExecution(
            ExpeditionBatchSnapshot snapshot,
            List<Long> generatedWorkItemIds,
            List<Map<String, Object>> documentosFormaisAssinados
    ) {
    }

    public record ExpeditionTemplate(
            String code,
            String label,
            WorkItemType workItemType,
            int priority,
            boolean blocking,
            int dueHours
    ) {
    }
}
