package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionamentoEnderecoResponse {
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String referencia;
    private boolean autoPreenchido;
    private boolean valido;
    private String origem;
    @Builder.Default
    private List<String> avisos = new ArrayList<>();
}
