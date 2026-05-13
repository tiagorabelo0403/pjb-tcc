package com.tcc.pjb.backend.service.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SigiloProcessoProofChallenge;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SigiloProcessoProofChallengeRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class SigiloZeroKnowledgeProofService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProcessoRepository processoRepository;
    private final SigiloProcessoProofChallengeRepository challengeRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public SigiloZeroKnowledgeProofService(ProcessoRepository processoRepository,
                                           SigiloProcessoProofChallengeRepository challengeRepository,
                                           CurrentUserService currentUserService,
                                           ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.challengeRepository = Objects.requireNonNull(challengeRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public ChallengeView emitirDesafio(Long processoId, ChallengeRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = currentUserService.getOrNull();
        String nonce = randomToken(18);
        String statement = defaultText(request.statement(), "Provar conhecimento do snapshot sigiloso sem revelar o conteúdo do processo.");
        String escopo = defaultText(request.escopo(), "PROCESSO_SIGILOSO");
        String snapshotHash = snapshotHash(processo, escopo);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("nonce", nonce);
        payload.put("processoId", processo.getId());
        payload.put("statement", statement);
        payload.put("escopo", escopo);
        payload.put("exp", Instant.now().plusSeconds(300).toString());
        String payloadJson = writeJson(payload);
        String commitment = Hashes.sha256Hex(snapshotHash + "|" + nonce + "|" + statement + "|" + processo.getId());

        SigiloProcessoProofChallenge entity = new SigiloProcessoProofChallenge();
        entity.setChallengeId("ZK-" + URL_ENCODER.encodeToString(Hashes.sha256((processoId + "|" + nonce).getBytes(StandardCharsets.UTF_8))).substring(0, 28));
        entity.setProcesso(processo);
        entity.setSolicitante(usuario);
        entity.setEscopo(escopo);
        entity.setStatement(statement);
        entity.setChallengePayload(payloadJson);
        entity.setCommitmentHash(commitment);
        entity.setStatus("EMITIDO");
        entity.setExpiraEm(Instant.now().plusSeconds(300));
        SigiloProcessoProofChallenge saved = challengeRepository.save(entity);

        return new ChallengeView(saved.getChallengeId(), processo.getId(), processo.getNumeroProcesso(), escopo, statement,
                payloadJson, commitment, saved.getExpiraEm());
    }

    @Transactional
    public VerificationView verificar(String challengeId, VerificationRequest request) {
        SigiloProcessoProofChallenge challenge = challengeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge ZK não localizado."));
        if (challenge.getExpiraEm() != null && challenge.getExpiraEm().isBefore(Instant.now())) {
            challenge.setStatus("EXPIRADO");
            challengeRepository.save(challenge);
            throw new IllegalStateException("Challenge expirado.");
        }
        Processo processo = challenge.getProcesso();
        String expectedSnapshotHash = snapshotHash(processo, challenge.getEscopo());
        Map<String, Object> payload = readJson(challenge.getChallengePayload());
        String nonce = String.valueOf(payload.getOrDefault("nonce", ""));
        String expectedProof = Hashes.sha256Hex(defaultText(request.snapshotHashConhecido(), "") + "|" + nonce + "|" + challenge.getStatement() + "|" + processo.getId());
        boolean snapshotOk = constantEquals(expectedSnapshotHash, defaultText(request.snapshotHashConhecido(), ""));
        boolean proofOk = constantEquals(expectedProof, defaultText(request.proofToken(), ""));
        challenge.setResponseHash(defaultText(request.proofToken(), null));
        challenge.setStatus(snapshotOk && proofOk ? "VERIFICADO" : "NEGADO");
        if (snapshotOk && proofOk) {
            challenge.setVerificadoEm(Instant.now());
        }
        challengeRepository.save(challenge);
        return new VerificationView(challenge.getChallengeId(), processo.getId(), processo.getNumeroProcesso(), snapshotOk && proofOk,
                snapshotOk, proofOk, challenge.getStatus(), challenge.getVerificadoEm());
    }

    private String snapshotHash(Processo processo, String escopo) {
        return Hashes.sha256Hex(String.join("|",
                "PJB-ZK-SIGILO-2026",
                String.valueOf(processo.getId()),
                defaultText(processo.getNumeroProcesso(), ""),
                processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : "PUBLICO",
                defaultText(processo.getMaterialProbatorioHash(), ""),
                defaultText(processo.getParteAutoraCpf(), ""),
                defaultText(processo.getParteReuCpf(), ""),
                defaultText(escopo, "PROCESSO_SIGILOSO")
        ));
    }

    private boolean constantEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar challenge ZK.", e);
        }
    }

    private Map<String, Object> readJson(String value) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao ler challenge ZK.", e);
        }
    }

    private String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ChallengeRequest(String escopo, String statement) {
    }

    public record VerificationRequest(String snapshotHashConhecido, String proofToken) {
    }

    public record ChallengeView(String challengeId,
                                Long processoId,
                                String numeroProcesso,
                                String escopo,
                                String statement,
                                String challengePayload,
                                String commitmentHash,
                                Instant expiraEm) {
    }

    public record VerificationView(String challengeId,
                                   Long processoId,
                                   String numeroProcesso,
                                   boolean verificado,
                                   boolean snapshotHashConfere,
                                   boolean provaConfere,
                                   String status,
                                   Instant verificadoEm) {
    }
}
