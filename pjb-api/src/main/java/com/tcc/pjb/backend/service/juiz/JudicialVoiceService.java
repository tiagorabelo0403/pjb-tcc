package com.tcc.pjb.backend.service.juiz;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.juiz.JudicialVoiceSession;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.JudicialVoiceSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;

@Service
public class JudicialVoiceService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final JudicialVoiceSessionRepository voiceSessionRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final JudicialVoiceDraftIntelligenceService draftIntelligenceService;

    public JudicialVoiceService(ProcessoRepository processoRepository,
                                WorkItemRepository workItemRepository,
                                JudicialVoiceSessionRepository voiceSessionRepository,
                                CurrentUserService currentUserService,
                                PjbAuthorizationService authorizationService,
                                InstitutionalActorRoutingService institutionalActorRoutingService,
                                JudicialVoiceDraftIntelligenceService draftIntelligenceService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.voiceSessionRepository = Objects.requireNonNull(voiceSessionRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.draftIntelligenceService = Objects.requireNonNull(draftIntelligenceService);
    }

    @Transactional
    public VoiceDraftResponse estruturar(VoiceDraftRequest request) {
        Usuario usuario = requireMagistrate();
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        JudicialVoiceSession sessao = persistSession(usuario, processo, defaultText(request.modoDocumento(), "SENTENCA"), request.transcricaoBruta());
        gerarWorkItemSeNecessario(usuario, processo, sessao);
        return toResponse(sessao);
    }

    @Transactional
    public VoiceSessionView abrirSessao(VoiceSessionOpenRequest request) {
        Usuario usuario = requireMagistrate();
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        JudicialVoiceSession sessao = persistSession(usuario, processo, defaultText(request.modoDocumento(), "SENTENCA"), request.primeiraCaptura());
        return toSessionView(sessao);
    }

    @Transactional
    public VoiceSessionView adicionarTrecho(Long sessaoId, VoiceSessionChunkRequest request) {
        Usuario usuario = requireMagistrate();
        JudicialVoiceSession sessao = findOwned(sessaoId, usuario.getId());
        ensureMutable(sessao);
        String acumulado = defaultText(sessao.getTranscricaoIntegral(), "");
        String trecho = defaultText(request.trecho(), "");
        sessao.setTranscricaoIntegral((acumulado + (acumulado.isBlank() || trecho.isBlank() ? "" : " ") + trecho).trim());
        recomputeDrafts(sessao);
        sessao.setStatus(request.parcial() ? "CAPTURANDO" : "RASCUNHO_ATUALIZADO");
        return toSessionView(voiceSessionRepository.save(sessao));
    }

    @Transactional
    public VoiceSessionView finalizarSessao(Long sessaoId) {
        Usuario usuario = requireMagistrate();
        JudicialVoiceSession sessao = findOwned(sessaoId, usuario.getId());
        ensureMutable(sessao);
        sessao.setStatus("FINALIZADA");
        sessao.setFinalizadaEm(Instant.now());
        JudicialVoiceSession saved = voiceSessionRepository.save(sessao);
        gerarWorkItemSeNecessario(usuario, saved.getProcesso(), saved);
        return toSessionView(saved);
    }

    @Transactional(readOnly = true)
    public VoiceSessionView detalharSessao(Long sessaoId) {
        Usuario usuario = requireMagistrate();
        return toSessionView(findOwned(sessaoId, usuario.getId()));
    }

    @Transactional(readOnly = true)
    public List<VoiceSessionSummary> listarSessoesRecentes() {
        Usuario usuario = requireMagistrate();
        return voiceSessionRepository.findTop50ByMagistrado_IdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    private JudicialVoiceSession persistSession(Usuario usuario, Processo processo, String modoDocumento, String transcricao) {
        JudicialVoiceSession sessao = new JudicialVoiceSession();
        sessao.setProcesso(processo);
        sessao.setMagistrado(usuario);
        sessao.setModoDocumento(modoDocumento);
        sessao.setStatus("ABERTA");
        sessao.setTranscricaoIntegral(defaultText(transcricao, ""));
        recomputeDrafts(sessao);
        return voiceSessionRepository.save(sessao);
    }

    private void recomputeDrafts(JudicialVoiceSession sessao) {
        String texto = defaultText(sessao.getTranscricaoIntegral(), "");
        JudicialVoiceDraftIntelligenceService.DraftProjection projection = draftIntelligenceService.project(sessao.getProcesso(), sessao.getModoDocumento(), texto);
        String normalized = defaultText(projection.transcricaoNormalizada(), texto);
        sessao.setRelatorioDraft("Relatório: " + extrairSentencas(normalized, 2));
        sessao.setFundamentacaoDraft("Fundamentação: " + extrairSentencas(normalized, 4));
        sessao.setDispositivoDraft("Dispositivo: " + buildDispositivo(sessao.getModoDocumento(), sessao.getProcesso().getNumeroProcesso()));
        sessao.setComandoResumo("Comandos reconhecidos: " + extrairComandos(normalized));
        sessao.setAudioPreviewText(buildAudioPreview(sessao));
    }

    private void gerarWorkItemSeNecessario(Usuario usuario, Processo processo, JudicialVoiceSession sessao) {
        String templateCode = "VOICE_DRAFT:" + processo.getId() + ":" + usuario.getId() + ":" + sessao.getModoDocumento() + ":" + sessao.getId();
        if (workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO).isEmpty()) {
            TipoUsuario reviewRole = resolveReviewRole(usuario);
            InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                    processo.getId(),
                    reviewRole,
                    "VOICE_DRAFT"
            );
            workItemRepository.save(WorkItem.builder()
                    .processo(processo)
                    .faseOrigem(processo.getFaseAtual())
                    .templateCode(templateCode)
                    .type(resolveType(sessao.getModoDocumento()))
                    .titulo("Rascunho por voz — " + sessao.getModoDocumento() + " — " + processo.getNumeroProcesso())
                    .descricao(defaultText(sessao.getRelatorioDraft(), "") + " | " + defaultText(sessao.getFundamentacaoDraft(), "") + " | " + defaultText(sessao.getDispositivoDraft(), ""))
                    .queueCode(route.queueCode())
                    .inboxKey(route.inboxKey())
                    .assignedRole(route.assignedRole())
                    .status(WorkItemStatus.PENDENTE)
                    .prioridade(1)
                    .uf(usuario.getUf())
                    .comarca(usuario.getComarca())
                    .dueAt(Instant.now().plus(6, ChronoUnit.HOURS))
                    .build());
        }
    }

    private void ensureMutable(JudicialVoiceSession sessao) {
        if (sessao.getStatus() != null && "FINALIZADA".equalsIgnoreCase(sessao.getStatus())) {
            throw new IllegalStateException("Sessão de voice drafting já finalizada.");
        }
    }

    private JudicialVoiceSession findOwned(Long sessaoId, Long magistradoId) {
        return voiceSessionRepository.findByIdAndMagistrado_Id(sessaoId, magistradoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão de voice drafting não localizada."));
    }

    private TipoUsuario resolveReviewRole(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return TipoUsuario.ASSESSOR_JUDICIAL;
        }
        return switch (usuario.getTipoUsuario()) {
            case MINISTRO -> TipoUsuario.ASSESSOR_MINISTRO;
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> TipoUsuario.ASSESSOR_DESEMBARGADOR;
            default -> TipoUsuario.ASSESSOR_JUDICIAL;
        };
    }

    private Usuario requireMagistrate() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isMagistratura()) {
            throw new IllegalStateException("Operacao exclusiva da magistratura.");
        }
        return usuario;
    }

    private String extrairSentencas(String texto, int limite) {
        String[] partes = texto.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        int usados = 0;
        for (String parte : partes) {
            String limpa = parte.trim();
            if (limpa.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(limpa);
            usados++;
            if (usados >= limite) {
                break;
            }
        }
        return sb.isEmpty() ? texto : sb.toString();
    }

    private String extrairComandos(String texto) {
        String normalized = defaultText(texto, "").toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        if (normalized.contains("intime")) {
            sb.append("intimação; ");
        }
        if (normalized.contains("cumpra-se") || normalized.contains("cumpra se")) {
            sb.append("cumprimento; ");
        }
        if (normalized.contains("vista")) {
            sb.append("vista; ");
        }
        return sb.isEmpty() ? "nenhum comando explícito reconhecido" : sb.substring(0, sb.length() - 2);
    }

    private String buildAudioPreview(JudicialVoiceSession sessao) {
        return "Prévia de voz institucional para " + sessao.getModoDocumento() + ": "
                + defaultText(sessao.getDispositivoDraft(), "") + " "
                + defaultText(sessao.getComandoResumo(), "");
    }

    private String buildDispositivo(String modoDocumento, String numeroProcesso) {
        String modo = modoDocumento == null ? "ATO" : modoDocumento.trim().toUpperCase(Locale.ROOT);
        return switch (modo) {
            case "DESPACHO" -> "Determino o regular prosseguimento do feito, com impulso oficial nos termos do gabinete. Processo " + numeroProcesso + ".";
            case "DECISAO" -> "Decido conforme os fundamentos acima, intimem-se e cumpra-se. Processo " + numeroProcesso + ".";
            default -> "Julgo nos termos da fundamentação, com as comunicações e registros cabíveis no processo " + numeroProcesso + ".";
        };
    }

    private WorkItemType resolveType(String modoDocumento) {
        String modo = modoDocumento == null ? "SENTENCA" : modoDocumento.trim().toUpperCase(Locale.ROOT);
        return switch (modo) {
            case "DESPACHO" -> WorkItemType.DESPACHO;
            case "DECISAO" -> WorkItemType.DECISAO;
            default -> WorkItemType.SENTENCA;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private VoiceDraftResponse toResponse(JudicialVoiceSession sessao) {
        JudicialVoiceDraftIntelligenceService.DraftProjection projection = draftIntelligenceService.project(
                sessao.getProcesso(),
                sessao.getModoDocumento(),
                sessao.getTranscricaoIntegral()
        );
        return new VoiceDraftResponse(
                sessao.getId(),
                sessao.getProcesso().getId(),
                sessao.getProcesso().getNumeroProcesso(),
                sessao.getModoDocumento(),
                sessao.getRelatorioDraft(),
                sessao.getFundamentacaoDraft(),
                sessao.getDispositivoDraft(),
                sessao.getAudioPreviewText(),
                projection.transcricaoNormalizada(),
                projection.fundamentosSugeridos(),
                projection.precedentesSugeridos(),
                projection.profile(),
                sessao.getUpdatedAt() != null ? sessao.getUpdatedAt() : sessao.getCreatedAt()
        );
    }

    private VoiceSessionSummary toSummary(JudicialVoiceSession sessao) {
        return new VoiceSessionSummary(
                sessao.getId(),
                sessao.getProcesso().getId(),
                sessao.getProcesso().getNumeroProcesso(),
                sessao.getModoDocumento(),
                sessao.getStatus(),
                sessao.getCreatedAt(),
                sessao.getFinalizadaEm()
        );
    }

    private VoiceSessionView toSessionView(JudicialVoiceSession sessao) {
        JudicialVoiceDraftIntelligenceService.DraftProjection projection = draftIntelligenceService.project(
                sessao.getProcesso(),
                sessao.getModoDocumento(),
                sessao.getTranscricaoIntegral()
        );
        return new VoiceSessionView(
                sessao.getId(),
                sessao.getProcesso().getId(),
                sessao.getProcesso().getNumeroProcesso(),
                sessao.getModoDocumento(),
                sessao.getStatus(),
                sessao.getTranscricaoIntegral(),
                projection.transcricaoNormalizada(),
                sessao.getRelatorioDraft(),
                sessao.getFundamentacaoDraft(),
                sessao.getDispositivoDraft(),
                sessao.getComandoResumo(),
                sessao.getAudioPreviewText(),
                projection.fundamentosSugeridos(),
                projection.precedentesSugeridos(),
                projection.profile(),
                sessao.getCreatedAt(),
                sessao.getFinalizadaEm()
        );
    }

    public record VoiceDraftRequest(@jakarta.validation.constraints.NotNull Long processoId, String modoDocumento, String transcricaoBruta) {
    }

    public record VoiceDraftResponse(
            Long sessaoId,
            Long processoId,
            String numeroProcesso,
            String modoDocumento,
            String relatorio,
            String fundamentacao,
            String dispositivo,
            String audioPreviewText,
            String transcricaoNormalizada,
            List<String> fundamentosSugeridos,
            List<java.util.Map<String, Object>> precedentesSugeridos,
            String profile,
            Instant estruturadoEm
    ) {
    }

    public record VoiceSessionOpenRequest(@jakarta.validation.constraints.NotNull Long processoId, String modoDocumento, String primeiraCaptura) {
    }

    public record VoiceSessionChunkRequest(@jakarta.validation.constraints.NotBlank String trecho, boolean parcial) {
    }

    public record VoiceSessionSummary(Long sessaoId,
                                      Long processoId,
                                      String numeroProcesso,
                                      String modoDocumento,
                                      String status,
                                      Instant criadaEm,
                                      Instant finalizadaEm) {
    }

    public record VoiceSessionView(Long sessaoId,
                                   Long processoId,
                                   String numeroProcesso,
                                   String modoDocumento,
                                   String status,
                                   String transcricaoIntegral,
                                   String transcricaoNormalizada,
                                   String relatorio,
                                   String fundamentacao,
                                   String dispositivo,
                                   String comandoResumo,
                                   String audioPreviewText,
                                   List<String> fundamentosSugeridos,
                                   List<java.util.Map<String, Object>> precedentesSugeridos,
                                   String profile,
                                   Instant criadaEm,
                                   Instant finalizadaEm) {
    }
}
