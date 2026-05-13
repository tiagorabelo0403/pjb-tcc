package com.tcc.pjb.backend.modules.laiane.jobs;

public record LaianeProtocolSubmitJobInput(
        Long protocolId,
        Long equipeId,
        Long executorUserId,
        Long signerUserId,
        Long queueItemId,
        String integrityHash
) {
}
