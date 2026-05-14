package com.tcc.pjb.backend.service.gigs;

public final class GigsVisibilityPolicy {

    private GigsVisibilityPolicy() {}

    public record VisibilidadeAtividade(
            boolean visivel,
            boolean editavel,
            String motivo
    ) {}

    public VisibilidadeAtividade avaliar(
            GigsActivityType tipo,
            String responsavelId,
            String consultanteId,
            String consultanteRole,
            boolean temSigilo) {
        if (temSigilo && !consultanteRole.contains("MAGISTRADO") && !consultanteRole.contains("SERVIDOR")) {
            return new VisibilidadeAtividade(false, false, "Atividade sigilosa — acesso restrito.");
        }
        boolean eResponsavel = responsavelId != null && responsavelId.equals(consultanteId);
        boolean editavel = eResponsavel && !tipo.eAtoJurisdicional();
        String motivo = eResponsavel ? "Responsável pela atividade." : "Visível para servidor da unidade.";
        return new VisibilidadeAtividade(true, editavel, motivo);
    }
}
