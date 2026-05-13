package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRecepcaoStatusSnapshot(Long recepcaoId,
                                        String status,
                                        Instant receivedAt,
                                        Instant processedAt) {}
