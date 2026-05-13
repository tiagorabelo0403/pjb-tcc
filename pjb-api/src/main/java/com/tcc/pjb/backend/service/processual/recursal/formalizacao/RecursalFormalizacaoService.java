package com.tcc.pjb.backend.service.processual.recursal.formalizacao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoCommand;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoOpcoes;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoTextos;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaStructuredAnalysis;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.model.dto.processual.recursal.protocolo.RecursalProtocolArtifactReadiness;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalFilingBlueprintAssembler;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSecretariatTopologyService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfArtifactValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalProtocolArtifactReadinessService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursalFormalizacaoService {

    private final LegalDraftingService legalDraftingService;
    private final JudicialConnectorOperationalProfileService judicialConnectorOperationalProfileService;
    private final ObjectMapper objectMapper;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final RecursalPdfExportService recursalPdfExportService;
    private final RecursalPdfNativeSignatureService recursalPdfNativeSignatureService;
    private final RecursalPdfLongTermValidationService recursalPdfLongTermValidationService;
    private final RecursalPdfProofEnvelopeService recursalPdfProofEnvelopeService;
    private final RecursalPdfArtifactValidationService recursalPdfArtifactValidationService;
    private final RecursalProtocolArtifactReadinessService recursalProtocolArtifactReadinessService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;
    private final RecursalSecretariatTopologyService recursalSecretariatTopologyService;

    public RecursalFormalizacaoService(LegalDraftingService legalDraftingService,
                                       JudicialConnectorOperationalProfileService judicialConnectorOperationalProfileService,
                                       ObjectMapper objectMapper,
                                       RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                                       RecursalPdfExportService recursalPdfExportService,
                                       RecursalPdfNativeSignatureService recursalPdfNativeSignatureService,
                                       RecursalPdfLongTermValidationService recursalPdfLongTermValidationService,
                                       RecursalPdfProofEnvelopeService recursalPdfProofEnvelopeService,
                                       RecursalPdfArtifactValidationService recursalPdfArtifactValidationService,
                                       RecursalProtocolArtifactReadinessService recursalProtocolArtifactReadinessService,
                                       RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService,
                                       RecursalSecretariatTopologyService recursalSecretariatTopologyService) {
        this.legalDraftingService = Objects.requireNonNull(legalDraftingService);
        this.judicialConnectorOperationalProfileService = Objects.requireNonNull(judicialConnectorOperationalProfileService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService);
        this.recursalPdfExportService = Objects.requireNonNull(recursalPdfExportService);
        this.recursalPdfNativeSignatureService = Objects.requireNonNull(recursalPdfNativeSignatureService);
        this.recursalPdfLongTermValidationService = Objects.requireNonNull(recursalPdfLongTermValidationService);
        this.recursalPdfProofEnvelopeService = Objects.requireNonNull(recursalPdfProofEnvelopeService);
        this.recursalPdfArtifactValidationService = Objects.requireNonNull(recursalPdfArtifactValidationService);
        this.recursalProtocolArtifactReadinessService = Objects.requireNonNull(recursalProtocolArtifactReadinessService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
        this.recursalSecretariatTopologyService = Objects.requireNonNull(recursalSecretariatTopologyService);
    }

    public Map<String, Object> formalizar(Processo processo,
                                          Usuario usuario,
                                          String profileCode,
                                          WorkItem peticaoRecursal,
                                          WorkItem recursoPrincipal,
                                          LegalAppealType appealType,
                                          RecursalMeshSpeciesType speciesType,
                                          String razoes,
                                          String fundamentacao,
                                          boolean pedidoEfeitoSuspensivo,
                                          boolean preparoDispensado,
                                          String observacoes,
                                          RecursalAdmissibilityResponse admissibility,
                                          RecursalIaConferenciaResponse aiReview,
                                          Map<String, Object> sigiloRecursal) {
        return formalizar(new RecursalFormalizacaoCommand(
                processo,
                usuario,
                profileCode,
                peticaoRecursal,
                recursoPrincipal,
                appealType,
                speciesType,
                new RecursalFormalizacaoTextos(razoes, fundamentacao, observacoes),
                new RecursalFormalizacaoOpcoes(pedidoEfeitoSuspensivo, preparoDispensado),
                admissibility,
                aiReview,
                sigiloRecursal
        )).toMap();
    }

    @Transactional(readOnly = true)
    public RecursalFormalizacaoResult formalizar(RecursalFormalizacaoCommand command) {
        if (command == null || !command.ready()) {
            return RecursalFormalizacaoResult.unavailable();
        }
        Processo processo = command.processo();
        Usuario usuario = command.usuario();
        String profileCode = command.profileCode();
        WorkItem peticaoRecursal = command.peticaoRecursal();
        WorkItem recursoPrincipal = command.recursoPrincipal();
        LegalAppealType appealType = command.appealType();
        RecursalMeshSpeciesType speciesType = command.speciesType();
        String razoes = command.razoes();
        String fundamentacao = command.fundamentacao();
        boolean pedidoEfeitoSuspensivo = command.pedidoEfeitoSuspensivo();
        boolean preparoDispensado = command.preparoDispensado();
        String observacoes = command.observacoes();
        RecursalAdmissibilityResponse admissibility = command.admissibility();
        RecursalIaConferenciaResponse aiReview = command.aiReview();
        Map<String, Object> sigiloRecursal = safeMap(command.sigiloRecursal());
        RecursalIaStructuredAnalysis structured = aiReview == null ? null : aiReview.analiseEstruturada();
        RepresentacaoProcessualPolicyResponse representacao = representacaoProcessualPolicyService.resolve(
                processo,
                usuario,
                profileCode != null && profileCode.toUpperCase(java.util.Locale.ROOT).contains("AUDIENCIA") ? InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA.name() : null,
                null,
                null,
                false,
                false,
                null,
                null
        );
        Map<String, Object> topologiaSecretariaRecursal = recursalSecretariatTopologyService.resolve(processo, appealType, admissibility);
        Map<String, Object> pecaFormalPrincipal = new LinkedHashMap<>(buildPecaFormalPrincipal(processo, usuario, profileCode, peticaoRecursal, recursoPrincipal, appealType, speciesType, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes, admissibility, structured, sigiloRecursal));
        Map<String, Object> contrarrazoesAtoAutonomo = new LinkedHashMap<>(buildContrarrazoesAtoAutonomo(processo, recursoPrincipal, appealType, admissibility, structured, sigiloRecursal));
        Map<String, Object> embargosAtoAutonomo = new LinkedHashMap<>(buildEmbargosAtoAutonomo(processo, usuario, recursoPrincipal, appealType, structured, sigiloRecursal));
        if (!topologiaSecretariaRecursal.isEmpty()) {
            pecaFormalPrincipal.put("topologiaSecretariaRecursal", topologiaSecretariaRecursal);
            if (!contrarrazoesAtoAutonomo.isEmpty()) {
                contrarrazoesAtoAutonomo.put("topologiaSecretariaRecursal", topologiaSecretariaRecursal);
            }
            if (!embargosAtoAutonomo.isEmpty()) {
                embargosAtoAutonomo.put("topologiaSecretariaRecursal", topologiaSecretariaRecursal);
            }
        }
        Map<String, Object> assinaturaVinculada = buildAssinaturaVinculada(processo, usuario, peticaoRecursal, recursoPrincipal, appealType, structured, pecaFormalPrincipal, admissibility, sigiloRecursal);
        RecursalPdfArtifact pecaFormalPrincipalPdf = recursalPdfExportService.export(processo, usuario, appealType, pecaFormalPrincipal, assinaturaVinculada, sigiloRecursal);
        pecaFormalPrincipalPdf = recursalPdfNativeSignatureService.applyNativeSignature(processo, usuario, appealType, pecaFormalPrincipalPdf, assinaturaVinculada, sigiloRecursal);
        pecaFormalPrincipalPdf = recursalPdfLongTermValidationService.prepare(processo, appealType, pecaFormalPrincipalPdf, assinaturaVinculada, sigiloRecursal);
        pecaFormalPrincipalPdf = recursalPdfProofEnvelopeService.seal(processo, usuario, appealType, pecaFormalPrincipalPdf, assinaturaVinculada, sigiloRecursal);
        pecaFormalPrincipalPdf = recursalPdfLongTermValidationService.finalizeEvidence(processo, appealType, pecaFormalPrincipalPdf, assinaturaVinculada, sigiloRecursal);
        RecursalPdfValidationResult pecaFormalPrincipalPdfValidation = recursalPdfArtifactValidationService.validate(pecaFormalPrincipalPdf, admissibility != null && admissibility.certificateRequired());
        Map<String, Object> protocoloConectorJudicial = buildProtocoloConectorJudicial(processo, usuario, peticaoRecursal, recursoPrincipal, appealType, structured, pecaFormalPrincipal, pecaFormalPrincipalPdf, pecaFormalPrincipalPdfValidation, assinaturaVinculada, admissibility, sigiloRecursal);
        return new RecursalFormalizacaoResult(
                true,
                "FORMALIZACAO_RECURSAL_PREPARADA",
                pecaFormalPrincipal,
                pecaFormalPrincipalPdf,
                contrarrazoesAtoAutonomo,
                embargosAtoAutonomo,
                assinaturaVinculada,
                protocoloConectorJudicial,
                representacao == null ? Map.of() : safeMap(representacao.envelope()),
                sigiloRecursal
        );
    }

    private Map<String, Object> buildPecaFormalPrincipal(Processo processo,
                                                         Usuario usuario,
                                                         String profileCode,
                                                         WorkItem peticaoRecursal,
                                                         WorkItem recursoPrincipal,
                                                         LegalAppealType appealType,
                                                         RecursalMeshSpeciesType speciesType,
                                                         String razoes,
                                                         String fundamentacao,
                                                         boolean pedidoEfeitoSuspensivo,
                                                         boolean preparoDispensado,
                                                         String observacoes,
                                                         RecursalAdmissibilityResponse admissibility,
                                                         RecursalIaStructuredAnalysis structured,
                                                         Map<String, Object> sigiloRecursal) {
        String lineageKey = stableKey("RECURSAL_PECA", processo.getId(), appealType.name(), usuario == null ? null : usuario.getId(), usuario == null ? null : usuario.getCpf());
        List<String> pedidos = new ArrayList<>();
        pedidos.add("Conhecimento e provimento do recurso para reformar, integrar ou invalidar o pronunciamento recorrido, conforme a espécie recursal.");
        if (pedidoEfeitoSuspensivo) {
            pedidos.add("Concessão de efeito suspensivo ou tutela recursal adequada, com demonstração de risco processual e utilidade do provimento.");
        }
        if (preparoDispensado) {
            pedidos.add("Reconhecimento da dispensa de preparo com base no perfil institucional, justiça gratuita ou hipótese legal aplicável.");
        }
        List<String> blueprint = safeList(structured == null ? null : structured.tesesPrioritarias());
        List<String> controlPoints = safeList(structured == null ? null : structured.checklistBlindagem());
        LinkedHashMap<String, Object> draftContext = new LinkedHashMap<>();
        put(draftContext, "numeroProcesso", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        put(draftContext, "autor", firstNonBlank(usuario == null ? null : usuario.getNome(), processo.getParteAutoraNome()));
        put(draftContext, "reu", firstNonBlank(processo.getParteReuNome(), processo.getParteAutoraNome()));
        put(draftContext, "tempestividade", buildTempestividade(admissibility));
        put(draftContext, "decisao", firstNonBlank(processo.getResultadoFinal(), processo.getResumoIA(), processo.getAnaliseTriagemV1()));
        put(draftContext, "fundamentos", firstNonBlank(fundamentacao, razoes, processo.getResumoIA()));
        draftContext.put("pedidos", pedidos);
        put(draftContext, "assinatura", buildAssinaturaLinha(usuario, profileCode));
        put(draftContext, "localData", "[LOCAL], [DATA]");
        draftContext.put("pleadingBlueprint", blueprint);
        draftContext.put("controlPoints", controlPoints);
        String minuta = safeDraftRecurso(draftContext, razoes, fundamentacao, appealType);
        String revisionHash = sha256Hex(String.join("|",
                safe(lineageKey),
                safe(minuta),
                safe(razoes),
                safe(fundamentacao),
                pedidoEfeitoSuspensivo ? "SUSPENSIVO" : "SEM_SUSPENSIVO",
                preparoDispensado ? "PREPARO_DISPENSADO" : "PREPARO_REGULAR"));
        Map<String, Object> peticionamentoAssistidoRecursal = RecursalFilingBlueprintAssembler.assemble(
                processo,
                appealType,
                admissibility,
                pedidoEfeitoSuspensivo,
                preparoDispensado
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PECA_FORMAL_PREPARADA");
        out.put("atoAutonomo", true);
        out.put("tipoAto", WorkItemType.PETICAO.name());
        out.put("tipoRecursoCanonico", appealType.name());
        put(out, "meshSpeciesType", speciesType == null ? null : speciesType.name());
        out.put("lineageKey", lineageKey);
        out.put("versioningModel", "LINEAGE_KEY_PLUS_REVISION_HASH");
        out.put("revisionHash", revisionHash);
        out.put("revisionToken", revisionHash.substring(0, Math.min(16, revisionHash.length())));
        put(out, "bindingWorkItemPeticao", peticaoRecursal.getId());
        put(out, "bindingWorkItemRecurso", recursoPrincipal.getId());
        put(out, "templateCodePeticao", peticaoRecursal.getTemplateCode());
        put(out, "templateCodeRecurso", recursoPrincipal.getTemplateCode());
        put(out, "queueCodePeticao", peticaoRecursal.getQueueCode());
        put(out, "queueCodeRecurso", recursoPrincipal.getQueueCode());
        put(out, "perfilOrigem", profileCode);
        put(out, "titulo", recursoPrincipal.getTitulo());
        put(out, "numeroProcesso", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        put(out, "tempestividade", buildTempestividade(admissibility));
        put(out, "efeitoPretendido", pedidoEfeitoSuspensivo ? "SUSPENSIVO_OU_TUTELA_RECURSAL" : "DEVOLUTIVO_OU_PROPRIO_DA_ESPECIE");
        put(out, "regimePreparo", preparoDispensado ? "DISPENSADO" : "A_CONFERIR_NO_ENVELOPE_RECURSAL");
        put(out, "fundamentacaoBase", fundamentacao);
        put(out, "observacoes", observacoes);
        out.put("conteudoMinuta", minuta);
        put(out, "peticionamentoAssistidoRecursal", peticionamentoAssistidoRecursal);
        put(out, "blocosObrigatoriosRecursais", peticionamentoAssistidoRecursal.get("blocosObrigatorios"));
        put(out, "camposObrigatoriosRecursais", peticionamentoAssistidoRecursal.get("camposObrigatorios"));
        put(out, "camposCondicionadosRecursais", peticionamentoAssistidoRecursal.get("camposCondicionados"));
        put(out, "documentosObrigatoriosRecursais", peticionamentoAssistidoRecursal.get("documentosObrigatorios"));
        put(out, "dossieDocumentalEssencialRecursal", peticionamentoAssistidoRecursal.get("dossieDocumentalEssencial"));
        put(out, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        out.put("revisaoJudicialSigilo", boolValue(sigiloRecursal, "revisaoJudicialObrigatoria"));
        out.put("decretoExclusivoMagistrado", boolValue(sigiloRecursal, "decretoExclusivoMagistrado"));
        if (structured != null) {
            out.put("checklistBlindagem", safeList(structured.checklistBlindagem()));
            out.put("tesesPrioritarias", safeList(structured.tesesPrioritarias()));
            out.put("riscosAnulacao", safeList(structured.riscosAnulacao()));
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildContrarrazoesAtoAutonomo(Processo processo,
                                                              WorkItem recursoPrincipal,
                                                              LegalAppealType appealType,
                                                              RecursalAdmissibilityResponse admissibility,
                                                              RecursalIaStructuredAnalysis structured,
                                                              Map<String, Object> sigiloRecursal) {
        String mode = admissibility == null ? null : normalizeNullable(admissibility.counterReasonsMode());
        boolean habilitado = mode != null && !"NONE".equalsIgnoreCase(mode) && !"NAO_APLICAVEL".equalsIgnoreCase(mode);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", habilitado ? "ATO_AUTONOMO_HABILITADO" : "ATO_AUTONOMO_LATENTE");
        out.put("atoAutonomo", true);
        out.put("tipoAto", WorkItemType.MANIFESTACAO.name());
        out.put("gatilho", "INTIMACAO_DO_RECURSO_ADVERSO");
        out.put("habilitado", habilitado);
        put(out, "counterReasonsMode", mode);
        put(out, "counterReasonsDesk", admissibility == null ? null : admissibility.counterReasonsDesk());
        put(out, "queueCodeSugerido", admissibility == null ? null : admissibility.counterReasonsDesk());
        put(out, "workItemPaiRecurso", recursoPrincipal.getId());
        put(out, "lineageKey", stableKey("RECURSAL_CONTRARRAZOES", processo.getId(), appealType.name(), recursoPrincipal.getId()));
        put(out, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        out.put("contrarrazoesControladas", boolValue(sigiloRecursal, "contrarrazoesControladas"));
        if (structured != null && !structured.contrarrazoes().isEmpty()) {
            out.put("planejamento", structured.contrarrazoes());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildEmbargosAtoAutonomo(Processo processo,
                                                         Usuario usuario,
                                                         WorkItem recursoPrincipal,
                                                         LegalAppealType appealType,
                                                         RecursalIaStructuredAnalysis structured,
                                                         Map<String, Object> sigiloRecursal) {
        boolean ativo = appealType == LegalAppealType.EMBARGOS_DECLARACAO
                || appealType == LegalAppealType.EMBARGOS_INFRINGENTES
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL
                || appealType == LegalAppealType.EMBARGOS_TERCEIRO;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", ativo ? "FLUXO_AUTONOMO_ATIVO" : "FLUXO_AUTONOMO_POTENCIAL");
        out.put("atoAutonomo", true);
        out.put("tipoAto", WorkItemType.RECURSO.name());
        out.put("embargosAtivos", ativo);
        out.put("tipoRecursoCanonico", appealType.name());
        put(out, "queueCodeSugerido", recursoPrincipal.getQueueCode());
        put(out, "lineageKey", stableKey("RECURSAL_EMBARGOS", processo.getId(), appealType.name(), recursoPrincipal.getId()));
        put(out, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        out.put("embargosSigilosos", boolValue(sigiloRecursal, "embargosSigilosos"));
        if (structured != null && !structured.embargosEspecializados().isEmpty()) {
            out.put("planejamento", structured.embargosEspecializados());
        }
        if (ativo) {
            String fundamentacaoEmbargos = structured != null && !structured.embargosEspecializados().isEmpty()
                    ? structured.embargosEspecializados().entrySet().stream()
                            .map(entry -> entry.getKey() + ": " + String.valueOf(entry.getValue()))
                            .collect(java.util.stream.Collectors.joining(System.lineSeparator()))
                    : firstNonBlank(recursoPrincipal.getDescricao(), processo.getResumoIA(), "Embargos formalizados para integração, esclarecimento ou revisão do ato recorrido.");
            String dispositivoEmbargos = switch (appealType) {
                case EMBARGOS_DECLARACAO -> "Requer o recebimento e processamento dos embargos de declaração, com saneamento do vício apontado e integração do julgado.";
                case EMBARGOS_INFRINGENTES -> "Requer o processamento dos embargos infringentes e a prevalência da conclusão divergente mais favorável à tese embargante.";
                case EMBARGOS_EXECUCAO -> "Requer o recebimento dos embargos à execução, com apreciação integral das matérias defensivas executivas.";
                case EMBARGOS_EXECUCAO_FISCAL -> "Requer o recebimento dos embargos à execução fiscal, com revisão dos pressupostos do título e dos atos constritivos.";
                case EMBARGOS_TERCEIRO -> "Requer o recebimento dos embargos de terceiro, com liberação do bem indevidamente constrito e proteção da esfera patrimonial autônoma.";
                default -> "Requer o regular processamento dos embargos autônomos com enfrentamento integral das questões integrativas ou revisionais deduzidas.";
            };
            Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                    processo.getId(),
                    recursoTitle(appealType) + " — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO_SEM_NUMERO"),
                    fundamentacaoEmbargos,
                    dispositivoEmbargos,
                    resolveRecursalOrgao(processo, recursoPrincipal, usuario),
                    resolveRecursalNivelInstancia(appealType),
                    "EMBARGOS_AUTONOMOS",
                    Map.of(
                            "tipoEmbargos", appealType.name(),
                            "queueCodeOrigem", safe(recursoPrincipal.getQueueCode()),
                            "lineageEmbargos", stableKey("RECURSAL_EMBARGOS_DOCUMENTO", processo.getId(), appealType.name(), recursoPrincipal.getId())
                    )
            );
            out.put("documentoFormalAssinado", documentoFormalAssinado);
            out.put("assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"));
            out.put("validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana"));
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildAssinaturaVinculada(Processo processo,
                                                         Usuario usuario,
                                                         WorkItem peticaoRecursal,
                                                         WorkItem recursoPrincipal,
                                                         LegalAppealType appealType,
                                                         RecursalIaStructuredAnalysis structured,
                                                         Map<String, Object> pecaFormalPrincipal,
                                                         RecursalAdmissibilityResponse admissibility,
                                                         Map<String, Object> sigiloRecursal) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ASSINATURA_VINCULADA_PREPARADA");
        out.put("lineageKey", stableKey("RECURSAL_SIGNATURE", processo.getId(), peticaoRecursal.getId(), recursoPrincipal.getId(), appealType.name()));
        out.put("tipoRecursoCanonico", appealType.name());
        put(out, "processoId", processo.getId());
        put(out, "signerUserId", usuario == null ? null : usuario.getId());
        put(out, "signerCpf", usuario == null ? null : normalizeNullable(usuario.getCpf()));
        out.put("linkedWorkItems", safeObjects(peticaoRecursal.getId(), recursoPrincipal.getId()));
        out.put("linkedTemplateCodes", safeList(List.of(safe(peticaoRecursal.getTemplateCode()), safe(recursoPrincipal.getTemplateCode()))));
        put(out, "linkedRevisionHash", pecaFormalPrincipal.get("revisionHash"));
        out.put("stepUpRequired", admissibility != null && admissibility.stepUpRequired());
        out.put("certificateRequired", admissibility != null && admissibility.certificateRequired());
        out.put("proofEnvelopeMode", admissibility == null ? "PADRAO" : firstNonBlank(admissibility.proofBundleMode(), admissibility.evidencePolicy(), admissibility.evidenceRetentionPolicy()));
        boolean sigiloStepUp = boolValue(sigiloRecursal, "stepUpAcessoRecurso");
        boolean sigiloCertificado = boolValue(sigiloRecursal, "certificateOrStrongCredentialRequired");
        out.put("signatureMode", (admissibility != null && admissibility.certificateRequired()) || sigiloCertificado
                ? "CERTIFICADO_OU_CREDENCIAL_REFORCADA"
                : (admissibility != null && admissibility.stepUpRequired()) || sigiloStepUp
                ? "STEP_UP_FORTE"
                : "ASSINATURA_CONTROLADA");
        out.put("sigiloStepUp", sigiloStepUp);
        put(out, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        if (structured != null && !structured.assinaturaRecursal().isEmpty()) {
            out.put("planejamento", structured.assinaturaRecursal());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildProtocoloConectorJudicial(Processo processo,
                                                               Usuario usuario,
                                                               WorkItem peticaoRecursal,
                                                               WorkItem recursoPrincipal,
                                                               LegalAppealType appealType,
                                                               RecursalIaStructuredAnalysis structured,
                                                               Map<String, Object> pecaFormalPrincipal,
                                                               RecursalPdfArtifact pecaFormalPrincipalPdf,
                                                               RecursalPdfValidationResult pecaFormalPrincipalPdfValidation,
                                                               Map<String, Object> assinaturaVinculada,
                                                               RecursalAdmissibilityResponse admissibility,
                                                               Map<String, Object> sigiloRecursal) {
        JudicialSystem system = resolveSystem(admissibility, processo);
        ProceduralSubmissionBlueprintReport blueprint = processo.getSubmissionBlueprint() != null
                ? processo.getSubmissionBlueprint()
                : synthesizeBlueprint(processo, recursoPrincipal, appealType, admissibility, pecaFormalPrincipal, assinaturaVinculada, system);
        ProceduralConnectorExecutionReport execution = processo.getConnectorExecution() != null
                ? processo.getConnectorExecution()
                : synthesizeExecution(processo, usuario, recursoPrincipal, admissibility, system);
        ProtocolSubmissionRequest request = buildProtocolSubmissionRequest(processo, usuario, appealType, pecaFormalPrincipal, pecaFormalPrincipalPdf, pecaFormalPrincipalPdfValidation, assinaturaVinculada, blueprint, execution, admissibility, sigiloRecursal);
        JudicialConnectorOperationalProfileReport operationalProfile = judicialConnectorOperationalProfileService.analyze(system, request);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        RecursalProtocolArtifactReadiness readiness = recursalProtocolArtifactReadinessService.assess(
                pecaFormalPrincipalPdf,
                pecaFormalPrincipalPdfValidation,
                operationalProfile,
                admissibility != null && admissibility.certificateRequired()
        );
        boolean proofValid = readiness.protocolArtifactValid();
        out.put("status", readiness.status());
        out.put("judicialSystem", system.name());
        put(out, "requestId", request.requestId());
        put(out, "tribunalCodigo", request.tribunalCodigo());
        put(out, "unidadeJudiciariaCodigo", request.unidadeJudiciariaCodigo());
        put(out, "titulo", request.title());
        put(out, "readyForTribunalSubmission", readiness.connectorSubmissionReady());
        put(out, "readyForProduction", readiness.readyForProduction());
        put(out, "connectorAuthMode", operationalProfile.authMode() == null ? null : operationalProfile.authMode().name());
        put(out, "payloadJson", request.payloadJson());
        put(out, "primaryPdfValidation", pecaFormalPrincipalPdfValidation == null ? null : pecaFormalPrincipalPdfValidation.toMap());
        put(out, "integrityHash", request.integrityHash());
        out.put("linkedWorkItems", safeObjects(peticaoRecursal.getId(), recursoPrincipal.getId()));
        out.put("submissionBlueprint", blueprint.toMap());
        out.put("connectorExecution", execution.toMap());
        out.put("protocolSubmissionRequest", protocolRequestMap(request));
        out.put("connectorOperationalProfile", operationalProfile.toMap());
        put(out, "nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        put(out, "protocolSubmissionMode", stringValue(sigiloRecursal, "protocolSubmissionMode"));
        out.put("protocolArtifactValidated", proofValid);
        out.put("protocolArtifactReadiness", readiness.toMap());
        out.put("mascaramentoObrigatorio", boolValue(sigiloRecursal, "mascaramentoObrigatorio"));
        if (structured != null && !structured.protocoloExterno().isEmpty()) {
            out.put("planejamento", structured.protocoloExterno());
        }
        return Collections.unmodifiableMap(out);
    }

    private ProtocolSubmissionRequest buildProtocolSubmissionRequest(Processo processo,
                                                                    Usuario usuario,
                                                                    LegalAppealType appealType,
                                                                    Map<String, Object> pecaFormalPrincipal,
                                                                    RecursalPdfArtifact pecaFormalPrincipalPdf,
                                                                    RecursalPdfValidationResult pecaFormalPrincipalPdfValidation,
                                                                    Map<String, Object> assinaturaVinculada,
                                                                    ProceduralSubmissionBlueprintReport blueprint,
                                                                    ProceduralConnectorExecutionReport execution,
                                                                    RecursalAdmissibilityResponse admissibility,
                                                                    Map<String, Object> sigiloRecursal) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processo.getId());
        payload.put("numeroProcesso", firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()));
        payload.put("tipoRecursoCanonico", appealType.name());
        payload.put("pecaFormalPrincipal", pecaFormalPrincipal);
        if (pecaFormalPrincipalPdf != null && pecaFormalPrincipalPdf.available()) {
            payload.put("documentosProtocolaveis", List.of(pecaFormalPrincipalPdf.toMap()));
        }
        payload.put("assinaturaVinculada", assinaturaVinculada);
        if (pecaFormalPrincipalPdfValidation != null) {
            payload.put("primaryPdfValidation", pecaFormalPrincipalPdfValidation.toMap());
        }
        if (pecaFormalPrincipalPdf != null && pecaFormalPrincipalPdf.metadata().containsKey("longTermValidationBundle")) {
            payload.put("primaryPdfLongTermValidation", pecaFormalPrincipalPdf.metadata().get("longTermValidationBundle"));
        }
        if (pecaFormalPrincipalPdf != null) {
            payload.put("primaryPdfDssStatus", pecaFormalPrincipalPdf.metadata().get("dssMaterializationStatus"));
            payload.put("primaryPdfDssMaterialized", Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("dssMaterialized")));
            payload.put("primaryPdfVriMaterialized", Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("vriMaterialized")));
            payload.put("primaryPdfRevocationMaterialized", Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("revocationMaterialized")));
        }
        if (sigiloRecursal != null && !sigiloRecursal.isEmpty()) {
            payload.put("sigiloRecursal", protocolSigiloSummary(sigiloRecursal));
        }
        payload.put("connectorExecution", execution.toMap());
        payload.put("submissionBlueprint", blueprint.toMap());
        String payloadJson = serialize(payload);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo.getId());
        metadata.put("profileCode", usuario == null ? null : usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name());
        metadata.put("appealType", appealType.name());
        metadata.put("govbrStepUp", admissibility != null && admissibility.stepUpRequired());
        metadata.put("certificateRequired", admissibility != null && admissibility.certificateRequired());
        metadata.put("connectorSystem", blueprint.judicialSystem() == null ? null : blueprint.judicialSystem().name());
        metadata.put("submissionLane", execution.submissionLane());
        metadata.put("tribunalTargetKey", execution.tribunalTargetKey());
        metadata.put("signerMode", execution.signerMode());
        metadata.put("retryPolicy", execution.retryPolicy());
        metadata.put("templateCodePeticao", pecaFormalPrincipal.get("templateCodePeticao"));
        metadata.put("primaryPdfFilename", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.filename());
        metadata.put("primaryPdfSha256", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.sha256());
        metadata.put("primaryPdfPages", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.pageCount());
        metadata.put("primaryPdfValidationStatus", pecaFormalPrincipalPdfValidation == null ? null : pecaFormalPrincipalPdfValidation.status());
        metadata.put("primaryPdfValidationOk", pecaFormalPrincipalPdfValidation != null && pecaFormalPrincipalPdfValidation.valid());
        metadata.put("primaryPdfProofEnvelopeId", proofEnvelopeId(pecaFormalPrincipalPdf));
        metadata.put("primaryPdfPadesProfileCandidate", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.metadata().get("padesProfileCandidate"));
        metadata.put("primaryPdfDssStatus", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.metadata().get("dssMaterializationStatus"));
        metadata.put("primaryPdfDssMaterialized", pecaFormalPrincipalPdf != null && Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("dssMaterialized")));
        metadata.put("primaryPdfVriMaterialized", pecaFormalPrincipalPdf != null && Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("vriMaterialized")));
        metadata.put("primaryPdfRevocationMaterialized", pecaFormalPrincipalPdf != null && Boolean.TRUE.equals(pecaFormalPrincipalPdf.metadata().get("revocationMaterialized")));
        metadata.put("primaryPdfLongTermBundle", pecaFormalPrincipalPdf == null ? null : pecaFormalPrincipalPdf.metadata().get("longTermValidationBundle"));
        metadata.put("templateCodeRecurso", pecaFormalPrincipal.get("templateCodeRecurso"));
        metadata.put("revisionHash", pecaFormalPrincipal.get("revisionHash"));
        metadata.put("lineageKey", pecaFormalPrincipal.get("lineageKey"));
        metadata.put("nivelSigiloRecursal", stringValue(sigiloRecursal, "nivelRecomendado"));
        metadata.put("protocolSubmissionMode", stringValue(sigiloRecursal, "protocolSubmissionMode"));
        metadata.put("requestPreview", payload);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new ProtocolSubmissionRequest(
                firstNonBlank(blueprint.requestId(), "REC-SUB-" + processo.getId() + '-' + appealType.name()),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                firstNonBlank(recursoTitle(appealType), processo.getClasseProcessual(), processo.getAssunto(), recursoPrincipalTitle(appealType)),
                firstNonBlank(blueprint.tribunalCodigo(), processo.getTribunalCodigoRoteado(), processo.getTribunal()),
                firstNonBlank(blueprint.unidadeJudiciariaCodigo(), processo.getUnidadeJudiciariaCodigo()),
                firstNonBlank(blueprint.unidadeJudiciariaNome(), processo.getVara()),
                firstNonBlank(blueprint.rito(), processo.getRito() == null ? null : processo.getRito().name()),
                firstNonBlank(blueprint.classeTpuCodigo(), processo.getClasseTpuCodigo()),
                processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                payloadJson,
                sha256Hex(payloadJson),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getId(),
                false,
                Collections.unmodifiableMap(metadata)
        );
    }

    private ProceduralSubmissionBlueprintReport synthesizeBlueprint(Processo processo,
                                                                    WorkItem recursoPrincipal,
                                                                    LegalAppealType appealType,
                                                                    RecursalAdmissibilityResponse admissibility,
                                                                    Map<String, Object> pecaFormalPrincipal,
                                                                    Map<String, Object> assinaturaVinculada,
                                                                    JudicialSystem system) {
        LinkedHashMap<String, Object> preview = new LinkedHashMap<>();
        preview.put("tipoRecursoCanonico", appealType.name());
        preview.put("workItemIdRecurso", recursoPrincipal.getId());
        preview.put("revisionHash", pecaFormalPrincipal.get("revisionHash"));
        preview.put("signatureLineage", assinaturaVinculada.get("lineageKey"));
        preview.put("stepUpRequired", admissibility != null && admissibility.stepUpRequired());
        preview.put("certificateRequired", admissibility != null && admissibility.certificateRequired());
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("origin", "RECURSAL_FORMALIZACAO_FACADE");
        metadata.put("protocolDesk", admissibility == null ? null : admissibility.protocolDesk());
        metadata.put("integrationChannel", admissibility == null ? null : admissibility.integrationChannel());
        metadata.put("payloadPolicy", admissibility == null ? null : admissibility.payloadPolicy());
        metadata.put("transmissionMode", admissibility == null ? null : admissibility.transmissionMode());
        metadata.put("manualSubmissionDesk", admissibility == null ? null : admissibility.manualSubmissionDesk());
        metadata.put("proofBundleMode", admissibility == null ? null : admissibility.proofBundleMode());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        boolean ready = admissibility != null && admissibility.tempestivo() && (!admissibility.preparoExigido() || admissibility.preparoSatisfeito() || admissibility.preparoDispensado());
        return new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "REC-" + processo.getId() + '-' + recursoPrincipal.getId(),
                ready ? "RECURSAL_BLUEPRINT_READY" : "RECURSAL_BLUEPRINT_REVIEW_REQUIRED",
                true,
                ready,
                true,
                system,
                firstNonBlank(processo.getTribunalCodigoRoteado(), admissibility == null ? null : admissibility.tribunalDestino(), processo.getTribunal()),
                firstNonBlank(processo.getTribunal(), admissibility == null ? null : admissibility.tribunalDestino()),
                processo.getClasseTpuCodigo(),
                processo.getClasseProcessual(),
                processo.getUnidadeJudiciariaCodigo(),
                processo.getVara(),
                processo.getRito() == null ? null : processo.getRito().name(),
                appealType.name(),
                processo.getCompetenciaTerritorialModo(),
                processo.getPreventionMode(),
                processo.getLinkageMode(),
                null,
                List.of(),
                true,
                admissibility != null && admissibility.stepUpRequired(),
                admissibility != null && admissibility.certificateRequired(),
                ready ? "DRY_RUN_READY" : "REVIEW_REQUIRED",
                "DRY-REC-" + processo.getId() + '-' + recursoPrincipal.getId(),
                List.of(),
                safeList(admissibility == null ? null : admissibility.alertas()),
                safeList(admissibility == null ? null : admissibility.connectorWarnings()),
                Map.copyOf(preview),
                Collections.unmodifiableMap(metadata)
        );
    }

    private ProceduralConnectorExecutionReport synthesizeExecution(Processo processo,
                                                                  Usuario usuario,
                                                                  WorkItem recursoPrincipal,
                                                                  RecursalAdmissibilityResponse admissibility,
                                                                  JudicialSystem system) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("origin", "RECURSAL_FORMALIZACAO_FACADE");
        metadata.put("processoId", processo.getId());
        metadata.put("executorUserId", usuario == null ? null : usuario.getId());
        metadata.put("protocolDesk", admissibility == null ? null : admissibility.protocolDesk());
        metadata.put("reviewDesk", admissibility == null ? null : admissibility.reviewDesk());
        metadata.put("supportDesk", admissibility == null ? null : admissibility.supportDesk());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        boolean allowed = admissibility != null && admissibility.tempestivo() && (!admissibility.preparoExigido() || admissibility.preparoSatisfeito() || admissibility.preparoDispensado());
        return new ProceduralConnectorExecutionReport(
                Instant.now(),
                allowed ? "REAL_CONNECTOR_READY" : "REVIEW_REQUIRED",
                firstNonBlank(admissibility == null ? null : admissibility.protocolDesk(), admissibility == null ? null : admissibility.manualSubmissionDesk(), recursoPrincipal.getQueueCode()),
                firstNonBlank(processo.getTribunalCodigoRoteado(), admissibility == null ? null : admissibility.tribunalDestino(), processo.getTribunal()),
                system,
                firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()),
                processo.getUnidadeJudiciariaCodigo(),
                processo.getClasseTpuCodigo(),
                "REC-SUB-" + processo.getId() + '-' + recursoPrincipal.getId(),
                admissibility != null && admissibility.certificateRequired() ? "CERTIFICATE" : admissibility != null && admissibility.stepUpRequired() ? "STEP_UP" : "STANDARD",
                firstNonBlank(admissibility == null ? null : admissibility.retryMode(), "STANDARD_RETRY_POLICY"),
                allowed,
                !allowed,
                admissibility != null && admissibility.stepUpRequired(),
                admissibility != null && admissibility.certificateRequired(),
                List.of("FORMALIZAR_PECA", "VINCULAR_ASSINATURA", "ENVELOPAR_PROTOCOLO", "SUBMETER_CONECTOR"),
                safeList(admissibility == null ? null : admissibility.fundamentos()),
                List.of(),
                safeList(admissibility == null ? null : admissibility.alertas()),
                Collections.unmodifiableMap(metadata)
        );
    }

    private Map<String, Object> protocolRequestMap(ProtocolSubmissionRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (request == null) {
            return Map.of();
        }
        put(out, "requestId", request.requestId());
        put(out, "numeroUnificado", request.numeroUnificado());
        put(out, "title", request.title());
        put(out, "tribunalCodigo", request.tribunalCodigo());
        put(out, "unidadeJudiciariaCodigo", request.unidadeJudiciariaCodigo());
        put(out, "unidadeJudiciariaNome", request.unidadeJudiciariaNome());
        put(out, "rito", request.rito());
        put(out, "classeTpu", request.classeTpu());
        put(out, "ramoDireito", request.ramoDireito());
        put(out, "integrityHash", request.integrityHash());
        put(out, "signerUserId", request.signerUserId());
        put(out, "executorUserId", request.executorUserId());
        out.put("dryRun", request.dryRun());
        out.put("metadata", safeMap(request.metadata()));
        return Collections.unmodifiableMap(out);
    }

    private JudicialSystem resolveSystem(RecursalAdmissibilityResponse admissibility, Processo processo) {
        String raw = firstNonBlank(admissibility == null ? null : admissibility.connectorSystem(), processo.getConnectorSystem());
        if (raw == null) {
            return JudicialSystem.PJE;
        }
        try {
            return JudicialSystem.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return JudicialSystem.OUTRO;
        }
    }

    private String buildTempestividade(RecursalAdmissibilityResponse admissibility) {
        if (admissibility == null) {
            return "Conferir a data da intimação, o termo inicial e a contagem do prazo antes da assinatura final da peça.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Prazo ").append(admissibility.tipoPrazo() == null ? "recursal" : admissibility.tipoPrazo().toLowerCase(Locale.ROOT));
        if (admissibility.dataProtocolo() != null) {
            sb.append(" com protocolo previsto/registrado em ").append(admissibility.dataProtocolo()).append('.');
        }
        if (admissibility.dataLimite() != null) {
            sb.append(" Data-limite considerada: ").append(admissibility.dataLimite()).append('.');
        }
        if (!admissibility.tempestivo()) {
            sb.append(" Há alerta de intempestividade ou de necessidade de revisão humana da contagem.");
        }
        return sb.toString();
    }

    private String buildAssinaturaLinha(Usuario usuario, String profileCode) {
        String nome = firstNonBlank(usuario == null ? null : usuario.getNome(), "[ASSINANTE RESPONSÁVEL]");
        String documento = firstNonBlank(usuario == null ? null : usuario.getCpf(), usuario == null ? null : usuario.getEmail());
        if (documento == null) {
            return nome + " — " + firstNonBlank(profileCode, "PJB");
        }
        return nome + " — " + documento;
    }

    private String resolveRecursalNivelInstancia(LegalAppealType appealType) {
        if (appealType == null) {
            return "SEGUNDO_GRAU";
        }
        return switch (appealType) {
            case RESP, RE, AGRAVO_RESP_RE, AGRAVO_REGIMENTAL, RECLAMACAO_CONSTITUCIONAL, CONFLITO_COMPETENCIA -> "ULTIMA_INSTANCIA";
            default -> "SEGUNDO_GRAU";
        };
    }

    private String resolveRecursalOrgao(Processo processo, WorkItem recursoPrincipal, Usuario usuario) {
        return firstNonBlank(
                processo == null ? null : processo.getTribunal(),
                recursoPrincipal == null ? null : recursoPrincipal.getQueueCode(),
                usuario == null ? null : usuario.getComarca(),
                "ORGAO_RECURSAL"
        );
    }


    private String safeDraftRecurso(Map<String, Object> draftContext,
                                    String razoes,
                                    String fundamentacao,
                                    LegalAppealType appealType) {
        String draft = legalDraftingService.draftRecurso(Map.copyOf(draftContext));
        if (draft != null && !draft.isBlank()) {
            return draft;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(recursoTitle(appealType)).append("\n\n");
        if (fundamentacao != null && !fundamentacao.isBlank()) {
            sb.append(fundamentacao).append("\n\n");
        }
        if (razoes != null && !razoes.isBlank()) {
            sb.append(razoes).append("\n");
        }
        return sb.toString().trim();
    }

    private boolean boolValue(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return false;
        }
        Object value = source.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
    }

    private String stringValue(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String serialize(Object payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(jsonSafe(payload));
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
    }

    private Object jsonSafe(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor || value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> {
                if (key != null) {
                    out.put(String.valueOf(key), jsonSafe(entryValue));
                }
            });
            return out;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::jsonSafe).toList();
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> out = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                out.add(jsonSafe(Array.get(value, index)));
            }
            return out;
        }
        return String.valueOf(value);
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalizeNullable(key);
            if (normalizedKey == null || value == null) {
                return;
            }
            if (value instanceof String text) {
                String normalizedValue = normalizeNullable(text);
                if (normalizedValue != null) {
                    out.put(normalizedKey, normalizedValue);
                }
                return;
            }
            out.put(normalizedKey, value);
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private List<Object> safeObjects(Object... values) {
        ArrayList<Object> out = new ArrayList<>();
        if (values == null || values.length == 0) {
            return List.of();
        }
        for (Object value : values) {
            if (value != null) {
                out.add(value);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private Map<String, Object> protocolSigiloSummary(Map<String, Object> sigiloRecursal) {
        if (sigiloRecursal == null || sigiloRecursal.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "status", sigiloRecursal.get("status"));
        put(out, "nivelRecomendado", sigiloRecursal.get("nivelRecomendado"));
        put(out, "protocolSubmissionMode", sigiloRecursal.get("protocolSubmissionMode"));
        put(out, "revisaoJudicialObrigatoria", sigiloRecursal.get("revisaoJudicialObrigatoria"));
        put(out, "decretoExclusivoMagistrado", sigiloRecursal.get("decretoExclusivoMagistrado"));
        put(out, "stepUpAcessoRecurso", sigiloRecursal.get("stepUpAcessoRecurso"));
        put(out, "mascaramentoObrigatorio", sigiloRecursal.get("mascaramentoObrigatorio"));
        return Collections.unmodifiableMap(out);
    }


    private String proofEnvelopeId(RecursalPdfArtifact artifact) {
        if (artifact == null || artifact.metadata().isEmpty()) {
            return null;
        }
        Object value = artifact.metadata().get("proofEnvelopeId");
        return value == null ? null : normalizeNullable(String.valueOf(value));
    }

    private String recursoTitle(LegalAppealType appealType) {
        return switch (appealType) {
            case EMBARGOS_DECLARACAO -> "Embargos de Declaração";
            case EMBARGOS_TERCEIRO -> "Embargos de Terceiro";
            case EMBARGOS_EXECUCAO -> "Embargos à Execução";
            case EMBARGOS_EXECUCAO_FISCAL -> "Embargos à Execução Fiscal";
            case EMBARGOS_INFRINGENTES -> "Embargos Infringentes";
            case RE -> "Recurso Extraordinário";
            case RESP -> "Recurso Especial";
            case AGRAVO_INSTRUMENTO -> "Agravo de Instrumento";
            case AGRAVO_INTERNO -> "Agravo Interno";
            case AGRAVO_REGIMENTAL -> "Agravo Regimental";
            case AGRAVO_RESP_RE -> "Agravo em Recurso para Tribunal Superior";
            case APELACAO, APELACAO_PENAL -> "Apelação";
            case PEDIDO_UNIFORMIZACAO -> "Pedido de Uniformização";
            default -> appealType.name();
        };
    }

    private String recursoPrincipalTitle(LegalAppealType appealType) {
        return recursoTitle(appealType) + " — envelope de protocolo judicial";
    }

    private String stableKey(String prefix, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix == null ? "REC" : prefix);
        if (parts != null) {
            for (Object part : parts) {
                sb.append('|').append(safe(part));
            }
        }
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(safe(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return Integer.toHexString(Objects.hashCode(value));
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        String normalizedKey = normalizeNullable(key);
        if (normalizedKey == null) {
            return;
        }
        if (value instanceof String text) {
            String normalizedValue = normalizeNullable(text);
            if (normalizedValue != null) {
                target.put(normalizedKey, normalizedValue);
            }
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(normalizedKey, value);
    }
}
