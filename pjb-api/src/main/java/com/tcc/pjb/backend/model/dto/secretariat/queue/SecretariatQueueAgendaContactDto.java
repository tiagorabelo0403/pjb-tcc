package com.tcc.pjb.backend.model.dto.secretariat.queue;

public record SecretariatQueueAgendaContactDto(
    String role,
    String side,
    String nome,
    String documento,
    String email,
    String telefone,
    String numeroOab,
    boolean contactReady,
    String source
) {
}
