package com.tcc.pjb.backend.service;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.Attachment;

@Service
public class ChatModerationService {

    private static final Set<String> PALAVRAS_PROIBIDAS = Set.of(
            "palavrão", "ofensa", "discurso de ódio", "injúria"
    );

    private static final List<String> TIPOS_BLOQUEADOS = List.of(
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/x-dosexec",
            "application/java-archive",
            "application/vnd.android.package-archive",
            "application/x-sh",
            "application/x-bat",
            "application/x-ms-installer"
    );

    private static final List<String> EXTENSOES_SUSPEITAS = List.of(
            ".exe", ".bat", ".cmd", ".vbs", ".js", ".scr", ".pif", ".msi", ".jar", ".apk"
    );

    public boolean containsInappropriateText(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return PALAVRAS_PROIBIDAS.stream().anyMatch(lower::contains);
    }

    public boolean isAttachmentAllowed(Attachment attachment) {
        if (attachment == null || attachment.getContentType() == null || attachment.getName() == null)
            return false;

        String contentType = attachment.getContentType().toLowerCase();
        String fileName = attachment.getName().toLowerCase();

        if (TIPOS_BLOQUEADOS.stream().anyMatch(contentType::equals)) return false;
        if (EXTENSOES_SUSPEITAS.stream().anyMatch(fileName::endsWith)) return false;

        return true;
    }
}
