package com.tcc.pjb.backend.modules.laiane.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeRegraValidacaoResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeValidacaoSoberanaResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LaianeValidacaoSoberanaResponseTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static LaianeRegraValidacaoResponse regra(int i) {
        return new LaianeRegraValidacaoResponse("REGRA_" + i, "Descrição " + i, "APROVADO");
    }

    @Test
    void regrasAplicadasVazia_passaValidacao() {
        var response = new LaianeValidacaoSoberanaResponse(
                "APROVADA", "ICP-BRASIL", "PA_v2",
                true, true, true, true, true,
                "PROMOTOR", "MP", "1G", "MP/CE",
                "s-hash", "r-hash", "d-hash",
                List.of()
        );
        Set<ConstraintViolation<LaianeValidacaoSoberanaResponse>> violacoes = validator.validate(response);
        assertThat(violacoes).isEmpty();
    }

    @Test
    void regrasAplicadasCom100Elementos_passaValidacao() {
        List<LaianeRegraValidacaoResponse> regras = IntStream.rangeClosed(1, 100)
                .mapToObj(LaianeValidacaoSoberanaResponseTest::regra)
                .toList();
        var response = new LaianeValidacaoSoberanaResponse(
                "APROVADA", "ICP-BRASIL", "PA_v2",
                true, true, true, true, true,
                "PROMOTOR", "MP", "1G", "MP/CE",
                "s-hash", "r-hash", "d-hash",
                regras
        );
        Set<ConstraintViolation<LaianeValidacaoSoberanaResponse>> violacoes = validator.validate(response);
        assertThat(violacoes).isEmpty();
    }

    @Test
    void regrasAplicadasCom101Elementos_falhaValidacao() {
        List<LaianeRegraValidacaoResponse> regras = IntStream.rangeClosed(1, 101)
                .mapToObj(LaianeValidacaoSoberanaResponseTest::regra)
                .toList();
        var response = new LaianeValidacaoSoberanaResponse(
                "APROVADA", "ICP-BRASIL", "PA_v2",
                true, true, true, true, true,
                "PROMOTOR", "MP", "1G", "MP/CE",
                "s-hash", "r-hash", "d-hash",
                regras
        );
        Set<ConstraintViolation<LaianeValidacaoSoberanaResponse>> violacoes = validator.validate(response);
        assertThat(violacoes).isNotEmpty();
        assertThat(violacoes).anyMatch(v -> v.getPropertyPath().toString().equals("regrasAplicadas"));
    }
}
