package com.tcc.pjb.backend.integration.judicial.financeiro;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResult;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InfojudConsultaService {

    private final ProcessoRepository processoRepository;
    private final InfojudConsultaRepository consultaRepository;
    private final InfojudHttpClient httpClient;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;

    public InfojudConsultaService(ProcessoRepository processoRepository,
                                  InfojudConsultaRepository consultaRepository,
                                  InfojudHttpClient httpClient,
                                  CurrentUserService currentUserService,
                                  PjbAuthorizationService authorizationService,
                                  AuditLedgerService auditLedger,
                                  ReadAfterWriteConsistencyPolicy rawPolicy) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.consultaRepository = Objects.requireNonNull(consultaRepository);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "infojud.consulta.persist", maxMillis = 2500, critical = true)
    @CircuitBreaker(name = "infojud-consulta")
    @Retry(name = "infojud-consulta")
    @Bulkhead(name = "infojud-consulta")
    public InfojudConsultaResult consultar(InfojudConsultaRequest request, String authzTrailId) {
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + request.processoId()));
        var operador = currentUserService.getRequired();
        authorizationService.requireRequestInfojud(operador, request.delegatedOperation());
        InfojudConsulta entity = InfojudConsulta.builder()
                .processoId(processo.getId())
                .cpfCnpjConsultado(request.cpfCnpjConsultado())
                .status("PENDING")
                .tentativas(0)
                .createdAt(Instant.now())
                .operadorId(operador.getId())
                .authzTrailId(authzTrailId)
                .build();
        consultaRepository.save(entity);
        try {
            var response = httpClient.consultar(request.cpfCnpjConsultado());
            entity.setProtocoloReceita(response.protocolo());
            entity.setResumoRetorno(response.resumoRetorno());
            entity.setStatus("CONFIRMED");
            entity.setConfirmadoEm(Instant.now());
            entity.setProximoRetryEm(null);
            consultaRepository.save(entity);
            rawPolicy.markWrite();
            auditLedger.appendSafely("INFOJUD_CONSULTA_OK", "PROCESSO", String.valueOf(processo.getId()),
                    "protocolo=" + response.protocolo() + " alvo=" + Hashes.sha256HexPrefix(request.cpfCnpjConsultado(), 8));
            return InfojudConsultaResult.success(entity.getId(), response.protocolo(), response.resumoRetorno());
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setTentativas(entity.getTentativas() + 1);
            entity.setResumoRetorno(e.getMessage());
            entity.setProximoRetryEm(Instant.now().plus(Math.max(1, entity.getTentativas()), ChronoUnit.MINUTES));
            consultaRepository.save(entity);
            auditLedger.appendSafely("INFOJUD_CONSULTA_FAIL", "PROCESSO", String.valueOf(processo.getId()), "erro=" + e.getMessage());
            return InfojudConsultaResult.failed(entity.getId(), e.getMessage());
        }
    }
}
