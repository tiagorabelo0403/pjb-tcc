package com.tcc.pjb.backend.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoRequest {

    @NotBlank(message = "Classe é obrigatória")
    private String classe;

    @Size(max = 180, message = "Assunto muito longo")
    private String assunto;

    @Size(max = 5000, message = "Resumo fático muito longo")
    private String resumoFatico;

    @Size(max = 240, message = "Objeto processual muito longo")
    private String objetoProcessual;

    @Size(max = 240, message = "Pedido principal muito longo")
    private String pedidoPrincipal;

    @Builder.Default
    private List<String> pedidos = new ArrayList<>();

    @Builder.Default
    private List<String> provas = new ArrayList<>();

    @Size(max = 200, message = "Nome do autor muito longo")
    private String parteAutoraNome;

    @Size(max = 20, message = "CPF do autor inválido")
    private String parteAutoraCpf;

    @Size(max = 200, message = "Nome do réu muito longo")
    private String parteReuNome;

    @Size(max = 20, message = "CPF do réu inválido")
    private String parteReuCpf;

    @Size(max = 2, message = "UF do autor inválida")
    private String ufAutor;

    @Size(max = 120, message = "Comarca do autor muito longa")
    private String comarcaAutor;

    @Size(max = 120, message = "Cidade do autor muito longa")
    private String cidadeAutor;

    @Size(max = 120, message = "Foro pretendido do autor muito longo")
    private String foroAutor;

    @Size(max = 120, message = "Subseção judiciária do autor muito longa")
    private String subsecaoJudiciariaAutor;

    @Size(max = 2, message = "UF do réu inválida")
    private String ufReu;

    @Size(max = 120, message = "Comarca do réu muito longa")
    private String comarcaReu;

    @Size(max = 120, message = "Cidade do réu muito longa")
    private String cidadeReu;

    @Size(max = 120, message = "Foro pretendido do réu muito longo")
    private String foroReu;

    @Size(max = 120, message = "Subseção judiciária do réu muito longa")
    private String subsecaoJudiciariaReu;

    @Size(max = 120, message = "Cidade do fato muito longa")
    private String cidadeFato;

    @Size(max = 120, message = "Município do fato muito longo")
    private String municipioFato;

    @Size(max = 120, message = "Foro pretendido muito longo")
    private String foroPretendido;

    @Size(max = 120, message = "Seção judiciária pretendida muito longa")
    private String secaoJudiciariaPretendida;

    @Size(max = 120, message = "Subseção judiciária pretendida muito longa")
    private String subsecaoJudiciariaPretendida;

    @Size(max = 120, message = "Circunscrição pretendida muito longa")
    private String circunscricaoPretendida;

    @Size(max = 120, message = "Tipo de ação muito longo")
    private String tipoAcao;

    @Size(max = 160, message = "Vara pretendida muito longa")
    private String varaPretendida;

    @Size(max = 30, message = "Tipo de justiça pretendida muito longo")
    private String tipoJusticaPretendida;

    @Size(max = 30, message = "Tribunal pretendido muito longo")
    private String tribunalPretendido;

    @NotBlank(message = "Matéria é obrigatória")
    private String materia;

    @NotBlank(message = "Rito é obrigatório")
    private String rito;

    @Positive(message = "Valor da causa deve ser positivo")
    private BigDecimal valorCausa;

    @Builder.Default
    private NivelSigilo nivelSigilo = NivelSigilo.PUBLICO;

    @Builder.Default
    private boolean juizo100Digital = false;

    private boolean envolveMenor;
    private boolean envolveSaude;

    @Valid
    @Builder.Default
    private List<AnexoDeclarado> anexosDeclarados = new ArrayList<>();
}
