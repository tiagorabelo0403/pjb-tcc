package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaCommunicationFormalModelService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final OfficialDocumentTemplateService officialDocumentTemplateService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;

    public OficialJusticaCommunicationFormalModelService(OfficialDocumentTemplateService officialDocumentTemplateService,
                                                         OficialJusticaContextEnvelopeService contextEnvelopeService) {
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
    }

    public Map<String, Object> buildProfile(Processo processo, WorkItem item, Usuario oficial) {
        String natureza = resolveNaturezaComunicacao(processo, item, null);
        String justicaAxis = resolveJusticaAxis(processo, item, oficial);
        String tribunalCodigo = resolveTribunalCodigo(processo, item);
        String rito = processo != null && processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO";
        String ramo = resolveRamo(processo, rito);
        String formalModelCode = formalModelCode(natureza, justicaAxis, rito, ramo);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("formalModelCode", formalModelCode);
        out.put("naturezaComunicacao", natureza);
        out.put("servicoPessoalExigido", requiresPersonalService(natureza));
        out.put("justicaAxis", justicaAxis);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunal", processo != null ? trimToNull(processo.getTribunal()) : null);
        out.put("vara", processo != null ? trimToNull(processo.getVara()) : null);
        out.put("comarca", processo != null ? trimToNull(processo.getComarca()) : null);
        out.put("uf", processo != null ? trimToNull(processo.getUf()) : null);
        out.put("ramoDireito", ramo);
        out.put("ritoProcessual", rito);
        out.put("destinatarioPreferencial", resolveDestinatario(processo, item));
        out.put("baseLegalSintetica", buildLegalBasis(processo, natureza, justicaAxis, ramo, rito));
        out.put("manualActions", buildActions(item, processo, natureza, justicaAxis, "MANUAL"));
        out.put("automaticActions", buildActions(item, processo, natureza, justicaAxis, "AUTOMATICO"));
        out.put("documentFamily", resolveDocumentFamily(natureza));
        out.put("manualLabel", "Lançamento manual formal do oficial");
        out.put("automaticLabel", "Geração automática formal do oficial");
        out.put("scopeLabel", justicaAxis.startsWith("FEDERAL") ? "OFICIAL_FEDERAL" : "OFICIAL_ESTADUAL");
        out.put("officialLaneCode", justicaAxis.startsWith("FEDERAL") ? "OFICIAL_JUSTICA_FEDERAL" : "OFICIAL_JUSTICA_ESTADUAL");
        out.put("eligiblePersonalService", requiresPersonalService(natureza));
        out.put("contactEnvelope", buildContactEnvelope(processo));
        out.put("formalChecklist", List.of(
                "enquadramento material do ato",
                "destinatário pessoal identificado",
                "modelo formal compatível com rito e ramo",
                "registro do resultado da diligência",
                "persistência assinada no processo"
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> formalizeOutcome(Processo processo,
                                                WorkItem item,
                                                Usuario oficial,
                                                Object rawRequest,
                                                boolean frustrado) {
        CommunicationCommand command = coerce(rawRequest, processo, item, frustrado);
        Map<String, Object> profile = buildProfile(processo, item, oficial);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "OFICIAL_COMUNICACAO_FORMAL_V1");
        out.put("profile", profile);
        out.put("command", command.asMap());
        if (processo == null || processo.getId() == null) {
            out.put("generatedDocuments", List.of());
            out.put("primaryDocument", null);
            out.put("companionDocument", null);
            out.put("alerts", List.of("processo_nao_identificado_para_materializacao_documental"));
            return Collections.unmodifiableMap(out);
        }
        if (!command.shouldGerarDocumentoFormal()) {
            out.put("generatedDocuments", List.of());
            out.put("primaryDocument", null);
            out.put("companionDocument", null);
            return Collections.unmodifiableMap(out);
        }
        OfficialDocumentTemplateRenderResponse primary = renderPrimaryDocument(processo, item, oficial, command, profile);
        OfficialDocumentTemplateRenderResponse companion = renderCompanionDocument(processo, item, oficial, command, profile);
        ArrayList<Map<String, Object>> generated = new ArrayList<>();
        if (primary != null) {
            generated.add(documentView(primary, true));
        }
        if (companion != null) {
            generated.add(documentView(companion, false));
        }
        out.put("primaryDocument", primary == null ? null : documentView(primary, true));
        out.put("companionDocument", companion == null ? null : documentView(companion, false));
        out.put("generatedDocuments", List.copyOf(generated));
        return Collections.unmodifiableMap(out);
    }

    public String appendFormalTrace(String currentDescription,
                                    Processo processo,
                                    WorkItem item,
                                    Usuario oficial,
                                    Object rawRequest,
                                    boolean frustrado,
                                    Map<String, Object> formalization) {
        CommunicationCommand command = coerce(rawRequest, processo, item, frustrado);
        Map<String, Object> profile = buildProfile(processo, item, oficial);
        StringBuilder sb = new StringBuilder();
        if (currentDescription != null && !currentDescription.isBlank()) {
            sb.append(currentDescription.trim()).append("\n\n");
        }
        sb.append("oficial_comunicacao_formal_mode=").append(firstNonBlank(command.operationMode(), "AUTOMATICO")).append('\n');
        sb.append("oficial_comunicacao_formal_resultado=").append(firstNonBlank(command.resultadoComunicacao(), frustrado ? "NEGATIVO" : "POSITIVO")).append('\n');
        sb.append("oficial_comunicacao_formal_natureza=").append(stringValue(profile.get("naturezaComunicacao"))).append('\n');
        sb.append("oficial_comunicacao_formal_justica=").append(stringValue(profile.get("justicaAxis"))).append('\n');
        sb.append("oficial_comunicacao_formal_tribunal=").append(stringValue(profile.get("tribunalCodigo"))).append('\n');
        sb.append("oficial_comunicacao_formal_rito=").append(stringValue(profile.get("ritoProcessual"))).append('\n');
        sb.append("oficial_comunicacao_formal_ramo=").append(stringValue(profile.get("ramoDireito"))).append('\n');
        sb.append("oficial_comunicacao_formal_modelo=").append(stringValue(profile.get("formalModelCode"))).append('\n');
        sb.append("oficial_comunicacao_formal_base=").append(stringValue(profile.get("baseLegalSintetica"))).append('\n');
        Object generatedDocuments = formalization == null ? null : formalization.get("generatedDocuments");
        if (generatedDocuments instanceof List<?> list && !list.isEmpty()) {
            sb.append("oficial_comunicacao_formal_documentos=").append(list.size()).append('\n');
        }
        if (command.observacoes() != null && !command.observacoes().isBlank()) {
            sb.append("oficial_comunicacao_formal_observacoes=").append(command.observacoes().trim()).append('\n');
        }
        return sb.toString();
    }

    public String resolveNaturezaComunicacao(Processo processo, WorkItem item, String override) {
        String normalizedOverride = normalizeToken(override);
        if (!normalizedOverride.isBlank()) {
            if (normalizedOverride.contains("CITAC")) {
                return "CITACAO_PESSOAL";
            }
            if (normalizedOverride.contains("INTIMA")) {
                return "INTIMACAO_PESSOAL";
            }
            if (normalizedOverride.contains("NOTIFICA")) {
                return "NOTIFICACAO_PESSOAL";
            }
        }
        String raw = String.join(" ",
                safe(processo != null ? processo.getClasseProcessual() : null),
                safe(processo != null ? processo.getAssunto() : null),
                safe(item != null ? item.getTemplateCode() : null),
                safe(item != null ? item.getQueueCode() : null),
                safe(item != null ? item.getInboxKey() : null),
                safe(item != null ? item.getTitulo() : null),
                safe(item != null ? item.getDescricao() : null),
                safe(item != null ? item.getBaseLegal() : null));
        String normalized = normalizeToken(raw);
        if (containsAny(normalized, "CITACAO", "CITAR", "CITACAO PESSOAL")) {
            return "CITACAO_PESSOAL";
        }
        if (containsAny(normalized, "INTIMACAO", "INTIMAR", "CIENTE INTIMACAO", "CIENCIA INTIMACAO")) {
            return "INTIMACAO_PESSOAL";
        }
        if (containsAny(normalized, "NOTIFICACAO", "NOTIFICAR")) {
            return "NOTIFICACAO_PESSOAL";
        }
        if (containsAny(normalized, "PENHORA", "AVALIACAO", "ARRESTO")) {
            return "DILIGENCIA_EXECUTIVA";
        }
        return "DILIGENCIA_PESSOAL_DIVERSA";
    }

    public String resolveJusticaAxis(Processo processo, WorkItem item, Usuario oficial) {
        TipoJustica tipoJustica = processo != null ? processo.getTipoJustica() : null;
        if (tipoJustica == TipoJustica.FEDERAL) {
            return "FEDERAL";
        }
        if (tipoJustica == TipoJustica.ESTADUAL) {
            return "ESTADUAL";
        }
        String tribunal = firstNonBlank(processo != null ? processo.getTribunal() : null, item != null ? item.getQueueCode() : null, item != null ? item.getInboxKey() : null, oficial != null ? oficial.getPerfil() : null);
        String normalized = normalizeToken(tribunal);
        if (containsAny(normalized, "TRF", "JUSTICA FEDERAL", "SUBSECAO JUDICIARIA", "JF ")) {
            return "FEDERAL";
        }
        if (oficial != null && Boolean.TRUE.equals(oficial.atuaNaUniao())) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    public boolean requiresPersonalService(String naturezaComunicacao) {
        return switch (firstNonBlank(naturezaComunicacao, "DILIGENCIA_PESSOAL_DIVERSA")) {
            case "CITACAO_PESSOAL", "INTIMACAO_PESSOAL", "NOTIFICACAO_PESSOAL", "DILIGENCIA_PESSOAL_DIVERSA" -> true;
            default -> false;
        };
    }

    private String formalModelCode(String naturezaComunicacao, String justicaAxis, String rito, String ramo) {
        return String.join(":",
                "OFICIAL_FORMAL",
                firstNonBlank(justicaAxis, "ESTADUAL"),
                firstNonBlank(naturezaComunicacao, "DILIGENCIA_PESSOAL_DIVERSA"),
                firstNonBlank(ramo, "CIVIL"),
                firstNonBlank(rito, "COMUM_ORDINARIO"));
    }

    private String resolveDocumentFamily(String naturezaComunicacao) {
        return switch (firstNonBlank(naturezaComunicacao, "DILIGENCIA_PESSOAL_DIVERSA")) {
            case "CITACAO_PESSOAL" -> "MANDADO_E_CERTIDAO_DE_CITACAO";
            case "INTIMACAO_PESSOAL" -> "CARTA_E_CERTIDAO_DE_INTIMACAO";
            case "NOTIFICACAO_PESSOAL" -> "CARTA_E_CERTIDAO_DE_NOTIFICACAO";
            case "DILIGENCIA_EXECUTIVA" -> "AUTO_E_CERTIDAO_EXECUTIVA";
            default -> "CERTIDAO_OPERACIONAL_PESSOAL";
        };
    }

    private List<Map<String, Object>> buildActions(WorkItem item,
                                                   Processo processo,
                                                   String naturezaComunicacao,
                                                   String justicaAxis,
                                                   String operationMode) {
        if (item == null || item.getId() == null) {
            return List.of();
        }
        String basePath = "/api/v1/oficial-justica/mandados/" + item.getId();
        LinkedHashSet<Map<String, Object>> actions = new LinkedHashSet<>();
        actions.add(action("ABRIR_FORMALIZACAO_" + operationMode, basePath + "/formalizacao-processual", operationMode, "FORMALIZAR_PROCESSUALMENTE", processo, justicaAxis));
        actions.add(action("GERAR_CERTIDAO_" + operationMode, basePath + "/certidoes/auto", operationMode, "GERAR_CERTIDAO_FORMAL", processo, justicaAxis));
        actions.add(action("GERAR_JUNTADA_" + operationMode, basePath + "/juntadas-automaticas", operationMode, "GERAR_JUNTADA_FORMAL", processo, justicaAxis));
        if ("CITACAO_PESSOAL".equals(naturezaComunicacao)) {
            actions.add(action("REGISTRAR_CITACAO_PESSOAL_" + operationMode, basePath + "/cumprimento", operationMode, "CITADO_PESSOALMENTE", processo, justicaAxis));
        }
        if ("INTIMACAO_PESSOAL".equals(naturezaComunicacao) || "NOTIFICACAO_PESSOAL".equals(naturezaComunicacao)) {
            actions.add(action("REGISTRAR_INTIMACAO_PESSOAL_" + operationMode, basePath + "/cumprimento", operationMode, "INTIMADO_PESSOALMENTE", processo, justicaAxis));
        }
        actions.add(action("REGISTRAR_AUSENCIA_" + operationMode, basePath + "/frustracao", operationMode, "AUSENTE", processo, justicaAxis));
        actions.add(action("REGISTRAR_NAO_LOCALIZADO_" + operationMode, basePath + "/frustracao", operationMode, "NAO_LOCALIZADO", processo, justicaAxis));
        actions.add(action("REGISTRAR_RECUSA_" + operationMode, basePath + "/frustracao", operationMode, "RECUSA_RECEBIMENTO", processo, justicaAxis));
        actions.add(action("REGISTRAR_OCULTACAO_" + operationMode, basePath + "/frustracao", operationMode, "OCULTACAO", processo, justicaAxis));
        return List.copyOf(actions);
    }

    private Map<String, Object> action(String code,
                                       String path,
                                       String operationMode,
                                       String result,
                                       Processo processo,
                                       String justicaAxis) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("path", path);
        out.put("method", "POST");
        out.put("operationMode", operationMode);
        out.put("resultadoComunicacao", result);
        out.put("processoNumero", processo == null ? null : firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado()));
        out.put("justicaAxis", justicaAxis);
        out.put("gerarDocumentoFormal", Boolean.TRUE);
        out.put("surface", result.startsWith("GERAR_") || result.startsWith("FORMALIZAR_") ? "FORMALIZACAO_OFICIAL" : "CUMPRIMENTO_OFICIAL");
        out.put("manual", "MANUAL".equalsIgnoreCase(operationMode));
        out.put("automatico", "AUTOMATICO".equalsIgnoreCase(operationMode));
        return Collections.unmodifiableMap(out);
    }

    private OfficialDocumentTemplateRenderResponse renderPrimaryDocument(Processo processo,
                                                                        WorkItem item,
                                                                        Usuario oficial,
                                                                        CommunicationCommand command,
                                                                        Map<String, Object> profile) {
        TemplateDocumentoOficial template = selectPrimaryTemplate(stringValue(profile.get("naturezaComunicacao")), command.resultadoComunicacao());
        if (template == null) {
            return null;
        }
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                template,
                buildPrimaryTitle(processo, profile, command),
                buildPrimaryVariables(template, processo, item, oficial, command, profile),
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private OfficialDocumentTemplateRenderResponse renderCompanionDocument(Processo processo,
                                                                          WorkItem item,
                                                                          Usuario oficial,
                                                                          CommunicationCommand command,
                                                                          Map<String, Object> profile) {
        if (command.resultadoNegativo() && !command.shouldGerarCertidaoCompanheira()) {
            return null;
        }
        if (!command.resultadoNegativo() && !command.shouldGerarCertidaoCompanheira()) {
            return null;
        }
        TemplateDocumentoOficial template = command.resultadoNegativo()
                ? TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO
                : TemplateDocumentoOficial.CERTIDAO_CUMPRIMENTO;
        return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
                processo.getId(),
                template,
                buildCompanionTitle(processo, profile, command),
                buildCompanionVariables(template, processo, item, oficial, command, profile),
                Boolean.TRUE,
                Boolean.TRUE
        ));
    }

    private Map<String, String> buildPrimaryVariables(TemplateDocumentoOficial template,
                                                      Processo processo,
                                                      WorkItem item,
                                                      Usuario oficial,
                                                      CommunicationCommand command,
                                                      Map<String, Object> profile) {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        String legalBasis = stringValue(profile.get("baseLegalSintetica"));
        String destinatario = firstNonBlank(command.destinatarioNome(), resolveDestinatario(processo, item), processo != null ? processo.getParteReuNome() : null, "DESTINATÁRIO NÃO IDENTIFICADO");
        String processNumber = contextEnvelopeService.processNumber(processo);
        String modoLabel = "MANUAL".equals(command.operationMode()) ? "registro manual" : "geração automática controlada";
        String observacao = firstNonBlank(command.observacoes(), "Sem observação complementar.");
        switch (template) {
            case MANDADO -> {
                vars.put("qualificacaoPartes", qualifyParties(processo, destinatario));
                vars.put("ordemJudicial", buildNarrative(processo, command, profile, true));
                vars.put("prazoCumprimento", resolvePrazoResposta(command, processo));
                vars.put("fundamentoLegal", legalBasis);
                vars.put("modo_operacao", modoLabel);
                vars.put("observacao", observacao);
            }
            case INTIMACAO_FORMAL -> {
                vars.put("destinatario", destinatario);
                vars.put("conteudoIntimacao", buildNarrative(processo, command, profile, true));
                vars.put("prazoResposta", resolvePrazoResposta(command, processo));
                vars.put("fundamentoLegal", legalBasis);
                vars.put("modo_operacao", modoLabel);
                vars.put("numero_processo", processNumber);
                vars.put("observacao", observacao);
            }
            default -> {
                vars.put("referenciaMandado", item != null && item.getId() != null ? String.valueOf(item.getId()) : processNumber);
                vars.put("descricaoCumprimento", buildNarrative(processo, command, profile, true));
                vars.put("resultadoDiligencia", firstNonBlank(command.resultadoComunicacao(), "CUMPRIDO"));
                vars.put("fundamentoLegal", legalBasis);
                vars.put("observacao", observacao);
            }
        }
        return Map.copyOf(vars);
    }

    private Map<String, String> buildCompanionVariables(TemplateDocumentoOficial template,
                                                        Processo processo,
                                                        WorkItem item,
                                                        Usuario oficial,
                                                        CommunicationCommand command,
                                                        Map<String, Object> profile) {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        String officialName = firstNonBlank(oficial != null ? oficial.getNome() : null, "OFICIAL NÃO IDENTIFICADO");
        String now = DATE_TIME.format(command.ocorridoEm().atOffset(ZoneOffset.UTC));
        if (template == TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO) {
            vars.put("motivoFrustracao", buildNarrative(processo, command, profile, false));
            vars.put("dataDiligencia", now);
            vars.put("responsavelCertificacao", officialName);
        } else {
            vars.put("atoCumprido", buildNarrative(processo, command, profile, false));
            vars.put("dataCumprimento", now);
            vars.put("responsavelCertificacao", officialName);
        }
        vars.put("fundamentoLegal", stringValue(profile.get("baseLegalSintetica")));
        vars.put("numero_processo", contextEnvelopeService.processNumber(processo));
        vars.put("destinatario", firstNonBlank(command.destinatarioNome(), resolveDestinatario(processo, item)));
        return Map.copyOf(vars);
    }

    private Map<String, Object> documentView(OfficialDocumentTemplateRenderResponse response, boolean primary) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("primary", primary);
        out.put("template", response.template() != null ? response.template().name() : null);
        out.put("tituloDocumento", response.tituloDocumento());
        out.put("documentoId", response.documentoId());
        out.put("persistido", response.persistido());
        out.put("selado", response.selado());
        out.put("hashSha256", response.hashSha256());
        out.put("assinaturaQualificada", response.assinaturaQualificada());
        out.put("validacaoSoberana", response.validacaoSoberana());
        out.put("alertas", response.alertas());
        return Collections.unmodifiableMap(out);
    }

    private CommunicationCommand coerce(Object rawRequest, Processo processo, WorkItem item, boolean frustrado) {
        if (rawRequest instanceof CommunicationCommand command) {
            return command.normalized(processo, item, frustrado);
        }
        if (rawRequest instanceof Map<?, ?> map) {
            return new CommunicationCommand(
                    firstNonBlank(stringValue(map.get("operationMode")), stringValue(map.get("modoOperacao"))),
                    firstNonBlank(stringValue(map.get("tipoComunicacao")), resolveNaturezaComunicacao(processo, item, null)),
                    stringValue(map.get("resultadoComunicacao")),
                    stringValue(map.get("formaCumprimento")),
                    stringValue(map.get("canalUtilizado")),
                    stringValue(map.get("destinatarioNome")),
                    stringValue(map.get("destinatarioDocumento")),
                    stringValue(map.get("observacoes")),
                    stringValue(map.get("prazoResposta")),
                    toStringList(map.get("evidenceReferences")),
                    coerceBoolean(map.get("gerarDocumentoFormal")),
                    coerceBoolean(map.get("gerarCertidaoCompanheira")),
                    parseInstant(map.get("ocorridoEm"))
            ).normalized(processo, item, frustrado);
        }
        if (rawRequest instanceof String str && !str.isBlank()) {
            return new CommunicationCommand(null, null, null, null, null, null, null, str.trim(), null, List.of(), null, null, null)
                    .normalized(processo, item, frustrado);
        }
        return new CommunicationCommand(null, null, null, null, null, null, null, null, null, List.of(), null, null, null)
                .normalized(processo, item, frustrado);
    }

    private TemplateDocumentoOficial selectPrimaryTemplate(String naturezaComunicacao, String resultadoComunicacao) {
        if (isNegativeResult(resultadoComunicacao)) {
            return TemplateDocumentoOficial.CERTIDAO_NAO_CUMPRIMENTO;
        }
        return switch (firstNonBlank(naturezaComunicacao, "DILIGENCIA_PESSOAL_DIVERSA")) {
            case "CITACAO_PESSOAL" -> TemplateDocumentoOficial.MANDADO;
            case "INTIMACAO_PESSOAL", "NOTIFICACAO_PESSOAL" -> TemplateDocumentoOficial.INTIMACAO_FORMAL;
            case "DILIGENCIA_EXECUTIVA" -> TemplateDocumentoOficial.AUTO_CUMPRIMENTO;
            default -> TemplateDocumentoOficial.CERTIDAO_CUMPRIMENTO;
        };
    }

    private String buildPrimaryTitle(Processo processo, Map<String, Object> profile, CommunicationCommand command) {
        String number = contextEnvelopeService.processNumber(processo);
        String label = switch (stringValue(profile.get("naturezaComunicacao"))) {
            case "CITACAO_PESSOAL" -> "Mandado formal de citação pessoal";
            case "INTIMACAO_PESSOAL" -> "Carta formal de intimação pessoal";
            case "NOTIFICACAO_PESSOAL" -> "Carta formal de notificação pessoal";
            case "DILIGENCIA_EXECUTIVA" -> "Auto formal de diligência executiva";
            default -> "Certidão formal de diligência pessoal";
        };
        return label + " — " + number + " — " + firstNonBlank(command.operationMode(), "AUTOMATICO");
    }

    private String buildCompanionTitle(Processo processo, Map<String, Object> profile, CommunicationCommand command) {
        String number = contextEnvelopeService.processNumber(processo);
        String prefix = command.resultadoNegativo() ? "Certidão formal negativa" : "Certidão formal de cumprimento";
        return prefix + " — " + number + " — " + stringValue(profile.get("naturezaComunicacao"));
    }

    private String buildNarrative(Processo processo,
                                  CommunicationCommand command,
                                  Map<String, Object> profile,
                                  boolean primaryDocument) {
        StringBuilder sb = new StringBuilder();
        String natureLabel = humanize(stringValue(profile.get("naturezaComunicacao")));
        String justica = humanize(stringValue(profile.get("justicaAxis")));
        String ramo = humanize(stringValue(profile.get("ramoDireito")));
        String rito = humanize(stringValue(profile.get("ritoProcessual")));
        String destinatario = firstNonBlank(command.destinatarioNome(), resolveDestinatario(processo, null), "destinatário não identificado");
        sb.append("Ato pessoal de ").append(natureLabel.toLowerCase(Locale.ROOT)).append(" dirigido a ").append(destinatario).append(".");
        sb.append(' ').append("Modelo compatibilizado para a ").append(justica.toLowerCase(Locale.ROOT)).append(", ramo ").append(ramo.toLowerCase(Locale.ROOT)).append(" e rito ").append(rito.toLowerCase(Locale.ROOT)).append('.');
        sb.append(' ').append("Registro ").append("MANUAL".equals(command.operationMode()) ? "manual" : "automático").append(" do oficial de justiça.");
        if (command.resultadoComunicacao() != null && !command.resultadoComunicacao().isBlank()) {
            sb.append(' ').append("Resultado informado: ").append(humanize(command.resultadoComunicacao())).append('.');
        }
        if (command.formaCumprimento() != null && !command.formaCumprimento().isBlank()) {
            sb.append(' ').append("Forma de cumprimento: ").append(command.formaCumprimento().trim()).append('.');
        }
        if (command.canalUtilizado() != null && !command.canalUtilizado().isBlank()) {
            sb.append(' ').append("Canal operacional: ").append(command.canalUtilizado().trim()).append('.');
        }
        if (primaryDocument) {
            sb.append(' ').append(stringValue(profile.get("baseLegalSintetica")));
        }
        if (command.observacoes() != null && !command.observacoes().isBlank()) {
            sb.append(' ').append("Observações: ").append(command.observacoes().trim()).append('.');
        }
        if (!command.evidenceReferences().isEmpty()) {
            sb.append(' ').append("Referências probatórias: ").append(String.join(", ", command.evidenceReferences())).append('.');
        }
        return sb.toString().trim();
    }

    private String buildLegalBasis(Processo processo,
                                   String naturezaComunicacao,
                                   String justicaAxis,
                                   String ramo,
                                   String rito) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comunicação pessoal materializada por Oficial de Justiça na malha ")
                .append(justicaAxis.toLowerCase(Locale.ROOT))
                .append(", com enquadramento estruturado do PJB para o ramo ")
                .append(humanize(ramo).toLowerCase(Locale.ROOT))
                .append(" e o rito ")
                .append(humanize(rito).toLowerCase(Locale.ROOT))
                .append('.');
        switch (firstNonBlank(naturezaComunicacao, "DILIGENCIA_PESSOAL_DIVERSA")) {
            case "CITACAO_PESSOAL" -> sb.append(' ').append("Ato tratado como comunicação de ingresso pessoal do destinatário na relação processual.");
            case "INTIMACAO_PESSOAL" -> sb.append(' ').append("Ato tratado como comunicação pessoal para ciência e cumprimento de determinação processual.");
            case "NOTIFICACAO_PESSOAL" -> sb.append(' ').append("Ato tratado como notificação pessoal vinculada ao expediente judicial.");
            case "DILIGENCIA_EXECUTIVA" -> sb.append(' ').append("Ato tratado como diligência executiva pessoal em campo.");
            default -> sb.append(' ').append("Ato tratado como diligência pessoal diversa, sem substituição por publicação genérica.");
        }
        if (processo != null && processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            sb.append(' ').append("Classe processual de referência: ").append(processo.getClasseProcessual().trim()).append('.');
        }
        return sb.toString();
    }

    private Map<String, Object> buildContactEnvelope(Processo processo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (processo == null) {
            return Map.of();
        }
        out.put("autor", Map.of("nome", firstNonBlank(processo.getParteAutoraNome(), "AUTOR NÃO IDENTIFICADO")));
        out.put("reu", Map.of("nome", firstNonBlank(processo.getParteReuNome(), "RÉU NÃO IDENTIFICADO")));
        out.put("advogados", List.of());
        return Collections.unmodifiableMap(out);
    }

    private String resolveDestinatario(Processo processo, WorkItem item) {
        String raw = firstNonBlank(processo != null ? processo.getParteReuNome() : null, processo != null ? processo.getParteAutoraNome() : null, item != null ? item.getTitulo() : null);
        return raw == null ? "DESTINATÁRIO NÃO IDENTIFICADO" : raw;
    }

    private String resolveTribunalCodigo(Processo processo, WorkItem item) {
        String raw = firstNonBlank(processo != null ? processo.getTribunalCodigoRoteado() : null, processo != null ? processo.getTribunal() : null, item != null ? item.getQueueCode() : null);
        if (raw == null) {
            return "TRIBUNAL_NAO_IDENTIFICADO";
        }
        String normalized = normalizeToken(raw).replace(' ', '_');
        return normalized.isBlank() ? "TRIBUNAL_NAO_IDENTIFICADO" : normalized;
    }

    private String resolveRamo(Processo processo, String rito) {
        RamoDireito ramo = processo != null ? processo.getRamoDireito() : null;
        if (ramo != null) {
            return ramo.name();
        }
        RitoProcessual ritoProcessual = processo != null ? processo.getRito() : null;
        if (ritoProcessual != null && ritoProcessual.suggestedRamo() != null) {
            return ritoProcessual.suggestedRamo().name();
        }
        return firstNonBlank(rito, "CIVIL");
    }

    private String qualifyParties(Processo processo, String destinatario) {
        ArrayList<String> parts = new ArrayList<>();
        if (processo != null && processo.getParteAutoraNome() != null && !processo.getParteAutoraNome().isBlank()) {
            parts.add("Autor: " + processo.getParteAutoraNome().trim());
        }
        if (processo != null && processo.getParteReuNome() != null && !processo.getParteReuNome().isBlank()) {
            parts.add("Réu: " + processo.getParteReuNome().trim());
        }
        parts.add("Destinatário da diligência: " + destinatario);
        return String.join(" | ", parts);
    }

    private String resolvePrazoResposta(CommunicationCommand command, Processo processo) {
        return firstNonBlank(command.prazoResposta(), processo != null && processo.getRito() != null && processo.getRito().isPenal() ? "prazo conforme determinação judicial penal" : null, "prazo conforme determinação judicial e rito aplicável");
    }

    private static boolean isNegativeResult(String result) {
        String normalized = normalizeToken(result);
        return containsAny(normalized, "NAO", "RECUSA", "AUSENTE", "OCULTACAO", "FRUSTR", "NEGAT");
    }

    private Boolean coerceBoolean(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        String normalized = normalizeToken(String.valueOf(raw));
        if (normalized.equals("TRUE") || normalized.equals("SIM") || normalized.equals("1")) {
            return Boolean.TRUE;
        }
        if (normalized.equals("FALSE") || normalized.equals("NAO") || normalized.equals("0")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Instant parseInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Instant.parse(String.valueOf(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> toStringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(value -> value != null && !value.isBlank()).toList();
        }
        return List.of();
    }

    private static boolean containsAny(String normalized, String... needles) {
        if (normalized == null || normalized.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalizeToken(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String humanize(String code) {
        if (code == null || code.isBlank()) {
            return "não identificado";
        }
        return code.replace('_', ' ').trim();
    }

    public record CommunicationCommand(String operationMode,
                                       String tipoComunicacao,
                                       String resultadoComunicacao,
                                       String formaCumprimento,
                                       String canalUtilizado,
                                       String destinatarioNome,
                                       String destinatarioDocumento,
                                       String observacoes,
                                       String prazoResposta,
                                       List<String> evidenceReferences,
                                       Boolean gerarDocumentoFormal,
                                       Boolean gerarCertidaoCompanheira,
                                       Instant ocorridoEm) {

        public CommunicationCommand {
            evidenceReferences = evidenceReferences == null ? List.of() : List.copyOf(evidenceReferences);
        }

        public CommunicationCommand normalized(Processo processo, WorkItem item, boolean frustrado) {
            String resolvedTipo = firstNonBlank(tipoComunicacao, null);
            String resolvedResult = firstNonBlank(resultadoComunicacao, frustrado ? "NAO_LOCALIZADO" : null, "INTIMADO_PESSOALMENTE");
            String resolvedMode = firstNonBlank(operationMode, "AUTOMATICO");
            return new CommunicationCommand(
                    resolvedMode,
                    resolvedTipo,
                    resolvedResult,
                    firstNonBlank(formaCumprimento, frustrado ? "CERTIDAO_NEGATIVA" : "CIENCIA_PESSOAL"),
                    firstNonBlank(canalUtilizado, "PRESENCIAL"),
                    destinatarioNome,
                    destinatarioDocumento,
                    observacoes,
                    prazoResposta,
                    evidenceReferences,
                    gerarDocumentoFormal == null ? Boolean.TRUE : gerarDocumentoFormal,
                    gerarCertidaoCompanheira == null ? Boolean.TRUE : gerarCertidaoCompanheira,
                    ocorridoEm == null ? Instant.now() : ocorridoEm
            );
        }

        public boolean resultadoNegativo() {
            return isNegativeResult(resultadoComunicacao);
        }

        public boolean shouldGerarDocumentoFormal() {
            return Boolean.TRUE.equals(gerarDocumentoFormal);
        }

        public boolean shouldGerarCertidaoCompanheira() {
            return Boolean.TRUE.equals(gerarCertidaoCompanheira);
        }

        public Map<String, Object> asMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("operationMode", operationMode);
            out.put("tipoComunicacao", tipoComunicacao);
            out.put("resultadoComunicacao", resultadoComunicacao);
            out.put("formaCumprimento", formaCumprimento);
            out.put("canalUtilizado", canalUtilizado);
            out.put("destinatarioNome", destinatarioNome);
            out.put("destinatarioDocumento", destinatarioDocumento);
            out.put("observacoes", observacoes);
            out.put("prazoResposta", prazoResposta);
            out.put("evidenceReferences", evidenceReferences);
            out.put("gerarDocumentoFormal", shouldGerarDocumentoFormal());
            out.put("gerarCertidaoCompanheira", shouldGerarCertidaoCompanheira());
            out.put("ocorridoEm", ocorridoEm);
            return Collections.unmodifiableMap(out);
        }
    }
}
