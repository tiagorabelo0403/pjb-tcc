package com.tcc.pjb.backend.modules.laiane.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LaianeLawyerSubstabelecimentoIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    LaianeProcuracaoRepository procuracaoRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CapabilityRateLimiter capabilityRateLimiter;

    private Usuario criarAdvogado(String nome, String email, String cpf) {
        Usuario advogado = new Usuario();
        advogado.setNome(nome);
        advogado.setEmail(email);
        advogado.setCpf(cpf);
        advogado.setAtivo(true);
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        advogado.setPerfil(TipoUsuario.ADVOGADO.name());
        return usuarioRepository.save(advogado);
    }

    @Test
    void substabelecimentoSemReservaCriaProcuracaoParaDestinatarioERevogaOrigem() throws Exception {
        Usuario substabelecente = criarAdvogado("Advogado Origem IT", "adv.origem.it@pjb.local", "10101010101");
        Usuario destinatario = criarAdvogado("Advogado Destino IT", "adv.destino.it@pjb.local", "20202020202");

        LaianeProcuracao origem = procuracaoRepository.save(LaianeProcuracao.builder()
                .advogado(substabelecente)
                .clienteId(9001L)
                .status(LaianeProcuracaoStatus.ATIVA)
                .inicioVigencia(LocalDate.now())
                .poderes("Ad judicia et extra")
                .build());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/laiane/lawyer/procuracoes/{id}/substabelecer", origem.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(substabelecente.getId())))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADVOGADO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "advogadoDestinoId", destinatario.getId(),
                                "comReservaDePoderes", false))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("substabelecimento com procuracao ativa e destinatario advogado deve ser aceito")
                .isEqualTo(200);

        LaianeProcuracao origemAtualizada = procuracaoRepository.findById(origem.getId()).orElseThrow();
        assertThat(origemAtualizada.getStatus())
                .as("substabelecimento sem reserva revoga a procuracao de origem")
                .isEqualTo(LaianeProcuracaoStatus.REVOGADA);
    }

    @Test
    void substabelecimentoDeProcuracaoDeOutroAdvogadoRecebe403() throws Exception {
        Usuario dono = criarAdvogado("Advogado Dono IT", "adv.dono.it@pjb.local", "30303030303");
        Usuario terceiro = criarAdvogado("Advogado Terceiro IT", "adv.terceiro.it@pjb.local", "40404040404");
        Usuario destinatario = criarAdvogado("Advogado Destino2 IT", "adv.destino2.it@pjb.local", "50505050505");

        LaianeProcuracao origem = procuracaoRepository.save(LaianeProcuracao.builder()
                .advogado(dono)
                .clienteId(9002L)
                .status(LaianeProcuracaoStatus.ATIVA)
                .inicioVigencia(LocalDate.now())
                .poderes("Ad judicia et extra")
                .build());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/laiane/lawyer/procuracoes/{id}/substabelecer", origem.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(terceiro.getId())))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADVOGADO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "advogadoDestinoId", destinatario.getId(),
                                "comReservaDePoderes", false))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("advogado que nao e titular da procuracao nao pode substabelece-la")
                .isEqualTo(403);
    }
}
