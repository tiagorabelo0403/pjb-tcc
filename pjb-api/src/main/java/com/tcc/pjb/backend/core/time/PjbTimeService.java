package com.tcc.pjb.backend.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class PjbTimeService {

    private final Clock clock;
    private final ZoneId legalZone;

    public PjbTimeService(Clock pjbClock, ZoneId pjbLegalZone) {
        this.clock = pjbClock;
        this.legalZone = pjbLegalZone;
    }

    public Instant nowUtc() {
        return Instant.now(clock);
    }

    public ZoneId legalZone() {
        return legalZone;
    }

    public Instant endOfDayLegal(LocalDate date) {
        if (date == null) return null;
        
        return date.atTime(LocalTime.of(23, 59, 59)).atZone(legalZone).toInstant();
    }

    public Instant toInstantLegal(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(legalZone).toInstant();
    }

    public Instant toInstantUtc(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneOffset.UTC).toInstant();
    }
}
