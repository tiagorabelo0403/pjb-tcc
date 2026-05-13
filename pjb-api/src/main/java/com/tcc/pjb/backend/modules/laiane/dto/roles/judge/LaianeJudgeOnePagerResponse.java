package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeMovimentacaoLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeOnePagerResponse {
    private Long processoId;
    private String numeroUnificado;
    private String classeProcessual;
    private String assunto;
    private String rito;
    private String faseAtual;
    private String status;
    private LocalDateTime ultimaMovimentacao;
    private String resumoProcessual;
    private String fatosRelevantes;
    private List<String> pedidosCentrais;
    private List<String> provasChave;
    private List<String> tesesProvaveis;
    private String badgeSigilo;
    private Integer scoreComplexidade;
    private List<LaianeWorkItemLiteDto> proximosPrazos;
    private List<LaianeMovimentacaoLiteDto> ultimasMovimentacoes;
    private List<LaianePrecedenteLiteDto> precedentesRelacionados;
}
