package com.tcc.pjb.backend.model.dto.publico;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaHitDTO;
import com.tcc.pjb.backend.model.dto.consultapublica.PublicPageResolveResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PublicDtoSemIdInternoTest {

    private static final long ID_INTERNO = 9001L;
    private static final String NUMERO = "0001234-56.2026.8.06.0001";

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private static PublicProcessoResumoCardDto card() {
        return new PublicProcessoResumoCardDto(ID_INTERNO, NUMERO, "TJCE", "CE", null, null, null, null,
                null, null, null, null, null, false, null, List.of(), null);
    }

    static Stream<Arguments> respostasPublicas() {
        return Stream.of(
                Arguments.of("resumo", card(), "processoId"),
                Arguments.of("consulta", new PublicProcessoConsultaResponse(ID_INTERNO, NUMERO, null, null, null, null,
                        null, null, false, null, null, null, List.of(), List.of()), "processoId"),
                Arguments.of("julgamentos", new PublicJulgamentosConsultaResponse(ID_INTERNO, NUMERO, true,
                        List.of(new PublicJulgamentoAcordaoDto(77L, "SEGUNDO_GRAU", "TJCE", null, null, null,
                                null, null, null, null, null))), "processoId"),
                Arguments.of("midiaDaSessao", new SessaoPublicaMediaDto(ID_INTERNO, "VIDEO", "Sessão", null, null, null), "id"),
                Arguments.of("esclarecimentoDaSessao", new SessaoPublicaEsclarecimentoDto(ID_INTERNO, "Dúvida", null, "ABERTO",
                        null, null), "id"),
                Arguments.of("sessao", new SessaoPublicaDto(5L, ID_INTERNO, NUMERO, null, null, null, null, null,
                        null, null, null, null), "processoId"),
                Arguments.of("sessaoDetalhe", new SessaoPublicaDetalheDto(5L, ID_INTERNO, NUMERO,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        List.of(), List.of()), "processoId"),
                Arguments.of("hit", ConsultaPublicaHitDTO.builder().processoId(ID_INTERNO).numeroUnificado(NUMERO).build(),
                        "processoId"),
                Arguments.of("pagina", PublicPageResolveResponse.builder().processoId(ID_INTERNO).numeroUnificado(NUMERO).build(),
                        "processoId")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("respostasPublicas")
    void respostaPublicaIdentificaPeloNumeroSemExporIdDeBanco(String nome, Object dto, String campoInterno) throws Exception {
        var json = mapper.readTree(mapper.writeValueAsString(dto));

        assertThat(json.has(campoInterno)).as(json.toString()).isFalse();
        assertThat(json.toString()).doesNotContain(String.valueOf(ID_INTERNO));
    }

    @Test
    void sessaoMantemOIdQueEChaveDaRotaPublica() throws Exception {
        var json = mapper.readTree(mapper.writeValueAsString(new SessaoPublicaDto(5L, ID_INTERNO, NUMERO, null, null,
                null, null, null, null, null, null, null)));

        assertThat(json.get("sessaoId").asLong()).isEqualTo(5L);
    }

    @Test
    void acordaoMantemOIdDoJulgamentoQueTambemEChaveDaSessaoPublica() throws Exception {
        var json = mapper.readTree(mapper.writeValueAsString(new PublicJulgamentoAcordaoDto(5L, "SEGUNDO_GRAU", "TJCE",
                null, null, null, null, null, null, null, null)));

        assertThat(json.get("julgamentoId").asLong()).isEqualTo(5L);
    }

    @Test
    void resumoPublicoSobreviveAIdaEVoltaEmJsonSemOIdInterno() throws Exception {
        PublicProcessoResumoCardDto lido = mapper.readValue(mapper.writeValueAsString(card()), PublicProcessoResumoCardDto.class);

        assertThat(lido.numero()).isEqualTo(NUMERO);
        assertThat(lido.processoId()).isNull();
    }
}
