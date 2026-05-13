package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeSentencaDraftRequest {

    @NotNull
    private Long processoId;

    @Size(max = 160)
    private String orgaoJudiciario;

    @Size(max = 20000)
    private String fatos;

    @Size(max = 20000)
    private String fundamentos;

    @Size(max = 50)
    private List<@Size(max = 700) String> pedidos;

    @Size(max = 50)
    private List<@Size(max = 700) String> provas;

    @Size(max = 40)
    private String valorCausa;

    @Size(max = 12000)
    private String dispositivoExtra;

    @Size(max = 120)
    private String localData;

    @Size(max = 200)
    private String assinatura;

    private Boolean incluirAnalise;
}
