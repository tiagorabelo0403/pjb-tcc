package com.tcc.pjb.backend.service.offline.domain;

public record ConflictResolution(
        boolean safe,
        boolean requiresReview,
        int acoesCount,
        int onlineMovsCount,
        String summary
) {
    public static ConflictResolution noConflict() {
        return new ConflictResolution(true, false, 0, 0, "sem conflito");
    }

    public static ConflictResolution replaySafe(int acoes) {
        return new ConflictResolution(true, false, acoes, 0, "replay seguro");
    }

    public static ConflictResolution replayWithNote(int acoes, String summary) {
        return new ConflictResolution(true, false, acoes, 0, summary);
    }

    public static ConflictResolution requiresReview(int acoes, int movs, String summary) {
        return new ConflictResolution(false, true, acoes, movs, summary);
    }
}
