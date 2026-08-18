package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbApiRouteGovernanceCoverageTest {

    @Test
    void governancaDeRotaDeveCobrirChatEIndicesAdministrativosDeJurisprudencia() throws Exception {
        String content = Files.readString(Path.of("src/main/resources/application.yml"))
                + "\n" + Files.readString(Path.of("src/main/resources/application-api-governance.yml"));
        assertTrue(content.contains("- name: chat-operational"), "application.yml deve declarar policy chat-operational.");
        assertTrue(content.contains("/api/v1/chat/**"), "application.yml deve cobrir /api/v1/chat/** na governanca de rota.");
        assertTrue(content.contains("- name: jurisprudencia-index-admin"), "application.yml deve declarar policy jurisprudencia-index-admin.");
        assertTrue(content.contains("/api/v1/jurisprudencia/index/**"), "application.yml deve cobrir o indice administrativo da jurisprudencia.");
        assertTrue(content.contains("- name: admin-metrics-sensitive"), "application.yml deve declarar policy admin-metrics-sensitive.");
        assertTrue(content.contains("/api/v1/admin/metrics/**"), "application.yml deve cobrir /api/v1/admin/metrics/** na governanca de rota.");
        assertTrue(content.contains("- name: cidadao-profile-sensitive"), "application.yml deve declarar policy cidadao-profile-sensitive.");
        assertTrue(content.contains("/api/v1/cidadao/perfil/**"), "application.yml deve cobrir /api/v1/cidadao/perfil/** na governanca de rota.");
        assertTrue(content.contains("- name: forum-operacional"), "application.yml deve declarar policy forum-operacional.");
        assertTrue(content.contains("/api/v1/forum/**"), "application.yml deve cobrir /api/v1/forum/** na governanca de rota.");
        assertTrue(content.contains("- name: admin-atlas-sensitive"), "application.yml deve declarar policy admin-atlas-sensitive.");
        assertTrue(content.contains("/api/v1/admin/atlas/**"), "application.yml deve cobrir /api/v1/admin/atlas/** na governanca de rota.");
        assertTrue(content.contains("- name: admin-federalismo-sensitive"), "application.yml deve declarar policy admin-federalismo-sensitive.");
        assertTrue(content.contains("/api/v1/admin/federalismo/**"), "application.yml deve cobrir /api/v1/admin/federalismo/** na governanca de rota.");
        assertTrue(content.contains("- name: security-device-sensitive"), "application.yml deve declarar policy security-device-sensitive.");
        assertTrue(content.contains("/api/v1/security/devices/**"), "application.yml deve cobrir /api/v1/security/devices/** na governanca de rota.");
        assertTrue(content.contains("- name: process-twin-sensitive"), "application.yml deve declarar policy process-twin-sensitive.");
        assertTrue(content.contains("/api/v1/processos/**/twin"), "application.yml deve cobrir /api/v1/processos/**/twin na governanca de rota.");
    }
}
