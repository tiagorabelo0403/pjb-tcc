package com.tcc.pjb.backend.controller.publico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class ConsultasPublicasControllerIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private DocumentoPaginaRepository documentoPaginaRepository;

    @Autowired
    private MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @MockitoBean
    private CapabilityRateLimiter capabilityRateLimiter;

    @Test
    void deveServirWorkspaceSearchDetailEPageResolveNaSurfaceHttpPublica() throws Exception {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CP-HTTP-2026-01")
                .numeroUnificado("0009101-11.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Procedimento comum")
                .assunto("Obrigação de fazer")
                .parteAutoraNome("João Pereira")
                .parteReuNome("Município de Quixadá")
                .tribunal("TJCE")
                .comarca("Quixadá")
                .vara("2ª Vara Cível")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 8, 0))
                .build());

        Instant base = Instant.parse("2026-04-16T10:00:00Z");
        for (int i = 0; i < 4; i++) {
            movimentacaoProcessualRepository.save(MovimentacaoProcessual.builder()
                    .processo(processo)
                    .faseDe(FaseProcessual.CONHECIMENTO)
                    .fasePara(FaseProcessual.INSTRUTORIA)
                    .descricao("Movimentação pública " + i)
                    .dataMovimentacao(base.minus(i, ChronoUnit.HOURS))
                    .build());
        }

        DocumentoProcessual acordao = documentoProcessualRepository.save(DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Acórdão público")
                .nomeOriginal("acordao-publico.pdf")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .categoria(DocumentoCategoria.PUBLICO)
                .criadoEm(LocalDateTime.of(2026, 4, 16, 10, 0))
                .build());

        documentoPaginaRepository.save(DocumentoPagina.builder()
                .documento(acordao)
                .pageNumber(2)
                .pageId("CP-HTTP-PAGE-2026-0001")
                .fingerprint("sha256:cp-http-page-1")
                .textoExtraido("Trecho público consolidado do acórdão com decisão colegiada definitiva.")
                .criadoEm(LocalDateTime.of(2026, 4, 16, 10, 1))
                .build());

        mockMvc.perform(get("/api/v1/public/consultas-publicas/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PUBLIC_ONLY"))
                .andExpect(jsonPath("$.routes.search").value("/api/v1/public/consultas-publicas/search"))
                .andExpect(jsonPath("$.sections").isArray());

        mockMvc.perform(get("/api/v1/public/consultas-publicas/search")
                        .param("q", "João Pereira")
                        .param("tipoJustica", "ESTADUAL")
                        .param("ramoDireito", "CIVIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.hits", hasSize(1)))
                .andExpect(jsonPath("$.hits[0].numeroUnificado").value("0009101-11.2026.8.06.0001"));

        mockMvc.perform(get("/api/v1/public/consultas-publicas/processos/{numero}", "0009101-11.2026.8.06.0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.numero").value("0009101-11.2026.8.06.0001"))
                .andExpect(jsonPath("$.resumo.ultimasMovimentacoes", hasSize(4)))
                .andExpect(jsonPath("$.warnings[0]", containsString("trilha pública")));

        mockMvc.perform(get("/api/v1/public/consultas-publicas/pages/{pageId}", "CP-HTTP-PAGE-2026-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("CP-HTTP-PAGE-2026-0001"))
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.texto", containsString("Trecho público consolidado")));
    }

    @Test
    void deveMascararDetailQuandoProcessoExigirCredencial() throws Exception {
        processoRepository.save(Processo.builder()
                .numeroProcesso("CP-HTTP-2026-02")
                .numeroUnificado("0009102-22.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.FAMILIA)
                .classeProcessual("Ação de guarda")
                .assunto("Guarda unilateral")
                .parteAutoraNome("Pessoa Autora")
                .parteReuNome("Pessoa Ré")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        mockMvc.perform(get("/api/v1/public/consultas-publicas/processos/{numero}", "0009102-22.2026.8.06.0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.acessoRestrito").value(true))
                .andExpect(jsonPath("$.resumo.orientacaoAcesso", containsString("credencial temporária de sigilo")));
    }
}
