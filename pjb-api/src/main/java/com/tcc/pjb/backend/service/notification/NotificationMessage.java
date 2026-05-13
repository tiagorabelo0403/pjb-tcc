package com.tcc.pjb.backend.service.notification;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;

public record NotificationMessage(
        Usuario destinatario,
        Processo processo,
        String titulo,
        String mensagem,
        String urlAcesso
) {}
