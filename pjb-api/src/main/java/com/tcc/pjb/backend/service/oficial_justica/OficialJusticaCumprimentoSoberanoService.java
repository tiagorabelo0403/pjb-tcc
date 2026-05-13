package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCumprimentoEncerramentoResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationResponse;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaCumprimentoEncerramentoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.profile.DiligenceAutomaticFilingService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalClosureService;
import com.tcc.pjb.backend.service.profile.DiligenceProcessFormalizationService;
import com.tcc.pjb.backend.service.secretariat.oficial.SecretariaOficialCumprimentoRoutingService;
import java.time.Instant;
import java.util.ArrayList;
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
public class OficialJusticaCumprimentoSoberanoService {

    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);

    private final CurrentUserService currentUserService;
    private final WorkItemRepository workItemRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final DiligenceOperationalClosureService closureService;
    private final DiligenceProcessFormalizationService formalizationService;
    private final DiligenceAutomaticFilingService filingService;
    private final OficialJusticaNotificationCenterService notificationCenterService;
    private final OficialJusticaPainelService painelService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final SecretariaOficialCumprimentoRoutingService secretariatRoutingService;
    private final PainelServiceCommons commons;
    private final com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService officialDocumentTemplateService;

    public OficialJusticaCumprimentoSoberanoService(CurrentUserService currentUserService,
                                                    WorkItemRepository workItemRepository,
                                                    NotificationHistoryRepository notificationHistoryRepository,
                                                    DiligenceOperationalClosureService closureService,
                                                    DiligenceProcessFormalizationService formalizationService,
                                                    DiligenceAutomaticFilingService filingService,
                                                    OficialJusticaNotificationCenterService notificationCenterService,
                                                    OficialJusticaPainelService painelService,
                                                    OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                    SecretariaOficialCumprimentoRoutingService secretariatRoutingService,
                                                    PainelServiceCommons commons,
                                                    com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.closureService = Objects.requireNonNull(closureService);
        this.formalizationService = Objects.requireNonNull(formalizationService);
        this.filingService = Objects.requireNonNull(filingService);
        this.notificationCenterService = Objects.requireNonNull(notificationCenterService);
        this.painelService = Objects.requireNonNull(painelService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.secretariatRoutingService = Objects.requireNonNull(secretariatRoutingService);
        this.commons = Objects.requireNonNull(commons);
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
    }

    @Transactional
    public OficialJusticaCumprimentoEncerramentoResponse encerrar(String mandadoId,
                                                                  OficialJusticaCumprimentoEncerramentoRequest request) {
        Usuario oficial = currentUserService.getRequired();
        validateOfficial(oficial);
        WorkItem mandado = resolveMandado(mandadoId, oficial);
        Processo processo = Objects.requireNonNull(mandado.getProcesso(), "processo_requerido");
        OficialJusticaCumprimentoEncerramentoRequest safe = request == null
                ? new OficialJusticaCumprimentoEncerramentoRequest(null, null, null, null, null, null, List.of(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE)
                : request;
        boolean cienciaObrigatoria = requiresScience(mandado);
        Map<String, Object> cienciaProcessual = Map.of();
        boolean cienciaConfirmada = hasConfirmedScience(oficial.getId(), processo.getId());
        if (cienciaObrigatoria && !cienciaConfirmada) {
            if (!safe.confirmarCienciaSePendenteResolvido()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ciência obrigatória pendente antes do encerramento do cumprimento");
            }
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "confirmação de ciência do oficial exige etapa de dois fatores e assinatura qualificada no endpoint dedicado antes do encerramento do cumprimento");
        }
        boolean oficioOriginalObrigatorio = requiresOriginalOnly(mandado);
        boolean emitirOficioDireto = oficioOriginalObrigatorio || safe.emitirOficioOriginalDiretoResolvido();
        Map<String, Object> oficioOriginalDireto = Map.of();
        boolean oficioOriginalEmitido = false;
        List<String> alerts = new ArrayList<>();
        if (emitirOficioDireto) {
            oficioOriginalDireto = painelService.emitirOficio(processo.getId(), buildGovernedOriginalOficioRequest(mandado, processo, oficial, safe, safe.outcomeResolvido().name()));
            oficioOriginalEmitido = "OFICIO_REGISTRADO_DIRETO_NO_PROCESSO".equals(String.valueOf(oficioOriginalDireto.get("status")));
            if (!oficioOriginalEmitido) {
                alerts.add("OFICIO_ORIGINAL_DIRETO_NAO_CONFIRMADO");
            }
        }
        DiligenceOperationalClosureResponse closure = closureService.close(
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                normalizedReference(mandadoId, mandado),
                new DiligenceOperationalClosureRequest(
                        safe.certidaoId(),
                        safe.checkpointEventId(),
                        safe.outcomeResolvido(),
                        safe.idempotencyKey(),
                        safe.evidenceChaveCustodia(),
                        safe.observacoes(),
                        safe.documentoIds()
                )
        );
        DiligenceProcessFormalizationResponse formalizacao = formalizationService.formalize(
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                normalizedReference(mandadoId, mandado),
                new DiligenceProcessFormalizationRequest(
                        closure.encerramentoId(),
                        closure.certidaoId(),
                        suffixKey(safe.idempotencyKey(), "FORM"),
                        safe.registrarMovimentacaoResolvido(),
                        safe.gerarMinutaResolvido(),
                        buildMinuteTitle(processo, safe),
                        buildFormalizationComplement(mandado, processo, safe, closure),
                        safe.evidenceChaveCustodia()
                )
        );
        DiligenceAutomaticFilingResponse filing = filingService.file(
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                normalizedReference(mandadoId, mandado),
                new DiligenceAutomaticFilingRequest(
                        formalizacao.formalizacaoId(),
                        suffixKey(safe.idempotencyKey(), "FILE"),
                        safe.gerarPacotePdfResolvido(),
                        safe.registrarMovimentacaoResolvido(),
                        safe.exportarMalhaExternaResolvido(),
                        safe.externalSystemCode(),
                        buildPacketTitle(processo, safe, closure),
                        buildFilingComplement(processo, safe, closure)
                )
        );
        OficialJusticaCumprimentoEncerramentoResponse.ChecklistSummary checklist = buildChecklist(safe, closure, formalizacao, filing, cienciaConfirmada, oficioOriginalEmitido, oficioOriginalObrigatorio);
        Map<String, Object> recebimentoSecretaria = secretariatRoutingService.registrarRecebimentoAutomatico(
                processo,
                mandado,
                oficial,
                safe.outcomeResolvido(),
                checklist.digestSha256(),
                filing.bundleReference(),
                cienciaConfirmada,
                oficioOriginalEmitido,
                mandado.getDueAt()
        );
        appendSovereignClosureTrace(mandado, processo, oficial, safe, closure, formalizacao, filing, checklist, cienciaObrigatoria, cienciaConfirmada, oficioOriginalObrigatorio, oficioOriginalEmitido);
        commons.publishUserHistory(oficial, "OFICIAL", "ENCERRAMENTO_SOBERANO_CUMPRIMENTO", "Encerramento soberano do cumprimento registrado no processo " + processNumber(processo) + ".", processo, mandado.getId());
        if (oficioOriginalEmitido) {
            commons.publishTerritoryHistory(oficial, "OFICIAL", "ENCERRAMENTO_SOBERANO_COM_OFICIO_DIRETO", "Ofício original governado emitido diretamente no processo após o encerramento soberano do cumprimento.", processo, mandado.getId());
        }
        OfficialDocumentTemplateRenderResponse mandadoFormal = renderMandadoFormal(mandado, processo, officialTitle(oficial), closure, safe);
        OfficialDocumentTemplateRenderResponse certidaoFormal = renderCertidaoFormal(mandado, processo, closure, safe);
        OfficialDocumentTemplateRenderResponse autoCumprimento = renderAutoCumprimento(mandado, processo, closure, safe);
        Map<String, Object> processoContexto = contextEnvelopeService.processEnvelope(oficial, processo, mandado, null, null);
        Map<String, Object> oficialResponsavel = contextEnvelopeService.oficialEnvelope(oficial, null);
        return new OficialJusticaCumprimentoEncerramentoResponse(
                "ENCERRAMENTO_SOBERANO_CONCLUIDO",
                Instant.now(),
                processo.getId(),
                processNumber(processo),
                mandado.getId(),
                normalizedReference(mandadoId, mandado),
                closure.outcome(),
                cienciaObrigatoria,
                cienciaConfirmada,
                oficioOriginalObrigatorio,
                oficioOriginalEmitido,
                checklist,
                new OficialJusticaCumprimentoEncerramentoResponse.OperationalBundle(
                        closure.encerramentoId(),
                        closure.certidaoId(),
                        formalizacao.formalizacaoId(),
                        formalizacao.minutaDocumentoId(),
                        filing.juntadaId(),
                        filing.pacoteDocumentoId(),
                        filing.bundleReference(),
                        filing.bundleDigestSha256(),
                        closure.workItemStatusFinal()
                ),
                cienciaProcessual,
                oficioOriginalDireto,
                summarizeRenderedDocument(mandadoFormal),
                summarizeRenderedDocument(certidaoFormal),
                summarizeRenderedDocument(autoCumprimento),
                recebimentoSecretaria,
                processoContexto,
                oficialResponsavel,
                List.copyOf(alerts)
        );
    }

    private OfficialDocumentTemplateRenderResponse renderMandadoFormal(WorkItem mandado,
                                                                       Processo processo,
                                                                       String oficialNome,
                                                                       DiligenceOperationalClosureResponse closure,
                                                                       OficialJusticaCumprimentoEncerramentoRequest request) {
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                TemplateDocumentoOficial.MANDADO,
                "Mandado judicial materializado — " + processNumber(processo) + " — " + humanize(request.tipoDiligenciaResolvido()),
                Map.of(
                        "qualificacaoPartes", processNumber(processo) + " | Oficial: " + firstNonBlank(oficialNome, "OFICIAL_NAO_IDENTIFICADO"),
                        "ordemJudicial", buildFormalizationComplement(mandado, processo, request, closure),
                        "prazoCumprimento", String.valueOf(mandado.getDueAt() == null ? Instant.now() : mandado.getDueAt())
                ),
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private OfficialDocumentTemplateRenderResponse renderCertidaoFormal(WorkItem mandado,
                                                                        Processo processo,
                                                                        DiligenceOperationalClosureResponse closure,
                                                                        OficialJusticaCumprimentoEncerramentoRequest request) {
        TemplateDocumentoOficial template = closure.outcome() != null && closure.outcome().contains("FRUSTRADO")
                ? TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO
                : TemplateDocumentoOficial.CERTIDAO_CUMPRIMENTO;
        Map<String, String> variaveis = template == TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO
                ? Map.of(
                        "motivoFrustracao", firstNonBlank(request.observacoes(), closure.outcome(), "FRUSTRACAO_OPERACIONAL_REGISTRADA"),
                        "dataDiligencia", String.valueOf(Instant.now()),
                        "responsavelCertificacao", officialTitle(currentUserService.getRequired())
                )
                : Map.of(
                        "atoCumprido", buildFormalizationComplement(mandado, processo, request, closure),
                        "dataCumprimento", String.valueOf(Instant.now()),
                        "responsavelCertificacao", officialTitle(currentUserService.getRequired())
                );
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                template,
                (template == TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO ? "Certidão de não cumprimento — " : "Certidão de cumprimento — ") + processNumber(processo),
                variaveis,
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private OfficialDocumentTemplateRenderResponse renderAutoCumprimento(WorkItem mandado,
                                                                         Processo processo,
                                                                         DiligenceOperationalClosureResponse closure,
                                                                         OficialJusticaCumprimentoEncerramentoRequest request) {
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                TemplateDocumentoOficial.AUTO_CUMPRIMENTO,
                "Auto de cumprimento — " + processNumber(processo) + " — " + humanize(request.tipoDiligenciaResolvido()),
                Map.of(
                        "referenciaMandado", normalizedReference(String.valueOf(mandado.getId()), mandado),
                        "descricaoCumprimento", buildFormalizationComplement(mandado, processo, request, closure),
                        "resultadoDiligencia", humanize(closure.outcome())
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

    private String officialTitle(Usuario oficial) {
        return oficial == null ? "OFICIAL_NAO_IDENTIFICADO" : firstNonBlank(oficial.getNome(), oficial.getPerfil(), oficial.getTipoUsuario() != null ? oficial.getTipoUsuario().name() : null, "OFICIAL_NAO_IDENTIFICADO");
    }

    private void validateOfficial(Usuario oficial) {
        if (oficial == null || oficial.getId() == null || oficial.getTipoUsuario() == null || !OFFICIAL_ROLES.contains(oficial.getTipoUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "usuário atual não é Oficial de Justiça válido para encerramento soberano");
        }
    }

    private WorkItem resolveMandado(String mandadoId, Usuario oficial) {
        if (mandadoId == null || mandadoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mandado_referencia_obrigatoria");
        }
        WorkItem item = parseLong(mandadoId) != null
                ? workItemRepository.findById(parseLong(mandadoId)).orElse(null)
                : workItemRepository.findByTemplateCode(mandadoId.trim()).orElse(null);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "mandado não encontrado");
        }
        boolean assignedToCurrent = item.getAssignedUser() != null && Objects.equals(item.getAssignedUser().getId(), oficial.getId());
        boolean roleCompatible = item.getAssignedRole() != null && OFFICIAL_ROLES.contains(item.getAssignedRole());
        boolean allowed = item.getAssignedUser() == null ? roleCompatible : assignedToCurrent;
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "mandado fora da alçada operacional do oficial atual");
        }
        if (item.isSemInteresse() || item.getStatus() == WorkItemStatus.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "mandado sem janela operacional válida para encerramento soberano");
        }
        return item;
    }

    private boolean hasConfirmedScience(Long usuarioId, Long processoId) {
        if (usuarioId == null || processoId == null) {
            return false;
        }
        return notificationHistoryRepository.findTop50ByUsuarioIdAndProcessoIdOrderByEnviadoEmDesc(usuarioId, processoId).stream()
                .map(NotificationHistory::getCienciaConfirmadaEm)
                .anyMatch(Objects::nonNull);
    }

    private boolean requiresScience(WorkItem item) {
        return item != null && (item.isBlocking() || containsOperationalMarker(item, "CIÊNCIA OBRIGATÓRIA: SIM") || containsOperationalMarker(item, "CIENCIA OBRIGATORIA: SIM"));
    }

    private boolean requiresOriginalOnly(WorkItem item) {
        return item != null && (containsOperationalMarker(item, "OFÍCIO ORIGINAL GOVERNADO ONLY") || containsOperationalMarker(item, "OFICIO ORIGINAL GOVERNADO ONLY"));
    }

    private boolean containsOperationalMarker(WorkItem item, String marker) {
        if (marker == null || marker.isBlank() || item == null) {
            return false;
        }
        String needle = marker.toUpperCase();
        return upper(item.getDescricao()).contains(needle)
                || upper(item.getBaseLegal()).contains(needle)
                || upper(item.getTitulo()).contains(needle);
    }

    private OficialJusticaOficioRequest buildGovernedOriginalOficioRequest(WorkItem mandado,
                                                                           Processo processo,
                                                                           Usuario oficial,
                                                                           OficialJusticaCumprimentoEncerramentoRequest request,
                                                                           String outcomeCode) {
        String outcome = outcomeCode == null || outcomeCode.isBlank() ? "CUMPRIMENTO_POSITIVO" : outcomeCode;
        String fundamento = firstNonBlank(mandado.getBaseLegal(), "Ofício original governado emitido no encerramento soberano do cumprimento pelo Oficial de Justiça.");
        String conteudo = String.join(" ",
                "Encerramento soberano do cumprimento registrado no PJB para o processo " + processNumber(processo) + ".",
                "Resultado operacional: " + humanize(outcome) + ".",
                "Tipo de diligência: " + humanize(request.tipoDiligenciaResolvido()) + ".",
                request.observacoes() == null ? "" : "Observações oficiais: " + request.observacoes() + '.').trim();
        return new OficialJusticaOficioRequest(
                "Ofício original de encerramento do cumprimento — " + processNumber(processo),
                "PROCESSO_JUDICIAL_BRASILEIRO",
                conteudo,
                fundamento,
                normalizedReference(String.valueOf(mandado.getId()), mandado),
                "OFICIO_CUMPRIMENTO_MANDADO",
                "MINUTA_CUMPRIMENTO_MANDADO",
                null,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE
        );
    }

    private OficialJusticaCumprimentoEncerramentoResponse.ChecklistSummary buildChecklist(OficialJusticaCumprimentoEncerramentoRequest request,
                                                                                          DiligenceOperationalClosureResponse closure,
                                                                                          DiligenceProcessFormalizationResponse formalizacao,
                                                                                          DiligenceAutomaticFilingResponse filing,
                                                                                          boolean cienciaConfirmada,
                                                                                          boolean oficioOriginalEmitido,
                                                                                          boolean oficioOriginalObrigatorio) {
        LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
        raw.put("tipoDiligencia", request.tipoDiligenciaResolvido());
        raw.put("mandadoConferido", request.checklistMandadoConferidoResolvido());
        raw.put("alvoConfirmado", request.checklistAlvoConfirmadoResolvido());
        raw.put("enderecoConferido", request.checklistEnderecoConferidoResolvido());
        raw.put("resultadoRegistrado", request.checklistResultadoRegistradoResolvido());
        raw.put("cadeiaCustodiaConferida", request.checklistCadeiaCustodiaConferidaResolvido());
        raw.put("certidaoGerada", closure.certidaoId() != null);
        raw.put("formalizacaoGerada", formalizacao.formalizacaoId() != null);
        raw.put("juntadaGerada", filing.juntadaId() != null);
        raw.put("cienciaConfirmada", cienciaConfirmada);
        raw.put("oficioOriginalDiretoConcluido", !oficioOriginalObrigatorio || oficioOriginalEmitido);
        String digest = Hashes.sha256Hex(raw.toString());
        return new OficialJusticaCumprimentoEncerramentoResponse.ChecklistSummary(
                request.tipoDiligenciaResolvido(),
                request.checklistMandadoConferidoResolvido(),
                request.checklistAlvoConfirmadoResolvido(),
                request.checklistEnderecoConferidoResolvido(),
                request.checklistResultadoRegistradoResolvido(),
                request.checklistCadeiaCustodiaConferidaResolvido(),
                closure.certidaoId() != null,
                formalizacao.formalizacaoId() != null,
                filing.juntadaId() != null,
                cienciaConfirmada,
                !oficioOriginalObrigatorio || oficioOriginalEmitido,
                digest
        );
    }

    private void appendSovereignClosureTrace(WorkItem mandado,
                                             Processo processo,
                                             Usuario oficial,
                                             OficialJusticaCumprimentoEncerramentoRequest request,
                                             DiligenceOperationalClosureResponse closure,
                                             DiligenceProcessFormalizationResponse formalizacao,
                                             DiligenceAutomaticFilingResponse filing,
                                             OficialJusticaCumprimentoEncerramentoResponse.ChecklistSummary checklist,
                                             boolean cienciaObrigatoria,
                                             boolean cienciaConfirmada,
                                             boolean oficioOriginalObrigatorio,
                                             boolean oficioOriginalEmitido) {
        String marker = "encerramento_soberano_digest=" + checklist.digestSha256();
        String current = mandado.getDescricao();
        if (current != null && current.contains(marker)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (current != null && !current.isBlank()) {
            sb.append(current.trim()).append("\n\n");
        }
        sb.append("encerramento_soberano_tipo=").append(request.tipoDiligenciaResolvido()).append('\n');
        sb.append("encerramento_soberano_outcome=").append(closure.outcome()).append('\n');
        sb.append("encerramento_soberano_processo=").append(processNumber(processo)).append('\n');
        sb.append("encerramento_soberano_certidao_id=").append(closure.certidaoId()).append('\n');
        sb.append("encerramento_soberano_formalizacao_id=").append(formalizacao.formalizacaoId()).append('\n');
        sb.append("encerramento_soberano_juntada_id=").append(filing.juntadaId()).append('\n');
        sb.append("encerramento_soberano_bundle_ref=").append(nullSafe(filing.bundleReference())).append('\n');
        sb.append("encerramento_soberano_oficial=").append(firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO")).append('\n');
        sb.append("encerramento_soberano_ciencia_obrigatoria=").append(cienciaObrigatoria).append('\n');
        sb.append("encerramento_soberano_ciencia_confirmada=").append(cienciaConfirmada).append('\n');
        sb.append("encerramento_soberano_oficio_original_obrigatorio=").append(oficioOriginalObrigatorio).append('\n');
        sb.append("encerramento_soberano_oficio_original_emitido=").append(oficioOriginalEmitido).append('\n');
        sb.append("encerramento_soberano_digest=").append(checklist.digestSha256()).append('\n');
        sb.append("encerramento_soberano_observacoes=").append(nullSafe(request.observacoes())).append('\n');
        mandado.setDescricao(sb.toString());
        if (closure.workItemStatusFinal() != null) {
            try {
                mandado.setStatus(WorkItemStatus.valueOf(closure.workItemStatusFinal()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        mandado.setSemInteresse(false);
        workItemRepository.save(mandado);
    }

    private String normalizedReference(String mandadoId, WorkItem mandado) {
        if (mandadoId != null && !mandadoId.isBlank()) {
            return mandadoId.trim();
        }
        if (mandado != null && mandado.getId() != null) {
            return String.valueOf(mandado.getId());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referencia_do_mandado_nao_resolvida");
    }

    private String buildMinuteTitle(Processo processo, OficialJusticaCumprimentoEncerramentoRequest request) {
        return "Minuta de encerramento soberano — " + processNumber(processo) + " — " + humanize(request.tipoDiligenciaResolvido());
    }

    private String buildFormalizationComplement(WorkItem mandado,
                                                Processo processo,
                                                OficialJusticaCumprimentoEncerramentoRequest request,
                                                DiligenceOperationalClosureResponse closure) {
        return String.join(" ",
                "Formalização soberana do cumprimento do Oficial no processo " + processNumber(processo) + ".",
                "Mandado: " + normalizedReference(String.valueOf(mandado.getId()), mandado) + ".",
                "Resultado: " + humanize(closure.outcome()) + ".",
                "Tipo de diligência: " + humanize(request.tipoDiligenciaResolvido()) + ".",
                request.observacoes() == null ? "" : "Observações: " + request.observacoes() + '.').trim();
    }

    private String buildPacketTitle(Processo processo,
                                    OficialJusticaCumprimentoEncerramentoRequest request,
                                    DiligenceOperationalClosureResponse closure) {
        return "Pacote de juntada do encerramento soberano — " + processNumber(processo) + " — " + humanize(closure.outcome()) + " — " + humanize(request.tipoDiligenciaResolvido());
    }

    private String buildFilingComplement(Processo processo,
                                         OficialJusticaCumprimentoEncerramentoRequest request,
                                         DiligenceOperationalClosureResponse closure) {
        return String.join(" ",
                "Juntada automática do encerramento soberano do Oficial no processo " + processNumber(processo) + ".",
                "Resultado: " + humanize(closure.outcome()) + ".",
                "Tipo de diligência: " + humanize(request.tipoDiligenciaResolvido()) + ".").trim();
    }

    private String suffixKey(String base, String suffix) {
        if (base == null || base.isBlank()) {
            return null;
        }
        String normalized = base.trim().toUpperCase();
        String candidate = normalized + '_' + suffix;
        return candidate.length() <= 64 ? candidate : candidate.substring(0, 64);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String processNumber(Processo processo) {
        if (processo == null) {
            return "PROCESSO_NAO_IDENTIFICADO";
        }
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado(), "PROCESSO_NAO_IDENTIFICADO");
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase();
    }

    private String humanize(String code) {
        if (code == null || code.isBlank()) {
            return "NÃO INFORMADO";
        }
        return code.replace('_', ' ').trim();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
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
