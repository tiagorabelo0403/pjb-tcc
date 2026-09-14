package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.configs.EquipeFiltroContextoQuery;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolve, para a GUC de RLS de {@code tb_processo}, o mesmo estado de ownership (usuário/equipe)
 * que {@code EquipeFiltroRepositoryAspect} já usa pra habilitar o {@code @Filter} Hibernate
 * {@code filtroEquipeProcesso}. Mesma fonte de verdade — {@link EquipeFiltroContextoQuery} —,
 * nunca uma re-derivação por papel/role que poderia divergir do comportamento real.
 *
 * <p>O filtro Hibernate só é ativado para atores tipo-advogado ({@code Usuario.isAdvogado()},
 * ver {@code EquipeSwitchInterceptor}); pra qualquer outro papel (juiz, servidor, MP, etc.) ele
 * nunca liga, e a policy de RLS deve ser igualmente permissiva nesses casos — não é uma
 * aproximação, é o espelho do que a aplicação já faz hoje.</p>
 */
@Component
public class PjbRlsEquipeResolver {

    public static final EquipeSettings INACTIVE = new EquipeSettings(false, "", "");

    private final EquipeFiltroContextoQuery equipeFiltroContextoQuery;

    public PjbRlsEquipeResolver(EquipeFiltroContextoQuery equipeFiltroContextoQuery) {
        this.equipeFiltroContextoQuery = Objects.requireNonNull(equipeFiltroContextoQuery, "equipeFiltroContextoQuery");
    }

    public EquipeSettings currentOrInactive() {
        return equipeFiltroContextoQuery.currentOrEmpty()
                .map(estado -> new EquipeSettings(true, String.valueOf(estado.usuarioId()), String.valueOf(estado.equipeIdParam())))
                .orElse(INACTIVE);
    }

    public record EquipeSettings(boolean active, String usuarioId, String equipeId) {
    }
}
