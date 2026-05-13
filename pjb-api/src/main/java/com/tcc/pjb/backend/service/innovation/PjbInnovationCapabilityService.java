package com.tcc.pjb.backend.service.innovation;

import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewRequest;
import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbInnovationCapabilityService {

    private final PjbMigrationHygieneService migrationHygieneService;

    public PjbInnovationCapabilityService(PjbMigrationHygieneService migrationHygieneService) {
        this.migrationHygieneService = Objects.requireNonNull(migrationHygieneService);
    }

    public Map<String, Object> superiorityCapabilities() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("positioning", List.of(
                "escala adaptativa por ramo, instância e mesa operacional",
                "agenda viva da secretaria integrada à política de filas, cache e SLA",
                "pesquisa jurisprudencial contextual com expansão semântica",
                "linguagem simples e acessibilidade operacional nativas",
                "higienização de migração e malha de interoperabilidade multissistema",
                "inteligência de mídias de audiência e indexação por eventos",
                "trilha institucional de secretaria colegiada e especializada"
        ));
        out.put("beyondLegacySystems", List.of(
                capability("interoperabilityMesh", "troca de contexto processual entre conectores e malha administrativa sem depender de migração manual por unidade"),
                capability("migrationHygiene", "pré-validação automática de bloqueios de migração, saneamento cadastral e pronta indicação de correções"),
                capability("contextualJurisprudence", "busca contextual com expansão por ramo, rito e sinais processuais"),
                capability("plainLanguage", "conversão de texto judicial para leitura simplificada dentro da própria API do produto"),
                capability("hearingIntelligence", "catálogo de indexação audiovisual e trilha de eventos úteis para sessões, audiências e sustentações"),
                capability("deskAwareScale", "governança de fila e SLA ajustada à mesa operacional em vez de regra única para toda a secretaria"),
                capability("institutionalAudit", "auditoria de aderência institucional da secretaria, colegiado e ramos especializados")
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> interoperabilityMesh() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("connectors", List.of(
                connector("PJE", List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO", "AUDIENCIA"), List.of("application/pdf", "application/xml", "image/png", "image/jpeg", "video/mp4")),
                connector("ESAJ", List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO", "CUMPRIMENTO"), List.of("application/pdf", "image/png", "image/jpeg")),
                connector("EPROC", List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO", "AUDIENCIA", "SESSAO"), List.of("application/pdf", "image/png", "image/jpeg", "video/mp4", "application/zip")),
                connector("CRETA", List.of("JEF_DISTRIBUICAO", "PETICAO", "CUMPRIMENTO"), List.of("application/pdf", "image/png", "image/jpeg")),
                connector("PROJUDI", List.of("JUIZADO_DISTRIBUICAO", "PETICAO", "SENTENCA"), List.of("application/pdf", "image/png", "image/jpeg")),
                connector("MNI", List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO"), List.of("application/pdf", "application/xml", "image/png", "image/jpeg", "video/mp4")),
                connector("PDPJ", List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO", "CUMPRIMENTO", "EXECUCAO"), List.of("application/pdf", "application/xml", "application/zip", "image/png", "video/mp4"))
        ));
        out.put("differentials", List.of(
                "catálogo único de capacidades por conector",
                "preview de higienização antes da migração",
                "expansão para agenda, fila e contexto institucional em vez de mero protocolo",
                "mesma superfície administrativa para monitorar escala, secretaria, interoperabilidade e acessibilidade",
                "governança de conectores consciente da fila real da secretaria, da migração e das mídias processuais"
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> legacySuperiorityDelta() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("referenceSystems", List.of("PJE", "ESAJ", "EPROC", "CRETA", "PROJUDI"));
        out.put("deltaMatrix", List.of(
                delta("migração", "preview de bloqueios, saneamento e prontidão por processo"),
                delta("busca jurisprudencial", "expansão contextual, sinais processuais e agrupamento por intenção"),
                delta("acessibilidade", "linguagem simples, perfil de leitura e sugestão de preset/flags"),
                delta("mídias", "indexação de eventos, tópicos e marcos úteis para audiências e sessões"),
                delta("governança operacional", "SLA e escala conscientes da mesa operacional")
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> migrationHygieneRules() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("blockingRules", List.of(
                "assinaturas pendentes",
                "audiências ou julgamentos já agendados",
                "prazos em aberto",
                "recursos pendentes no tribunal",
                "partes sem CPF ou CNPJ",
                "classificação TPU inconsistente"
        ));
        out.put("sanitationTracks", List.of(
                "regularização cadastral das partes",
                "normalização de classe, assunto e rito",
                "revisão de marcos processuais e agenda",
                "checagem de mídia e anexos críticos"
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> digitalHearingIntelligence() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("capabilities", List.of(
                capability("eventIndexing", "indexação por abertura, depoimentos, sustentação oral, encerramento e incidentes"),
                capability("transcriptAnchors", "âncoras textuais com minutos relevantes para consulta rápida"),
                capability("secretariatTimeline", "espelhamento da mídia em agenda, fila e mesa operacional"),
                capability("accessibilityReady", "trilha preparada para contraste, zoom, leitura guiada e simplificação textual")
        ));
        return Collections.unmodifiableMap(out);
    }

    public PjbMigrationHygienePreviewResponse previewMigrationHygiene(PjbMigrationHygienePreviewRequest request) {
        return migrationHygieneService.preview(request);
    }

    public Map<String, Object> connectorSecretariatGovernance() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("product", "PJB");
        out.put("decisionRules", List.of(
                Map.of("signal", "migrationReadiness=BLOCKED", "connectorAction", "HOLD_CONNECTOR_DISPATCH", "secretariatAction", "RETER_NA_SECRETARIA_E_SANEAMENTO"),
                Map.of("signal", "migrationReadiness=READY_WITH_ATTENTION", "connectorAction", "PREPARE_CONNECTOR_MIGRATION_AND_ACK", "secretariatAction", "SANEAMENTO_ASSISTIDO_E_MIGRACAO_CONTROLADA"),
                Map.of("signal", "migrationReadiness=READY", "connectorAction", "MIGRAR_E_REDISTRIBUIR_AUTOMATICAMENTE", "secretariatAction", "REDISTRIBUICAO_AUTOMATICA_POR_MESA"),
                Map.of("signal", "hearingMediaIndexingMode=TRANSCRICAO_ANCORAS_EVENTOS", "connectorAction", "SINCRONIZAR_MARCADORES_COM_CONECTOR", "secretariatAction", "REFLETIR_EM_AGENDA_FILA_E_MESA"),
                Map.of("signal", "hearingMediaIndexingMode=INDEXACAO_MIDIA_BASICA", "connectorAction", "INDEXAR_NO_PJB_E_PUBLICAR_REFERENCIAS", "secretariatAction", "MESA_DE_MIDIAS_PROCESSUAIS")
        ));
        out.put("queueAwareOutcomes", List.of(
                "bloqueio de migração por assinatura, prazo, agenda ou recurso pendente",
                "indicação automática da mesa operacional de destino",
                "decisão de hold, migração assistida ou migração automática por conector",
                "espelhamento de mídia de audiência ou sessão em agenda, fila e mesa operacional"
        ));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> capability(String code, String description) {
        return Map.of("code", code, "description", description);
    }

    private Map<String, Object> connector(String system, List<String> scopes, List<String> mediaTypes) {
        return Map.of(
                "system", system,
                "supportedScopes", scopes,
                "acceptedMediaTypes", mediaTypes
        );
    }

    private Map<String, Object> delta(String area, String pjbAdvantage) {
        return Map.of("area", area, "pjbAdvantage", pjbAdvantage);
    }
}
