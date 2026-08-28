package com.tcc.pjb.backend.configs;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Ponte pública e mínima para {@link EquipeFiltroContexto} (package-private, de propósito — é um
 * ThreadLocal interno consumido só por {@link EquipeFiltroRepositoryAspect}). Existe pra que o
 * wrapper de RLS em {@code configs.datasource} possa publicar as mesmas GUCs de ownership
 * (usuário/equipe) que o {@code @Filter} Hibernate já usa, sem duplicar a resolução nem expor o
 * ThreadLocal em si fora do pacote.
 */
@Component
public class EquipeFiltroContextoQuery {

    public Optional<UsuarioEquipeAtivo> currentOrEmpty() {
        EquipeFiltroContexto.Estado estado = EquipeFiltroContexto.getOrNull();
        if (estado == null) {
            return Optional.empty();
        }
        return Optional.of(new UsuarioEquipeAtivo(estado.usuarioId(), estado.equipeIdParam()));
    }

    public record UsuarioEquipeAtivo(long usuarioId, long equipeIdParam) {
    }
}
