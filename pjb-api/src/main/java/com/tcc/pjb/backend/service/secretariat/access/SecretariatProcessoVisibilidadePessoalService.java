package com.tcc.pjb.backend.service.secretariat.access;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceFieldResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.cidadao.ProcessoVisibilidadePessoalOverride;
import com.tcc.pjb.backend.service.outbox.FederatedOutboxDispatchService;
import com.tcc.pjb.backend.service.processual.postarchive.tombstone.ProcessoTombstonePolicyEngine;
import com.tcc.pjb.backend.service.processual.postarchive.tombstone.ProcessoTombstonePolicyReport;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.cidadao.ProcessoVisibilidadePessoalOverrideRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatProcessoVisibilidadePessoalService {

    public static final String ESCOPO_CIDADAO_PAINEL = "CIDADAO_PAINEL_PESSOAL";

    private final ProcessoRepository processoRepository;
    private final ProcessoVisibilidadePessoalOverrideRepository overrideRepository;
    private final CurrentUserService currentUserService;
    private final ProcessoTombstonePolicyEngine processoTombstonePolicyEngine;
    private final FederatedOutboxDispatchService federatedOutboxDispatchService;
    private final AuditLedgerService auditLedgerService;

    public SecretariatProcessoVisibilidadePessoalService(ProcessoRepository processoRepository,
                                                         ProcessoVisibilidadePessoalOverrideRepository overrideRepository,
                                                         CurrentUserService currentUserService,
                                                         ProcessoTombstonePolicyEngine processoTombstonePolicyEngine,
                                                         FederatedOutboxDispatchService federatedOutboxDispatchService,
                                                         AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.overrideRepository = Objects.requireNonNull(overrideRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoTombstonePolicyEngine = Objects.requireNonNull(processoTombstonePolicyEngine);
        this.federatedOutboxDispatchService = Objects.requireNonNull(federatedOutboxDispatchService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public SurfaceSnapshotResponse snapshot(Long processoId) {
        Processo processo = resolveProcesso(processoId);
        String nupn = resolveNupn(processo);
        ProcessoVisibilidadePessoalOverride current = overrideRepository.findByNupnAndEscopo(nupn, ESCOPO_CIDADAO_PAINEL).orElse(null);
        Instant now = Instant.now();
        boolean ativa = current != null && current.ativa(now);
        ProcessoTombstonePolicyReport tombstone = processoTombstonePolicyEngine.evaluate(processo, null, current, now);
        List<SurfaceFieldResponse> fields = new ArrayList<>();
        fields.add(new SurfaceFieldResponse("processoId", processo.getId()));
        fields.add(new SurfaceFieldResponse("nupn", nupn));
        fields.add(new SurfaceFieldResponse("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null));
        fields.add(new SurfaceFieldResponse("visibilidadeControladaAtiva", ativa));
        fields.add(new SurfaceFieldResponse("visivelPainelPessoal", ativa && current.isVisivel()));
        fields.add(new SurfaceFieldResponse("fundamento", current != null ? current.getFundamento() : null));
        fields.add(new SurfaceFieldResponse("concedidoPorUsuarioId", current != null ? current.getConcedidoPorUsuarioId() : null));
        fields.add(new SurfaceFieldResponse("concedidoPorPerfil", current != null ? current.getConcedidoPorPerfil() : null));
        fields.add(new SurfaceFieldResponse("expiraEm", current != null ? current.getExpiraEm() : null));
        fields.add(new SurfaceFieldResponse("atualizadoEm", current != null ? current.getAtualizadoEm() : null));
        fields.add(new SurfaceFieldResponse("tombstone", tombstone.toMap()));
        return new SurfaceSnapshotResponse("SECRETARIA_VISIBILIDADE_PESSOAL", fields);
    }

    @Transactional
    public SurfaceActionResponse definir(Long processoId, boolean visivel, String fundamento, Integer diasValidade) {
        Processo processo = resolveProcesso(processoId);
        String nupn = resolveNupn(processo);
        Usuario operador = currentUserService.getRequired();
        ProcessoVisibilidadePessoalOverride override = overrideRepository.findByNupnAndEscopo(nupn, ESCOPO_CIDADAO_PAINEL)
                .orElseGet(() -> ProcessoVisibilidadePessoalOverride.builder()
                        .nupn(nupn)
                        .processoLocalId(processo.getId())
                        .escopo(ESCOPO_CIDADAO_PAINEL)
                        .build());
        override.setVisivel(visivel);
        override.setFundamento(normalizeFundamento(fundamento));
        override.setConcedidoPorUsuarioId(operador.getId());
        override.setConcedidoPorPerfil(resolvePerfil(operador));
        override.setExpiraEm(resolveExpiry(visivel, diasValidade));
        ProcessoVisibilidadePessoalOverride persisted = overrideRepository.save(override);
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("overrideId", persisted.getId());
        payload.put("fundamento", persisted.getFundamento());
        payload.put("expiraEm", persisted.getExpiraEm());
        payload.put("processoId", processo.getId());
        payload.put("nupn", nupn);
        federatedOutboxDispatchService.dispatch(
                "secretariat-processo-visibilidade",
                "PROCESSO_VISIBILIDADE_PESSOAL_OVERRIDE_SET",
                "PJB",
                processo.getTribunalCodigoRoteado(),
                "PROCESSO",
                String.valueOf(processo.getId()),
                nupn,
                1L,
                java.util.Map.of(
                        "escopo", ESCOPO_CIDADAO_PAINEL,
                        "visivel", persisted.isVisivel(),
                        "concedidoPorUsuarioId", persisted.getConcedidoPorUsuarioId(),
                        "concedidoPorPerfil", persisted.getConcedidoPorPerfil()
                ),
                payload
        );
        auditLedgerService.appendSafely("PROCESSO_VISIBILIDADE_PESSOAL_OVERRIDE_SET", "PROCESSO", String.valueOf(processo.getId()), null, nupn);
        List<SurfaceFieldResponse> fields = new ArrayList<>();
        fields.add(new SurfaceFieldResponse("processoId", processo.getId()));
        fields.add(new SurfaceFieldResponse("nupn", nupn));
        fields.add(new SurfaceFieldResponse("visivelPainelPessoal", persisted.isVisivel()));
        fields.add(new SurfaceFieldResponse("fundamento", persisted.getFundamento()));
        fields.add(new SurfaceFieldResponse("expiraEm", persisted.getExpiraEm()));
        fields.add(new SurfaceFieldResponse("concedidoPorUsuarioId", persisted.getConcedidoPorUsuarioId()));
        fields.add(new SurfaceFieldResponse("concedidoPorPerfil", persisted.getConcedidoPorPerfil()));
        fields.add(new SurfaceFieldResponse("overrideId", persisted.getId()));
        return new SurfaceActionResponse(
                "SECRETARIA_VISIBILIDADE_PESSOAL",
                visivel ? "REEXPOR_PAINEL_PESSOAL" : "OCULTAR_PAINEL_PESSOAL",
                processo.getId(),
                "OK",
                fields
        );
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId obrigatório");
        }
        return processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado para visibilidade pessoal controlada"));
    }

    private static String resolveNupn(Processo processo) {
        String nupn = processo.getNumeroUnificado();
        if (nupn == null || nupn.isBlank()) {
            nupn = processo.getNumeroProcesso();
        }
        if (nupn == null || nupn.isBlank()) {
            throw new IllegalArgumentException("Processo sem número nacional apto para visibilidade pessoal controlada");
        }
        return nupn.trim();
    }

    private static String resolvePerfil(Usuario usuario) {
        if (usuario.getTipoUsuario() != null) {
            return usuario.getTipoUsuario().name();
        }
        if (usuario.getPerfil() != null && !usuario.getPerfil().isBlank()) {
            return usuario.getPerfil().trim().toUpperCase(Locale.ROOT);
        }
        return "OPERADOR";
    }

    private static String normalizeFundamento(String fundamento) {
        String value = fundamento == null ? "" : fundamento.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Fundamento obrigatório para controle de visibilidade pessoal");
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private static Instant resolveExpiry(boolean visivel, Integer diasValidade) {
        if (!visivel) {
            return null;
        }
        if (diasValidade == null || diasValidade <= 0) {
            return null;
        }
        return Instant.now().plus(diasValidade.longValue(), ChronoUnit.DAYS);
    }
}
