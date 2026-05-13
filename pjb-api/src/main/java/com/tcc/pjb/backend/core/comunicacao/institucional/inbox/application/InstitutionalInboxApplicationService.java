package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.EstruturaCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.application.InstitutionalCommunicationGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.AlvoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class InstitutionalInboxApplicationService {

    private final InstitutionalInboxStateRepository repository;
    private final VinculoUsuarioCaixaInstitucionalResolver vinculoResolver;
    private final AutorizacaoCaixaInstitucionalService autorizacaoService;
    private final CurrentUserService currentUserService;
    private final InstitutionalCommunicationAuditApplicationService auditService;
    private final InstitutionalCommunicationGateApplicationService gateService;
    private final OutboxPublisher outboxPublisher;
    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;

    public InstitutionalInboxApplicationService(InstitutionalInboxStateRepository repository,
                                                VinculoUsuarioCaixaInstitucionalResolver vinculoResolver,
                                                AutorizacaoCaixaInstitucionalService autorizacaoService,
                                                CurrentUserService currentUserService,
                                                InstitutionalCommunicationAuditApplicationService auditService,
                                                InstitutionalCommunicationGateApplicationService gateService,
                                                OutboxPublisher outboxPublisher,
                                                CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                                EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService) {
        this.repository = Objects.requireNonNull(repository);
        this.vinculoResolver = Objects.requireNonNull(vinculoResolver);
        this.autorizacaoService = Objects.requireNonNull(autorizacaoService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditService = Objects.requireNonNull(auditService);
        this.gateService = Objects.requireNonNull(gateService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
    }

    @Transactional
    public InstitutionalInboxItem disponibilizar(Processo processo,
                                                 CitacaoIntimacaoEngine.ExpedicaoResponse response,
                                                 ResolucaoRoteamentoInstitucionalResult roteamento) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(roteamento, "roteamento");
        Instant base = response.expedidaEm() == null ? Instant.now() : response.expedidaEm();
        AlvoInstitucional alvo = roteamento.alvo();
        List<String> justificativas = new ArrayList<>(roteamento.justificativas());
        justificativas.add("expedicaoUuid=" + response.expedicaoUuid());
        InstitutionalInboxItem item = new InstitutionalInboxItem(
                UUID.nameUUIDFromBytes((response.expedicaoUuid() + "|INBOX").getBytes(StandardCharsets.UTF_8)).toString(),
                response.expedicaoUuid(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado()),
                alvo.unidade().codigo(),
                alvo.unidade().sigla(),
                alvo.destinatarioKind(),
                alvo.papelProcessual(),
                roteamento.tipoComunicacaoEfetiva(),
                alvo.caixa().codigo(),
                alvo.caixa().codigo(),
                roteamento.planoEntrega().canalPrincipal().canal().name(),
                StatusComunicacaoInstitucional.DISPONIBILIZADA,
                roteamento.gateCode(),
                roteamento.bloqueiaFluxo(),
                null,
                null,
                base,
                null,
                null,
                null,
                base.plus(roteamento.slaCienciaHoras(), ChronoUnit.HOURS),
                base.plus(roteamento.slaRespostaHoras(), ChronoUnit.HOURS),
                base,
                justificativas,
                hash(response.expedicaoUuid(), alvo.unidade().codigo(), alvo.caixa().codigo(), StatusComunicacaoInstitucional.DISPONIBILIZADA, base)
        );
        repository.save(item);
        auditService.registrarDisponibilizacao(item, "Roteamento institucional consolidado na caixa inicial.");
        gateService.criarSeNecessario(item);
        emitDomainEvent("INSTITUTIONAL_INBOX_AVAILABLE", item, "disponibilizada");
        return item;
    }

    @Transactional(readOnly = true)
    public List<InstitutionalInboxItem> listarMinhasCaixas(StatusComunicacaoInstitucional status, Long processoId) {
        currentUserService.getRequired();
        return selectItems(processoId).stream()
                .filter(item -> status == null || item.status() == status)
                .filter(item -> autorizacaoService.autorizar(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), CapacidadeCaixaInstitucional.VISUALIZAR).autorizado())
                .sorted(Comparator.comparing(InstitutionalInboxItem::updatedAt).reversed())
                .toList();
    }

    @Transactional
    public InstitutionalInboxActionResult receber(String expedicaoUuid, String detalhe) {
        InstitutionalInboxItem atual = loadVisible(expedicaoUuid);
        autorizacaoService.require(atual.expedicaoUuid(), atual.unidadeCodigo(), atual.caixaCodigoAtual(), CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO);
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        List<String> justificativas = append(atual.justificativas(), detalhe, "recebimento_institucional");
        InstitutionalInboxItem updated = atual.withRecebimento(actor.getId(), now, hash(atual.expedicaoUuid(), atual.unidadeCodigo(), atual.caixaCodigoAtual(), StatusComunicacaoInstitucional.RECEBIDA, now), justificativas);
        repository.save(updated);
        auditService.registrarRecebimento(updated, actor, detalhe);
        emitDomainEvent("INSTITUTIONAL_INBOX_RECEIVED", updated, detalhe);
        return toActionResult(updated, gateService.consultarPorExpedicao(updated.expedicaoUuid()).map(g -> g.status()).orElse(null));
    }

    @Transactional
    public InstitutionalInboxActionResult redistribuir(String expedicaoUuid, String caixaDestinoCodigo, String detalhe) {
        InstitutionalInboxItem atual = loadVisible(expedicaoUuid);
        autorizacaoService.require(atual.expedicaoUuid(), atual.unidadeCodigo(), atual.caixaCodigoAtual(), CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE);
        validarCaixaDestino(atual.unidadeCodigo(), caixaDestinoCodigo);
        Usuario actor = currentUserService.getRequired();
        String origem = atual.caixaCodigoAtual();
        Instant now = Instant.now();
        List<String> justificativas = append(atual.justificativas(), detalhe, "redistribuicao_interna=" + caixaDestinoCodigo);
        InstitutionalInboxItem updated = atual.withRedistribuicao(caixaDestinoCodigo, actor.getId(), now, hash(atual.expedicaoUuid(), atual.unidadeCodigo(), caixaDestinoCodigo, StatusComunicacaoInstitucional.DISPONIBILIZADA, now), justificativas);
        repository.save(updated);
        auditService.registrarRedistribuicao(updated, actor, origem, detalhe);
        emitDomainEvent("INSTITUTIONAL_INBOX_REDISTRIBUTED", updated, detalhe);
        return toActionResult(updated, gateService.consultarPorExpedicao(updated.expedicaoUuid()).map(g -> g.status()).orElse(null));
    }

    @Transactional
    public InstitutionalInboxActionResult certificarCiencia(String expedicaoUuid, String detalhe) {
        InstitutionalInboxItem atual = loadVisible(expedicaoUuid);
        requireAny(atual, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA, CapacidadeCaixaInstitucional.DAR_CIENCIA);
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        List<String> justificativas = append(atual.justificativas(), detalhe, "ciencia_certificada");
        InstitutionalInboxItem updated = atual.withCiencia(actor.getId(), now, hash(atual.expedicaoUuid(), atual.unidadeCodigo(), atual.caixaCodigoAtual(), StatusComunicacaoInstitucional.CIENTIFICADA, now), justificativas);
        repository.save(updated);
        auditService.registrarCiencia(updated, actor, detalhe);
        InstitutionalGateStatus gateStatus = gateService.marcarCiencia(updated, actor, detalhe).map(g -> g.status()).orElse(null);
        emitDomainEvent("INSTITUTIONAL_INBOX_SCIENCE", updated, detalhe);
        return toActionResult(updated, gateStatus);
    }

    @Transactional
    public InstitutionalInboxActionResult cumprir(String expedicaoUuid, String detalhe) {
        InstitutionalInboxItem atual = loadVisible(expedicaoUuid);
        requireAny(atual, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO);
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        List<String> justificativas = append(atual.justificativas(), detalhe, "cumprimento_institucional");
        InstitutionalInboxItem updated = atual.withCumprimento(actor.getId(), now, hash(atual.expedicaoUuid(), atual.unidadeCodigo(), atual.caixaCodigoAtual(), StatusComunicacaoInstitucional.CUMPRIDA, now), justificativas);
        repository.save(updated);
        auditService.registrarCumprimento(updated, actor, detalhe);
        InstitutionalGateStatus gateStatus = gateService.marcarCumprimento(updated, actor, detalhe).map(g -> g.status()).orElse(InstitutionalGateStatus.LIBERADO);
        emitDomainEvent("INSTITUTIONAL_INBOX_COMPLETED", updated, detalhe);
        return toActionResult(updated, gateStatus);
    }

    private InstitutionalInboxActionResult toActionResult(InstitutionalInboxItem item, InstitutionalGateStatus gateStatus) {
        InstitutionalGateStatus resolved = gateStatus == null ? gateService.consultarPorExpedicao(item.expedicaoUuid()).map(g -> g.status()).orElse(null) : gateStatus;
        return new InstitutionalInboxActionResult(
                item.expedicaoUuid(),
                item.unidadeCodigo(),
                item.caixaCodigoAtual(),
                item.status(),
                resolved,
                resolved != null && resolved.isBloqueado(),
                item.justificativas(),
                item.hashIntegridade()
        );
    }

    private List<InstitutionalInboxItem> selectItems(Long processoId) {
        return processoId == null ? repository.findAll() : repository.findByProcessoId(processoId);
    }

    @Transactional(readOnly = true)
    public InstitutionalInboxItem loadVisible(String expedicaoUuid) {
        InstitutionalInboxItem item = load(expedicaoUuid);
        currentUserService.getRequired();
        if (!autorizacaoService.autorizar(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), CapacidadeCaixaInstitucional.VISUALIZAR).autorizado()) {
            throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException("Usuário atual não possui visualização para a caixa institucional solicitada.");
        }
        return item;
    }

    private InstitutionalInboxItem load(String expedicaoUuid) {
        return repository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InstitutionalInboxItem", expedicaoUuid));
    }

    private Set<String> caixasVisiveis(Usuario actor) {
        Set<String> out = new LinkedHashSet<>();
        for (VinculoUsuarioCaixaInstitucional vinculo : vinculoResolver.resolver(actor, null, null, null)) {
            if (vinculo.permite(CapacidadeCaixaInstitucional.VISUALIZAR)) {
                out.add(InstitutionalTopologyKeys.queueKey(vinculo.unidade().codigo(), vinculo.caixa().codigo()));
            }
        }
        return out;
    }

    private void requireAny(InstitutionalInboxItem item, CapacidadeCaixaInstitucional a, CapacidadeCaixaInstitucional b) {
        boolean ok = autorizacaoService.autorizar(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), a).autorizado()
                || autorizacaoService.autorizar(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), b).autorizado();
        if (!ok) {
            autorizacaoService.require(item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoAtual(), a);
        }
    }

    private void emitDomainEvent(String eventType, InstitutionalInboxItem item, String detalhe) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("expedicaoUuid", item.expedicaoUuid());
        payload.put("processoId", item.processoId());
        if (item.processoNumero() != null) {
            payload.put("processoNumero", item.processoNumero());
        }
        payload.put("unidadeCodigo", item.unidadeCodigo());
        payload.put("caixaCodigo", item.caixaCodigoAtual());
        payload.put("status", item.status().name());
        payload.put("detalhe", detalhe == null ? "" : detalhe);
        outboxPublisher.enqueueTracked(
                "processual.comunicacao.institucional",
                eventType,
                payload,
                java.util.Map.of(
                        "aggregateType", "EXPEDICAO_JUDICIAL",
                        "aggregateId", item.expedicaoUuid()
                ),
                "institutional:" + eventType + ":" + item.expedicaoUuid() + ":" + item.hashIntegridade(),
                "EXPEDICAO_JUDICIAL",
                item.expedicaoUuid()
        );
    }

    private void validarCaixaDestino(String unidadeCodigo, String caixaDestinoCodigo) {
        var unidade = catalogoInstitucionalUnificadoService.listarPorTipo(null).stream()
                .filter(candidate -> candidate.codigo().equalsIgnoreCase(unidadeCodigo))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeInstitucional", unidadeCodigo));
        boolean exists = estruturaCaixaInstitucionalService.expandir(unidade).stream()
                .anyMatch(caixa -> caixa.codigo().equalsIgnoreCase(caixaDestinoCodigo));
        if (!exists) {
            throw new RecursoNaoEncontradoException("CaixaInstitucional", caixaDestinoCodigo);
        }
    }

    private List<String> append(List<String> original, String detalhe, String marker) {
        ArrayList<String> list = new ArrayList<>(original == null ? List.of() : original);
        if (marker != null && !marker.isBlank()) {
            list.add(marker);
        }
        if (detalhe != null && !detalhe.isBlank()) {
            list.add(detalhe.trim());
        }
        return List.copyOf(list);
    }

    private String hash(String expedicaoUuid, String unidade, String caixa, StatusComunicacaoInstitucional status, Instant instant) {
        return Hashes.sha256Hex(expedicaoUuid + "|" + unidade + "|" + caixa + "|" + status.name() + "|" + instant.toString());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
