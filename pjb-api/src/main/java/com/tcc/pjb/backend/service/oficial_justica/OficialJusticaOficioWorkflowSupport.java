package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualRequest;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualResult;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class OficialJusticaOficioWorkflowSupport {

    private OficialJusticaOficioWorkflowSupport() {
    }

    static ResolucaoDestinatarioProcessualResult resolveDestinatario(DestinatarioProcessualResolverApplicationService resolver,
                                                                     OficialJusticaOficioRequest request) {
        OficialJusticaOficioRequest.DestinatarioInstitucionalRequest structured = request.destinatarioEstruturado();
        return resolver.resolver(new ResolucaoDestinatarioProcessualRequest(
                null,
                null,
                null,
                null,
                structured != null ? structured.destinatarioProcessualKind() : null,
                structured != null && structured.destinatarioInstitucionalKind() != null ? structured.destinatarioInstitucionalKind() : inferDestinatarioInstitucional(request.destinatario()),
                structured != null ? structured.papelProcessualInstitucional() : null,
                structured != null ? structured.unidadeInstitucionalCodigo() : null,
                structured != null ? structured.documento() : null,
                structured != null && structured.nome() != null && !structured.nome().isBlank() ? structured.nome() : request.destinatario(),
                structured != null ? structured.email() : null,
                structured != null ? structured.telefone() : null,
                structured != null ? structured.oabNumero() : null,
                structured != null ? structured.govbrAccountId() : null,
                structured != null ? structured.uf() : null,
                structured != null ? structured.comarca() : null,
                structured != null ? structured.foro() : null,
                structured != null ? structured.possuiContaGovBr() : null,
                structured != null ? structured.possuiAdvogado() : null,
                structured != null ? structured.fazendaPublica() : null,
                structured != null ? structured.intimacaoPessoalInstitucional() : null,
                null,
                Boolean.TRUE
        ));
    }

    static Map<String, Object> buildDestinatarioMap(ResolucaoDestinatarioProcessualResult destinatario,
                                                    OficialJusticaOficioRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfNotNull(out, "nomeExibicao", destinatario.destinatario().nomeExibicao());
        putIfNotNull(out, "documentoPrincipal", destinatario.destinatario().documentoPrincipal());
        putIfNotNull(out, "hashResolucao", destinatario.hashResolucao());
        putIfNotNull(out, "trilho", destinatario.trilho().name());
        putIfNotNull(out, "destinatarioProcessualKind", destinatario.destinatario().kind().name());
        putIfNotNull(out, "destinatarioInstitucionalKind", destinatario.destinatario().destinatarioInstitucionalKind() != null ? destinatario.destinatario().destinatarioInstitucionalKind().name() : null);
        putIfNotNull(out, "papelProcessualInstitucional", destinatario.destinatario().papelProcessualInstitucional() != null ? destinatario.destinatario().papelProcessualInstitucional().name() : null);
        putIfNotNull(out, "unidadeInstitucionalCodigo", destinatario.destinatario().unidadeInstitucionalCodigo());
        putIfNotNull(out, "email", destinatario.destinatario().email());
        putIfNotNull(out, "telefone", destinatario.destinatario().telefone());
        putIfNotNull(out, "destinatarioLivre", request.destinatario());
        putIfNotNull(out, "justificativas", destinatario.justificativas());
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    static WorkItem criarJuntadaDiretaNoProcesso(WorkItemRepository workItemRepository,
                                                 Processo processo,
                                                 Usuario usuario,
                                                 WorkItem principal,
                                                 OficialJusticaOficioRequest request,
                                                 Map<String, Object> minutaGovernada,
                                                 boolean resposta) {
        WorkItem juntada = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode((resposta ? "JUNTADA_DIRETA_RESPOSTA_OFICIO_ORIGINAL:" : "JUNTADA_DIRETA_OFICIO_ORIGINAL:") + principal.getId())
                .type(WorkItemType.JUNTADA)
                .titulo(resposta ? "Juntada direta da resposta original do oficial de justiça" : "Juntada direta do ofício original do oficial de justiça")
                .descricao(composeDirectFilingDescricao(request, principal.getId(), minutaGovernada, resposta))
                .queueCode("PROCESSO_DIRETO_OFICIAL")
                .inboxKey("PROCESSO_DIRETO:" + processo.getId())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .blocking(false)
                .dueAt(Instant.now())
                .uf(firstNonBlank(processo.getUf(), usuario.getUf()))
                .comarca(firstNonBlank(processo.getComarca(), usuario.getComarca()))
                .baseLegal(normalizeFundamento(request.fundamento()))
                .build();
        return workItemRepository.save(juntada);
    }

    static Map<String, Object> directProcessDispatchTopology(Processo processo,
                                                             Usuario usuario,
                                                             WorkItem juntadaDireta,
                                                             String executionId,
                                                             Map<String, Object> destinatarioMap,
                                                             Map<String, Object> minutaGovernada,
                                                             boolean resposta) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", resposta ? "JUNTADA_DIRETA_RESPOSTA_ORIGINAL_PROCESSO" : "JUNTADA_DIRETA_OFICIO_ORIGINAL_PROCESSO");
        out.put("currentChannel", "PROCESSO_DIRETO_PJB");
        out.put("protocoladoDiretoNoProcesso", Boolean.TRUE);
        out.put("executionId", executionId);
        out.put("workItemId", juntadaDireta.getId());
        LinkedHashMap<String, Object> caixa = new LinkedHashMap<>();
        caixa.put("expectedCartorioAck", Boolean.FALSE);
        caixa.put("executionId", executionId);
        caixa.put("workItemId", juntadaDireta.getId());
        caixa.put("queueCode", firstNonBlank(juntadaDireta.getQueueCode(), "PROCESSO_DIRETO_OFICIAL"));
        caixa.put("inboxKey", firstNonBlank(juntadaDireta.getInboxKey(), "PROCESSO_DIRETO:" + processo.getId()));
        caixa.put("unidadeCodigo", firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getTribunalCodigoRoteado(), usuario.getUf() + ":" + usuario.getComarca(), "PJB:PROCESSO_DIRETO"));
        out.put("caixaInstitucional", caixa.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new)));
        LinkedHashMap<String, Object> externalDispatch = new LinkedHashMap<>();
        externalDispatch.put("status", "DIRECT_PROCESS_FILING");
        externalDispatch.put("providerStatus", "PJB_PROCESS_TIMELINE");
        externalDispatch.put("deliveryStatus", "PROTOCOLADO_DIRETO_NO_PROCESSO");
        externalDispatch.put("providerReference", executionId);
        out.put("externalDispatch", externalDispatch);
        LinkedHashMap<String, Object> reconciliation = new LinkedHashMap<>();
        reconciliation.put("mode", "OFICIAL_OFICIO_DIRETO_PROCESSO");
        reconciliation.put("executionId", executionId);
        reconciliation.put("contentHash", minutaGovernada.get("contentHash"));
        reconciliation.put("destinatarioHash", destinatarioMap.get("hashResolucao"));
        reconciliation.put("workItemId", juntadaDireta.getId());
        reconciliation.put("materializedAt", Instant.now().toString());
        reconciliation.put("divergent", Boolean.FALSE);
        out.put("reconciliationMaterialized", reconciliation.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new)));
        return out;
    }

    static String composeOficioDescricao(OficialJusticaOficioRequest request,
                                         boolean resposta,
                                         OficialJusticaOficioCatalogService.OficioTypeDefinition oficioType,
                                         OficialJusticaOficioCatalogService.TemplateDefinition template,
                                         Map<String, Object> destinatarioResolvido,
                                         Map<String, Object> minutaGovernada) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", resposta ? "RESPOSTA_OFICIO_OFICIAL_JUSTICA" : "OFICIO_OFICIAL_JUSTICA");
        payload.put("assunto", request.assunto());
        payload.put("destinatario", request.destinatario());
        payload.put("destinatarioResolvido", destinatarioResolvido);
        payload.put("conteudo", request.conteudo());
        payload.put("fundamento", normalizeFundamento(request.fundamento()));
        payload.put("referenciaMandadoId", request.referenciaMandadoId());
        payload.put("tipoOficio", oficioType != null ? oficioType.asMap() : Map.of());
        payload.put("templateCode", template != null ? template.code() : null);
        payload.put("templateHash", minutaGovernada != null ? minutaGovernada.get("contentHash") : null);
        payload.put("inlineMediaCount", request.midiaInline() == null ? 0 : request.midiaInline().size());
        payload.put("documentosProbatorios", request.provasDocumentais() == null ? 0 : request.provasDocumentais().size());
        payload.put("documentosRepresentacao", request.documentosRepresentacao() == null ? 0 : request.documentosRepresentacao().size());
        payload.put("documentosAnexados", request.documentosAnexados() == null ? 0 : request.documentosAnexados().size());
        return payload.toString();
    }

    static String normalizeFundamento(String fundamento) {
        String text = fundamento == null ? null : fundamento.trim();
        return text == null || text.isEmpty() ? "Fundamento institucional do oficial de justiça" : text;
    }

    private static String composeDirectFilingDescricao(OficialJusticaOficioRequest request,
                                                       Long principalId,
                                                       Map<String, Object> minutaGovernada,
                                                       boolean resposta) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", resposta ? "RESPOSTA_OFICIO_ORIGINAL_GOVERNADA" : "OFICIO_ORIGINAL_GOVERNADO");
        payload.put("principalWorkItemId", principalId);
        payload.put("assunto", request.assunto());
        payload.put("destinatario", request.destinatario());
        payload.put("fundamento", normalizeFundamento(request.fundamento()));
        payload.put("templateCode", minutaGovernada.get("templateCode"));
        payload.put("contentHash", minutaGovernada.get("contentHash"));
        payload.put("renderedBody", minutaGovernada.get("renderedBody"));
        payload.put("originalOnly", Boolean.TRUE);
        payload.put("anexosAdicionaisBloqueados", Boolean.TRUE);
        payload.put("midiaInlineBloqueada", Boolean.TRUE);
        return payload.toString();
    }

    private static com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind inferDestinatarioInstitucional(String destinatario) {
        return com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind.fromTexto(destinatario);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
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
