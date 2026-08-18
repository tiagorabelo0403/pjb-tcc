package com.tcc.pjb.backend.model.entity.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventIdTest {

    @Test
    void equalsEHashCodeBaseiaEmIdECreatedMonth() {
        UUID id = UUID.randomUUID();
        OutboxEventId a = new OutboxEventId(id, 202606);
        OutboxEventId b = new OutboxEventId(id, 202606);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void naoIgualComIdDiferente() {
        OutboxEventId a = new OutboxEventId(UUID.randomUUID(), 202606);
        OutboxEventId b = new OutboxEventId(UUID.randomUUID(), 202606);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void naoIgualComCreatedMonthDiferente() {
        UUID id = UUID.randomUUID();
        OutboxEventId a = new OutboxEventId(id, 202606);
        OutboxEventId b = new OutboxEventId(id, 202607);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void fromRowComInteger() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, Integer.valueOf(202606)};
        OutboxEventId result = OutboxEventId.fromRow(row);
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getCreatedMonth()).isEqualTo(202606);
    }

    @Test
    void fromRowComLong() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, Long.valueOf(202612)};
        OutboxEventId result = OutboxEventId.fromRow(row);
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getCreatedMonth()).isEqualTo(202612);
    }

    @Test
    void fromRowArmazenaValorExato() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, Integer.valueOf(202501)};
        OutboxEventId result = OutboxEventId.fromRow(row);
        assertThat(result.getCreatedMonth()).isEqualTo(202501);
    }

    @Test
    void fromRowTipoInvalidoLancaClassCastException() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, "202606"};
        assertThatThrownBy(() -> OutboxEventId.fromRow(row))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void noArgConstructorPermitidoPelaJpa() {
        OutboxEventId id = new OutboxEventId();
        assertThat(id.getId()).isNull();
        assertThat(id.getCreatedMonth()).isEqualTo(0);
    }
}
