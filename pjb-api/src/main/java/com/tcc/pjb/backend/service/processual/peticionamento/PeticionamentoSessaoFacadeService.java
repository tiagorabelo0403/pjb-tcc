package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoAutomacaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoGuardrailResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoModo;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoAssistService;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.upload.UploadCapacityGovernanceService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoInitialIntakeWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoJurisprudenciaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMultimidiaComposerService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoThreatSentinelService;
import com.tcc.pjb.backend.integration.serpro.datavalid.CpfValidacaoService;

@Service
public class PeticionamentoSessaoFacadeService {

    private final CurrentUserService currentUserService;
    private final PeticionamentoEnderecoAutomationService enderecoAutomationService;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final LaianePeticaoInicialDraftService laianePeticaoInicialDraftService;
    private final PeticionamentoInitialIntakeWorkspaceService intakeWorkspaceService;
    private final LaianePeticaoAssistService laianePeticaoAssistService;
    private final SigiloService sigiloService;
    private final PeticionamentoPreventiveGuardrailService peticionamentoPreventiveGuardrailService;
    private final PeticionamentoPayloadHardeningService payloadHardeningService;
    private final PeticionamentoDocumentBatchReadingStrategyService documentBatchReadingStrategyService;
    private final PeticionamentoProcedureSpecificVerifierService procedureSpecificVerifierService;
    private final PeticionamentoProtocolEnvelopeHardeningService protocolEnvelopeHardeningService;
    private final PeticionamentoMultimidiaComposerService multimidiaComposerService;
    private final PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService;
    private final PeticionamentoThreatSentinelService threatSentinelService;
    private final PeticionamentoMediaStorageShieldService mediaStorageShieldService;
    private final PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService;
    private final PeticionamentoMediaPublicationGateService mediaPublicationGateService;
    private final UploadCapacityGovernanceService uploadCapacityGovernanceService;
    private final PeticionamentoJurisprudenciaWorkspaceService jurisprudenciaWorkspaceService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final CpfValidacaoService cpfValidacaoService;

    public PeticionamentoSessaoFacadeService(CurrentUserService currentUserService,
                                             PeticionamentoEnderecoAutomationService enderecoAutomationService,
                                             RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                                             LaianePeticaoInicialDraftService laianePeticaoInicialDraftService,
                                             PeticionamentoInitialIntakeWorkspaceService intakeWorkspaceService,
                                             LaianePeticaoAssistService laianePeticaoAssistService,
                                             SigiloService sigiloService,
                                             PeticionamentoPreventiveGuardrailService peticionamentoPreventiveGuardrailService,
                                             PeticionamentoPayloadHardeningService payloadHardeningService,
                                             PeticionamentoDocumentBatchReadingStrategyService documentBatchReadingStrategyService,
                                             PeticionamentoProcedureSpecificVerifierService procedureSpecificVerifierService,
                                             PeticionamentoProtocolEnvelopeHardeningService protocolEnvelopeHardeningService,
                                             PeticionamentoMultimidiaComposerService multimidiaComposerService,
                                             PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService,
                                             PeticionamentoThreatSentinelService threatSentinelService,
                                             PeticionamentoMediaStorageShieldService mediaStorageShieldService,
                                             PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService,
                                             PeticionamentoMediaPublicationGateService mediaPublicationGateService,
                                             UploadCapacityGovernanceService uploadCapacityGovernanceService,
                                             PeticionamentoJurisprudenciaWorkspaceService jurisprudenciaWorkspaceService,
                                             InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                             ObjectProvider<OfficeProcessWorkspaceScopeService> officeProcessWorkspaceScopeServiceProvider,
                                             CpfValidacaoService cpfValidacaoService) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.enderecoAutomationService = Objects.requireNonNull(enderecoAutomationService, "enderecoAutomationService");
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService, "representacaoProcessualPolicyService");
        this.laianePeticaoInicialDraftService = Objects.requireNonNull(laianePeticaoInicialDraftService, "laianePeticaoInicialDraftService");
        this.intakeWorkspaceService = Objects.requireNonNull(intakeWorkspaceService, "intakeWorkspaceService");
        this.laianePeticaoAssistService = Objects.requireNonNull(laianePeticaoAssistService, "laianePeticaoAssistService");
        this.sigiloService = Objects.requireNonNull(sigiloService, "sigiloService");
        this.peticionamentoPreventiveGuardrailService = Objects.requireNonNull(peticionamentoPreventiveGuardrailService, "peticionamentoPreventiveGuardrailService");
        this.payloadHardeningService = Objects.requireNonNull(payloadHardeningService, "payloadHardeningService");
        this.documentBatchReadingStrategyService = Objects.requireNonNull(documentBatchReadingStrategyService, "documentBatchReadingStrategyService");
        this.procedureSpecificVerifierService = Objects.requireNonNull(procedureSpecificVerifierService, "procedureSpecificVerifierService");
        this.protocolEnvelopeHardeningService = Objects.requireNonNull(protocolEnvelopeHardeningService, "protocolEnvelopeHardeningService");
        this.multimidiaComposerService = Objects.requireNonNull(multimidiaComposerService, "multimidiaComposerService");
        this.mediaSecurityPipelineService = Objects.requireNonNull(mediaSecurityPipelineService, "mediaSecurityPipelineService");
        this.threatSentinelService = Objects.requireNonNull(threatSentinelService, "threatSentinelService");
        this.mediaStorageShieldService = Objects.requireNonNull(mediaStorageShieldService, "mediaStorageShieldService");
        this.periciaEvidenceIntelligenceService = Objects.requireNonNull(periciaEvidenceIntelligenceService, "periciaEvidenceIntelligenceService");
        this.mediaPublicationGateService = Objects.requireNonNull(mediaPublicationGateService, "mediaPublicationGateService");
        this.uploadCapacityGovernanceService = Objects.requireNonNull(uploadCapacityGovernanceService, "uploadCapacityGovernanceService");
        this.jurisprudenciaWorkspaceService = Objects.requireNonNull(jurisprudenciaWorkspaceService, "jurisprudenciaWorkspaceService");
        this.institutionalMultimediaWorkspaceService = Objects.requireNonNull(institutionalMultimediaWorkspaceService, "institutionalMultimediaWorkspaceService");
        this.officeProcessWorkspaceScopeService = officeProcessWorkspaceScopeServiceProvider.getIfAvailable();
        this.cpfValidacaoService = Objects.requireNonNull(cpfValidacaoService, "cpfValidacaoService");
    }

    public PeticionamentoSessaoResponse abrirSessaoInicial(PeticionamentoSessaoRequest request) {
        PeticionamentoPayloadHardeningService.HardenedPayload hardened = payloadHardeningService.harden(request);
        PeticionamentoSessaoRequest safe = hardened.request();
        Usuario usuario = requirePeticionante();
        cpfValidacaoService.validarParaPeticionamento(usuario.getCpf());
        PeticionamentoModo modo = safe.modoResolvido();
        String sessionKey = buildSessionKey(usuario, safe, modo, hardened.fingerprint());

        ensureWorkspaceAccessIfExistingProcess(safe);
        PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake = intakeWorkspaceService.analyze(safe, usuario);
        PeticionamentoEnderecoResponse enderecoAutor = enderecoAutomationService.resolve(defaultEndereco(safe.getEnderecoAutor()), safe.resolverEnderecoAutomaticamenteResolvido());
        PeticionamentoEnderecoResponse enderecoReu = enderecoAutomationService.resolve(defaultEndereco(safe.getEnderecoReu()), safe.resolverEnderecoAutomaticamenteResolvido());
        RepresentacaoProcessualPolicyResponse representacao = representacaoProcessualPolicyService.resolve(
                safe.getRamoDireito(),
                safe.getRitoProcessual(),
                tribunalFromCtx(safe.getCtx()),
                usuario.getTipoUsuario(),
                safe.getTipoInstrumentoRepresentacao(),
                safe.getAudienciaId(),
                safe.getTipoAudiencia(),
                Boolean.TRUE.equals(safe.getContextoConsensual()),
                Boolean.TRUE.equals(safe.getPoderesEspeciaisTransigir()),
                safe.getTermoAudienciaReferencia(),
                safe.getAtaAudienciaReferencia()
        );
        SigiloService.SigiloDecision sigiloDecision = sigiloService.avaliarCorpus(buildCorpus(safe, enderecoAutor, enderecoReu));

        LaianePeticaoInicialDraftService.DraftView manualDraft = null;
        if (modo.includeManual()) {
            manualDraft = laianePeticaoInicialDraftService.estruturar(intake.resolvedDraftRequest());
        }

        String effectiveDraftMarkdown = hasText(safe.getDraftMarkdown())
                ? safe.getDraftMarkdown()
                : manualDraft != null ? manualDraft.minutaInicial() : null;
        LaianePeticaoAssistRequest assistRequest = safe.toAssistRequest(effectiveDraftMarkdown, enderecoAutor, enderecoReu);
        enrichAssistRequest(assistRequest, intake);

        LaianePeticaoAssistResponse assistiveAnalysis = null;
        LaianePeticaoProtocolPackageResponse protocolPackage = null;
        if (modo.includeAssistive()) {
            assistiveAnalysis = laianePeticaoAssistService.draftAndPreflight(assistRequest);
            if (safe.prepararPacoteProtocoloResolvido()) {
                protocolPackage = laianePeticaoAssistService.createProtocolPackage(assistRequest);
            }
        }

        PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading = documentBatchReadingStrategyService.plan(
                new PeticionamentoDocumentBatchReadingStrategyService.ResolveRequest(
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getTipoJustica(),
                        safe.getMateriaPrincipal(),
                        safe.getNaturezaJuridica(),
                        effectiveDraftMarkdown,
                        safe.getTextoPeticaoLivre(),
                        safe.getTextoFatosResumido(),
                        safe.getDocumentosAnexados(),
                        safe.tutelaUrgenciaResolvida(),
                        Boolean.TRUE.equals(safe.getCasoUrgente()),
                        safe.prepararPacoteProtocoloResolvido(),
                        representacao != null && representacao.exigeProcuracaoFormal(),
                        sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()
                )
        );
        PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier = procedureSpecificVerifierService.analyze(
                new PeticionamentoProcedureSpecificVerifierService.ResolveRequest(
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getAssuntoTpu(),
                        safe.getMateriaPrincipal(),
                        safe.getNaturezaJuridica(),
                        safe.getTipoJustica(),
                        firstNonBlank(effectiveDraftMarkdown, safe.getTextoPeticaoLivre(), safe.getTextoFatosResumido()),
                        safe.getFatos(),
                        safe.getPedidos(),
                        safe.getDocumentosAnexados(),
                        safe.tutelaUrgenciaResolvida(),
                        Boolean.TRUE.equals(safe.getCasoUrgente()),
                        safe.prepararPacoteProtocoloResolvido(),
                        representacao == null || representacao.regularidadeSuficiente(),
                        sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial(),
                        usuario.getTipoUsuario()
                )
        );

        PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition = multimidiaComposerService.compose(
                new PeticionamentoMultimidiaComposerService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        safe.getDocumentosPessoais(),
                        safe.getDocumentosRepresentacao(),
                        safe.getDocumentosAnexados()
                )
        );
        PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity = mediaSecurityPipelineService.assess(
                new PeticionamentoMediaSecurityPipelineService.ResolveRequest(
                        safe.getMidiaInline(),
                        usuario.getTipoUsuario(),
                        sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()
                )
        );
        PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel = threatSentinelService.plan(
                new PeticionamentoThreatSentinelService.ResolveRequest(
                        sessionKey,
                        safe.getMidiaInline(),
                        sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()
                )
        );

        PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield = mediaStorageShieldService.plan(
                new PeticionamentoMediaStorageShieldService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        safe.getDocumentosPessoais(),
                        safe.getDocumentosRepresentacao(),
                        safe.getDocumentosAnexados(),
                        safe.prepararPacoteProtocoloResolvido()
                )
        );
        PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence = periciaEvidenceIntelligenceService.analyze(
                new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()
                )
        );
        PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate = mediaPublicationGateService.resolve(
                new PeticionamentoMediaPublicationGateService.ResolveRequest(
                        safe.getMidiaInline(),
                        multimediaComposition,
                        mediaSecurity,
                        mediaStorageShield,
                        periciaEvidence,
                        safe.prepararPacoteProtocoloResolvido()
                )
        );
        Map<String, Object> uploadGovernance = uploadCapacityGovernanceService.governanceSummary();

        PeticionamentoGuardrailResponse guardrails = peticionamentoPreventiveGuardrailService.analyze(representacao, sigiloDecision, manualDraft, assistiveAnalysis, protocolPackage != null);

        PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope = protocolEnvelopeHardeningService.harden(
                new PeticionamentoProtocolEnvelopeHardeningService.ResolveRequest(
                        sessionKey,
                        safe.getProcessoId(),
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getTipoJustica(),
                        usuario.getTipoUsuario(),
                        representacao == null ? null : representacao.resolvedInstrument(),
                        sigiloDecision == null || sigiloDecision.nivel() == null ? null : sigiloDecision.nivel().name(),
                        representacao == null || representacao.regularidadeSuficiente(),
                        safe.prepararPacoteProtocoloResolvido(),
                        assistiveAnalysis != null && assistiveAnalysis.isProntaParaProtocolo(),
                        hardened.fingerprint(),
                        safe.getDocumentosAnexados(),
                        batchReading.profile(),
                        batchReading.blockingIssues(),
                        procedureVerifier.profile(),
                        procedureVerifier.resolvedTrack(),
                        procedureVerifier.blockers(),
                        protocolPackage == null ? null : protocolPackage.getProtocolPackage()
                )
        );

        if (protocolPackage != null) {
            protocolPackage.setBatchReading(batchReading.workspace());
            protocolPackage.setAiVerifier(procedureVerifier.workspace());
            protocolPackage.setStrategicEnvelope(protocolEnvelope.strategicEnvelope());
            protocolPackage.setFinalGates(protocolEnvelope.finalGates());
            protocolPackage.setMultimediaComposition(multimediaComposition.protocolSection());
            protocolPackage.setMediaSecurityStatus(mediaSecurity.protocolSection());
            protocolPackage.setThreatSentinel(threatSentinel.workspace());
            protocolPackage.setMediaStorageShield(mediaStorageShield.protocolSection());
            protocolPackage.setUploadGovernance(uploadGovernance);
            protocolPackage.setPericiaEvidence(periciaEvidence.protocolSection());
            protocolPackage.setMediaPublicationGate(mediaPublicationGate.protocolSection());
        }

        PeticionamentoAutomacaoResponse automacao = buildAutomacaoResponse(
                safe,
                enderecoAutor,
                enderecoReu,
                representacao,
                sigiloDecision,
                manualDraft,
                assistiveAnalysis,
                guardrails,
                intake,
                hardened,
                batchReading,
                procedureVerifier,
                protocolEnvelope,
                multimediaComposition,
                mediaSecurity,
                threatSentinel,
                mediaStorageShield,
                periciaEvidence,
                mediaPublicationGate,
                uploadGovernance
        );
        return PeticionamentoSessaoResponse.builder()
                .modoSolicitado(normalizeNullable(safe.getModo()))
                .modoResolvido(modo.name())
                .papelArquitetural(usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().papelArquitetural())
                .status(resolveStatus(modo, manualDraft, assistiveAnalysis, guardrails))
                .sessionKey(sessionKey)
                .automacao(automacao)
                .guardrails(guardrails)
                .manualDraft(manualDraft)
                .assistiveAnalysis(assistiveAnalysis)
                .protocolPackage(protocolPackage)
                .passosSugeridos(buildPassos(modo, automacao, manualDraft, assistiveAnalysis, protocolPackage, guardrails, batchReading, procedureVerifier, protocolEnvelope, multimediaComposition, mediaSecurity, mediaStorageShield, periciaEvidence, mediaPublicationGate))
                .workspace(buildWorkspace(safe, usuario.getTipoUsuario(), modo, representacao, sigiloDecision, manualDraft, assistiveAnalysis, protocolPackage, guardrails, intake, hardened, batchReading, procedureVerifier, protocolEnvelope, multimediaComposition, mediaSecurity, threatSentinel, mediaStorageShield, periciaEvidence, mediaPublicationGate, uploadGovernance))
                .build();
    }

    public PeticionamentoEnderecoResponse resolverEndereco(PeticionamentoEnderecoRequest request) {
        return enderecoAutomationService.resolve(request, true);
    }

    private void ensureWorkspaceAccessIfExistingProcess(PeticionamentoSessaoRequest request) {
        if (request == null || request.getProcessoId() == null || officeProcessWorkspaceScopeService == null || !officeProcessWorkspaceScopeService.supportsCurrentUser()) {
            return;
        }
        HttpServletRequest httpRequest = currentHttpRequest();
        officeProcessWorkspaceScopeService.requireAccess(request.getProcessoId(), OfficeActionType.PETICIONAR, httpRequest);
    }

    private void appendOfficeProcessAccess(Map<String, Object> workspace, Long processoId) {
        if (workspace == null || processoId == null || officeProcessWorkspaceScopeService == null || !officeProcessWorkspaceScopeService.supportsCurrentUser()) {
            return;
        }
        var access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.PETICIONAR, currentHttpRequest());
        LinkedHashMap<String, Object> officeAccess = new LinkedHashMap<>();
        officeAccess.put("allowed", access.allowed());
        officeAccess.put("visibleInWorkspace", access.visibleInWorkspace());
        officeAccess.put("queueRequired", access.queueRequired());
        officeAccess.put("effectiveSignerUserId", access.effectiveSignerUserId());
        officeAccess.put("effectiveSignerNome", access.effectiveSignerNome());
        officeAccess.put("blockers", access.blockers());
        officeAccess.put("warnings", access.warnings());
        workspace.put("officeProcessAccess", officeAccess);
    }

    private HttpServletRequest currentHttpRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }

    private Usuario requirePeticionante() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico());
        if (!permitido) {
            throw new AccessDeniedPjbException("A sessão de peticionamento é exclusiva para advocacia, defensoria, procuradoria e Ministério Público.");
        }
        return usuario;
    }

    private PeticionamentoAutomacaoResponse buildAutomacaoResponse(PeticionamentoSessaoRequest request,
                                                                   PeticionamentoEnderecoResponse enderecoAutor,
                                                                   PeticionamentoEnderecoResponse enderecoReu,
                                                                   RepresentacaoProcessualPolicyResponse representacao,
                                                                   SigiloService.SigiloDecision sigiloDecision,
                                                                   LaianePeticaoInicialDraftService.DraftView manualDraft,
                                                                   LaianePeticaoAssistResponse assistiveAnalysis,
                                                                   PeticionamentoGuardrailResponse guardrails,
                                                                   PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake,
                                                                   PeticionamentoPayloadHardeningService.HardenedPayload hardened,
                                                                   PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading,
                                                                   PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier,
                                                                   PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope,
                                                                   PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
                                                                   PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
                                                                   PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel,
                                                                   PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                                                   PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                                                   PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate,
                                                                   Map<String, Object> uploadGovernance) {
        ArrayList<String> automacoesAplicadas = new ArrayList<>();
        ArrayList<String> pendencias = new ArrayList<>();
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();

        if (enderecoAutor != null && enderecoAutor.isAutoPreenchido()) {
            automacoesAplicadas.add("CEP do autor convertido em logradouro, bairro, cidade e UF.");
        }
        if (enderecoReu != null && enderecoReu.isAutoPreenchido()) {
            automacoesAplicadas.add("CEP do réu convertido em logradouro, bairro, cidade e UF.");
        }
        if (representacao != null) {
            automacoesAplicadas.add("Representação processual avaliada com base no perfil do ator e no ramo processual.");
            if (!representacao.regularidadeSuficiente()) {
                pendencias.add("Regularizar instrumento de representação antes do protocolo sensível.");
            }
            if (representacao.exigeProcuracaoFormal()) {
                pendencias.add("Anexar instrumento formal de representação ou comprovar dispensa institucional.");
            }
            if (representacao.exigeTermoOuAtaAudiencia()) {
                pendencias.add("Vincular termo ou ata de audiência exigidos para a moldura de representação escolhida.");
            }
        }
        if (manualDraft != null && manualDraft.readinessScore() != null && manualDraft.readinessScore() < 70) {
            pendencias.add("Completar fatos, pedidos, fundamentos ou provas para elevar a prontidão do peticionamento manual.");
        }
        if (assistiveAnalysis != null && !assistiveAnalysis.isProntaParaProtocolo()) {
            pendencias.add("Resolver bloqueios apontados no preflight assistido antes do protocolo final.");
        }
        if (guardrails != null) {
            pendencias.addAll(guardrails.bloqueios());
            pendencias.addAll(guardrails.alertas());
            put(envelope, "guardrailStatus", guardrails.status());
            put(envelope, "guardrailNextAction", guardrails.nextAction());
            put(envelope, "guardrailBlocking", guardrails.bloqueante());
        }
        if (hardened != null) {
            automacoesAplicadas.add("Payload de peticionamento canonizado com normalização, limites estruturais e fingerprint determinístico.");
            put(envelope, "payloadFingerprint", hardened.fingerprint());
            put(envelope, "payloadHardeningProfile", hardened.metadata().get("profile"));
            if (!hardened.diagnostics().isEmpty()) {
                put(envelope, "payloadHardeningDiagnostics", hardened.diagnostics());
            }
        }
        if (intake != null) {
            automacoesAplicadas.addAll(intake.automacoes());
            pendencias.addAll(intake.pendencias());
            if (intake.envelope() != null && !intake.envelope().isEmpty()) {
                intake.envelope().forEach((key, value) -> put(envelope, key, value));
            }
        }
        if (sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()) {
            automacoesAplicadas.add("Sigilo sensível detectado com necessidade de trilha reforçada de leitura e protocolo.");
        }
        if (batchReading != null) {
            automacoesAplicadas.add("Leitura em lote documental classificada por prioridade, categoria material e sequência obrigatória antes do protocolo.");
            pendencias.addAll(batchReading.blockingIssues());
            pendencias.addAll(batchReading.alerts());
            put(envelope, "batchReadingProfile", batchReading.profile());
            put(envelope, "batchReadiness", batchReading.metadata().get("readinessScore"));
        }
        if (procedureVerifier != null) {
            automacoesAplicadas.add("Verificador jurídico descido para a subespécie procedimental do caso concreto.");
            pendencias.addAll(procedureVerifier.blockers());
            pendencias.addAll(procedureVerifier.alerts());
            put(envelope, "procedureVerifierProfile", procedureVerifier.profile());
            put(envelope, "procedureTrack", procedureVerifier.resolvedTrack());
        }
        if (protocolEnvelope != null) {
            automacoesAplicadas.add("Envelope final de assinatura e protocolo endurecido com hash determinístico, gates e trilha de auditoria.");
            put(envelope, "protocolEnvelopeHash", protocolEnvelope.deterministicHash());
            put(envelope, "protocolEnvelopeBlocking", protocolEnvelope.blocking());
            put(envelope, "finalGates", protocolEnvelope.finalGates());
        }
        if (multimediaComposition != null && multimediaComposition.enabled()) {
            automacoesAplicadas.add("Petição multimídia estruturada por blocos inline com seções próprias para provas documentais, documentos pessoais e representação.");
            pendencias.addAll(multimediaComposition.blockers());
            pendencias.addAll(multimediaComposition.alerts());
            put(envelope, "multimediaProfile", multimediaComposition.profile());
            put(envelope, "multimediaProtocolSafe", multimediaComposition.protocolSafe());
        }
        if (mediaSecurity != null) {
            automacoesAplicadas.add("Pipeline multimídia blindado em três reforços: validação estrutural, antivírus/hunting/canonicalização e governança de conteúdo sensível.");
            pendencias.addAll(mediaSecurity.blockers());
            pendencias.addAll(mediaSecurity.alerts());
            put(envelope, "mediaSecurityProfile", mediaSecurity.profile());
            put(envelope, "mediaSecurityBlocking", mediaSecurity.blocking());
        }
        if (threatSentinel != null && !threatSentinel.watchSignals().isEmpty()) {
            automacoesAplicadas.add("Sentinela de ameaças planejado com vigilância contínua para uploads, quarentena, storage sensível e anomalias de execução.");
            put(envelope, "threatSentinelProfile", threatSentinel.profile());
        }
        if (mediaStorageShield != null) {
            automacoesAplicadas.add("Plano de storage blindado para petição multimídia com upload direto, object storage, metadados no banco e bloco pós-petição de anexos.");
            pendencias.addAll(mediaStorageShield.blockers());
            pendencias.addAll(mediaStorageShield.alerts());
            put(envelope, "mediaStorageProfile", mediaStorageShield.profile());
            put(envelope, "mediaStorageBlocking", mediaStorageShield.blocking());
        }
        if (periciaEvidence != null) {
            automacoesAplicadas.add("Inteligência pericial ativada para sugerir especialidades, cadeia de custódia, transcrição e trilha audiovisual conforme a prova inserida.");
            pendencias.addAll(periciaEvidence.alerts());
            put(envelope, "periciaEvidenceProfile", periciaEvidence.profile());
            put(envelope, "periciaEvidence", periciaEvidence.workspace());
        }
        if (mediaPublicationGate != null) {
            automacoesAplicadas.add("Status por arquivo ativado no workspace para acompanhar upload, quarentena, canonicalização, trilha pericial e liberação de publicação.");
            put(envelope, "mediaPublicationProfile", mediaPublicationGate.profile());
            put(envelope, "mediaPublicationBlocking", mediaPublicationGate.blocking());
            put(envelope, "mediaPublicationPending", mediaPublicationGate.pendingPublication());
            put(envelope, "mediaPublicationGates", mediaPublicationGate.publicationGates());
        }
        if (uploadGovernance != null && !uploadGovernance.isEmpty()) {
            automacoesAplicadas.add("Governança de upload aplicada com limites prudenciais por arquivo, inline, lote e trilha de processamento pesada.");
            put(envelope, "uploadGovernance", uploadGovernance);
        }
        put(envelope, "tipoJustica", request.getTipoJustica());
        put(envelope, "ramoDireito", request.getRamoDireito());
        put(envelope, "ritoProcessual", request.getRitoProcessual());
        put(envelope, "classeProcessual", request.getClasseProcessual());
        put(envelope, "tituloCaso", request.getTituloCaso());
        put(envelope, "sigiloScore", sigiloDecision == null ? null : sigiloDecision.score());
        put(envelope, "requestedInstrument", representacao == null ? null : representacao.requestedInstrument());
        put(envelope, "resolvedInstrument", representacao == null ? null : representacao.resolvedInstrument());
        put(envelope, "manualReadiness", manualDraft == null ? null : manualDraft.readinessScore());
        put(envelope, "assistReadiness", assistiveAnalysis == null ? null : assistiveAnalysis.getReadinessScore());

        put(envelope, "modoOperacao", request.modoResolvido().name());
        put(envelope, "readinessGeral", resolveReadinessGeral(manualDraft, assistiveAnalysis));
        put(envelope, "protocoloAssistidoDisponivel", assistiveAnalysis != null && assistiveAnalysis.isProntaParaProtocolo());
        put(envelope, "representacaoRegular", representacao == null || representacao.regularidadeSuficiente());
        put(envelope, "enderecoAutorValido", enderecoAutor == null || enderecoAutor.isValido());
        put(envelope, "enderecoReuValido", enderecoReu == null || enderecoReu.isValido());

        return PeticionamentoAutomacaoResponse.builder()
                .enderecoAutor(enderecoAutor)
                .enderecoReu(enderecoReu)
                .representacao(representacao)
                .nivelSigiloSugerido(sigiloDecision == null || sigiloDecision.nivel() == null ? null : sigiloDecision.nivel().name())
                .sigiloRecomendacoes(safeStringList(sigiloDecision == null ? null : sigiloDecision.recomendacoes()))
                .automacoesAplicadas(safeStringList(automacoesAplicadas))
                .pendenciasDeterministicas(safeStringList(pendencias))
                .envelope(Map.copyOf(envelope))
                .build();
    }

    private List<String> buildPassos(PeticionamentoModo modo,
                                     PeticionamentoAutomacaoResponse automacao,
                                     LaianePeticaoInicialDraftService.DraftView manualDraft,
                                     LaianePeticaoAssistResponse assistiveAnalysis,
                                     LaianePeticaoProtocolPackageResponse protocolPackage,
                                     PeticionamentoGuardrailResponse guardrails,
                                     PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading,
                                     PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier,
                                     PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope,
                                     PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
                                     PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
                                     PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                     PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                     PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate) {
        ArrayList<String> passos = new ArrayList<>();
        if (modo.includeManual()) {
            passos.add("Preencher a inicial por blocos: partes, local do fato, competência, narrativa, fundamentos, pedidos, provas e fechamento.");
            passos.add("Revisar a minuta guiada, checklist documental e coerência dos pedidos antes da assinatura.");
        }
        if (modo.includeAssistive()) {
            passos.add("Carregar a peça pronta ou o texto-base para leitura assistida, extração de blocos e autopreenchimento do protocolo.");
            passos.add("Usar a Laiane para revisar coerência jurídica, competência, triagem, precedentes e risco protocolar.");
        }
        if (automacao != null && automacao.getRepresentacao() != null && automacao.getRepresentacao().exigeProcuracaoFormal()) {
            passos.add("Conferir procuração, poderes especiais e documentos de representação antes do protocolo.");
        }
        if (automacao != null && automacao.getNivelSigiloSugerido() != null && !"PUBLICO".equalsIgnoreCase(automacao.getNivelSigiloSugerido())) {
            passos.add("Confirmar o nível de sigilo sugerido e preparar a peça para trilha restrita de leitura e protocolo.");
        }
        if (manualDraft != null && manualDraft.readinessScore() != null && manualDraft.readinessScore() < 70) {
            passos.add("Completar fatos, fundamentos, pedidos e provas para elevar a prontidão do fluxo manual.");
        }
        if (assistiveAnalysis != null && !assistiveAnalysis.isProntaParaProtocolo()) {
            passos.add("Resolver os bloqueios do preflight assistido antes de montar o pacote definitivo de protocolo.");
        }
        if (guardrails != null) {
            passos.addAll(guardrails.checklist());
        }
        if (batchReading != null && !batchReading.mandatorySequence().isEmpty()) {
            passos.add("Executar a leitura documental em lote seguindo a sequência obrigatória: " + String.join(" | ", batchReading.mandatorySequence()) + ".");
        }
        if (procedureVerifier != null) {
            passos.add("Revisar o verificador jurídico da subespécie " + procedureVerifier.resolvedLabel() + " antes da assinatura final.");
        }
        if (protocolEnvelope != null && protocolEnvelope.blocking()) {
            passos.add("Resolver os gates finais do envelope estratégico antes de liberar assinatura e submissão.");
        }
        if (multimediaComposition != null && multimediaComposition.enabled()) {
            passos.add("Inserir mídias inline somente nos pontos narrativos essenciais e manter provas documentais, documentos pessoais e representação em seções separadas.");
        }
        if (mediaSecurity != null && mediaSecurity.blocking()) {
            passos.add("Liberar a trilha multimídia apenas após a validação tripla de quarentena, hunting e governança de conteúdo sensível.");
        }
        if (mediaStorageShield != null) {
            passos.add("Manter imagem, áudio e vídeo apenas como mídia inline narrativa; documentos, representação e peças de apoio devem seguir no bloco pós-petição de anexos.");
            if (mediaStorageShield.blocking()) {
                passos.add("Regularizar o plano de storage da peça multimídia antes do protocolo, evitando blob relacional, base64 inline e excesso de carga na ingestão.");
            }
        }
        if (periciaEvidence != null) {
            passos.add("Revisar a trilha pericial sugerida para confirmar especialidade, cadeia de custódia, transcrição e recortes audiovisuais antes do protocolo.");
        }
        if (mediaPublicationGate != null) {
            passos.add("Acompanhar por arquivo os estados de upload, quarentena, canonicalização e snapshot protocolar antes de liberar a publicação controlada da mídia.");
            if (mediaPublicationGate.blocking()) {
                passos.add("Regularizar os arquivos bloqueados no workspace antes da assinatura ou do protocolo sensível.");
            } else if (mediaPublicationGate.pendingPublication()) {
                passos.add("Aguardar o triplo ok dos arquivos pendentes antes de consolidar o snapshot protocolar multimídia.");
            }
        }
        if (protocolPackage != null) {
            passos.add("Pacote de protocolo assistido pronto para seguir para assinatura, governança do escritório e conector judicial.");
        }
        return List.copyOf(passos);
    }

    private Map<String, Object> buildWorkspace(PeticionamentoSessaoRequest request,
                                               TipoUsuario tipoUsuario,
                                               PeticionamentoModo modo,
                                               RepresentacaoProcessualPolicyResponse representacao,
                                               SigiloService.SigiloDecision sigiloDecision,
                                               LaianePeticaoInicialDraftService.DraftView manualDraft,
                                               LaianePeticaoAssistResponse assistiveAnalysis,
                                               LaianePeticaoProtocolPackageResponse protocolPackage,
                                               PeticionamentoGuardrailResponse guardrails,
                                               PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake,
                                               PeticionamentoPayloadHardeningService.HardenedPayload hardened,
                                               PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading,
                                               PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier,
                                               PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope,
                                               PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
                                               PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
                                               PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel,
                                               PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                               PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                               PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate,
                                               Map<String, Object> uploadGovernance) {
        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("modo", modo.name());
        workspace.put("manualDisponivel", modo.includeManual());
        workspace.put("assistidoDisponivel", modo.includeAssistive());
        workspace.put("nextAction", resolveUnifiedNextAction(modo, representacao, manualDraft, assistiveAnalysis, protocolPackage, guardrails, batchReading, procedureVerifier, protocolEnvelope, mediaSecurity, mediaStorageShield, periciaEvidence, mediaPublicationGate));
        workspace.put("capabilities", buildCapabilities(modo, tipoUsuario, representacao, sigiloDecision, protocolPackage, multimediaComposition, mediaStorageShield, periciaEvidence, mediaPublicationGate));
        workspace.put("manualEndpoints", List.of(
                "/api/v1/peticionamento/inicial/rascunhos/estruturar",
                "/api/v1/peticionamento/inicial/rascunhos/salvar",
                "/api/v1/peticionamento/inicial/rascunhos/minhas"
        ));
        workspace.put("assistiveEndpoints", List.of(
                "/api/v1/laiane/lawyer/peticao/preflight",
                "/api/v1/laiane/lawyer/peticao/draft-preflight",
                "/api/v1/laiane/lawyer/peticao/protocol-package"
        ));
        workspace.put("unifiedEndpoints", List.of(
                "/api/v1/peticionamento/inicial/sessao",
                "/api/v1/peticionamento/enderecos/cep",
                "/api/v1/peticionamento/inicial/rascunhos/estruturar",
                "/api/v1/peticionamento/inicial/rascunhos/salvar",
                "/api/v1/peticionamento/inicial/rascunhos/minhas"
        ));
        if (tipoUsuario != null) {
            workspace.put("perfilPeticionante", tipoUsuario.name());
            workspace.put("papelArquitetural", tipoUsuario.papelArquitetural());
            if (tipoUsuario.isAdvocacia() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isProcuradoria()) {
                workspace.put("legacyManualEndpoints", List.of(
                        "/api/v1/advogado/laiane/peticao-inicial/estruturar",
                        "/api/v1/advogado/laiane/peticao-inicial/salvar",
                        "/api/v1/advogado/laiane/peticao-inicial/minhas"
                ));
            }
        }
        put(workspace, "processoId", request.getProcessoId());
        appendOfficeProcessAccess(workspace, request.getProcessoId());
        put(workspace, "resolvedInstrument", representacao == null ? null : representacao.resolvedInstrument());
        put(workspace, "sigilo", sigiloDecision == null || sigiloDecision.nivel() == null ? null : sigiloDecision.nivel().name());
        put(workspace, "manualReadiness", manualDraft == null ? null : manualDraft.readinessScore());
        put(workspace, "assistReadiness", assistiveAnalysis == null ? null : assistiveAnalysis.getReadinessScore());
        put(workspace, "protocolPackageReady", protocolPackage != null);
        put(workspace, "guardrails", guardrails == null ? null : guardrails.toMap());
        put(workspace, "aiDocumentBatchReading", batchReading == null ? null : batchReading.workspace());
        put(workspace, "aiProcedureVerifier", procedureVerifier == null ? null : procedureVerifier.workspace());
        put(workspace, "aiProtocolEnvelope", protocolEnvelope == null ? null : protocolEnvelope.strategicEnvelope());
        put(workspace, "multimediaComposition", multimediaComposition == null ? null : multimediaComposition.workspace());
        put(workspace, "mediaSecurityStatus", mediaSecurity == null ? null : mediaSecurity.workspace());
        put(workspace, "threatSentinel", threatSentinel == null ? null : threatSentinel.workspace());
        put(workspace, "mediaStorageShield", mediaStorageShield == null ? null : mediaStorageShield.workspace());
        put(workspace, "periciaEvidence", periciaEvidence == null ? null : periciaEvidence.workspace());
        put(workspace, "mediaPublicationStatus", mediaPublicationGate == null ? null : mediaPublicationGate.workspace());
        put(workspace, "uploadGovernance", uploadGovernance == null || uploadGovernance.isEmpty() ? null : uploadGovernance);
        if (hardened != null) {
            LinkedHashMap<String, Object> payloadHardening = new LinkedHashMap<>();
            put(payloadHardening, "fingerprint", hardened.fingerprint());
            if (hardened.metadata() != null && !hardened.metadata().isEmpty()) {
                hardened.metadata().forEach((key, value) -> put(payloadHardening, key, value));
            }
            if (!hardened.diagnostics().isEmpty()) {
                payloadHardening.put("diagnostics", hardened.diagnostics());
            }
            put(workspace, "payloadHardening", payloadHardening);
        }
        if (intake != null) {
            put(workspace, "intake", intake.workspace());
            LinkedHashMap<String, Object> resolvedDraftSeed = new LinkedHashMap<>();
            put(resolvedDraftSeed, "tituloCaso", intake.resolvedDraftRequest().tituloCaso());
            put(resolvedDraftSeed, "ramoDireito", intake.resolvedDraftRequest().ramoDireito());
            put(resolvedDraftSeed, "ritoProcessual", intake.resolvedDraftRequest().ritoProcessual());
            put(resolvedDraftSeed, "classeProcessual", intake.resolvedDraftRequest().classeProcessual());
            put(resolvedDraftSeed, "cidadeFato", intake.resolvedDraftRequest().cidadeFato());
            put(resolvedDraftSeed, "ufFato", intake.resolvedDraftRequest().ufFato());
            put(resolvedDraftSeed, "cidadeProtocolo", intake.resolvedDraftRequest().cidadeProtocolo());
            put(resolvedDraftSeed, "ufProtocolo", intake.resolvedDraftRequest().ufProtocolo());
            put(resolvedDraftSeed, "naturezaJuridica", intake.resolvedDraftRequest().naturezaJuridica());
            put(workspace, "resolvedDraftSeed", resolvedDraftSeed);
        }
        PeticionamentoJurisprudenciaWorkspaceService.WorkspaceProjection jurisprudencia = jurisprudenciaWorkspaceService.resolve(request, intake);
        put(workspace, "jurisprudenciaSugerida", jurisprudencia.toMap());
        return Map.copyOf(workspace);
    }

    private void enrichAssistRequest(LaianePeticaoAssistRequest request,
                                    PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake) {
        if (request == null || intake == null) {
            return;
        }
        var resolved = intake.resolvedDraftRequest();
        if (!hasText(request.getRamoDireito())) {
            request.setRamoDireito(resolved.ramoDireito());
        }
        if (!hasText(request.getClasseTpu())) {
            request.setClasseTpu(resolved.classeProcessual());
        }
        if (!hasText(request.getRitoSugerido())) {
            request.setRitoSugerido(resolved.ritoProcessual());
        }
        if (!hasText(request.getProtocolTitle())) {
            request.setProtocolTitle(resolved.tituloCaso());
        }
        if (!hasText(request.getTextoFatosResumido()) && resolved.fatos() != null && !resolved.fatos().isEmpty()) {
            request.setTextoFatosResumido(String.join(" ", resolved.fatos()));
        }
        if (request.getValorCausa() == null) {
            request.setValorCausa(resolved.valorCausa());
        }
        LinkedHashMap<String, Object> ctx = request.getCtx() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getCtx());
        put(ctx, "cidadeFato", resolved.cidadeFato());
        put(ctx, "ufFato", resolved.ufFato());
        put(ctx, "cidadeProtocolo", resolved.cidadeProtocolo());
        put(ctx, "ufProtocolo", resolved.ufProtocolo());
        put(ctx, "naturezaJuridica", resolved.naturezaJuridica());
        request.setCtx(ctx);
    }

    private static String tribunalFromCtx(Map<String, Object> ctx) {
        if (ctx == null || ctx.isEmpty()) {
            return null;
        }
        Object tribunal = ctx.get("tribunal");
        if (tribunal == null) {
            tribunal = ctx.get("tribunalCodigo");
        }
        if (tribunal == null) {
            tribunal = ctx.get("orgaoJulgador");
        }
        if (tribunal == null) {
            return null;
        }
        String value = tribunal.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private static String buildCorpus(PeticionamentoSessaoRequest request,
                                      PeticionamentoEnderecoResponse enderecoAutor,
                                      PeticionamentoEnderecoResponse enderecoReu) {
        StringBuilder sb = new StringBuilder();
        append(sb, request.getTituloCaso());
        append(sb, request.getRamoDireito());
        append(sb, request.getRitoProcessual());
        append(sb, request.getClasseProcessual());
        append(sb, request.getNaturezaJuridica());
        append(sb, request.getCidadeFato());
        append(sb, request.getUfFato());
        append(sb, request.getCidadeProtocolo());
        append(sb, request.getUfProtocolo());
        append(sb, request.getTextoFatosResumido());
        append(sb, request.getTextoPeticaoLivre());
        appendAll(sb, request.getFatos());
        appendAll(sb, request.getFundamentosJuridicos());
        appendAll(sb, request.getPedidos());
        appendAll(sb, request.getProvasIndicadas());
        appendAll(sb, request.getProvasDocumentais());
        appendAll(sb, request.getDocumentosPessoais());
        appendAll(sb, request.getDocumentosRepresentacao());
        appendMedia(sb, request.getMidiaInline());
        append(sb, enderecoAutor == null ? null : enderecoAutor.getLogradouro());
        append(sb, enderecoReu == null ? null : enderecoReu.getLogradouro());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String value) {
        if (!hasText(value)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private static void appendAll(StringBuilder sb, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            append(sb, value);
        }
    }

    private static void appendMedia(StringBuilder sb, List<PeticionamentoMediaBlocoRequest> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (PeticionamentoMediaBlocoRequest value : values) {
            if (value == null) {
                continue;
            }
            append(sb, value.getTitulo());
            append(sb, value.getDescricao());
            append(sb, value.tipoResolvido());
            append(sb, value.categoriaResolvida());
        }
    }

    private static String buildSessionKey(Usuario usuario, PeticionamentoSessaoRequest request, PeticionamentoModo modo, String payloadFingerprint) {
        String actor = usuario.getId() == null ? String.valueOf(usuario.hashCode()) : usuario.getId().toString();
        String base = actor + "|" + modo.name() + "|" + normalizeNullable(request.getTituloCaso()) + "|" + normalizeNullable(request.getClasseProcessual()) + "|" + normalizeNullable(payloadFingerprint);
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String resolveStatus(PeticionamentoModo modo,
                                        LaianePeticaoInicialDraftService.DraftView manualDraft,
                                        LaianePeticaoAssistResponse assistiveAnalysis,
                                        PeticionamentoGuardrailResponse guardrails) {
        if (guardrails != null && guardrails.bloqueante()) {
            return switch (modo) {
                case MANUAL_GUIADO -> "MANUAL_BLOQUEADO";
                case ASSISTIDO_LAIANE -> "ASSISTIDO_BLOQUEADO";
                case HIBRIDO -> "HIBRIDO_BLOQUEADO";
            };
        }
        if (modo == PeticionamentoModo.MANUAL_GUIADO) {
            return manualDraft == null ? "PENDENTE" : "MANUAL_PRONTO";
        }
        if (modo == PeticionamentoModo.ASSISTIDO_LAIANE) {
            return assistiveAnalysis == null ? "PENDENTE" : assistiveAnalysis.isProntaParaProtocolo() ? "ASSISTIDO_PRONTO" : "ASSISTIDO_EM_AJUSTE";
        }
        boolean manualPronto = manualDraft != null;
        boolean assistPronto = assistiveAnalysis != null && assistiveAnalysis.isProntaParaProtocolo();
        if (manualPronto && assistPronto) {
            return "HIBRIDO_PRONTO";
        }
        if (manualPronto || assistiveAnalysis != null) {
            return "HIBRIDO_EM_PROGRESSO";
        }
        return "PENDENTE";
    }

    private static PeticionamentoEnderecoRequest defaultEndereco(PeticionamentoEnderecoRequest request) {
        return request == null ? new PeticionamentoEnderecoRequest() : request;
    }

    private static List<String> buildCapabilities(PeticionamentoModo modo,
                                                  TipoUsuario tipoUsuario,
                                                  RepresentacaoProcessualPolicyResponse representacao,
                                                  SigiloService.SigiloDecision sigiloDecision,
                                                  LaianePeticaoProtocolPackageResponse protocolPackage,
                                                  PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
                                                  PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                                  PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                                  PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate) {
        ArrayList<String> capacidades = new ArrayList<>();
        if (modo.includeManual()) {
            capacidades.add("PETICIONAMENTO_MANUAL_GUIADO");
            capacidades.add("EDITOR_NATIVO_BLOCOS");
        }
        if (modo.includeAssistive()) {
            capacidades.add("REVISAO_ASSISTIDA_LAIANE");
            capacidades.add("UPLOAD_INTELIGENTE_LEITURA_ASSISTIDA");
        }
        if (tipoUsuario != null && (tipoUsuario.isDefensoriaPublica() || tipoUsuario.isProcuradoria() || tipoUsuario.isMinisterioPublico())) {
            capacidades.add("ATUACAO_INSTITUCIONAL");
        }
        if (representacao != null && representacao.exigeProcuracaoFormal()) {
            capacidades.add("VALIDACAO_MANDATO");
        }
        if (sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial()) {
            capacidades.add("TRILHA_RESTRITA_SIGILO");
        }
        if (protocolPackage != null) {
            capacidades.add("PACOTE_PROTOCOLO_ASSISTIDO");
        }
        if (multimediaComposition != null && multimediaComposition.enabled()) {
            capacidades.add("PETICAO_MULTIMIDIA_SEGURA");
            capacidades.add("MODO_BLUR_CONTEUDO_SENSIVEL");
        }
        if (mediaStorageShield != null) {
            capacidades.add("STORAGE_EXTERNO_BLOB_ZERO");
            capacidades.add("BLOCO_POS_PETICAO_ANEXOS");
        }
        if (periciaEvidence != null) {
            capacidades.add("TRIAGEM_PERICIAL_MULTIMIDIA");
        }
        if (mediaPublicationGate != null) {
            capacidades.add("WORKSPACE_STATUS_MIDIA");
            capacidades.add("PUBLICACAO_CONTROLADA_DERIVADOS");
        }
        return safeStringList(capacidades);
    }


    private static String resolveUnifiedNextAction(PeticionamentoModo modo,
                                                   RepresentacaoProcessualPolicyResponse representacao,
                                                   LaianePeticaoInicialDraftService.DraftView manualDraft,
                                                   LaianePeticaoAssistResponse assistiveAnalysis,
                                                   LaianePeticaoProtocolPackageResponse protocolPackage,
                                                   PeticionamentoGuardrailResponse guardrails,
                                                   PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading,
                                                   PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier,
                                                   PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope,
                                                   PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
                                                   PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                                   PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                                   PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate) {
        if (representacao != null && !representacao.regularidadeSuficiente()) {
            return "REGULARIZAR_REPRESENTACAO";
        }
        if (batchReading != null && batchReading.blocking()) {
            return "ORGANIZAR_LOTE_DOCUMENTAL";
        }
        if (procedureVerifier != null && procedureVerifier.blocking()) {
            return "REVISAR_VERIFICADOR_SUBESPECIE";
        }
        if (mediaStorageShield != null && mediaStorageShield.blocking()) {
            return "REORGANIZAR_ANEXOS_E_STORAGE";
        }
        if (mediaSecurity != null && mediaSecurity.blocking()) {
            return "VALIDAR_MIDIA_EM_QUARENTENA";
        }
        if (periciaEvidence != null && !periciaEvidence.alerts().isEmpty()) {
            return "REVISAR_TRILHA_PERICIAL";
        }
        if (mediaPublicationGate != null && mediaPublicationGate.blocking()) {
            return "REGULARIZAR_PUBLICACAO_DE_MIDIA";
        }
        if (mediaPublicationGate != null && mediaPublicationGate.pendingPublication()) {
            return "ACOMPANHAR_QUARENTENA_E_PUBLICACAO";
        }
        if (protocolEnvelope != null && protocolEnvelope.blocking()) {
            return "LIBERAR_GATES_PROTOCOLO";
        }
        if (guardrails != null && hasText(guardrails.nextAction())) {
            return guardrails.nextAction();
        }
        return resolveNextAction(modo, representacao, manualDraft, assistiveAnalysis, protocolPackage);
    }

    private static String resolveNextAction(PeticionamentoModo modo,
                                            RepresentacaoProcessualPolicyResponse representacao,
                                            LaianePeticaoInicialDraftService.DraftView manualDraft,
                                            LaianePeticaoAssistResponse assistiveAnalysis,
                                            LaianePeticaoProtocolPackageResponse protocolPackage) {
        if (representacao != null && !representacao.regularidadeSuficiente()) {
            return "REGULARIZAR_REPRESENTACAO";
        }
        if (manualDraft != null && manualDraft.readinessScore() != null && manualDraft.readinessScore() < 70) {
            return "COMPLETAR_MINUTA_MANUAL";
        }
        if (modo.includeAssistive() && (assistiveAnalysis == null || !assistiveAnalysis.isProntaParaProtocolo())) {
            return "REVISAR_PREFLIGHT_ASSISTIDO";
        }
        if (protocolPackage == null && modo.includeAssistive()) {
            return "MONTAR_PACOTE_PROTOCOLO";
        }
        return "SEGUIR_PARA_ASSINATURA";
    }

    private static Integer resolveReadinessGeral(LaianePeticaoInicialDraftService.DraftView manualDraft,
                                                 LaianePeticaoAssistResponse assistiveAnalysis) {
        double total = 0.0d;
        int fontes = 0;
        if (manualDraft != null && manualDraft.readinessScore() != null) {
            total += manualDraft.readinessScore();
            fontes++;
        }
        if (assistiveAnalysis != null) {
            total += assistiveAnalysis.getReadinessScore();
            fontes++;
        }
        return fontes == 0 ? null : Math.max(0, Math.min(100, (int) Math.round(total / fontes)));
    }



    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static List<String> safeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
