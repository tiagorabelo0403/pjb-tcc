package com.tcc.pjb.backend.core.processo.anomalia.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaItem;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAntifraudeOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoAntifraudeOperacionalApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService;
    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final SecurityAlertService securityAlertService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoAntifraudeOperacionalApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                           UsuarioRepository usuarioRepository,
                                                           ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService,
                                                           ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                           SecurityAlertService securityAlertService,
                                                           OutboxPublisher outboxPublisher,
                                                           ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                           ObjectProvider<AuditLedgerService> auditLedgerServiceProvider,
                                                           ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoAnomaliaMalhaApplicationService = Objects.requireNonNull(processoAnomaliaMalhaApplicationService);
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.securityAlertService = Objects.requireNonNull(securityAlertService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    @Transactional
    public ProcessoAntifraudeOperacionalAggregate acionar(Long processoId) {
        Processo processo = processoRuntimeResolver.resolver(processoId).processo();
        ProcessoMalhaParallelExecutor.Dupla<ProcessoAnomaliaMalhaAggregate, ProcessoMalhaNacionalAggregate> consolidado = processoMalhaParallelExecutor.executar2(
                "malha-antifraude-operacional",
                () -> processoAnomaliaMalhaApplicationService.detalhar(processoId),
                () -> processoMalhaNacionalApplicationService.detalhar(processoId)
        );
        ProcessoAnomaliaMalhaAggregate anomalia = consolidado.primeiro();
        ProcessoMalhaNacionalAggregate malha = consolidado.segundo();
        List<ProcessoAnomaliaMalhaItem> itensAcionados = itensAcionados(anomalia, malha);
        List<Usuario> destinatarios = resolveDestinatarios(processo, anomalia, itensAcionados);
        ArrayList<String> acoesExecutadas = new ArrayList<>();
        ArrayList<String> destinatarioKeys = new ArrayList<>();
        for (Usuario usuario : destinatarios) {
            securityAlertService.create(
                    usuario,
                    "PROCESSO_MALHA_ANTIFRAUDE",
                    titulo(anomalia, processo),
                    detalhe(anomalia, malha, itensAcionados, processo),
                    "PROCESSO:" + processoId,
                    Math.max(anomalia.scoreGlobal(), severityBoost(usuario, itensAcionados))
            );
            acoesExecutadas.add("SECURITY_ALERT:" + usuario.getId());
            destinatarioKeys.add(destinatario(usuario));
        }
        if (destinatarios.isEmpty() && (anomalia.exigeEscalonamento() || malha.travaDistribuicaoOuFluxo())) {
            securityAlertService.create(
                    null,
                    "PROCESSO_MALHA_ANTIFRAUDE",
                    titulo(anomalia, processo),
                    detalhe(anomalia, malha, itensAcionados, processo),
                    "PROCESSO:" + processoId,
                    anomalia.scoreGlobal()
            );
            acoesExecutadas.add("SECURITY_ALERT:SISTEMICO");
        }
        Map<String, Object> payload = payload(processo, anomalia, malha, itensAcionados, destinatarioKeys, acoesExecutadas);
        outboxPublisher.enqueue(
                "processo.malha.antifraude",
                "PROCESSO_MALHA_ANTIFRAUDE_ACIONADO",
                payload,
                Map.of("processoId", processoId, "scoreGlobal", anomalia.scoreGlobal(), "nivelGlobal", anomalia.nivelGlobal()),
                "processo-malha-antifraude:" + processoId + ':' + Hashes.sha256Hex(payload.toString()),
                "PROCESSO",
                String.valueOf(processoId)
        );
        acoesExecutadas.add("OUTBOX:PROCESSO_MALHA_ANTIFRAUDE_ACIONADO");
        registrarExplicacao(processo, anomalia, malha, itensAcionados, destinatarioKeys);
        registrarLedger(processoId, payload);
        return new ProcessoAntifraudeOperacionalAggregate(
                processoId,
                processo.getNumero(),
                anomalia.nivelGlobal(),
                anomalia.scoreGlobal(),
                List.copyOf(destinatarioKeys),
                itensAcionados,
                List.copyOf(acoesExecutadas),
                fundamentos(anomalia, malha, itensAcionados),
                Instant.now()
        );
    }

    private List<ProcessoAnomaliaMalhaItem> itensAcionados(ProcessoAnomaliaMalhaAggregate anomalia, ProcessoMalhaNacionalAggregate malha) {
        return anomalia.itens().stream()
                .filter(item -> item.exigeEscalonamento() || item.score() >= 55 || malha.travaDistribuicaoOuFluxo())
                .limit(8)
                .toList();
    }

    private List<Usuario> resolveDestinatarios(Processo processo, ProcessoAnomaliaMalhaAggregate anomalia, List<ProcessoAnomaliaMalhaItem> itensAcionados) {
        LinkedHashMap<Long, Usuario> usuarios = new LinkedHashMap<>();
        Optional.ofNullable(processo.getUsuario()).filter(usuario -> usuario.getId() != null).ifPresent(usuario -> usuarios.put(usuario.getId(), usuario));
        resolveByCpf(processo.getParteAutoraCpf()).ifPresent(usuario -> usuarios.putIfAbsent(usuario.getId(), usuario));
        resolveByCpf(processo.getParteReuCpf()).ifPresent(usuario -> usuarios.putIfAbsent(usuario.getId(), usuario));
        if (anomalia.exigeEscalonamento() || itensAcionados.stream().anyMatch(item -> "CRITICO".equals(item.nivel()) || "ALTO".equals(item.nivel()))) {
            usuarioRepository.findByTipoUsuario(TipoUsuario.ADMINISTRADOR)
                    .stream()
                    .filter(Usuario::isAtivo)
                    .limit(2)
                    .forEach(usuario -> usuarios.putIfAbsent(usuario.getId(), usuario));
        }
        return List.copyOf(usuarios.values());
    }

    private Optional<Usuario> resolveByCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return Optional.empty();
        }
        String digits = cpf.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findByCpf(digits).filter(Usuario::isAtivo);
    }

    private String titulo(ProcessoAnomaliaMalhaAggregate anomalia, Processo processo) {
        if (anomalia.itens().isEmpty()) {
            return "Malha antifraude acionada para o processo " + processo.getNumero();
        }
        return anomalia.itens().getFirst().titulo();
    }

    private String detalhe(ProcessoAnomaliaMalhaAggregate anomalia,
                           ProcessoMalhaNacionalAggregate malha,
                           List<ProcessoAnomaliaMalhaItem> itensAcionados,
                           Processo processo) {
        String principal = itensAcionados.isEmpty() ? "Sem item específico" : itensAcionados.getFirst().detalhe();
        return "Processo=" + processo.getNumero()
                + "; score=" + anomalia.scoreGlobal()
                + "; nivel=" + anomalia.nivelGlobal()
                + "; bloqueios=" + malha.totalBloqueios()
                + "; hotspots=" + String.join(",", malha.hotspots())
                + "; principal=" + principal;
    }

    private int severityBoost(Usuario usuario, List<ProcessoAnomaliaMalhaItem> itensAcionados) {
        int base = itensAcionados.stream().mapToInt(ProcessoAnomaliaMalhaItem::score).max().orElse(0);
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isPerfilCritico()) {
            return Math.min(100, base + 10);
        }
        return base;
    }

    private String destinatario(Usuario usuario) {
        String identidade = usuario.getEmail() != null && !usuario.getEmail().isBlank() ? usuario.getEmail() : usuario.getCpf();
        return usuario.getTipoUsuario().name() + ':' + identidade;
    }

    private Map<String, Object> payload(Processo processo,
                                        ProcessoAnomaliaMalhaAggregate anomalia,
                                        ProcessoMalhaNacionalAggregate malha,
                                        List<ProcessoAnomaliaMalhaItem> itensAcionados,
                                        List<String> destinatarios,
                                        List<String> acoesExecutadas) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processo.getId());
        out.put("numeroProcesso", processo.getNumero());
        out.put("nivelGlobal", anomalia.nivelGlobal());
        out.put("scoreGlobal", anomalia.scoreGlobal());
        out.put("travaDistribuicaoOuFluxo", malha.travaDistribuicaoOuFluxo());
        out.put("totalBloqueios", malha.totalBloqueios());
        out.put("hotspots", malha.hotspots());
        out.put("destinatarios", destinatarios);
        out.put("itensAcionados", itensAcionados.stream().map(item -> Map.of(
                "codigo", item.codigo(),
                "categoria", item.categoria(),
                "nivel", item.nivel(),
                "score", item.score(),
                "titulo", item.titulo()
        )).toList());
        out.put("acoesExecutadas", acoesExecutadas);
        return Collections.unmodifiableMap(out);
    }

    private void registrarExplicacao(Processo processo,
                                     ProcessoAnomaliaMalhaAggregate anomalia,
                                     ProcessoMalhaNacionalAggregate malha,
                                     List<ProcessoAnomaliaMalhaItem> itensAcionados,
                                     List<String> destinatarios) {
        if (decisionTraceService == null) {
            return;
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(anomalia.fundamentos());
        fundamentos.addAll(malha.fundamentos());
        fundamentos.addAll(itensAcionados.stream().flatMap(item -> item.fundamentos().stream()).toList());
        fundamentos.add("Destinatários acionados=" + destinatarios.size());
        decisionTraceService.record(
                "PROCESSO_MALHA_ANTIFRAUDE",
                "PROCESSO",
                String.valueOf(processo.getId()),
                BigDecimal.valueOf(Math.min(1d, anomalia.scoreGlobal() / 100d)),
                itensAcionados.stream().map(item -> item.codigo() + ':' + item.nivel()).toList().toString(),
                List.copyOf(fundamentos).toString(),
                processo.getNumero(),
                anomalia.nivelGlobal() + ':' + anomalia.scoreGlobal(),
                "PJB_PROCESSO_MALHA_ANTIFRAUDE_V1",
                destinatarios.toString()
        );
    }

    private void registrarLedger(Long processoId, Map<String, Object> payload) {
        if (auditLedgerService == null) {
            return;
        }
        auditLedgerService.appendSafely(
                "PROCESSO_MALHA_ANTIFRAUDE_ACIONADO",
                "PROCESSO",
                String.valueOf(processoId),
                Hashes.sha256Hex(payload.toString())
        );
    }

    private List<String> fundamentos(ProcessoAnomaliaMalhaAggregate anomalia,
                                     ProcessoMalhaNacionalAggregate malha,
                                     List<ProcessoAnomaliaMalhaItem> itensAcionados) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(anomalia.fundamentos());
        fundamentos.addAll(malha.fundamentos());
        itensAcionados.forEach(item -> fundamentos.addAll(item.fundamentos()));
        return List.copyOf(fundamentos.stream().limit(80).toList());
    }
}
