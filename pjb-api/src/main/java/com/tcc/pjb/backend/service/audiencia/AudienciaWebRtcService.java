package com.tcc.pjb.backend.service.audiencia;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.access.PrivateResourceAccessGuardService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.audiencia.AudienciaWebRtcSessao;
import com.tcc.pjb.backend.model.repository.AudienciaWebRtcSessaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class AudienciaWebRtcService {

    private final AudienciaWebRtcSessaoRepository sessaoRepository;
    private final ProcessoRepository processoRepository;
    private final PrivateResourceAccessGuardService accessGuard;
    private final ObjectMapper objectMapper;

    public AudienciaWebRtcService(AudienciaWebRtcSessaoRepository sessaoRepository,
                                  ProcessoRepository processoRepository,
                                  PrivateResourceAccessGuardService accessGuard,
                                  ObjectMapper objectMapper) {
        this.sessaoRepository = Objects.requireNonNull(sessaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.accessGuard = Objects.requireNonNull(accessGuard);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public SessaoWebRtcResponse abrirSessao(SessaoWebRtcRequest request) {
        Objects.requireNonNull(request);
        Usuario usuario = accessGuard.requireCurrentUser();
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        accessGuard.requireReadProcesso(processo);
        String sessaoToken = UUID.nameUUIDFromBytes((request.audienciaId() + ":" + request.identificadorParticipante() + ":" + Instant.now()).getBytes()).toString();
        List<String> iceServers = List.of("stun:pjb.local:3478", "turn:pjb.local:3478?transport=udp");

        AudienciaWebRtcSessao sessao = new AudienciaWebRtcSessao();
        sessao.setAudienciaId(request.audienciaId());
        sessao.setProcesso(processo);
        sessao.setSessaoToken(sessaoToken);
        sessao.setParticipanteIdentificador(defaultText(request.identificadorParticipante(), usuario != null ? usuario.getNome() : "PARTICIPANTE"));
        sessao.setParticipanteUsuarioId(usuario != null ? usuario.getId() : null);
        sessao.setStatus("ABERTA");
        sessao.setIceServersCsv(String.join(",", iceServers));
        sessao.setExigirBiometria(request.exigirBiometria());
        sessao.setBiometriaStatus(request.exigirBiometria() ? "PENDENTE" : "DISPENSADA");
        sessao.setAbertaEm(Instant.now());
        sessao.setExpiraEm(Instant.now().plus(2, ChronoUnit.HOURS));
        AudienciaWebRtcSessao saved = sessaoRepository.save(sessao);

        return toResponse(saved, iceServers);
    }

    @Transactional
    public SinalizacaoWebRtcResponse registrarOffer(SinalizacaoWebRtcRequest request) {
        Objects.requireNonNull(request);
        AudienciaWebRtcSessao sessao = findByToken(request.sessaoToken());
        ensureSessionAccess(sessao);
        ensureMutable(sessao);
        String answer = "ANSWER-" + UUID.nameUUIDFromBytes((request.sessaoToken() + ":" + normalize(request.sdpOffer())).getBytes()).toString();
        sessao.setOfferHash(Hashes.sha256Hex(defaultText(request.sdpOffer(), "")));
        sessao.setAnswerHash(Hashes.sha256Hex(answer));
        sessao.setStatus("NEGOCIADA");
        sessaoRepository.save(sessao);
        return new SinalizacaoWebRtcResponse(sessao.getAudienciaId(), request.sessaoToken(), answer, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<SessaoWebRtcResumo> listarMinhasSessoes() {
        long userId = accessGuard.requireCurrentUser().getId();
        return sessaoRepository.findTop50ByParticipanteUsuarioIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessaoWebRtcDetalhe detalharSessao(String sessaoToken) {
        AudienciaWebRtcSessao sessao = findByToken(sessaoToken);
        ensureSessionAccess(sessao);
        return toDetalhe(sessao);
    }

    @Transactional
    public SessaoWebRtcDetalhe registrarTranscricao(TranscricaoWebRtcRequest request) {
        AudienciaWebRtcSessao sessao = findByToken(request.sessaoToken());
        ensureSessionAccess(sessao);
        ensureMutable(sessao);
        String atual = defaultText(sessao.getTranscricaoIntegral(), "");
        String trecho = defaultText(request.trecho(), "");
        sessao.setTranscricaoIntegral((atual + (atual.isBlank() || trecho.isBlank() ? "" : " ") + trecho).trim());
        sessao.setStatus(request.parcial() ? defaultText(sessao.getStatus(), "NEGOCIADA") : "TRANSCRICAO_ATUALIZADA");
        return toDetalhe(sessaoRepository.save(sessao));
    }

    @Transactional
    public BiometriaWebRtcResponse registrarBiometria(BiometriaWebRtcRequest request) {
        AudienciaWebRtcSessao sessao = findByToken(request.sessaoToken());
        ensureSessionAccess(sessao);
        ensureMutable(sessao);
        String status = request.similaridade() != null && request.similaridade() >= 0.82d ? "VALIDADA" : "REVISAR";
        sessao.setBiometriaStatus(status);
        LinkedHashMap<String, Object> biometria = new LinkedHashMap<>();
        biometria.put("dispositivoId", request.dispositivoId());
        biometria.put("referenciaHash", request.referenciaHash());
        biometria.put("similaridade", request.similaridade());
        biometria.put("status", status);
        biometria.put("validadoEm", Instant.now().toString());
        sessao.setMetricasJson(writeJson(biometria));
        sessaoRepository.save(sessao);
        return new BiometriaWebRtcResponse(sessao.getSessaoToken(), status, request.similaridade(), Instant.now());
    }

    @Transactional
    public SessaoWebRtcDetalhe encerrarSessao(EncerrarSessaoRequest request) {
        AudienciaWebRtcSessao sessao = findByToken(request.sessaoToken());
        ensureSessionAccess(sessao);
        ensureMutable(sessao);
        sessao.setGravacaoHash(request.gravacaoHash());
        LinkedHashMap<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("metricasResumo", request.metricasResumo());
        metricas.put("gravarTranscricaoFinal", request.gravarTranscricaoFinal());
        metricas.put("encerradoEm", Instant.now().toString());
        metricas.put("statusBiometria", sessao.getBiometriaStatus());
        sessao.setMetricasJson(writeJson(metricas));
        sessao.setStatus("ENCERRADA");
        sessao.setEncerradaEm(Instant.now());
        return toDetalhe(sessaoRepository.save(sessao));
    }

    private void ensureSessionAccess(AudienciaWebRtcSessao sessao) {
        accessGuard.requireParticipantOrPrivilegedOrReadProcesso(sessao.getParticipanteUsuarioId(), sessao.getProcesso(), "sessao WebRTC");
    }

    private void ensureMutable(AudienciaWebRtcSessao sessao) {
        if (sessao.getStatus() != null && "ENCERRADA".equalsIgnoreCase(sessao.getStatus())) {
            throw new IllegalStateException("Sessão WebRTC já encerrada.");
        }
        if (sessao.getExpiraEm() != null && Instant.now().isAfter(sessao.getExpiraEm())) {
            throw new IllegalStateException("Sessão WebRTC expirada.");
        }
    }

    private AudienciaWebRtcSessao findByToken(String sessaoToken) {
        return sessaoRepository.findBySessaoToken(sessaoToken)
                .orElseThrow(() -> new IllegalArgumentException("Sessão WebRTC não localizada."));
    }

    private SessaoWebRtcResponse toResponse(AudienciaWebRtcSessao sessao, List<String> iceServers) {
        return new SessaoWebRtcResponse(
                sessao.getAudienciaId(),
                sessao.getProcesso() != null ? sessao.getProcesso().getId() : null,
                sessao.getSessaoToken(),
                sessao.getParticipanteIdentificador(),
                sessao.isExigirBiometria(),
                iceServers,
                sessao.getAbertaEm(),
                sessao.getExpiraEm()
        );
    }

    private SessaoWebRtcResumo toResumo(AudienciaWebRtcSessao sessao) {
        return new SessaoWebRtcResumo(
                sessao.getAudienciaId(),
                sessao.getProcesso() != null ? sessao.getProcesso().getId() : null,
                sessao.getProcesso() != null ? sessao.getProcesso().getNumeroProcesso() : null,
                sessao.getSessaoToken(),
                sessao.getStatus(),
                sessao.getBiometriaStatus(),
                sessao.getAbertaEm(),
                sessao.getEncerradaEm()
        );
    }

    private SessaoWebRtcDetalhe toDetalhe(AudienciaWebRtcSessao sessao) {
        return new SessaoWebRtcDetalhe(
                sessao.getAudienciaId(),
                sessao.getProcesso() != null ? sessao.getProcesso().getId() : null,
                sessao.getProcesso() != null ? sessao.getProcesso().getNumeroProcesso() : null,
                sessao.getSessaoToken(),
                sessao.getParticipanteIdentificador(),
                sessao.getStatus(),
                sessao.isExigirBiometria(),
                sessao.getBiometriaStatus(),
                csvToList(sessao.getIceServersCsv()),
                sessao.getOfferHash(),
                sessao.getAnswerHash(),
                sessao.getTranscricaoIntegral(),
                sessao.getGravacaoHash(),
                sessao.getMetricasJson(),
                sessao.getAbertaEm(),
                sessao.getEncerradaEm(),
                sessao.getExpiraEm()
        );
    }

    private List<String> csvToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar métricas de audiência.", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record SessaoWebRtcRequest(@jakarta.validation.constraints.NotNull Long audienciaId, Long processoId, String identificadorParticipante, boolean exigirBiometria) {
    }

    public record SessaoWebRtcResponse(
            Long audienciaId,
            Long processoId,
            String sessaoToken,
            String identificadorParticipante,
            boolean exigirBiometria,
            List<String> iceServers,
            Instant abertaEm,
            Instant expiraEm
    ) {
    }

    public record SinalizacaoWebRtcRequest(Long audienciaId, String sessaoToken, String sdpOffer) {
    }

    public record SinalizacaoWebRtcResponse(Long audienciaId, String sessaoToken, String sdpAnswer, Instant respondidoEm) {
    }

    public record TranscricaoWebRtcRequest(String sessaoToken, String trecho, Integer sequencia, boolean parcial) {
    }

    public record BiometriaWebRtcRequest(String sessaoToken, String referenciaHash, Double similaridade, String dispositivoId) {
    }

    public record BiometriaWebRtcResponse(String sessaoToken, String status, Double similaridade, Instant validadoEm) {
    }

    public record EncerrarSessaoRequest(String sessaoToken, String gravacaoHash, String metricasResumo, boolean gravarTranscricaoFinal) {
    }

    public record SessaoWebRtcResumo(Long audienciaId,
                                     Long processoId,
                                     String numeroProcesso,
                                     String sessaoToken,
                                     String status,
                                     String biometriaStatus,
                                     Instant abertaEm,
                                     Instant encerradaEm) {
    }

    public record SessaoWebRtcDetalhe(Long audienciaId,
                                      Long processoId,
                                      String numeroProcesso,
                                      String sessaoToken,
                                      String participanteIdentificador,
                                      String status,
                                      boolean exigirBiometria,
                                      String biometriaStatus,
                                      List<String> iceServers,
                                      String offerHash,
                                      String answerHash,
                                      String transcricaoIntegral,
                                      String gravacaoHash,
                                      String metricasJson,
                                      Instant abertaEm,
                                      Instant encerradaEm,
                                      Instant expiraEm) {
    }
}
