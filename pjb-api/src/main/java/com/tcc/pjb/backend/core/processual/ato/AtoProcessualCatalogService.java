package com.tcc.pjb.backend.core.processual.ato;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;

@Service
public class AtoProcessualCatalogService {

    private final Map<ProcessoLifecycleAction, AtoProcessualDescriptor> descriptors;
    private final Map<String, AtoProcessualDescriptor> aliases;
    private final Map<String, String> canonicalActTypes;
    private final AtoProcessualDescriptor fallbackSensitiveDescriptor;

    public AtoProcessualCatalogService() {
        EnumMap<ProcessoLifecycleAction, AtoProcessualDescriptor> map = new EnumMap<>(ProcessoLifecycleAction.class);
        LinkedHashMap<String, AtoProcessualDescriptor> aliasMap = new LinkedHashMap<>();
        LinkedHashMap<String, String> canonicalMap = new LinkedHashMap<>();

        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.DISTRIBUIR,
                descriptor("DIST", "Distribuição", AtoProcessualCategoria.DISTRIBUICAO, WorkItemType.DISTRIBUICAO, "DISTRIBUICAO", "GABINETE_DESPACHO_INICIAL", "Regras nacionais de distribuição e competência", AtoProcessualSecurityProfile.standard()),
                "DISTRIBUICAO", "DISTRIBUIR");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.EXPEDIR_INTIMACAO,
                descriptor("INTM", "Expedição de intimação", AtoProcessualCategoria.COMUNICACAO, WorkItemType.INTIMACAO, "SECRETARIA_INTIMACOES", "PORTAL_INTIMACAO", "Arts. 269 e seguintes do CPC", AtoProcessualSecurityProfile.standard()),
                "INTIMACAO", "EXPEDIR_INTIMACAO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.REALIZAR_JUNTADA,
                descriptor("JUNT", "Juntada", AtoProcessualCategoria.ADMINISTRATIVO, WorkItemType.JUNTADA, "SECRETARIA_JUNTADA", "GABINETE_CIENCIA_JUNTADA", "Regularidade formal dos autos", AtoProcessualSecurityProfile.standard()),
                "JUNTADA", "REALIZAR_JUNTADA");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.CONCLUIR_PARA_DESPACHO,
                descriptor("CONC", "Conclusão para despacho", AtoProcessualCategoria.ADMINISTRATIVO, WorkItemType.DESPACHO, "SECRETARIA_CONCLUSO", "GABINETE_DESPACHO", "Fluxo ordinatório interno", AtoProcessualSecurityProfile.standard()),
                "CONCLUSAO", "CONCLUIR_PARA_DESPACHO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.DESIGNAR_AUDIENCIA,
                descriptor("AUD", "Designação de audiência", AtoProcessualCategoria.INSTRUTORIA, WorkItemType.AUDIENCIA, "SECRETARIA_AGENDA_AUDIENCIA", "SECRETARIA_AGENDA_AUDIENCIA", "Regras de pauta e instrução", AtoProcessualSecurityProfile.reinforced()),
                "AUDIENCIA", "DESIGNAR_AUDIENCIA");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.ASSINAR_DESPACHO,
                descriptor("DESP", "Despacho", AtoProcessualCategoria.DECISORIO, WorkItemType.DESPACHO, "GABINETE_DECISAO", "SECRETARIA_JUNTADA", "Poder ordinatório do juízo", AtoProcessualSecurityProfile.reinforced()),
                "DESPACHO", "ASSINAR_DESPACHO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.PROFERIR_SENTENCA,
                descriptor("SENT", "Sentença", AtoProcessualCategoria.DECISORIO, WorkItemType.SENTENCA, "GABINETE_SENTENCA", "SECRETARIA_PUBLICACAO_SENTENCA", "Julgamento do mérito ou extinção", AtoProcessualSecurityProfile.sovereignDecision()),
                "SENTENCA", "PROFERIR_SENTENCA");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.INTERPOR_RECURSO,
                descriptor("RECUR", "Interposição de recurso", AtoProcessualCategoria.RECURSAL, WorkItemType.RECURSO, "ADVOCACIA_RECURSO", "SECRETARIA_RECURSO_DISTRIBUICAO", "Sistema recursal conforme rito aplicável", AtoProcessualSecurityProfile.reinforced()),
                "RECURSO", "INTERPOR_RECURSO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.PROFERIR_VOTO,
                descriptor("VOTO", "Voto", AtoProcessualCategoria.RECURSAL, WorkItemType.DECISAO, "COLEGIADO_VOTO", "COLEGIADO_ACORDAO_LAVRATURA", "Atuação do relator ou vogal", AtoProcessualSecurityProfile.sovereignDecision()),
                "VOTO", "VOTO_PLENARIO", "PROFERIR_VOTO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.LAVRAR_ACORDAO,
                descriptor("ACOR", "Acórdão", AtoProcessualCategoria.RECURSAL, WorkItemType.SENTENCA, "COLEGIADO_ACORDAO", "SECRETARIA_PUBLICACAO_ACORDAO", "Julgamento colegiado", AtoProcessualSecurityProfile.sovereignDecision()),
                "ACORDAO", "PROCLAMACAO_PLENARIA", "LAVRAR_ACORDAO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.PEDIR_VISTA,
                descriptor("VISTA", "Pedido de vista", AtoProcessualCategoria.RECURSAL, WorkItemType.VISTA, "COLEGIADO_VISTA", "COLEGIADO_VISTA_RETORNO", "Dilação interna para exame do feito", AtoProcessualSecurityProfile.reinforced()),
                "VISTA", "PEDIR_VISTA");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.CERTIFICAR_TRANSITO,
                descriptor("TRANS", "Certidão de trânsito em julgado", AtoProcessualCategoria.CERTIFICACAO, WorkItemType.CERTIDAO, "SECRETARIA_TRANSITO_JULGADO", "CUMPRIMENTO_SENTENCA_TRIGGER", "Art. 502 do CPC", AtoProcessualSecurityProfile.terminalTransition()),
                "TRANSITO", "TRANSITO_EM_JULGADO", "CERTIFICAR_TRANSITO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
                descriptor("CUMP", "Início de cumprimento de sentença", AtoProcessualCategoria.EXECUTIVO, WorkItemType.CUMPRIMENTO_SENTENCA, "CUMPRIMENTO_SENTENCA", "JUIZ_CUMPRIMENTO_DESPACHO", "Art. 513 do CPC", AtoProcessualSecurityProfile.executionTransition()),
                "CUMPRIMENTO", "INICIAR_CUMPRIMENTO", "EXTINGUIR_CUMPRIMENTO", "EXTINCAO_CUMPRIMENTO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.ARQUIVAR,
                descriptor("ARQ", "Baixa e arquivamento", AtoProcessualCategoria.CERTIFICACAO, WorkItemType.CERTIDAO, "SECRETARIA_ARQUIVAMENTO", "ARQUIVO_DEFINITIVO", "Baixa definitiva do feito", AtoProcessualSecurityProfile.terminalTransition()),
                "ARQUIVAMENTO", "ARQUIVAR", "BAIXA_DEFINITIVA");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.DESARQUIVAR,
                descriptor("DESARQ", "Desarquivamento", AtoProcessualCategoria.ADMINISTRATIVO, WorkItemType.DISTRIBUICAO, "SECRETARIA_DESARQUIVAMENTO", "GABINETE_DESPACHO", "Reativação controlada do feito", AtoProcessualSecurityProfile.reinforced()),
                "DESARQUIVAMENTO", "DESARQUIVAR", "REATIVAR_PROCESSO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.REGISTRAR_ACORDO,
                descriptor("ACORDO", "Registro de acordo", AtoProcessualCategoria.DECISORIO, WorkItemType.ACORDO, "CEJUSC_ACORDO", "GABINETE_HOMOLOGACAO", "Autocomposição e homologação judicial", AtoProcessualSecurityProfile.reinforced()),
                "ACORDO", "REGISTRAR_ACORDO", "HOMOLOGAR_ACORDO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.APRESENTAR_LAUDO,
                descriptor("LAUDO", "Apresentação de laudo", AtoProcessualCategoria.INSTRUTORIA, WorkItemType.LAUDO, "PERICIA_LAUDO", "SECRETARIA_PERICIA_CIENCIA", "Prova técnica pericial", AtoProcessualSecurityProfile.standard()),
                "LAUDO", "APRESENTAR_LAUDO");
        register(map, aliasMap, canonicalMap,
                ProcessoLifecycleAction.RESPONDER_QUESITOS,
                descriptor("QUESITOS", "Resposta a quesitos", AtoProcessualCategoria.INSTRUTORIA, WorkItemType.PERICIA, "PERICIA_QUESITOS", "SECRETARIA_PERICIA_CIENCIA", "Complementação técnica da perícia", AtoProcessualSecurityProfile.standard()),
                "QUESITOS", "RESPONDER_QUESITOS");

        registerExternal(aliasMap, canonicalMap,
                "PUBLICAR_DECISAO",
                descriptor("PUB", "Publicação de ato judicial", AtoProcessualCategoria.CERTIFICACAO, WorkItemType.CERTIDAO, "SECRETARIA_PUBLICACAO", "PORTAL_PUBLICACAO", "Publicação oficial e liberação controlada do ato judicial", AtoProcessualSecurityProfile.publication()),
                "PUBLICAR_DECISAO", "PUBLICACAO", "PUBLICACAO_SENTENCA", "PUBLICACAO_ACORDAO", "PUBLICACAO_VOTO", "PUBLICAR_SENTENCA", "PUBLICAR_ACORDAO");
        registerExternal(aliasMap, canonicalMap,
                "EXPEDIR_MANDADO_SENSIVEL",
                descriptor("MAND", "Expedição de mandado sensível", AtoProcessualCategoria.COMUNICACAO, WorkItemType.EXPEDICAO, "CENTRAL_MANDADOS", "OFICIAL_CUMPRIMENTO", "Mandado judicial com rastreabilidade reforçada", AtoProcessualSecurityProfile.sensitiveMandate()),
                "MANDADO", "MANDADO_SENSIVEL", "EXPEDIR_MANDADO_SENSIVEL", "MANDADO_PRISAO", "MANDADO_BUSCA", "MANDADO_PENHORA", "MANDADO_CITACAO");
        registerExternal(aliasMap, canonicalMap,
                "EXPEDIR_ALVARA",
                descriptor("ALVARA", "Expedição de alvará", AtoProcessualCategoria.EXECUTIVO, WorkItemType.EXPEDICAO, "SECRETARIA_ALVARA", "CAIXA_OU_CUSTODIA", "Alvará judicial com prova reforçada de integridade", AtoProcessualSecurityProfile.releaseOrder()),
                "ALVARA", "ALVARA_SOLTURA", "EXPEDIR_ALVARA", "EXPEDIR_ALVARA_SOLTURA");

        this.descriptors = Map.copyOf(map);
        this.aliases = Map.copyOf(aliasMap);
        this.canonicalActTypes = Map.copyOf(canonicalMap);
        this.fallbackSensitiveDescriptor = descriptor("ATO", "Ato judicial sensível", AtoProcessualCategoria.DECISORIO, WorkItemType.DECISAO, "GABINETE_DECISAO", "SECRETARIA_CONFERENCIA_DECISORIA", "Blindagem padrão para ato judicial de alta sensibilidade", AtoProcessualSecurityProfile.reinforced());
    }

    public AtoProcessualDescriptor descriptorFor(ProcessoLifecycleAction action) {
        return descriptors.get(action);
    }

    public AtoProcessualDescriptor descriptorFor(String actType) {
        String normalized = normalizeKey(actType);
        if (normalized == null) {
            return fallbackSensitiveDescriptor;
        }
        AtoProcessualDescriptor descriptor = aliases.get(normalized);
        if (descriptor != null) {
            return descriptor;
        }
        if (normalized.contains("PUBLICA")) {
            return aliases.get(normalizeKey("PUBLICAR_DECISAO"));
        }
        if (normalized.contains("ALVARA")) {
            return aliases.get(normalizeKey("EXPEDIR_ALVARA"));
        }
        if (normalized.contains("MANDADO")) {
            return aliases.get(normalizeKey("EXPEDIR_MANDADO_SENSIVEL"));
        }
        if (normalized.contains("VOTO")) {
            return aliases.get(normalizeKey("VOTO"));
        }
        if (normalized.contains("ACORDAO") || normalized.contains("PROCLAMACAO")) {
            return aliases.get(normalizeKey("ACORDAO"));
        }
        if (normalized.contains("SENTENCA")) {
            return aliases.get(normalizeKey("SENTENCA"));
        }
        if (normalized.contains("DESPACHO")) {
            return aliases.get(normalizeKey("DESPACHO"));
        }
        if (normalized.contains("TRANSITO")) {
            return aliases.get(normalizeKey("TRANSITO"));
        }
        if (normalized.contains("ARQUIV")) {
            return aliases.get(normalizeKey("ARQUIVAMENTO"));
        }
        if (normalized.contains("CUMPRIMENTO") || normalized.contains("EXECUCAO")) {
            return aliases.get(normalizeKey("CUMPRIMENTO"));
        }
        return fallbackSensitiveDescriptor;
    }

    public String canonicalActType(String actType) {
        String normalized = normalizeKey(actType);
        if (normalized == null) {
            return "ATO_JUDICIAL_SENSIVEL";
        }
        return canonicalActTypes.getOrDefault(normalized, normalized);
    }

    private void register(Map<ProcessoLifecycleAction, AtoProcessualDescriptor> map,
                          Map<String, AtoProcessualDescriptor> aliasMap,
                          Map<String, String> canonicalMap,
                          ProcessoLifecycleAction action,
                          AtoProcessualDescriptor descriptor,
                          String... aliases) {
        map.put(action, descriptor);
        registerExternal(aliasMap, canonicalMap, action.name(), descriptor, aliases);
        registerAlias(aliasMap, canonicalMap, action.name(), descriptor, action.name());
    }

    private void registerExternal(Map<String, AtoProcessualDescriptor> aliasMap,
                                  Map<String, String> canonicalMap,
                                  String canonicalActType,
                                  AtoProcessualDescriptor descriptor,
                                  String... aliases) {
        for (String alias : aliases) {
            registerAlias(aliasMap, canonicalMap, canonicalActType, descriptor, alias);
        }
    }

    private void registerAlias(Map<String, AtoProcessualDescriptor> aliasMap,
                               Map<String, String> canonicalMap,
                               String canonicalActType,
                               AtoProcessualDescriptor descriptor,
                               String alias) {
        String normalized = normalizeKey(alias);
        if (normalized == null) {
            return;
        }
        aliasMap.put(normalized, descriptor);
        canonicalMap.put(normalized, canonicalActType);
    }

    private AtoProcessualDescriptor descriptor(String codigo,
                                               String titulo,
                                               AtoProcessualCategoria categoria,
                                               WorkItemType workItemType,
                                               String filaPadrao,
                                               String inboxPadrao,
                                               String fundamentoPadrao,
                                               AtoProcessualSecurityProfile securityProfile) {
        return new AtoProcessualDescriptor(codigo, titulo, categoria, workItemType, filaPadrao, inboxPadrao, fundamentoPadrao, securityProfile);
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
