package com.tcc.pjb.backend.integration.judicial.financeiro;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenajudRestricaoService {

    private final ProcessoRepository processoRepository;
    private final RenajudRestricaoRepository restricaoRepository;
    private final RenajudHttpClient httpClient;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;

    public RenajudRestricaoService(ProcessoRepository processoRepository,
                                   RenajudRestricaoRepository restricaoRepository,
                                   RenajudHttpClient httpClient,
                                   CurrentUserService currentUserService,
                                   PjbAuthorizationService authorizationService,
                                   AuditLedgerService auditLedger,
                                   ReadAfterWriteConsistencyPolicy rawPolicy) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.restricaoRepository = Objects.requireNonNull(restricaoRepository);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "renajud.restricao.persist", maxMillis = 2500, critical = true)
    @CircuitBreaker(name = "renajud-restricao")
    @Retry(name = "renajud-restricao")
    @Bulkhead(name = "renajud-restricao")
    public RenajudRestricaoResult solicitarRestricao(RenajudRestricaoRequest request, String authzTrailId) {
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + request.processoId()));
        var operador = currentUserService.getRequired();
        authorizationService.requireConsultRenajud(operador);
        RenajudRestricao entity = RenajudRestricao.builder()
                .processoId(processo.getId())
                .tipo(request.tipo())
                .placa(request.placa())
                .renavam(request.renavam())
                .status("PENDING")
                .tentativas(0)
                .createdAt(Instant.now())
                .operadorId(operador.getId())
                .authzTrailId(authzTrailId)
                .build();
        restricaoRepository.save(entity);
        try {
            var response = httpClient.solicitarRestricao(request.placa(), request.renavam(), request.tipo());
            entity.setProtocoloDenatran(response.protocolo());
            entity.setStatus("CONFIRMED");
            entity.setConfirmadoEm(Instant.now());
            entity.setProximoRetryEm(null);
            restricaoRepository.save(entity);
            rawPolicy.markWrite();
            auditLedger.appendSafely("RENAJUD_RESTRICAO_OK", "PROCESSO", String.valueOf(processo.getId()),
                    "protocolo=" + response.protocolo() + " placa=" + request.placa());
            return RenajudRestricaoResult.success(entity.getId(), response.protocolo(), response.resumoRetorno());
        } catch (Exception e) {
            entity.setTentativas(entity.getTentativas() + 1);
            entity.setStatus("FAILED");
            entity.setProximoRetryEm(Instant.now().plus(Math.max(1, entity.getTentativas()), ChronoUnit.MINUTES));
            restricaoRepository.save(entity);
            auditLedger.appendSafely("RENAJUD_RESTRICAO_FAIL", "PROCESSO", String.valueOf(processo.getId()), "erro=" + e.getMessage());
            return RenajudRestricaoResult.failed(entity.getId(), e.getMessage());
        }
    }
}