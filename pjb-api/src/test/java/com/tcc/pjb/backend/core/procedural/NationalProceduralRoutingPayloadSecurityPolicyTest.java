package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingPayloadSecurityPolicyTest {

    @Test
    void mustCreateDeepSnapshotAndDropUnsafeTopLevelObjects() {
        NationalProceduralRoutingPayloadSecurityPolicy policy = new NationalProceduralRoutingPayloadSecurityPolicy();
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        source.put(" classe ", "  indenizacao  ");
        source.put("unsafe", new Object());
        source.put("nested", Map.of(" pedido ", "  obrigacao de fazer  ", "unsafeNested", new Object()));
        source.put("items", List.of("  a  ", " ", Map.of("campo", " valor ")));
        source.put("__internal", new Object());

        LinkedHashMap<String, Object> snapshot = policy.snapshot(source);

        assertEquals("indenizacao", snapshot.get("classe"));
        assertFalse(snapshot.containsKey("unsafe"));
        assertTrue(snapshot.containsKey("__internal"));
        assertInstanceOf(Map.class, snapshot.get("nested"));
        Map<?, ?> nested = (Map<?, ?>) snapshot.get("nested");
        assertEquals("obrigacao de fazer", nested.get("pedido"));
        assertFalse(nested.containsKey("unsafeNested"));
        assertInstanceOf(List.class, snapshot.get("items"));
    }

    @Test
    void mustLimitArrayAndIterableSnapshotWithoutKeepingBlankValues() {
        NationalProceduralRoutingPayloadSecurityPolicy policy = new NationalProceduralRoutingPayloadSecurityPolicy();

        LinkedHashMap<String, Object> snapshot = policy.snapshot(Map.of(
                "pedidos", new String[]{"  item 1  ", "   ", "item 2"},
                "flags", Arrays.asList(true, null, false)
        ));

        assertEquals(List.of("item 1", "item 2"), snapshot.get("pedidos"));
        assertEquals(List.of(true, false), snapshot.get("flags"));
    }
}
