package com.tcc.pjb.backend.modules.laiane.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeAssinaturaQualificadaResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeDocumentoFormalAssinadoResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeValidacaoSoberanaResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LaianeDocumentoFormalAssinadoResponseTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String HASH_64 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static LaianeValidacaoSoberanaResponse validacaoMinima() {
        return new LaianeValidacaoSoberanaResponse(
                "APROVADA", "ICP", "PA_v2",
                true, true, true, true, true,
                "PROMOTOR", "MP", "1G", "MP/CE",
                "s", "r", HASH_64, List.of()
        );
    }

    private static LaianeAssinaturaQualificadaResponse assinaturaMinima() {
        return new LaianeAssinaturaQualificadaResponse(
                "ENV-001", "a-hash", "b-hash", HASH_64,
                true, LocalDate.now(), LocalTime.now(),
                "Quixadá/CE", "Promotor", "MP", "PROMOTOR_ESTADUAL",
                "ESTADUAL", "MP", "ESTADUAL", "1G",
                "MP/CE", "MP/CE", "REG-001",
                true, "s-hash", "r-hash", validacaoMinima()
        );
    }

    @Test
    void documentoCompleto_passaValidacao() {
        var doc = new LaianeDocumentoFormalAssinadoResponse(
                "Requisição de informações",
                "Conteúdo assinado do ofício",
                HASH_64,
                true,
                assinaturaMinima(),
                validacaoMinima()
        );
        Set<ConstraintViolation<LaianeDocumentoFormalAssinadoResponse>> violacoes = validator.validate(doc);
        assertThat(violacoes).isEmpty();
    }

    @Test
    void tituloBlank_falhaValidacao() {
        var doc = new LaianeDocumentoFormalAssinadoResponse(
                "",
                "Conteúdo",
                HASH_64,
                false,
                assinaturaMinima(),
                validacaoMinima()
        );
        Set<ConstraintViolation<LaianeDocumentoFormalAssinadoResponse>> violacoes = validator.validate(doc);
        Set<String> campos = violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(campos).contains("tituloDocumento");
    }

    @Test
    void hashForaDo64Chars_falhaValidacao() {
        var doc = new LaianeDocumentoFormalAssinadoResponse(
                "Título",
                "Conteúdo",
                "hash-curto",
                false,
                assinaturaMinima(),
                validacaoMinima()
        );
        Set<ConstraintViolation<LaianeDocumentoFormalAssinadoResponse>> violacoes = validator.validate(doc);
        Set<String> campos = violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(campos).contains("hashSha256");
    }

    @Test
    void assinaturaQualificadaNula_falhaValidacao() {
        var doc = new LaianeDocumentoFormalAssinadoResponse(
                "Título",
                "Conteúdo",
                HASH_64,
                false,
                null,
                validacaoMinima()
        );
        Set<ConstraintViolation<LaianeDocumentoFormalAssinadoResponse>> violacoes = validator.validate(doc);
        Set<String> campos = violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(campos).contains("assinaturaQualificada");
    }

    @Test
    void validacaoSoveranaNula_falhaValidacao() {
        var doc = new LaianeDocumentoFormalAssinadoResponse(
                "Título",
                "Conteúdo",
                HASH_64,
                false,
                assinaturaMinima(),
                null
        );
        Set<ConstraintViolation<LaianeDocumentoFormalAssinadoResponse>> violacoes = validator.validate(doc);
        Set<String> campos = violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(campos).contains("validacaoSoberana");
    }
}
