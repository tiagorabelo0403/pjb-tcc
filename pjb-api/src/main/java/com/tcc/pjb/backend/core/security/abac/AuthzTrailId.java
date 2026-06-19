package com.tcc.pjb.backend.core.security.abac;

import java.io.Serializable;
import java.util.Objects;

public final class AuthzTrailId implements Serializable {

    private Long id;
    private int occurredMonth;

    public AuthzTrailId() {}

    public AuthzTrailId(Long id, int occurredMonth) {
        this.id = id;
        this.occurredMonth = occurredMonth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthzTrailId that)) return false;
        return occurredMonth == that.occurredMonth && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, occurredMonth);
    }
}
