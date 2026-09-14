package com.tcc.pjb.backend.configs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EquipeFiltroContextoQueryTest {

    private final EquipeFiltroContextoQuery query = new EquipeFiltroContextoQuery();

    @AfterEach
    void limpar() {
        EquipeFiltroContexto.clear();
    }

    @Test
    void semContextoResolvido_retornaEmpty() {
        EquipeFiltroContexto.clear();

        assertThat(query.currentOrEmpty()).isEmpty();
    }

    @Test
    void comContextoResolvido_retornaUsuarioEEquipe() {
        EquipeFiltroContexto.set(new EquipeFiltroContexto.Estado(42L, 7L, null));

        assertThat(query.currentOrEmpty())
                .contains(new EquipeFiltroContextoQuery.UsuarioEquipeAtivo(42L, 7L));
    }

    @Test
    void aposClear_voltaAEmpty() {
        EquipeFiltroContexto.set(new EquipeFiltroContexto.Estado(42L, 7L, null));
        EquipeFiltroContexto.clear();

        assertThat(query.currentOrEmpty()).isEmpty();
    }
}
