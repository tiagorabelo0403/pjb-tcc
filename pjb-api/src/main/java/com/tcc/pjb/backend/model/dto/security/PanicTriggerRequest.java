package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class PanicTriggerRequest {

    @Size(max = 200)
    private String reason;

    @Min(10)
    @Max(43200)
    private Integer freezeMinutes;

    private boolean hard;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getFreezeMinutes() { return freezeMinutes; }
    public void setFreezeMinutes(Integer freezeMinutes) { this.freezeMinutes = freezeMinutes; }

    public boolean isHard() { return hard; }
    public void setHard(boolean hard) { this.hard = hard; }
}
