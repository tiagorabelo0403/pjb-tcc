package com.tcc.pjb.backend.service.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryCosineVectorIndexTest {

    @Test
    void shouldTrimVectorStoreWhenCardinalityOverflows() throws Exception {
        InMemoryCosineVectorIndex index = new InMemoryCosineVectorIndex();

        for (int i = 0; i < 20_500; i++) {
            index.upsert("id-" + i, new EmbeddingVector(new float[]{1f, i + 1f}), Map.of("ramo", "CIVIL"));
        }

        Field field = InMemoryCosineVectorIndex.class.getDeclaredField("store");
        field.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) field.get(index);

        assertEquals(20_000, store.size());
        assertEquals(20_000, index.size());
    }
}
