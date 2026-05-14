package com.tcc.pjb.backend.service.processual.note;

public final class ProcessoNoteVisibilityPolicy {

    private ProcessoNoteVisibilityPolicy() {}

    public record VisibilidadeInput(
            ProcessoNoteType tipo,
            String visibleToRole,
            String consultanteRole,
            String visibleToLocation,
            String consultanteLocation,
            java.time.Instant visibleUntil
    ) {}

    public record VisibilidadeResultado(
            boolean visivel,
            String motivo
    ) {}

    public VisibilidadeResultado avaliar(VisibilidadeInput input) {
        if (input.visibleUntil() != null
                && java.time.Instant.now().isAfter(input.visibleUntil())) {
            return new VisibilidadeResultado(false, "Nota expirada.");
        }
        if (input.tipo().eVisivelParaTodos()) {
            return new VisibilidadeResultado(true, "Alerta operacional visível a todos.");
        }
        if (input.visibleToRole() != null
                && !input.visibleToRole().equals(input.consultanteRole())) {
            return new VisibilidadeResultado(false,
                    "Nota restrita ao papel " + input.visibleToRole() + ".");
        }
        if (input.visibleToLocation() != null
                && !input.visibleToLocation().equals(input.consultanteLocation())) {
            return new VisibilidadeResultado(false,
                    "Nota restrita à lotação " + input.visibleToLocation() + ".");
        }
        return new VisibilidadeResultado(true, "Nota visível ao consultante.");
    }
}
