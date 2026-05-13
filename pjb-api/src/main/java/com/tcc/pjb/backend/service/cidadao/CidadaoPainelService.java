package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoAreaNavLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelBadgesDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelBootstrapResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelWidgetsResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPerfilResumoDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProximoEventoDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoWidgetDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardSnapshot;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.repository.UsuarioAvatarRepository;
import com.tcc.pjb.backend.repository.cidadao.CidadaoDashboardSnapshotRepository;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardRefreshRequest;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardSnapshotWriteService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class CidadaoPainelService {

    private static final TypeReference<List<CidadaoPendenciaDto>> TR_PEND = new TypeReference<>() {};
    private static final TypeReference<List<CidadaoProximoEventoDto>> TR_EVT = new TypeReference<>() {};
    private static final TypeReference<List<CidadaoProcessoCardDto>> TR_REC = new TypeReference<>() {};
    private static final TypeReference<List<CidadaoWidgetDto>> TR_WID = new TypeReference<>() {};

    private final CurrentUserService currentUser;
    private final CidadaoDashboardSnapshotRepository snapshotRepo;
    private final CidadaoDashboardSnapshotWriteService writer;
    private final OutboxPublisher outbox;
    private final ObjectMapper mapper;
    private final UsuarioAvatarRepository avatarRepo;
    private final GovBrOidcProperties govbr;

    public CidadaoPainelService(
            CurrentUserService currentUser,
            CidadaoDashboardSnapshotRepository snapshotRepo,
            CidadaoDashboardSnapshotWriteService writer,
            OutboxPublisher outbox,
            ObjectMapper mapper,
            UsuarioAvatarRepository avatarRepo,
            GovBrOidcProperties govbr) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.snapshotRepo = Objects.requireNonNull(snapshotRepo);
        this.writer = Objects.requireNonNull(writer);
        this.outbox = Objects.requireNonNull(outbox);
        this.mapper = Objects.requireNonNull(mapper);
        this.avatarRepo = Objects.requireNonNull(avatarRepo);
        this.govbr = Objects.requireNonNull(govbr);
    }

    public CidadaoPainelBootstrapResponse painelBootstrap() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelBootstrapCached(u.getId());
    }


    public java.util.List<CidadaoPendenciaDto> listarPendencias() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelCompletoCached(u.getId()).pendencias();
    }

    public java.util.List<CidadaoProximoEventoDto> listarProximosEventos() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelCompletoCached(u.getId()).proximosEventos();
    }

    public java.util.List<CidadaoProcessoCardDto> listarProcessosRecentes() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelCompletoCached(u.getId()).recentes();
    }

    public CidadaoGovHubDto govHub() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelCompletoCached(u.getId()).govHub();
    }

    public CidadaoPainelWidgetsResponse painelWidgets() {
        Usuario u = currentUser.getRequired();
        if (u.getTipoUsuario() != TipoUsuario.CIDADAO) throw new IllegalStateException("role");
        return painelWidgetsCached(u.getId());
    }

    @Cacheable(cacheNames = "cidadaoPainelBootstrap", key = "#cidadaoUserId")
    public CidadaoPainelBootstrapResponse painelBootstrapCached(Long cidadaoUserId) {
        Usuario u = currentUser.getRequired();
        return toBootstrap(painelCompletoCached(cidadaoUserId), u);
    }

    @Cacheable(cacheNames = "cidadaoPainelWidgets", key = "#cidadaoUserId")
    public CidadaoPainelWidgetsResponse painelWidgetsCached(Long cidadaoUserId) {
        return toWidgets(painelCompletoCached(cidadaoUserId));
    }

    @Cacheable(cacheNames = "cidadaoPainelCompleto", key = "#cidadaoUserId")
    public CidadaoPainelResponse painelCompletoCached(Long cidadaoUserId) {
        Usuario u = currentUser.getRequired();
        if (!Objects.equals(u.getId(), cidadaoUserId)) {
            throw new IllegalStateException("user");
        }
        CidadaoDashboardSnapshot snap = snapshotRepo.findById(cidadaoUserId).orElse(null);
        if (snap == null) {
            snap = writer.refreshForUser(u);
        } else if (isStale(snap.getUpdatedAt(), 45)) {
            enqueueRefresh(u.getCpf());
        }
        return toPainel(snap, u);
    }

    private boolean isStale(Instant at, long seconds) {
        if (at == null) return true;
        Instant now = Instant.now();
        return at.plusSeconds(seconds).isBefore(now);
    }

    private void enqueueRefresh(String cpf) {
        if (cpf == null || cpf.isBlank()) return;
        long t = Instant.now().getEpochSecond();
        String routingKey = "CIDASH:CPF:" + cpf.trim();
        String minute = String.valueOf(t / 60L);
        outbox.enqueue(
                routingKey,
                OutboxPublisher.EVT_CIDADAO_DASHBOARD_REFRESH,
                new CidadaoDashboardRefreshRequest(cpf.trim(), t),
                Map.of("topic", routingKey),
                "dashRefresh:" + cpf.trim() + ":" + minute,
                "CIDASH",
                cpf.trim()
        );
    }

    private CidadaoPainelResponse toPainel(CidadaoDashboardSnapshot snap, Usuario u) {
        LocalDateTime generatedAt = snap.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(snap.getUpdatedAt(), ZoneOffset.UTC)
                : LocalDateTime.now();
        CidadaoPainelBadgesDto badges = read(snap.getBadgesJson(), CidadaoPainelBadgesDto.class, new CidadaoPainelBadgesDto(0, 0, 0, 0));
        List<CidadaoPendenciaDto> pendencias = readList(snap.getPendenciasJson(), TR_PEND);
        List<CidadaoProximoEventoDto> proximos = readList(snap.getProximosEventosJson(), TR_EVT);
        List<CidadaoProcessoCardDto> recentes = readList(snap.getRecentesJson(), TR_REC);
        List<CidadaoWidgetDto> widgets = readList(snap.getWidgetsJson(), TR_WID);
        CidadaoGovHubDto govHub = read(snap.getGovHubJson(), CidadaoGovHubDto.class, new CidadaoGovHubDto("BR", List.of()));
        UsuarioAvatar avatar = avatarRepo.findByUsuarioId(u.getId()).orElse(null);
        String avatarUrl = avatar != null ? "/api/v1/cidadao/perfil/avatar" : null;
        String avatarEtag = avatar != null ? avatar.getSha256() : null;
        boolean govEnabled = govbr.enabled();
        String govStart = govEnabled ? "/api/v1/cidadao/govbr/link/start" : null;
        CidadaoPerfilResumoDto perfil = new CidadaoPerfilResumoDto(
                u.getNome(),
                maskCpf(u.getCpf()),
                u.getUf(),
                avatarUrl,
                avatarEtag,
                govEnabled,
                govStart
        );
        return new CidadaoPainelResponse(
                generatedAt,
                perfil,
                badges,
                widgets,
                pendencias,
                proximos,
                recentes,
                govHub,
                "/api/v1/ui/legend",
                defaultLinks(),
                navLinks()
        );
    }

    private CidadaoPainelBootstrapResponse toBootstrap(CidadaoPainelResponse painel, Usuario u) {
        String etag = "W/\"cidp-" + u.getId() + "-" + Integer.toHexString(Objects.hash(painel.generatedAt(), painel.perfil(), painel.badges(), painel.pendencias(), painel.proximosEventos(), painel.recentes(), painel.govHub())) + "\"";
        return new CidadaoPainelBootstrapResponse(
                etag,
                painel.generatedAt(),
                painel.perfil(),
                painel.badges(),
                painel.pendencias(),
                painel.proximosEventos(),
                painel.recentes(),
                painel.govHub(),
                painel.uiLegendUrl(),
                painel.links(),
                painel.nav()
        );
    }

    private CidadaoPainelWidgetsResponse toWidgets(CidadaoPainelResponse painel) {
        String etag = "W/\"cidw-" + Integer.toHexString(Objects.hash(painel.generatedAt(), painel.widgets())) + "\"";
        return new CidadaoPainelWidgetsResponse(etag, painel.generatedAt(), painel.widgets());
    }

    private <T> T read(String json, Class<T> type, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> ref) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<T> out = mapper.readValue(json, ref);
            return out == null ? List.of() : List.copyOf(out);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }

    private static CidadaoAreaNavLinks navLinks() {
        return new CidadaoAreaNavLinks(
                "/api/v1/cidadao/painel",
                "/api/v1/cidadao/carteira/catalogo",
                "/api/v1/cidadao/pendencias",
                "/api/v1/cidadao/meus-processos",
                "/api/v1/cidadao/pasta/processos",
                "/api/v1/cidadao/audiencias",
                "/api/v1/cidadao/live/stream",
                "/api/v1/cidadao/gov-hub",
                "/api/v1/public/consultas-publicas/search",
                "/api/v1/public/consultas-publicas/pages/{pageId}"
        );
    }

    private static String maskCpf(String cpf) {
      if (cpf == null) return null;
      String d = cpf.replaceAll("\\D", "");
      if (d.length() != 11) return "***";
      return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }
}
