package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.core.backfill.persistence.BackfillRun;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRunRepository;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.model.dto.admin.AdminAdvocaciaOpsSummaryDto;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminAdvocaciaOpsSurfaceFacadeService {

    private final ClienteRepository clienteRepository;
    private final BackfillRunRepository backfillRunRepository;

    public AdminAdvocaciaOpsSurfaceFacadeService(ClienteRepository clienteRepository,
                                                 BackfillRunRepository backfillRunRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.backfillRunRepository = Objects.requireNonNull(backfillRunRepository);
    }

    public AdminAdvocaciaOpsSummaryDto.OpsSummaryResponse summary(String inboxKey) {
        long total = clienteRepository.count();
        long semCpfHash = clienteRepository.countByCpfHashIsNull();
        long emAnalise = clienteRepository.countByStatus(StatusCliente.EM_ANALISE);
        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusCliente status : StatusCliente.values()) {
            porStatus.put(status.name(), clienteRepository.countByStatus(status));
        }
        AdminAdvocaciaOpsSummaryDto.ClienteStats clientes = new AdminAdvocaciaOpsSummaryDto.ClienteStats(
                total,
                semCpfHash,
                emAnalise,
                porStatus
        );
        Optional<BackfillRun> latest = backfillRunRepository.findLatest(JobType.ADV_CLIENTE_CANONICALIZE_SENSITIVE.name(), inboxKey);
        return new AdminAdvocaciaOpsSummaryDto.OpsSummaryResponse(clientes, latest.map(this::toLite).orElse(null));
    }

    private AdminAdvocaciaOpsSummaryDto.BackfillRunLite toLite(BackfillRun run) {
        return new AdminAdvocaciaOpsSummaryDto.BackfillRunLite(
                run.getJobId() != null ? run.getJobId().toString() : null,
                run.getType(),
                run.getInboxKey(),
                run.getRequestedBy(),
                run.getBatchSize(),
                run.isDryRun(),
                run.getAfterId(),
                run.getUntilId(),
                run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                run.getFinishedAt() != null ? run.getFinishedAt().toString() : null,
                run.getProcessed(),
                run.getUpdated(),
                run.getDuplicates(),
                run.getLastCursor(),
                run.getLastError()
        );
    }
}
