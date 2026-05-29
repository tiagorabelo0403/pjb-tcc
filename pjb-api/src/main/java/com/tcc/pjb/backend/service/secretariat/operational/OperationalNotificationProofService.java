package com.tcc.pjb.backend.service.secretariat.operational;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.core.security.device.StrongAuthUsageService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.processo.ProcessoNote;
import com.tcc.pjb.backend.model.repository.processo.ProcessoNoteRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationalNotificationProofService {

    private static final int MAX_EVIDENCE_REFERENCES = 12;

    private final CurrentUserService currentUserService;
    private final SecurityChallengeService securityChallengeService;
    private final StrongAuthUsageService strongAuthUsageService;
    private final QualifiedDocumentSignatureEnvelopeService signatureEnvelopeService;
    private final ProcessoNoteRepository processoNoteRepository;
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ObjectMapper objectMapper;

    public OperationalNotificationProofService(CurrentUserService currentUserService,
                                              SecurityChallengeService securityChallengeService,
                                              StrongAuthUsageService strongAuthUsageService,
                                              QualifiedDocumentSignatureEnvelopeService signatureEnvelopeService,
                                              ProcessoNoteRepository processoNoteRepository,
                                              InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationService,
                                              ClientIpResolver clientIpResolver,
                                              ObjectProvider<HttpServletRequest> requestProvider,
                                              ObjectMapper objectMapper) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.securityChallengeService = Objects.requireNonNull(securityChallengeService);
        this.strongAuthUsageService = Objects.requireNonNull(strongAuthUsageService);
        this.signatureEnvelopeService = Objects.requireNonNull(signatureEnvelopeService);
        this.processoNoteRepository = Objects.requireNonNull(processoNoteRepository);
        this.sensitiveActAuthorizationService = Objects.requireNonNull(sensitiveActAuthorizationService);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public OperationalStepUpChallengeResponse issueChallenge(Processo processo,
                                                             Long referenceId,
                                                             String actionCode,
                                                             String channelCode,
                                                             String actorScope) {
        Processo safeProcesso = requireProcesso(processo);
        Usuario usuario = currentUserService.getRequired();
        assertAuthorized(InstitutionalSensitiveAct.GERAR_CERTIDAO_DE_CIENCIA);
        String processNumber = firstNonBlank(safeProcesso.getNumeroUnificado(), safeProcesso.getNumeroProcesso(), safeProcesso.getNumero(), "PROCESSO_SEM_NUMERO");
        String details = compactDetails(actionCode, channelCode, actorScope, referenceId, processNumber);
        var challenge = securityChallengeService.createEmailOtp(usuario, resolveIp(), details);
        return new OperationalStepUpChallengeResponse(
                challenge.getId(),
                normalizeCode(challenge.getTipo()),
                "EMAIL_INSTITUCIONAL",
                normalizeCode(actionCode),
                challenge.getExpiresAt(),
                "Código OTP institucional emitido para assinatura da confirmação de intimação.",
                "/api/v1/security/challenges/" + challenge.getId() + "/verify-otp",
                true
        );
    }

    @Transactional
    public GeneratedNotificationProof materializeProof(Processo processo,
                                                       Long referenceId,
                                                       String actionCode,
                                                       String actorScope,
                                                       String documentKind,
                                                       String communicationChannel,
                                                       String communicationMode,
                                                       String proofSummary,
                                                       List<String> evidenceReferences,
                                                       Map<String, Object> contactEnvelope,
                                                       Long challengeId,
                                                       String otpCode,
                                                       String note) {
        Processo safeProcesso = requireProcesso(processo);
        Usuario usuario = currentUserService.getRequired();
        assertAuthorized(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO);
        if (challengeId == null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "challengeId é obrigatório para assinar a confirmação de intimação");
        }
        if (otpCode == null || otpCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "otpCode é obrigatório para assinar a confirmação de intimação");
        }
        securityChallengeService.consumeOtp(challengeId, usuario, otpCode);
        String actionHash = Hashes.sha256Hex(String.join("|",
                normalizeCode(actionCode),
                normalizeCode(actorScope),
                String.valueOf(referenceId),
                String.valueOf(safeProcesso.getId()),
                normalizeNullable(firstNonBlank(safeProcesso.getNumeroUnificado(), safeProcesso.getNumeroProcesso(), safeProcesso.getNumero())),
                normalizeNullable(proofSummary),
                normalizeNullable(communicationChannel),
                normalizeNullable(communicationMode)));
        strongAuthUsageService.consumeOnce(challengeId, usuario, actionHash, actionHash);
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        List<String> normalizedEvidence = normalizeEvidence(evidenceReferences);
        String title = buildTitle(documentKind, safeProcesso);
        String body = buildBody(safeProcesso, usuario, actorScope, documentKind, communicationChannel, communicationMode, proofSummary, normalizedEvidence, contactEnvelope, note, now);
        SignedDocumentEnvelope signed = signatureEnvelopeService.signFreeContent(
                safeProcesso,
                usuario,
                title,
                body,
                resolveSignerRole(actorScope, usuario),
                "PJB_CONFIRMACAO_INTIMACAO_OPERACIONAL",
                false,
                List.of(normalizeCode(actorScope).toLowerCase(Locale.ROOT), "confirmacao_intimacao", "trilha_auditavel", "sem_future_assincrono")
        );
        ProcessoNote noteEntity = saveProcessNote(safeProcesso, usuario, signed.renderedContent(), documentKind, actorScope, normalizedEvidence);
        LinkedHashMap<String, Object> document = new LinkedHashMap<>();
        document.put("processoId", safeProcesso.getId());
        document.put("processoNumero", firstNonBlank(safeProcesso.getNumeroUnificado(), safeProcesso.getNumeroProcesso(), safeProcesso.getNumero()));
        document.put("referenceId", referenceId);
        document.put("actionCode", normalizeCode(actionCode));
        document.put("documentKind", normalizeCode(documentKind));
        document.put("noteId", noteEntity.getId());
        document.put("title", title);
        document.put("contentHash", signed.contentHash());
        document.put("createdAt", now.toString());
        document.put("communicationChannel", trimToNull(communicationChannel));
        document.put("communicationMode", trimToNull(communicationMode));
        document.put("proofSummary", trimToNull(proofSummary));
        document.put("evidenceReferences", normalizedEvidence);
        document.put("assinaturaQualificada", signed.assinaturaQualificada());
        document.put("validacaoSoberana", signed.validacaoSoberana());
        document.put("processNotePath", "/api/v1/processos/" + safeProcesso.getId() + "/notes");
        return new GeneratedNotificationProof(noteEntity.getId(), signed.renderedContent(), immutableMap(document));
    }

    private void assertAuthorized(InstitutionalSensitiveAct act) {
        var authorization = sensitiveActAuthorizationService.autorizar(act, null, null);
        if (!authorization.allowed()) {
            String reason = authorization.findings().isEmpty() ? "ato sensível não autorizado" : String.join(", ", authorization.findings());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
        }
    }

    private ProcessoNote saveProcessNote(Processo processo,
                                         Usuario usuario,
                                         String body,
                                         String documentKind,
                                         String actorScope,
                                         List<String> evidenceReferences) {
        Instant now = Instant.now();
        return processoNoteRepository.save(ProcessoNote.builder()
                .processoId(processo.getId())
                .authorUsuarioId(usuario.getId())
                .authorTipo(usuario.getTipoUsuario() == null ? "UNKNOWN" : usuario.getTipoUsuario().name())
                .body(body)
                .tagsJson(writeTags(documentKind, actorScope, evidenceReferences))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private String writeTags(String documentKind, String actorScope, List<String> evidenceReferences) {
        try {
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            tags.add("comprovacao_intimacao");
            tags.add(normalizeCode(documentKind).toLowerCase(Locale.ROOT));
            tags.add(normalizeCode(actorScope).toLowerCase(Locale.ROOT));
            if (!evidenceReferences.isEmpty()) {
                tags.add("possui_provas_vinculadas");
            }
            return objectMapper.writeValueAsString(tags);
        } catch (Exception ex) {
            return "[\"comprovacao_intimacao\"]";
        }
    }

    private String buildTitle(String documentKind, Processo processo) {
        String processNumber = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero(), "PROCESSO_SEM_NUMERO");
        String prefix = switch (normalizeCode(documentKind)) {
            case "CERTIDAO" -> "CERTIDÃO DE CONFIRMAÇÃO DE INTIMAÇÃO";
            default -> "CARTA DE CONFIRMAÇÃO DE INTIMAÇÃO";
        };
        return prefix + " - " + processNumber;
    }

    private String buildBody(Processo processo,
                             Usuario usuario,
                             String actorScope,
                             String documentKind,
                             String communicationChannel,
                             String communicationMode,
                             String proofSummary,
                             List<String> evidenceReferences,
                             Map<String, Object> contactEnvelope,
                             String note,
                             Instant now) {
        String processNumber = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero(), "PROCESSO_SEM_NUMERO");
        StringBuilder sb = new StringBuilder(4096);
        sb.append(buildTitle(documentKind, processo)).append('\n').append('\n');
        sb.append("Processo: ").append(processNumber).append('\n');
        sb.append("Justiça: ").append(firstNonBlank(stringValue(processo.getTipoJustica()), "NAO_IDENTIFICADA")).append('\n');
        sb.append("Tribunal: ").append(firstNonBlank(processo.getTribunal(), "NAO_IDENTIFICADO")).append('\n');
        sb.append("Unidade/Vara: ").append(firstNonBlank(processo.getVara(), processo.getComarca(), "NAO_IDENTIFICADA")).append('\n');
        sb.append("Rito: ").append(firstNonBlank(stringValue(processo.getRito()), "NAO_IDENTIFICADO")).append('\n');
        sb.append("Parte autora: ").append(firstNonBlank(processo.getParteAutoraNome(), "NAO_IDENTIFICADA")).append('\n');
        sb.append("Parte ré: ").append(firstNonBlank(processo.getParteReuNome(), "NAO_IDENTIFICADA")).append('\n');
        appendContacts(sb, contactEnvelope);
        sb.append('\n');
        sb.append("Ato institucional registrado por ").append(firstNonBlank(usuario.getNome(), "USUARIO_INSTITUCIONAL")).append(" no escopo ").append(normalizeCode(actorScope)).append(".\n");
        sb.append("Canal de intimação: ").append(firstNonBlank(communicationChannel, "NAO_INFORMADO")).append('\n');
        sb.append("Forma de intimação: ").append(firstNonBlank(communicationMode, "NAO_INFORMADA")).append('\n');
        sb.append("Prova resumida: ").append(firstNonBlank(proofSummary, "NAO_INFORMADA")).append('\n');
        if (note != null && !note.isBlank()) {
            sb.append("Observação operacional: ").append(note.trim()).append('\n');
        }
        if (!evidenceReferences.isEmpty()) {
            sb.append("Referências probatórias: ").append(String.join(", ", evidenceReferences)).append('\n');
        }
        sb.append("Momento do registro: ").append(now.atOffset(ZoneOffset.UTC)).append('\n');
        sb.append('\n').append("Declaro, para fins de formalização processual, que a intimação foi cumprida conforme os dados e evidências acima descritos, ficando esta carta/certidão vinculada à trilha auditável do PJB e à assinatura qualificada institucional do emissor.");
        return sb.toString();
    }

    private void appendContacts(StringBuilder sb, Map<String, Object> contactEnvelope) {
        if (contactEnvelope == null || contactEnvelope.isEmpty()) {
            return;
        }
        appendPartyContact(sb, "Autor", nestedMap(contactEnvelope, "autor"));
        appendPartyContact(sb, "Réu", nestedMap(contactEnvelope, "reu"));
        Object advogados = contactEnvelope.get("advogados");
        if (advogados instanceof List<?> list && !list.isEmpty()) {
            int index = 1;
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> map) {
                    sb.append("Advogado ").append(index++).append(": ")
                            .append(firstNonBlank(stringValue(map.get("nome")), stringValue(map.get("displayName")), "NAO_IDENTIFICADO"));
                    String email = firstNonBlank(stringValue(map.get("email")), stringValue(map.get("contato")));
                    if (email != null) {
                        sb.append(" | contato ").append(email);
                    }
                    sb.append('\n');
                }
            }
        }
    }

    private void appendPartyContact(StringBuilder sb, String label, Map<String, Object> party) {
        if (party == null || party.isEmpty()) {
            return;
        }
        sb.append(label).append(": ")
                .append(firstNonBlank(stringValue(party.get("nome")), "NAO_IDENTIFICADO"));
        String email = firstNonBlank(stringValue(party.get("email")), stringValue(party.get("contato")));
        if (email != null) {
            sb.append(" | contato ").append(email);
        }
        String documento = stringValue(party.get("documento"));
        if (documento != null) {
            sb.append(" | documento ").append(documento);
        }
        sb.append('\n');
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return Map.of();
        }
        Object raw = source.get(key);
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private List<String> normalizeEvidence(List<String> evidenceReferences) {
        if (evidenceReferences == null || evidenceReferences.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String item : evidenceReferences) {
            String normalized = trimToNull(item);
            if (normalized != null) {
                out.add(normalized.length() > 180 ? normalized.substring(0, 180) : normalized);
            }
            if (out.size() >= MAX_EVIDENCE_REFERENCES) {
                break;
            }
        }
        return List.copyOf(out);
    }

    private Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private Processo requireProcesso(Processo processo) {
        if (processo == null || processo.getId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "processo operacional não encontrado para geração da confirmação");
        }
        return processo;
    }

    private String compactDetails(String actionCode, String channelCode, String actorScope, Long referenceId, String processNumber) {
        return String.join(" | ",
                "PJB_NOTIFICATION_PROOF",
                firstNonBlank(normalizeCode(actionCode), "ACTION"),
                firstNonBlank(normalizeCode(channelCode), "CHANNEL"),
                firstNonBlank(normalizeCode(actorScope), "ACTOR"),
                String.valueOf(referenceId),
                processNumber);
    }

    private String resolveSignerRole(String actorScope, Usuario usuario) {
        if ("OFICIAL_JUSTICA".equalsIgnoreCase(actorScope)) {
            return "OFICIAL_JUSTICA";
        }
        return usuario.getTipoUsuario() == null ? "SECRETARIA_JUDICIARIA" : usuario.getTipoUsuario().name();
    }

    private String resolveIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        return clientIpResolver.resolve(request);
    }

    private String normalizeNullable(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "-" : normalized;
    }

    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "NAO_INFORMADO" : normalized.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record GeneratedNotificationProof(Long processNoteId,
                                             String renderedContent,
                                             Map<String, Object> document) {
    }
}
