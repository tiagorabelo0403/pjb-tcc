package com.tcc.pjb.backend.model.entity.outbox;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class OutboxEventId implements Serializable {

    private UUID id;
    private int createdMonth;

    public OutboxEventId() {}

    public OutboxEventId(UUID id, int createdMonth) {
        this.id = Objects.requireNonNull(id, "id");
        this.createdMonth = createdMonth;
    }

    public static OutboxEventId fromRow(Object[] row) {
        UUID id = (UUID) row[0];
        int createdMonth = ((Number) row[1]).intValue();
        return new OutboxEventId(id, createdMonth);
    }

    public UUID getId() {
        return id;
    }

    public int getCreatedMonth() {
        return createdMonth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEventId that)) return false;
        return createdMonth == that.createdMonth && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdMonth);
    }
}
