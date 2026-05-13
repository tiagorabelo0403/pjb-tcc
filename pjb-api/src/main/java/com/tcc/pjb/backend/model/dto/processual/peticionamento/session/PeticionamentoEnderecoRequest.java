package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionamentoEnderecoRequest {
    @Pattern(regexp = "^$|^[0-9]{5}-?[0-9]{3}$")
    private String cep;
    @Size(max = 160)
    private String logradouro;
    @Size(max = 30)
    private String numero;
    @Size(max = 80)
    private String complemento;
    @Size(max = 80)
    private String bairro;
    @Size(max = 160)
    private String cidade;
    @Pattern(regexp = "^$|^[A-Za-z]{2}$")
    private String uf;
    @Size(max = 120)
    private String referencia;
}
