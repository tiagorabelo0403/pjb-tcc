package com.tcc.pjb.backend.service.processual.calculo;

public record CalculoJudicialGeracaoContext(
        String solicitanteNome,
        String solicitanteRegistro,
        String solicitanteRotulo,
        String solicitanteNomeArquivo,
        Long equipeAtivaId,
        String equipeAtivaNome,
        String equipeAtivaRotulo,
        String hashAuditoriaGeracao
) {

    public CalculoJudicialGeracaoContext {
        solicitanteNome = normalize(solicitanteNome);
        solicitanteRegistro = normalize(solicitanteRegistro);
        solicitanteRotulo = normalize(solicitanteRotulo);
        solicitanteNomeArquivo = normalize(solicitanteNomeArquivo);
        equipeAtivaNome = normalize(equipeAtivaNome);
        equipeAtivaRotulo = normalize(equipeAtivaRotulo);
        hashAuditoriaGeracao = normalize(hashAuditoriaGeracao);
    }

    public boolean hasEquipeAtiva() {
        return equipeAtivaId != null;
    }

    public String auditSubject() {
        return hasEquipeAtiva() ? equipeAtivaNome : solicitanteNome;
    }

    public boolean hasAuditHash() {
        return !hashAuditoriaGeracao.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
