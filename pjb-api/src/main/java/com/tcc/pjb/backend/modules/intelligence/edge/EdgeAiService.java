package com.tcc.pjb.backend.modules.intelligence.edge;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EdgeAiService {

    private final PjbEdgeAiProperties props;
    private volatile LocalLegalBrain brain;

    public EdgeAiService(PjbEdgeAiProperties props) {
        this.props = props;
    }

    public Optional<String> tryPredictMinuta(String resumoProcesso) {
        if (!props.enabled()) return Optional.empty();
        LocalLegalBrain b = getOrInitBrain();
        return Optional.ofNullable(b.predictDraft(resumoProcesso));
    }

    public void warmUp() {
        if (!props.enabled()) return;
        getOrInitBrain().load();
    }

    private LocalLegalBrain getOrInitBrain() {
        LocalLegalBrain local = brain;
        if (local != null) return local;
        synchronized (this) {
            if (brain == null) {
                brain = new ReflectionDjlOnnxLegalBrain(props);
            }
            return brain;
        }
    }
}
