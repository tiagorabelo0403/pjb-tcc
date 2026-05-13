package com.tcc.pjb.backend.model.dto.defensor.surface;

public record DefensoriaVulnerabilidadePriorizarRequest(
        Long processoId,
        String assistidoNome,
        String documentoIdentificador,
        boolean criancaOuAdolescente,
        boolean idoso,
        boolean pessoaComDeficiencia,
        boolean violenciaDomestica,
        boolean privacaoLiberdade,
        boolean saudeGrave,
        boolean semRendaOuRua,
        boolean mulherChefeFamilia,
        boolean riscoAlimentar,
        String observacoes
) {
}
