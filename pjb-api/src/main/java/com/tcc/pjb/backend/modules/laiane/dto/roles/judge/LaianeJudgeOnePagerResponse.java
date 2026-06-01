package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeMovimentacaoLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo one-pager do processo para o magistrado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeOnePagerResponse {
    @Schema(description = "ID do processo", example = "12345")
    private Long processoId;
    @Schema(description = "Número CNJ do processo", example = "0001234-56.2026.8.06.0001")
    private String numeroUnificado;
    @Schema(description = "Classe processual CNJ", example = "Ação Penal - Procedimento Ordinário")
    private String classeProcessual;
    @Schema(description = "Assunto principal do processo", example = "Furto simples")
    private String assunto;
    @Schema(description = "Rito processual aplicável", example = "RITO_ORDINARIO")
    private String rito;
    @Schema(description = "Fase atual do processo", example = "INSTRUCAO_PROCESSUAL")
    private String faseAtual;
    @Schema(description = "Status do processo", example = "EM_ANDAMENTO")
    private String status;
    @Schema(description = "Data e hora da última movimentação processual", example = "2026-05-30T14:00:00")
    private OffsetDateTime ultimaMovimentacao;
    @Size(max = 5000)
    @Schema(description = "Resumo narrativo do processo", example = "Ação penal por furto — réu preso em flagrante...")
    private String resumoProcessual;
    @Size(max = 5000)
    @Schema(description = "Fatos relevantes destacados pela IA", example = "Confissão espontânea; ausência de antecedentes")
    private String fatosRelevantes;
    @Size(max = 20)
    @Schema(description = "Pedidos centrais identificados (máx. 20)")
    private List<String> pedidosCentrais;
    @Size(max = 20)
    @Schema(description = "Provas-chave identificadas (máx. 20)")
    private List<String> provasChave;
    @Size(max = 20)
    @Schema(description = "Teses mais prováveis identificadas pela IA (máx. 20)")
    private List<String> tesesProvaveis;
    @Schema(description = "Badge de sigilo aplicável ao processo",
            example = "SEGREDO_JUSTICA",
            allowableValues = {"PUBLICO", "SEGREDO_JUSTICA", "SIGILOSO", "ULTRASIGILOSO"})
    private String badgeSigilo;
    @Schema(description = "Score de complexidade processual (0–100)", example = "72")
    private Integer scoreComplexidade;
    @Size(max = 10)
    @Schema(description = "Próximos prazos mais urgentes (máx. 10)")
    private List<LaianeWorkItemLiteDto> proximosPrazos;
    @Size(max = 10)
    @Schema(description = "Últimas movimentações processuais (máx. 10)")
    private List<LaianeMovimentacaoLiteDto> ultimasMovimentacoes;
    @Size(max = 10)
    @Schema(description = "Precedentes relacionados identificados pela IA (máx. 10)")
    private List<LaianePrecedenteLiteDto> precedentesRelacionados;
}
