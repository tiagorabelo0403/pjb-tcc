package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GigsCommentService {

    public record ComentarioGigs(
            UUID comentarioId,
            UUID atividadeId,
            String autorId,
            String texto,
            Instant criadoEm,
            boolean sigiloso
    ) {}

    public List<ComentarioGigs> filtrarVisiveis(List<ComentarioGigs> comentarios,
            String consultanteId, boolean temAcessoSigiloso) {
        return comentarios.stream()
                .filter(c -> !c.sigiloso() || temAcessoSigiloso || c.autorId().equals(consultanteId))
                .toList();
    }

    public ComentarioGigs criar(UUID atividadeId, String autorId, String texto, boolean sigiloso) {
        return new ComentarioGigs(UUID.randomUUID(), atividadeId, autorId, texto,
                Instant.now(), sigiloso);
    }
}
