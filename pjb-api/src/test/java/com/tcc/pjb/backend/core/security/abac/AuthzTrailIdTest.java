package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AuthzTrailIdTest {

    @Test
    void derivesOccurredMonthFromOccurredAt() {
        PjbAuthorizationTrailReadModelEntry entry = new PjbAuthorizationTrailReadModelEntry();
        entry.setOccurredAt(LocalDateTime.of(2026, 6, 15, 10, 0, 0));
        entry.prePersist();
        assertThat(entry.getOccurredMonth()).isEqualTo(202606);
    }

    @Test
    void derivesOccurredMonthInJanuaryEdge() {
        PjbAuthorizationTrailReadModelEntry entry = new PjbAuthorizationTrailReadModelEntry();
        entry.setOccurredAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        entry.prePersist();
        assertThat(entry.getOccurredMonth()).isEqualTo(202501);
    }

    @Test
    void derivesOccurredMonthInDecemberEdge() {
        PjbAuthorizationTrailReadModelEntry entry = new PjbAuthorizationTrailReadModelEntry();
        entry.setOccurredAt(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        entry.prePersist();
        assertThat(entry.getOccurredMonth()).isEqualTo(202612);
    }

    @Test
    void setsOccurredAtWhenNullInPrePersist() {
        PjbAuthorizationTrailReadModelEntry entry = new PjbAuthorizationTrailReadModelEntry();
        assertThat(entry.getOccurredAt()).isNull();
        entry.prePersist();
        assertThat(entry.getOccurredAt()).isNotNull();
        assertThat(entry.getOccurredMonth()).isGreaterThan(202400);
    }

    @Test
    void authzTrailIdEquality() {
        AuthzTrailId id1 = new AuthzTrailId(1L, 202606);
        AuthzTrailId id2 = new AuthzTrailId(1L, 202606);
        AuthzTrailId id3 = new AuthzTrailId(1L, 202607);
        AuthzTrailId id4 = new AuthzTrailId(2L, 202606);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id4);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
