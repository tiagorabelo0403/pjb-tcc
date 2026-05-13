package com.tcc.pjb.backend.ai.legalai.dreaming.application;

import com.tcc.pjb.backend.ai.legalai.config.LegalAiDreamingProperties;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamRepository;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicDreamingClient;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicDreamingClient.AnthropicDreamStatusRef;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.DreamOutboxJpaEntity;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.DreamOutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(prefix = "pjb.legal-ai.dreaming", name = "enabled", havingValue = "true")
public class DreamingScheduler {

    private static final Logger log = LoggerFactory.getLogger(DreamingScheduler.class);

    private final DreamOutboxJpaRepository outboxJpaRepository;
    private final DreamRepository dreamRepository;
    private final AnthropicDreamingClient dreamingClient;
    private final DreamResultApplicator dreamResultApplicator;
    private final LegalAiDreamingProperties properties;
    private final Clock clock;
    private final Executor dreamingPollingExecutor;

    public DreamingScheduler(
            DreamOutboxJpaRepository outboxJpaRepository,
            DreamRepository dreamRepository,
            AnthropicDreamingClient dreamingClient,
            DreamResultApplicator dreamResultApplicator,
            LegalAiDreamingProperties properties,
            Clock clock,
            @Qualifier("dreamingPollingExecutor") Executor dreamingPollingExecutor) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.dreamRepository = dreamRepository;
        this.dreamingClient = dreamingClient;
        this.dreamResultApplicator = dreamResultApplicator;
        this.properties = properties;
        this.clock = clock;
        this.dreamingPollingExecutor = dreamingPollingExecutor;
    }

    @Scheduled(cron = "${pjb.legal-ai.dreaming.schedule:0 0 2 * * *}")
    public void processarOutbox() {
        List<DreamOutboxJpaEntity> pendentes = outboxJpaRepository.findByProcessadoFalseOrderByCriadoEmAsc();
        if (pendentes.isEmpty()) {
            return;
        }
        log.info("Processando {} entradas do dream outbox", pendentes.size());
        for (DreamOutboxJpaEntity outboxEntry : pendentes) {
            dreamingPollingExecutor.execute(() -> despacharParaAnthropicComPolling(outboxEntry));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void despacharParaAnthropicComPolling(DreamOutboxJpaEntity outboxEntry) {
        UUID dreamIdValue = outboxEntry.getDreamId();
        Dream dream = dreamRepository.buscarPorId(DreamId.de(dreamIdValue)).orElse(null);
        if (dream == null) {
            log.warn("Dream {} não encontrado; removendo entrada do outbox", dreamIdValue);
            outboxJpaRepository.marcarProcessado(outboxEntry.getId(), Instant.now(clock));
            return;
        }

        try {
            List<String> sessionIds = extrairSessionIds(dream.input());
            String inputStoreId = dream.input().inputStoreId().value().toString();

            AnthropicDreamingClient.AnthropicDreamRef ref = dreamingClient.criarDream(
                    inputStoreId,
                    sessionIds,
                    dream.instrucoes(),
                    dream.modelo()
            );

            Dream emExecucao = dream.comAnthropicId(ref.id());
            dreamRepository.salvar(emExecucao);
            outboxJpaRepository.marcarProcessado(outboxEntry.getId(), Instant.now(clock));

            aguardarConclusao(emExecucao);
        } catch (Exception e) {
            log.error("Falha ao despachar dream {} para Anthropic: {}", dreamIdValue, e.getMessage());
            dreamResultApplicator.marcarFalha(dream, "Falha no despacho: " + e.getMessage());
            outboxJpaRepository.marcarProcessado(outboxEntry.getId(), Instant.now(clock));
        }
    }

    private void aguardarConclusao(Dream dream) {
        int tentativa = 0;
        long delaySegundos = properties.pollingInitialDelaySeconds();
        int maxTentativas = properties.pollingMaxAttempts();

        while (tentativa < maxTentativas) {
            try {
                Thread.sleep(Duration.ofSeconds(delaySegundos));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                dreamResultApplicator.marcarFalha(dream, "Polling interrompido");
                return;
            }

            Optional<AnthropicDreamStatusRef> statusOpt = dreamingClient.consultarDream(dream.anthropicDreamId());
            if (statusOpt.isEmpty()) {
                log.warn("Não foi possível consultar dream {}; tentativa {}/{}", dream.id().value(), tentativa + 1, maxTentativas);
                tentativa++;
                delaySegundos = Math.min(delaySegundos * 2, 300L);
                continue;
            }

            AnthropicDreamStatusRef status = statusOpt.get();
            if (status.isTerminal()) {
                dreamResultApplicator.aplicarResultado(dream, status);
                return;
            }

            tentativa++;
            delaySegundos = Math.min(delaySegundos * 2, 300L);
        }

        log.error("Dream {} excedeu o limite de polling ({} tentativas)", dream.id().value(), maxTentativas);
        dreamResultApplicator.marcarFalha(dream, "Timeout no polling após " + maxTentativas + " tentativas");
    }

    private List<String> extrairSessionIds(DreamInput input) {
        if (input instanceof DreamInput.SessionsInput si) {
            return si.sessionIds();
        }
        return List.of();
    }
}
