package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.MatrizCapacidadeCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryDeadLetterStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalCommunicationHardeningReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalHardeningFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalHardeningSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalAdapter;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalDispatchStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;

@Service
public class InstitutionalCommunicationHardeningApplicationService {

    private static final Duration REPORT_CACHE_TTL = Duration.ofSeconds(20);
    private static final List<String> UFS = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final InstitutionalInboxStateRepository inboxStateRepository;
    private final InstitutionalGateStateRepository gateStateRepository;
    private final InstitutionalDeliveryDeadLetterStateRepository deadLetterStateRepository;
    private final InstitutionalDeliveryJobStateRepository deliveryJobStateRepository;
    private final InstitutionalExternalDispatchStateRepository externalDispatchStateRepository;
    private final MatrizCapacidadeCaixaInstitucionalService matrizCapacidadeCaixaInstitucionalService;
    private final List<InstitutionalExternalAdapter> externalAdapters;
    private final AtomicReference<CachedReport> reportCache = new AtomicReference<>();

    public InstitutionalCommunicationHardeningApplicationService(CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                                                 InstitutionalInboxStateRepository inboxStateRepository,
                                                                 InstitutionalGateStateRepository gateStateRepository,
                                                                 InstitutionalDeliveryDeadLetterStateRepository deadLetterStateRepository,
                                                                 InstitutionalDeliveryJobStateRepository deliveryJobStateRepository,
                                                                 InstitutionalExternalDispatchStateRepository externalDispatchStateRepository,
                                                                 MatrizCapacidadeCaixaInstitucionalService matrizCapacidadeCaixaInstitucionalService,
                                                                 List<InstitutionalExternalAdapter> externalAdapters) {
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService, "catalogoInstitucionalUnificadoService");
        this.inboxStateRepository = Objects.requireNonNull(inboxStateRepository, "inboxStateRepository");
        this.gateStateRepository = Objects.requireNonNull(gateStateRepository, "gateStateRepository");
        this.deadLetterStateRepository = Objects.requireNonNull(deadLetterStateRepository, "deadLetterStateRepository");
        this.deliveryJobStateRepository = Objects.requireNonNull(deliveryJobStateRepository, "deliveryJobStateRepository");
        this.externalDispatchStateRepository = Objects.requireNonNull(externalDispatchStateRepository, "externalDispatchStateRepository");
        this.matrizCapacidadeCaixaInstitucionalService = Objects.requireNonNull(matrizCapacidadeCaixaInstitucionalService, "matrizCapacidadeCaixaInstitucionalService");
        this.externalAdapters = List.copyOf(Objects.requireNonNull(externalAdapters, "externalAdapters"));
    }

    public InstitutionalCommunicationHardeningReport gerarRelatorio() {
        CachedReport cache = reportCache.get();
        if (isFresh(cache)) {
            return cache.report();
        }
        Instant now = Instant.now();
        List<UnidadeInstitucional> unidades = catalogoInstitucionalUnificadoService.listarPorTipo(null);
        List<InstitutionalInboxItem> inbox = inboxStateRepository.findAll();
        List<InstitutionalGateState> gates = gateStateRepository.findAll();
        List<InstitutionalDeliveryJob> jobs = deliveryJobStateRepository.findAll();
        List<InstitutionalExternalDispatch> dispatches = externalDispatchStateRepository.findAll();
        List<InstitutionalHardeningFinding> findings = new ArrayList<>();

        appendUfCoverage(findings, unidades);
        appendExternalCoverage(findings);
        appendCapabilityCoverage(findings);
        appendBacklogFindings(findings, inbox, gates, jobs, dispatches);

        long totalUnidadesAtivas = unidades.stream().filter(UnidadeInstitucional::ativa).count();
        long totalInboxPendentes = inbox.stream().filter(item -> !item.status().isTerminal()).count();
        long totalGatesBloqueando = gates.stream().filter(InstitutionalGateState::bloqueado).count();
        long totalDlq = deadLetterStateRepository.countAll();
        long totalIntegracoesExternasComFalha = dispatches.stream().filter(dispatch -> dispatch.status() == StatusIntegracaoInstitucionalExterna.FALHA_TERMINAL || dispatch.status() == StatusIntegracaoInstitucionalExterna.FALHA_TRANSITORIA).count();
        long totalEntregasEmAberto = jobs.stream().filter(job -> !job.status().isTerminal()).count();
        List<String> canaisCobertos = supportedExternalChannels().stream().map(Enum::name).sorted().toList();
        boolean aprovado = findings.stream().noneMatch(finding -> finding.severity().isBlocking());
        String hash = buildHash(unidades.size(), totalUnidadesAtivas, totalInboxPendentes, totalGatesBloqueando, totalDlq, totalIntegracoesExternasComFalha, totalEntregasEmAberto, canaisCobertos, findings);
        InstitutionalCommunicationHardeningReport report = new InstitutionalCommunicationHardeningReport(
                aprovado,
                unidades.size(),
                totalUnidadesAtivas,
                totalInboxPendentes,
                totalGatesBloqueando,
                totalDlq,
                totalIntegracoesExternasComFalha,
                totalEntregasEmAberto,
                canaisCobertos,
                findings.stream().sorted(Comparator.comparing((InstitutionalHardeningFinding finding) -> finding.severity().ordinal()).reversed().thenComparing(InstitutionalHardeningFinding::code)).toList(),
                now,
                hash
        );
        reportCache.set(new CachedReport(report, Instant.now().plus(REPORT_CACHE_TTL)));
        return report;
    }

    private boolean isFresh(CachedReport cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private void appendUfCoverage(List<InstitutionalHardeningFinding> findings, List<UnidadeInstitucional> unidades) {
        List<DestinatarioInstitucionalKind> requiredKinds = List.of(
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA,
                DestinatarioInstitucionalKind.FAZENDA_PUBLICA
        );
        for (DestinatarioInstitucionalKind kind : requiredKinds) {
            Set<String> covered = unidades.stream()
                    .filter(UnidadeInstitucional::ativa)
                    .filter(unit -> unit.destinatarioKind() == kind)
                    .map(UnidadeInstitucional::uf)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> missing = UFS.stream().filter(uf -> !covered.contains(uf)).toList();
            if (!missing.isEmpty()) {
                findings.add(new InstitutionalHardeningFinding(
                        "UF_COVERAGE_" + kind.name(),
                        kind.isInstituicaoEssencialJustica() ? InstitutionalHardeningSeverity.ERROR : InstitutionalHardeningSeverity.WARN,
                        "Cobertura nacional incompleta para " + kind.name(),
                        List.of("ufsCobertas=" + covered.size(), "ufsFaltantes=" + String.join(",", missing))
                ));
            }
        }
    }

    private void appendExternalCoverage(List<InstitutionalHardeningFinding> findings) {
        Set<CanalComunicacaoInstitucional> supported = supportedExternalChannels();
        EnumSet<CanalComunicacaoInstitucional> required = EnumSet.of(
                CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO,
                CanalComunicacaoInstitucional.DJEN,
                CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL,
                CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL
        );
        List<String> missing = required.stream().filter(channel -> !supported.contains(channel)).map(Enum::name).sorted().toList();
        if (!missing.isEmpty()) {
            findings.add(new InstitutionalHardeningFinding(
                    "EXTERNAL_CHANNEL_COVERAGE",
                    InstitutionalHardeningSeverity.ERROR,
                    "Nem todos os canais jurídicos externos possuem adaptador registrado.",
                    List.of("faltantes=" + String.join(",", missing))
            ));
        }
    }

    private Set<CanalComunicacaoInstitucional> supportedExternalChannels() {
        return externalAdapters.stream()
                .flatMap(adapter -> EnumSet.allOf(CanalComunicacaoInstitucional.class).stream().filter(adapter::supports))
                .filter(CanalComunicacaoInstitucional::isPrincipalJuridico)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void appendCapabilityCoverage(List<InstitutionalHardeningFinding> findings) {
        Map<FuncaoOperacionalInstitucional, Set<CapacidadeCaixaInstitucional>> expected = Map.of(
                FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                EnumSet.of(CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA,
                        CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO),
                FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                EnumSet.allOf(CapacidadeCaixaInstitucional.class)
        );
        expected.forEach((funcao, mandatory) -> {
            Set<CapacidadeCaixaInstitucional> atual = matrizCapacidadeCaixaInstitucionalService.capacidades(funcao);
            List<String> faltantes = mandatory.stream().filter(capacidade -> !atual.contains(capacidade)).map(Enum::name).sorted().toList();
            if (!faltantes.isEmpty()) {
                findings.add(new InstitutionalHardeningFinding(
                        "CAPABILITY_MATRIX_" + funcao.name(),
                        InstitutionalHardeningSeverity.ERROR,
                        "Matriz de capacidade incompleta para " + funcao.name(),
                        List.of("faltantes=" + String.join(",", faltantes))
                ));
            }
        });
    }

    private void appendBacklogFindings(List<InstitutionalHardeningFinding> findings,
                                       List<InstitutionalInboxItem> inbox,
                                       List<InstitutionalGateState> gates,
                                       List<InstitutionalDeliveryJob> jobs,
                                       List<InstitutionalExternalDispatch> dispatches) {
        Instant now = Instant.now();
        long inboxPendentes = inbox.stream().filter(item -> !item.status().isTerminal()).count();
        if (inboxPendentes > 0L) {
            long stale = inbox.stream().filter(item -> !item.status().isTerminal()).filter(item -> Duration.between(item.updatedAt(), now).toHours() >= 24).count();
            InstitutionalHardeningSeverity severity = stale > 0 ? InstitutionalHardeningSeverity.ERROR : InstitutionalHardeningSeverity.WARN;
            findings.add(new InstitutionalHardeningFinding(
                    "INBOX_BACKLOG",
                    severity,
                    "Há comunicações institucionais pendentes na inbox.",
                    List.of("pendentes=" + inboxPendentes, "stale24h=" + stale)
            ));
        }
        long gatesBloqueando = gates.stream().filter(InstitutionalGateState::bloqueado).count();
        if (gatesBloqueando > 0L) {
            findings.add(new InstitutionalHardeningFinding(
                    "GATE_BLOCKING",
                    InstitutionalHardeningSeverity.WARN,
                    "Existem gates institucionais ainda bloqueando fluxo processual.",
                    List.of("bloqueando=" + gatesBloqueando)
            ));
        }
        long entregasAbertas = jobs.stream().filter(job -> !job.status().isTerminal()).count();
        if (entregasAbertas > 0L) {
            long retries = jobs.stream().filter(job -> job.status() == StatusEntregaInstitucional.AGUARDANDO_RETRY).count();
            findings.add(new InstitutionalHardeningFinding(
                    "DELIVERY_BACKLOG",
                    retries > 0 ? InstitutionalHardeningSeverity.WARN : InstitutionalHardeningSeverity.INFO,
                    "Existem entregas institucionais ainda não concluídas.",
                    List.of("abertas=" + entregasAbertas, "aguardandoRetry=" + retries)
            ));
        }
        long falhasExternas = dispatches.stream().filter(dispatch -> dispatch.status() == StatusIntegracaoInstitucionalExterna.FALHA_TRANSITORIA || dispatch.status() == StatusIntegracaoInstitucionalExterna.FALHA_TERMINAL).count();
        if (falhasExternas > 0L) {
            findings.add(new InstitutionalHardeningFinding(
                    "EXTERNAL_FAILURE_BACKLOG",
                    InstitutionalHardeningSeverity.WARN,
                    "Há integrações externas com falha registradas.",
                    List.of("falhasExternas=" + falhasExternas)
            ));
        }
        int dlq = Math.toIntExact(deadLetterStateRepository.countAll());
        if (dlq > 0) {
            findings.add(new InstitutionalHardeningFinding(
                    "DELIVERY_DLQ",
                    InstitutionalHardeningSeverity.ERROR,
                    "Há itens movidos para DLQ institucional aguardando saneamento.",
                    List.of("dlq=" + dlq)
            ));
        }
    }

    private String buildHash(long totalUnidades,
                             long totalUnidadesAtivas,
                             long totalInboxPendentes,
                             long totalGatesBloqueando,
                             long totalDlq,
                             long totalIntegracoesExternasComFalha,
                             long totalEntregasEmAberto,
                             List<String> canaisCobertos,
                             List<InstitutionalHardeningFinding> findings) {
        String payload = totalUnidades + "|" + totalUnidadesAtivas + "|" + totalInboxPendentes + "|" + totalGatesBloqueando + "|" + totalDlq + "|"
                + totalIntegracoesExternasComFalha + "|" + totalEntregasEmAberto + "|" + String.join(",", canaisCobertos) + "|"
                + findings.stream().map(f -> f.code() + ":" + f.severity().name() + ":" + f.message()).collect(Collectors.joining("|"));
        return Hashes.sha256Hex(payload);
    }

    private record CachedReport(InstitutionalCommunicationHardeningReport report, Instant expiresAt) { }
}
