package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCallTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalIntegrationCallTrailStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalIntegrationCredentialStateRepository;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalIntegrationCredentialStatus;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalIntegrationCredentialApplicationService {

    private final InstitutionalIntegrationCredentialStateRepository credentialRepository;
    private final InstitutionalIntegrationCallTrailStateRepository callTrailRepository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalIntegrationSecurityPolicyApplicationService policyApplicationService;
    private final SecureRandom secureRandom;

    public InstitutionalIntegrationCredentialApplicationService(InstitutionalIntegrationCredentialStateRepository credentialRepository,
                                                               InstitutionalIntegrationCallTrailStateRepository callTrailRepository,
                                                               InstitutionalAffiliationStateRepository affiliationRepository,
                                                               InstitutionalIntegrationSecurityPolicyApplicationService policyApplicationService) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.callTrailRepository = Objects.requireNonNull(callTrailRepository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.policyApplicationService = Objects.requireNonNull(policyApplicationService);
        this.secureRandom = new SecureRandom();
    }

    public IssuedCredential issue(String affiliationId,
                                  String displayName,
                                  List<String> integrationFamilies,
                                  List<String> originAllowlist,
                                  List<String> fundamentos) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("Afiliação institucional não encontrada."));
        InstitutionalIntegrationSecurityPolicy policy = policyApplicationService.listar(null, affiliationId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Política de integração não encontrada para a afiliação."));
        Instant now = Instant.now();
        SecretMaterial secret = newSecret();
        InstitutionalIntegrationCredential credential = new InstitutionalIntegrationCredential(
                UUID.randomUUID().toString(),
                affiliationId,
                displayName == null || displayName.isBlank() ? affiliation.orgaoSigla() + "::API" : displayName.trim(),
                sanitize(integrationFamilies),
                sanitize(originAllowlist),
                policy.requiresPayloadSignature(),
                policy.requiresMutualTls(),
                policy.requiresHumanApproval(),
                policy.requiresImmediateRevocation(),
                policy.credentialRotationDays(),
                InstitutionalIntegrationCredentialStatus.ATIVA,
                "kid_" + UUID.randomUUID().toString().replace("-", ""),
                secret.secretHash(),
                secret.preview(),
                now,
                null,
                now.plusSeconds(policy.credentialRotationDays() * 86400L),
                null,
                mergeFundamentos(fundamentos, List.of("credencial_emitida", "origin_allowlist=" + String.join(",", sanitize(originAllowlist)))),
                null
        );
        credentialRepository.save(credential);
        return new IssuedCredential(credential, secret.plaintext());
    }

    public IssuedCredential rotate(String credentialId, List<String> fundamentos) {
        InstitutionalIntegrationCredential current = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial institucional não encontrada."));
        SecretMaterial secret = newSecret();
        InstitutionalIntegrationCredential rotated = current.withRotation(secret.secretHash(), secret.preview(), Instant.now(),
                mergeFundamentos(fundamentos, List.of("credencial_rotacionada")));
        credentialRepository.save(rotated);
        return new IssuedCredential(rotated, secret.plaintext());
    }

    public InstitutionalIntegrationCredential revoke(String credentialId, List<String> fundamentos) {
        InstitutionalIntegrationCredential current = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial institucional não encontrada."));
        InstitutionalIntegrationCredential revoked = current.revoke(Instant.now(), mergeFundamentos(fundamentos, List.of("credencial_revogada_imediatamente")));
        return credentialRepository.save(revoked);
    }

    public InstitutionalIntegrationCallTrail registerCall(String credentialId,
                                                          String correlationId,
                                                          String origin,
                                                          String payloadDigest,
                                                          boolean payloadSignaturePresent,
                                                          String idempotencyKey,
                                                          String resultCode,
                                                          List<String> findings) {
        InstitutionalIntegrationCredential credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial institucional não encontrada."));
        ArrayList<String> allFindings = new ArrayList<>();
        if (findings != null) allFindings.addAll(findings);
        if (credential.requiresPayloadSignature() && !payloadSignaturePresent) {
            allFindings.add("payload_signature_missing");
        }
        if (credential.requiresMutualTls() && (origin == null || origin.isBlank())) {
            allFindings.add("origin_missing_for_mutual_tls_governance");
        }
        if (!credential.originAllowlist().isEmpty() && origin != null && credential.originAllowlist().stream().noneMatch(origin::equalsIgnoreCase)) {
            allFindings.add("origin_not_allowlisted");
        }
        InstitutionalIntegrationCallTrail trail = new InstitutionalIntegrationCallTrail(
                UUID.randomUUID().toString(),
                credentialId,
                correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId.trim(),
                origin,
                payloadDigest,
                payloadSignaturePresent,
                idempotencyKey,
                resultCode == null || resultCode.isBlank() ? "OK" : resultCode.trim(),
                List.copyOf(allFindings.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList()),
                Instant.now(),
                null
        );
        return callTrailRepository.save(trail);
    }

    public List<InstitutionalIntegrationCredential> list(String affiliationId) {
        List<InstitutionalIntegrationCredential> source = affiliationId == null || affiliationId.isBlank()
                ? credentialRepository.findAll()
                : credentialRepository.findByAffiliationId(affiliationId.trim());
        return source.stream()
                .sorted(Comparator.comparing(InstitutionalIntegrationCredential::issuedAt).reversed())
                .toList();
    }

    public List<InstitutionalIntegrationCallTrail> trails(String credentialId) {
        return callTrailRepository.findByCredentialId(credentialId).stream()
                .sorted(Comparator.comparing(InstitutionalIntegrationCallTrail::calledAt).reversed())
                .toList();
    }

    private SecretMaterial newSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String preview = plain.substring(Math.max(0, plain.length() - 8));
        return new SecretMaterial(plain, Hashes.sha256Hex(plain), preview);
    }

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private List<String> mergeFundamentos(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>();
        if (left != null) out.addAll(left);
        if (right != null) out.addAll(right);
        return List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList());
    }

    private record SecretMaterial(String plaintext, String secretHash, String preview) { }

    public record IssuedCredential(InstitutionalIntegrationCredential credential, String plaintextSecret) { }
}
