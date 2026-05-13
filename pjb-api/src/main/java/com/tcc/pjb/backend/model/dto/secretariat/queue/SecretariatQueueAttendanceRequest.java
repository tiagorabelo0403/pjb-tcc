package com.tcc.pjb.backend.model.dto.secretariat.queue;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record SecretariatQueueAttendanceRequest(
    @NotBlank String attendanceStatus,
    String role,
    String name,
    Instant registeredAt,
    String note
) {
}
