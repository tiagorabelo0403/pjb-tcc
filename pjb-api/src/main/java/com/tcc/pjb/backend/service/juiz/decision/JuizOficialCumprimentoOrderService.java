package com.tcc.pjb.backend.service.juiz.decision;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizOrdemCumprimentoOficialRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.ForumOfficialReturnReactivationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnOperationalService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaNotificationCenterService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class JuizOficialCumprimentoOrderService {

    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);

    private final ProcessoRepository processoRepository;
    private final PerfilDashboardContextFactory contextFactory;
    private final PjbAuthorizationService authorizationService;
    private final ForumOfficialReturnOperationalService forumOfficialReturnOperationalService;
    private final WorkItemRepository workItemRepository;
    private final OficialJusticaNotificationCenterService notificationCenterService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final PainelServiceCommons commons;
    private final com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService officialDocumentTemplateService;

    public JuizOficialCumprimentoOrderService(ProcessoRepository processoRepository,
                                              PerfilDashboardContextFactory contextFactory,
                                              PjbAuthorizationService authorizationService,
                                              ForumOfficialReturnOperationalService forumOfficialReturnOperationalService,
                                              WorkItemRepository workItemRepository,
                                              OficialJusticaNotificationCenterService notificationCenterService,
                                              OficialJusticaContextEnvelopeService contextEnvelopeService,
                                              PainelServiceCommons commons,
                                              com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.forumOfficialReturnOperationalService = Objects.requireNonNull(forumOfficialReturnOperationalService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.notificationCenterService = Objects.requireNonNull(notificationCenterService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.commons = Objects.requireNonNull(commons);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
    }

    @Transactional
    public Map<String, Object> ordenarCumprimento(Long processoId, JuizOrdemCumprimentoOficialRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario juiz = ctx.usuario();
        authorizationService.requireRole(juiz, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL");
        JuizOrdemCumprimentoOficialRequest safe = request == null
                ? new JuizOrdemCumprimentoOficialRequest(null, "Cumprimento judicial determinado no PJB.", null, null, null, null, null, null, null, Boolean.TRUE, Boolean.TRUE, null)
                : request;
        Instant dueAt = safe.dueAtResolvido() != null ? safe.dueAtResolvido() : Instant.now().plus(48, ChronoUnit.HOURS);
        ForumOfficialReturnReactivationRequest reactivationRequest = new ForumOfficialReturnReactivationRequest(
                safe.oficialId(),
                "JUIZO_DECISORIO",
                safe.fundamentoResolvido(),
                buildJudgeObservation(safe),
                dueAt,
                Boolean.FALSE
        );
        Map<String, Object> automatic = forumOfficialReturnOperationalService.reativarPorExpedicaoAutomatica(
                processo,
                reactivationRequest,
                "OFICIAL DE JUSTIÇA",
                safe.conteudoOperacionalResolvido()
        );
        WorkItem officialItem = resolveOfficialItem(automatic);
        Usuario oficial = officialItem.getAssignedUser();
        validateOfficial(oficial);
        officialItem.setType(WorkItemType.DILIGENCIA);
        officialItem.setStatus(WorkItemStatus.PENDENTE);
        officialItem.setSemInteresse(false);
        officialItem.setPrioridade(safe.prioridadeResolvida());
        officialItem.setBlocking(safe.cienciaObrigatoriaResolvida() || safe.prioridadeResolvida() == 0);
        officialItem.setDueAt(dueAt);
        officialItem.setTitulo(buildTitle(processo, safe));
        officialItem.setDescricao(buildDescription(processo, juiz, oficial, safe));
        officialItem.setBaseLegal(buildBaseLegal(processo, juiz, safe));
        officialItem = workItemRepository.save(officialItem);
        boolean notified = notificationCenterService.dispatchJudicialOrder(
                officialItem,
                safe.cienciaObrigatoriaResolvida(),
                safe.janelaTerritorialResolvida(),
                safe.tipoCumprimentoResolvido(),
                "JUIZO_DECISORIO"
        );
        String historyMessage = "Ordem judicial de cumprimento expedida ao Oficial de Justiça no processo "
                + processNumber(processo)
                + " com prioridade " + safe.prioridadeResolvida()
                + " e prazo " + dueAt + '.';
        commons.publishUserHistory(juiz, "JUIZ", "ORDEM_JUDICIAL_CUMPRIMENTO_OFICIAL", historyMessage, processo, officialItem.getId());
        commons.publishUserHistory(oficial, "OFICIAL", "ORDEM_JUDICIAL_CUMPRIMENTO_OFICIAL", historyMessage, processo, officialItem.getId());

        LinkedHashMap<String, Object> ordem = new LinkedHashMap<>();
        ordem.put("tipoCumprimento", safe.tipoCumprimentoResolvido());
        ordem.put("fundamento", safe.fundamentoResolvido());
        ordem.put("conteudoOperacional", safe.conteudoOperacionalResolvido());
        putIfNotBlank(ordem, "janelaTerritorial", safe.janelaTerritorialResolvida());
        putIfNotBlank(ordem, "bairroPreferencial", safe.bairroPreferencialResolvido());
        putIfNotBlank(ordem, "microterritorio", safe.microterritorioResolvido());
        putIfNotBlank(ordem, "observacao", safe.observacaoResolvida());
        ordem.put("prioridade", safe.prioridadeResolvida());
        ordem.put("dueAt", dueAt);
        ordem.put("cienciaObrigatoria", safe.cienciaObrigatoriaResolvida());
        ordem.put("oficioOriginalSomenteNoEncerramento", safe.exigirOficioOriginalNoEncerramentoResolvido());

        LinkedHashMap<String, Object> encerramento = new LinkedHashMap<>();
        encerramento.put("oficioDiretoOriginalOnly", safe.exigirOficioOriginalNoEncerramentoResolvido());
        encerramento.put("pathOficioDireto", "/api/v1/oficial-justica/processos/" + processoId + "/oficios");
        encerramento.put("pathRespostaDireta", "/api/v1/oficial-justica/processos/" + processoId + "/oficios/resposta");
        encerramento.put("cienciaPath", "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao");
        encerramento.put("securityMode", "ORIGINAL_GOVERNADO_ONLY_NO_ENCERRAMENTO");
        encerramento.put("midiaInlinePermitida", Boolean.FALSE);
        encerramento.put("documentosAdicionaisPermitidos", Boolean.FALSE);

        LinkedHashMap<String, Object> paths = new LinkedHashMap<>();
        paths.put("painelPath", "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench");
        paths.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        paths.put("balcaoVirtualPath", "/api/v1/oficial-justica/balcao-virtual/processos/" + processoId + "/sala");
        paths.put("notificacoesPath", "/api/v1/oficial-justica/notificacoes?limit=20");

        OfficialDocumentTemplateRenderResponse mandadoFormal = renderMandadoFormal(processo, juiz, oficial, safe, dueAt);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ORDEM_JUDICIAL_CUMPRIMENTO_EXPEDIDA");
        out.put("mode", "JUIZO_DECISORIO_OFICIAL_EXECUTION_V1");
        out.put("processoId", processoId);
        out.put("processoNumero", processNumber(processo));
        out.put("workItemId", officialItem.getId());
        out.put("reactivationAuditHash", automatic.get("reactivationAuditHash"));
        out.put("reativacaoAutomatica", automatic.get("reativacao"));
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(oficial, null));
        out.put("processoContexto", contextEnvelopeService.processEnvelope(oficial, processo, officialItem, null, null));
        out.put("ordemJudicial", safeCopy(ordem));
        out.put("mandadoFormalAssinado", summarizeRenderedDocument(mandadoFormal));
        out.put("encerramentoGovernado", safeCopy(encerramento));
        out.put("paths", safeCopy(paths));
        out.put("notificacaoInstantanea", notified);
        out.put("cienciaObrigatoria", safe.cienciaObrigatoriaResolvida());
        out.put("processoReapareceNoPainelDoOficial", Boolean.TRUE);
        out.put("oficioDiretoOriginalOnlyNoEncerramento", safe.exigirOficioOriginalNoEncerramentoResolvido());
        return safeCopy(out);
    }

    private OfficialDocumentTemplateRenderResponse renderMandadoFormal(Processo processo,
                                                                       Usuario juiz,
                                                                       Usuario oficial,
                                                                       JuizOrdemCumprimentoOficialRequest request,
                                                                       Instant dueAt) {
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                TemplateDocumentoOficial.MANDADO,
                "Mandado judicial de cumprimento — " + processNumber(processo) + " — " + request.tipoCumprimentoResolvido(),
                Map.of(
                        "qualificacaoPartes", processNumber(processo) + " | Oficial: " + firstNonBlank(oficial != null ? oficial.getNome() : null, "OFICIAL_NAO_IDENTIFICADO"),
                        "ordemJudicial", buildDescription(processo, juiz, oficial, request),
                        "prazoCumprimento", String.valueOf(dueAt)
                ),
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private Map<String, Object> summarizeRenderedDocument(OfficialDocumentTemplateRenderResponse render) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("documentoId", render.documentoId());
        out.put("template", render.template().name());
        out.put("tituloDocumento", render.tituloDocumento());
        out.put("hashSha256", render.hashSha256());
        out.put("assinaturaQualificada", render.assinaturaQualificada());
        out.put("validacaoSoberana", render.validacaoSoberana());
        out.put("selado", render.selado());
        return Collections.unmodifiableMap(out);
    }

    private WorkItem resolveOfficialItem(Map<String, Object> automatic) {
        Object nested = automatic == null ? null : automatic.get("reativacao");
        Object workItemId = null;
        if (nested instanceof Map<?, ?> map) {
            workItemId = map.get("workItemId");
        }
        if (workItemId == null && automatic != null) {
            workItemId = automatic.get("workItemId");
        }
        Long id = asLong(workItemId);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "não foi possível resolver o work item do oficial após a ordem judicial");
        }
        return workItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "work item do oficial não encontrado após a ordem judicial"));
    }

    private void validateOfficial(Usuario oficial) {
        if (oficial == null || oficial.getId() == null || oficial.getTipoUsuario() == null || !OFFICIAL_ROLES.contains(oficial.getTipoUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a ordem judicial não conseguiu resolver um Oficial de Justiça válido");
        }
    }

    private String buildTitle(Processo processo, JuizOrdemCumprimentoOficialRequest request) {
        return "Ordem judicial de cumprimento — " + request.tipoCumprimentoResolvido() + " — " + processNumber(processo);
    }

    private String buildDescription(Processo processo,
                                    Usuario juiz,
                                    Usuario oficial,
                                    JuizOrdemCumprimentoOficialRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.conteudoOperacionalResolvido());
        sb.append(" Fundamentação: ").append(request.fundamentoResolvido()).append('.');
        sb.append(" Processo: ").append(processNumber(processo)).append('.');
        sb.append(" Vara: ").append(firstNonBlank(processo != null ? processo.getVara() : null, processo != null ? processo.getComarca() : null, "VARA_NAO_IDENTIFICADA")).append('.');
        sb.append(" Oficial responsável: ").append(firstNonBlank(oficial != null ? oficial.getNome() : null, "OFICIAL_NAO_IDENTIFICADO")).append('.');
        if (request.janelaTerritorialResolvida() != null) {
            sb.append(" Janela territorial: ").append(request.janelaTerritorialResolvida()).append('.');
        }
        if (request.bairroPreferencialResolvido() != null) {
            sb.append(" Bairro preferencial: ").append(request.bairroPreferencialResolvido()).append('.');
        }
        if (request.microterritorioResolvido() != null) {
            sb.append(" Microterritório: ").append(request.microterritorioResolvido()).append('.');
        }
        sb.append(" Ciência obrigatória: ").append(request.cienciaObrigatoriaResolvida() ? "SIM" : "NAO").append('.');
        sb.append(" Encerramento com ofício original governado only: ").append(request.exigirOficioOriginalNoEncerramentoResolvido() ? "SIM" : "NAO").append('.');
        if (juiz != null && juiz.getNome() != null) {
            sb.append(" Magistrado expedidor: ").append(juiz.getNome()).append('.');
        }
        if (request.observacaoResolvida() != null) {
            sb.append(" Observação: ").append(request.observacaoResolvida()).append('.');
        }
        return sb.toString();
    }

    private String buildBaseLegal(Processo processo, Usuario juiz, JuizOrdemCumprimentoOficialRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ordem judicial de cumprimento processual no PJB.");
        sb.append(" Fundamento: ").append(request.fundamentoResolvido()).append('.');
        sb.append(" Tipo: ").append(request.tipoCumprimentoResolvido()).append('.');
        sb.append(" Prioridade operacional: ").append(request.prioridadeResolvida()).append('.');
        sb.append(" Ciência obrigatória: ").append(request.cienciaObrigatoriaResolvida() ? "SIM" : "NAO").append('.');
        sb.append(" Ofício original governado only no encerramento: ").append(request.exigirOficioOriginalNoEncerramentoResolvido() ? "SIM" : "NAO").append('.');
        if (request.janelaTerritorialResolvida() != null) {
            sb.append(" Janela territorial: ").append(request.janelaTerritorialResolvida()).append('.');
        }
        if (request.bairroPreferencialResolvido() != null) {
            sb.append(" Bairro preferencial: ").append(request.bairroPreferencialResolvido()).append('.');
        }
        if (request.microterritorioResolvido() != null) {
            sb.append(" Microterritório: ").append(request.microterritorioResolvido()).append('.');
        }
        if (processo != null && processo.getTribunal() != null) {
            sb.append(" Tribunal: ").append(processo.getTribunal()).append('.');
        }
        if (juiz != null && juiz.getNome() != null) {
            sb.append(" Magistrado expedidor: ").append(juiz.getNome()).append('.');
        }
        return sb.toString();
    }

    private String buildJudgeObservation(JuizOrdemCumprimentoOficialRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.observacaoResolvida() != null) {
            sb.append(request.observacaoResolvida()).append(' ');
        }
        if (request.janelaTerritorialResolvida() != null) {
            sb.append("Janela territorial: ").append(request.janelaTerritorialResolvida()).append(". ");
        }
        if (request.bairroPreferencialResolvido() != null) {
            sb.append("Bairro preferencial: ").append(request.bairroPreferencialResolvido()).append(". ");
        }
        if (request.microterritorioResolvido() != null) {
            sb.append("Microterritório: ").append(request.microterritorioResolvido()).append(". ");
        }
        sb.append("Ciência obrigatória: ").append(request.cienciaObrigatoriaResolvida() ? "SIM" : "NAO").append(". ");
        sb.append("Ofício original governado only no encerramento: ").append(request.exigirOficioOriginalNoEncerramentoResolvido() ? "SIM" : "NAO").append('.');
        return sb.toString().trim();
    }

    private String processNumber(Processo processo) {
        if (processo == null) {
            return "PROCESSO_NAO_IDENTIFICADO";
        }
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado(), "PROCESSO_NAO_IDENTIFICADO");
    }

    private Long asLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void putIfNotBlank(Map<String, Object> out, String key, String value) {
        if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
            out.put(key, value);
        }
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        if (input != null) {
            input.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    safe.put(key, value);
                }
            });
        }
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
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
}
