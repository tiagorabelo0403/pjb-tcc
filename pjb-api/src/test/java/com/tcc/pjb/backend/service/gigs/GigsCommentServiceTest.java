package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GigsCommentServiceTest {

    private final GigsCommentService service = new GigsCommentService();

    @Test
    void filtrarVisiveis_incluiNaoSigilosos() {
        var comentarios = List.of(
                comentario("srv-001", false),
                comentario("srv-002", false));
        var visiveis = service.filtrarVisiveis(comentarios, "srv-003", false);
        assertThat(visiveis).hasSize(2);
    }

    @Test
    void filtrarVisiveis_ocultaSigiloso_quandoSemAcesso() {
        var comentarios = List.of(
                comentario("srv-001", true),
                comentario("srv-002", false));
        var visiveis = service.filtrarVisiveis(comentarios, "srv-003", false);
        assertThat(visiveis).hasSize(1);
    }

    @Test
    void filtrarVisiveis_exibeSigiloso_quandoTemAcesso() {
        var comentarios = List.of(comentario("srv-001", true));
        var visiveis = service.filtrarVisiveis(comentarios, "srv-003", true);
        assertThat(visiveis).hasSize(1);
    }

    @Test
    void filtrarVisiveis_exibeSigiloso_quandoProprioAutor() {
        var comentarios = List.of(comentario("srv-001", true));
        var visiveis = service.filtrarVisiveis(comentarios, "srv-001", false);
        assertThat(visiveis).hasSize(1);
    }

    @Test
    void criar_comentario_comUuidETimestamp() {
        var comentario = service.criar(UUID.randomUUID(), "srv-001", "texto ok", false);
        assertThat(comentario.comentarioId()).isNotNull();
        assertThat(comentario.criadoEm()).isBefore(Instant.now().plusSeconds(1));
    }

    private GigsCommentService.ComentarioGigs comentario(String autorId, boolean sigiloso) {
        return new GigsCommentService.ComentarioGigs(
                UUID.randomUUID(), UUID.randomUUID(), autorId, "texto", Instant.now(), sigiloso);
    }
}
