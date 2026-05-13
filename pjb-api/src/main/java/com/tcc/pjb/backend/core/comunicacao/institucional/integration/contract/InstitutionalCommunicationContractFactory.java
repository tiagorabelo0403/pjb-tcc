package com.tcc.pjb.backend.core.comunicacao.institucional.integration.contract;

import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalCommunicationContractFactory {

    public InstitutionalCommunicationContractEnvelope build(InstitutionalDeliveryJob job, String provider) {
        InstitutionalCommunicationContractPayload payload = new InstitutionalCommunicationContractPayload(
                job.jobId(),
                job.expedicaoUuid(),
                job.processoId(),
                job.processoNumero(),
                job.unidadeCodigo(),
                job.caixaCodigo(),
                job.destinatarioKind().name(),
                job.papelProcessual().name(),
                job.currentChannel().name(),
                job.correlationKey(),
                job.justificativas()
        );
        String assinatura = Hashes.sha256Hex(job.jobId() + "|" + provider + "|" + job.currentChannel().name() + "|" + job.correlationKey());
        return new InstitutionalCommunicationContractEnvelope(
                "pjb.institutional.communication.contract/2026-03",
                provider,
                assinatura,
                Instant.now(),
                payload
        );
    }
}
