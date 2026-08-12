package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecretariaInstitucionalFilaServiceTest {

    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final SecretariaInstitucionalItemRepository itemRepository = mock(SecretariaInstitucionalItemRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final UnidadeInstitucionalVisibilityPolicy visibilityPolicy = new LotacaoVisibilityPolicy(lotacaoRepository);
    private final SecretariaInstitucionalFilaService service =
            new SecretariaInstitucionalFilaService(unidadeRepository, itemRepository, visibilityPolicy);

    @Test
    void promotorVePropriaFilaOrdenadaPorPrazoFatal() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(promotor);
        lotacao.setUnidade(unidade);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        when(lotacaoRepository.findAtivasByUsuario(promotor)).thenReturn(List.of(lotacao));

        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(item, "id", 1L);
        item.setProcessoId(10L);
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        item.setMotivo(MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA);
        item.setPrazoFatal(Instant.parse("2026-09-01T00:00:00Z"));
        item.setPrazoEmDobro(true);
        when(itemRepository.findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(1L)).thenReturn(List.of(item));

        SecretariaInstitucionalFilaResponse resposta = service.consultarFila(promotor, 1L);

        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().get(0).processoId()).isEqualTo(10L);
    }

    @Test
    void defensorSemLotacaoNaquelaUnidadeNaoPodeVerAFila() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        Usuario defensor = usuario(TipoUsuario.DEFENSOR_PUBLICO);
        when(lotacaoRepository.findAtivasByUsuario(defensor)).thenReturn(List.of());

        assertThatThrownBy(() -> service.consultarFila(defensor, 1L)).isInstanceOf(SecurityException.class);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
