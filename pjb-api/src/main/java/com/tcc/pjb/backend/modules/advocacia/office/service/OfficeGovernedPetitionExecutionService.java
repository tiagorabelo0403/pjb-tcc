package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Execução da governança de petição (assinatura qualificada + roteamento institucional).
 * Extraído de {@link OfficeGovernedProcessOperationService} porque {@code contextFactory},
 * {@code commons}, {@code workItemRepository}, {@code authorizationService},
 * {@code institutionalActorRoutingService} e {@code qualifiedDocumentSignatureEnvelopeService}
 * são usados exclusivamente por essa operação.
 */
@Service
public class OfficeGovernedPetitionExecutionService {

    private final ProcessoRepository processoRepository;
    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private final ObjectMapper objectMapper;

    public OfficeGovernedPetitionExecutionService(ProcessoRepository processoRepository,
                                                   PerfilDashboardContextFactory contextFactory,
                                                   PainelServiceCommons commons,
                                                   WorkItemRepository workItemRepository,
                                                   PjbAuthorizationService authorizationService,
                                                   InstitutionalActorRoutingService institutionalActorRoutingService,
                                                   QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService,
                                                   ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public java.util.Map<String, Object> execute(Long processoId,
                                                  String tipoPeticao,
                                                  String conteudo,
                                                  String fundamentacao,
                                                  Usuario signer,
                                                  AdvOfficeProcessOperation operation) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        Usuario resolvedSigner = signer == null ? usuario : signer;
        authorizationService.requireRole(usuario, "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        authorizationService.requireReadProcesso(processo);
        SignedDocumentEnvelope signedContent = qualifiedDocumentSignatureEnvelopeService.signGovernedContent(
                processo,
                resolvedSigner,
                petitionTitle(tipoPeticao),
                conteudo,
                resolvePetitionSignerRole(usuario, resolvedSigner),
                resolvePetitionPolicy(usuario, resolvedSigner),
                processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO,
                petitionGovernanceTags(operation, usuario, resolvedSigner, tipoPeticao)
        );
        String dedupKey = UUID.nameUUIDFromBytes(("PETICAO:" + processoId + ':' + tipoPeticao + ':' + stableActorKey(resolvedSigner)).getBytes(StandardCharsets.UTF_8)).toString();
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.secretaryReceipt(processoId, "PETICAO_" + normalizeToken(tipoPeticao));
        WorkItem peticao = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.PETICAO)
                .titulo(tipoPeticao + " — " + processo.getNumeroProcesso())
                .descricao(signedContent.renderedContent())
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(2)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .build();
        peticao = workItemRepository.save(peticao);
        if (operation != null) {
            LinkedHashMap<String, Object> signatureSnapshot = new LinkedHashMap<>();
            signatureSnapshot.put("renderedContent", signedContent.renderedContent());
            signatureSnapshot.put("assinaturaQualificada", signedContent.assinaturaQualificada());
            signatureSnapshot.put("validacaoSoberana", signedContent.validacaoSoberana());
            operation.setSignaturePayloadJson(writeJson(signatureSnapshot));
            operation.setSignatureHash(signedContent.contentHash());
            operation.setSignerNameSnapshot(resolveSignerName(resolvedSigner));
            operation.setSignerRegistrationSnapshot(resolveSignerRegistration(resolvedSigner));
        }
        commons.publishUserHistory(usuario, "ADVOGADO", "PETICAO_PROTOCOLIZADA", tipoPeticao + " protocolizada.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PETIÇÃO_PROTOCOLIZADA");
        out.put("tipo", tipoPeticao);
        out.put("processoId", processoId);
        out.put("workItemId", peticao.getId());
        out.put("dedupKey", dedupKey);
        out.put("signerUserId", resolvedSigner.getId());
        out.put("signerNome", resolveSignerName(resolvedSigner));
        out.put("signerRegistration", resolveSignerRegistration(resolvedSigner));
        out.put("signatureMode", resolveSignatureMode(usuario, resolvedSigner));
        out.put("signatureEnvelopeReady", Boolean.TRUE);
        out.put("signedContentHash", signedContent.contentHash());
        out.put("signedContent", signedContent.renderedContent());
        out.put("signatureEnvelope", signedContent.assinaturaQualificada());
        return out;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload da operacao processual.", ex);
        }
    }

    private List<String> petitionGovernanceTags(AdvOfficeProcessOperation operation,
                                                 Usuario executor,
                                                 Usuario signer,
                                                 String tipoPeticao) {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("advocacia");
        tags.add("peticionamento");
        tags.add("workspace_escritorio");
        if (tipoPeticao != null && !tipoPeticao.isBlank()) {
            tags.add("peticao_" + normalizeToken(tipoPeticao).replace(' ', '_').toLowerCase(Locale.ROOT));
        }
        if (operation != null && operation.getEquipe() != null && operation.getEquipe().getId() != null) {
            tags.add("equipe_" + operation.getEquipe().getId());
        }
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            tags.add("assinatura_patrono");
        }
        return List.copyOf(tags);
    }

    private String petitionTitle(String tipoPeticao) {
        return (tipoPeticao == null || tipoPeticao.isBlank() ? "Petição" : tipoPeticao.trim()) + " — assinatura qualificada";
    }

    private String resolvePetitionSignerRole(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "ADVOGADO_PATRONO";
        }
        return "ADVOGADO";
    }

    private String resolvePetitionPolicy(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "ADVOCACIA_ESCRITORIO_CERTIFICADO_PATRONO";
        }
        return "ADVOCACIA_PETICAO_QUALIFICADA";
    }

    private String resolveSignatureMode(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "PATRONO_CERTIFICATE";
        }
        return "SELF_CERTIFICATE";
    }

    private String resolveSignerName(Usuario signer) {
        if (signer == null || signer.getNome() == null || signer.getNome().isBlank()) {
            return null;
        }
        return signer.getNome().trim();
    }

    private String resolveSignerRegistration(Usuario signer) {
        if (signer == null) {
            return null;
        }
        if (signer.getOab() != null && !signer.getOab().isBlank()) {
            return signer.getOab().trim();
        }
        if (signer.getRegistroProfissional() != null && !signer.getRegistroProfissional().isBlank()) {
            return signer.getRegistroProfissional().trim();
        }
        if (signer.getCpf() != null && !signer.getCpf().isBlank()) {
            return signer.getCpf().replaceAll("[^0-9]", "");
        }
        return null;
    }

    private String stableActorKey(Usuario usuario) {
        if (usuario == null) {
            return "anon";
        }
        if (usuario.getId() != null) {
            return "id:" + usuario.getId();
        }
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            return "cpf:" + usuario.getCpf().replaceAll("[^0-9]", "");
        }
        return "email:" + (usuario.getEmail() == null ? "sem_email" : usuario.getEmail().trim().toLowerCase());
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "NA";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}
