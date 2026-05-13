package com.tcc.pjb.backend.integration.judicial;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorRegistry {

    private final Map<JudicialSystem, JudicialProcessConnector> bySystem = new EnumMap<>(JudicialSystem.class);

    public JudicialConnectorRegistry(List<JudicialProcessConnector> connectors) {
        for (JudicialProcessConnector c : connectors) {
            if (c == null || c.system() == null) {
                continue;
            }
            JudicialProcessConnector previous = bySystem.putIfAbsent(c.system(), c);
            if (previous != null) {
                throw new IllegalStateException("Conector judicial duplicado para system=" + c.system().name() + ". Classes=" + previous.getClass().getName() + " e " + c.getClass().getName());
            }
        }
    }

    public JudicialProcessConnector get(JudicialSystem system) {
        if (system == null) {
            system = JudicialSystem.OUTRO;
        }
        JudicialProcessConnector c = bySystem.get(system);
        if (c == null) {
            throw new IllegalStateException("Conector não registrado para system=" + system + ". Disponíveis=" + bySystem.keySet());
        }
        return c;
    }

    public Optional<JudicialProcessConnector> find(JudicialSystem system) {
        if (system == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bySystem.get(system));
    }

    public Collection<JudicialProcessConnector> all() {
        return List.copyOf(bySystem.values());
    }
}
