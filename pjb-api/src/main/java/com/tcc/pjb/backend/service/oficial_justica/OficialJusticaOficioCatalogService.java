package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaOficioCatalogService {

    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public OficialJusticaOficioCatalogService(QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    private static final List<OficioTypeDefinition> TYPES = List.of(
            new OficioTypeDefinition(
                    "OFICIO_CUMPRIMENTO_MANDADO",
                    "Ofício de cumprimento de mandado",
                    "Comunicação formal vinculada à execução e ao retorno operacional do mandado.",
                    "MINUTA_CUMPRIMENTO_MANDADO",
                    true,
                    List.of(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, DestinatarioInstitucionalKind.PROCURADORIA_ESTADO, DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO, DestinatarioInstitucionalKind.AGU, DestinatarioInstitucionalKind.FAZENDA_PUBLICA, DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL, DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO),
                    List.of("CAIXA_INSTITUCIONAL_PJB", "JUNTADA_CARTORARIA", "REMESSA_INTEROPERAVEL"),
                    List.of("cumprimento", "mandado", "retorno_operacional")
            ),
            new OficioTypeDefinition(
                    "OFICIO_REQUISICAO_DILIGENCIA_EXTERNA",
                    "Ofício de requisição de diligência externa",
                    "Requisição operacional a órgão ou unidade externa com confirmação controlada.",
                    "MINUTA_REQUISICAO_DILIGENCIA",
                    true,
                    List.of(DestinatarioInstitucionalKind.DELEGACIA_POLICIA, DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL, DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL, DestinatarioInstitucionalKind.POLICIA_PENAL, DestinatarioInstitucionalKind.UNIDADE_PRISIONAL, DestinatarioInstitucionalKind.CONSELHO_TUTELAR, DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO),
                    List.of("CAIXA_INSTITUCIONAL_PJB", "INTEROP_HUB_SOBERANO", "CONFIRMACAO_MANUAL_CARTORIO"),
                    List.of("requisicao", "diligencia", "externa")
            ),
            new OficioTypeDefinition(
                    "OFICIO_AVALIACAO_E_PENHORA",
                    "Ofício de avaliação e penhora",
                    "Fluxo formal para comunicação de avaliação patrimonial, penhora ou reforço executivo.",
                    "MINUTA_AVALIACAO_PENHORA",
                    true,
                    List.of(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, DestinatarioInstitucionalKind.PROCURADORIA_ESTADO, DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO, DestinatarioInstitucionalKind.AGU, DestinatarioInstitucionalKind.FAZENDA_PUBLICA, DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL),
                    List.of("CAIXA_INSTITUCIONAL_PJB", "JUNTADA_CARTORARIA", "REMESSA_INTEROPERAVEL"),
                    List.of("avaliacao", "penhora", "executivo")
            ),
            new OficioTypeDefinition(
                    "RESPOSTA_OFICIO_INSTITUCIONAL",
                    "Resposta institucional a ofício",
                    "Resposta governada a ofício recebido, com confirmação de entrega e trilha de retorno.",
                    "MINUTA_RESPOSTA_OFICIO",
                    false,
                    List.of(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, DestinatarioInstitucionalKind.PROCURADORIA_ESTADO, DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO, DestinatarioInstitucionalKind.AGU, DestinatarioInstitucionalKind.FAZENDA_PUBLICA, DestinatarioInstitucionalKind.DELEGACIA_POLICIA, DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL, DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL, DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL, DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO),
                    List.of("CAIXA_INSTITUCIONAL_PJB", "JUNTADA_CARTORARIA", "ESPERA_CONFIRMACAO_DESTINATARIO"),
                    List.of("resposta", "oficio", "confirmacao")
            )
    );

    private static final List<TemplateDefinition> TEMPLATES = List.of(
            new TemplateDefinition(
                    "MINUTA_CUMPRIMENTO_MANDADO",
                    "Minuta governada de cumprimento de mandado",
                    "OFICIO_CUMPRIMENTO_MANDADO",
                    List.of("assunto", "conteudo", "fundamento", "referenciaMandadoId", "destinatario_nome"),
                    """
                    OFÍCIO
                    
                    Assunto: {{assunto}}
                    
                    Destinatário: {{destinatario_nome}}
                    
                    Comunico, na qualidade de Oficial de Justiça, o seguinte: {{conteudo}}
                    
                    Referência operacional: {{referenciaMandadoId}}
                    
                    Fundamento institucional: {{fundamento}}
                    
                    Processo: {{numero_processo}}
                    Comarca/UF: {{comarca_uf}}
                    Emitido em: {{emitido_em}}
                    """
            ),
            new TemplateDefinition(
                    "MINUTA_REQUISICAO_DILIGENCIA",
                    "Minuta governada de requisição de diligência externa",
                    "OFICIO_REQUISICAO_DILIGENCIA_EXTERNA",
                    List.of("assunto", "conteudo", "fundamento", "destinatario_nome"),
                    """
                    OFÍCIO
                    
                    Assunto: {{assunto}}
                    
                    Ao(À): {{destinatario_nome}}
                    
                    Solicita-se a adoção da seguinte providência institucional: {{conteudo}}
                    
                    Fundamento institucional: {{fundamento}}
                    
                    Processo relacionado: {{numero_processo}}
                    Unidade emissora: {{unidade_emissora}}
                    Emitido em: {{emitido_em}}
                    """
            ),
            new TemplateDefinition(
                    "MINUTA_AVALIACAO_PENHORA",
                    "Minuta governada de avaliação e penhora",
                    "OFICIO_AVALIACAO_E_PENHORA",
                    List.of("assunto", "conteudo", "fundamento", "destinatario_nome"),
                    """
                    OFÍCIO
                    
                    Assunto: {{assunto}}
                    
                    Destinatário: {{destinatario_nome}}
                    
                    Registro operacional: {{conteudo}}
                    
                    Fundamento institucional: {{fundamento}}
                    
                    Processo relacionado: {{numero_processo}}
                    Unidade emissora: {{unidade_emissora}}
                    Emitido em: {{emitido_em}}
                    """
            ),
            new TemplateDefinition(
                    "MINUTA_RESPOSTA_OFICIO",
                    "Minuta governada de resposta a ofício",
                    "RESPOSTA_OFICIO_INSTITUCIONAL",
                    List.of("assunto", "conteudo", "fundamento", "destinatario_nome"),
                    """
                    RESPOSTA A OFÍCIO
                    
                    Assunto: {{assunto}}
                    
                    Ao(À): {{destinatario_nome}}
                    
                    Em resposta ao expediente, informa-se: {{conteudo}}
                    
                    Fundamento institucional: {{fundamento}}
                    
                    Processo relacionado: {{numero_processo}}
                    Unidade emissora: {{unidade_emissora}}
                    Emitido em: {{emitido_em}}
                    """
            )
    );

    public Map<String, Object> catalogo(TipoUsuario tipoUsuario) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("actorLane", "OFICIAL_JUSTICA");
        out.put("tipoUsuario", tipoUsuario != null ? tipoUsuario.name() : "OFICIAL_JUSTICA");
        out.put("oficioTypes", TYPES.stream().map(OficioTypeDefinition::asMap).toList());
        out.put("templates", TEMPLATES.stream().map(TemplateDefinition::asMap).toList());
        out.put("recipientKinds", List.of(
                recipientKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA),
                recipientKind(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.PROCURADORIA_ESTADO, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.AGU, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.FAZENDA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                recipientKind(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                recipientKind(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                recipientKind(DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL, PapelProcessualInstitucional.DESTINATARIO_OFICIO),
                recipientKind(DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO, PapelProcessualInstitucional.APOIO_TECNICO),
                recipientKind(DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO, PapelProcessualInstitucional.JUIZO_COOPERANTE)
        ));
        out.put("governance", List.of(
                "tipologia_governada_por_codigo",
                "minutas_oficiais_por_template_code",
                "destinatario_resolvido_com_hash_processual",
                "confirmacao_entrega_rastreavel_ponta_a_ponta",
                "juntada_direta_no_processo_somente_com_original_governado"
        ));
        return Collections.unmodifiableMap(out);
    }

    public OficioTypeDefinition resolveType(String typeCode, boolean resposta) {
        String effective = normalize(typeCode);
        if (effective == null) {
            return resposta ? findType("RESPOSTA_OFICIO_INSTITUCIONAL") : findType("OFICIO_CUMPRIMENTO_MANDADO");
        }
        return TYPES.stream().filter(item -> item.code().equals(effective)).findFirst().orElseGet(() -> resposta ? findType("RESPOSTA_OFICIO_INSTITUCIONAL") : findType("OFICIO_CUMPRIMENTO_MANDADO"));
    }

    public TemplateDefinition resolveTemplate(String templateCode, OficioTypeDefinition type) {
        String effective = normalize(templateCode);
        if (effective != null) {
            return TEMPLATES.stream().filter(item -> item.code().equals(effective)).findFirst().orElseGet(() -> findTemplate(type.defaultTemplateCode()));
        }
        return findTemplate(type.defaultTemplateCode());
    }

    public Map<String, Object> renderMinutaGovernada(OficialJusticaOficioRequest request,
                                                     Processo processo,
                                                     Usuario usuario,
                                                     Map<String, Object> destinatarioResolvido,
                                                     OficioTypeDefinition type,
                                                     TemplateDefinition template,
                                                     boolean resposta) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(template, "template");
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        if (request != null && request.camposMinuta() != null) {
            request.camposMinuta().forEach((key, value) -> values.put(key, value));
        }
        values.putIfAbsent("assunto", request != null ? safe(request.assunto(), type.displayName()) : type.displayName());
        values.putIfAbsent("conteudo", request != null ? safe(request.conteudo(), resposta ? "Resposta institucional em elaboração." : "Ofício institucional em elaboração.") : "Conteúdo institucional em elaboração.");
        values.putIfAbsent("fundamento", request != null ? safe(request.fundamento(), "Fundamento institucional do oficial de justiça") : "Fundamento institucional do oficial de justiça");
        values.putIfAbsent("referenciaMandadoId", request != null ? safe(request.referenciaMandadoId(), "SEM_REFERENCIA") : "SEM_REFERENCIA");
        values.putIfAbsent("destinatario_nome", resolveDestinatarioNome(request, destinatarioResolvido));
        values.putIfAbsent("numero_processo", processo != null ? safe(processo.getNumeroProcesso(), "PROCESSO_NAO_INFORMADO") : "PROCESSO_NAO_INFORMADO");
        values.putIfAbsent("comarca_uf", processo != null ? safe(processo.getComarca() + "/" + processo.getUf(), "COMARCA_NAO_INFORMADA") : "COMARCA_NAO_INFORMADA");
        values.putIfAbsent("unidade_emissora", usuario != null ? safe(usuario.getComarca() + "/" + usuario.getUf(), "UNIDADE_NAO_INFORMADA") : "UNIDADE_NAO_INFORMADA");
        values.putIfAbsent("emitido_em", Instant.now().toString());
        String rendered = template.templateBody();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        List<String> governanceTags = buildGovernanceTags(type, resposta);
        String titulo = type.displayName() + " — " + values.get("numero_processo");
        SignedDocumentEnvelope signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                processo,
                usuario,
                titulo,
                rendered,
                "OFICIAL_JUSTICA",
                resposta ? "RESPOSTA_OFICIO_QUALIFICADA_SOBERANA" : "OFICIO_QUALIFICADO_SOBERANO",
                true,
                governanceTags
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("templateCode", template.code());
        out.put("templateName", template.displayName());
        out.put("templateFamily", template.typeCode());
        out.put("requiredFields", template.requiredFields());
        out.put("rawContentHash", Hashes.sha256Hex(rendered));
        out.put("contentHash", signedContent.contentHash());
        out.put("renderedBody", signedContent.renderedContent());
        out.put("governed", true);
        out.put("governanceTags", governanceTags);
        out.put("assinaturaQualificada", signedContent.assinaturaQualificada());
        out.put("validacaoSoberana", signedContent.validacaoSoberana());
        return Collections.unmodifiableMap(out);
    }

    private static List<String> buildGovernanceTags(OficioTypeDefinition type, boolean resposta) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("template_code");
        tags.add("campos_minuta");
        tags.add("hash_minuta");
        tags.add("estrutura_oficial");
        if (type != null && type.governanceTags() != null) {
            tags.addAll(type.governanceTags());
        }
        tags.add(resposta ? "resposta_oficial_institucional" : "oficio_oficial_institucional");
        return List.copyOf(tags);
    }

    private static String resolveDestinatarioNome(OficialJusticaOficioRequest request, Map<String, Object> destinatarioResolvido) {
        Object nome = destinatarioResolvido == null ? null : destinatarioResolvido.get("nomeExibicao");
        if (nome != null && !String.valueOf(nome).isBlank()) {
            return String.valueOf(nome);
        }
        return request != null ? safe(request.destinatario(), "DESTINATARIO_INSTITUCIONAL") : "DESTINATARIO_INSTITUCIONAL";
    }

    private static Map<String, Object> recipientKind(DestinatarioInstitucionalKind kind, PapelProcessualInstitucional papel) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("kind", kind.name());
        out.put("papelPadrao", papel.name());
        out.put("admiteCanalNacionalPessoal", kind.admiteCanalNacionalPessoal());
        out.put("apoioTecnico", kind.isApoioTecnicoOuAuxiliar());
        out.put("essencialJustica", kind.isInstituicaoEssencialJustica());
        return Collections.unmodifiableMap(out);
    }

    private static OficioTypeDefinition findType(String code) {
        return TYPES.stream().filter(item -> item.code().equals(code)).findFirst().orElseThrow();
    }

    private static TemplateDefinition findTemplate(String code) {
        return TEMPLATES.stream().filter(item -> item.code().equals(code)).findFirst().orElseThrow();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record OficioTypeDefinition(
            String code,
            String displayName,
            String description,
            String defaultTemplateCode,
            boolean requiresDeliveryConfirmation,
            List<DestinatarioInstitucionalKind> allowedRecipientKinds,
            List<String> deliveryModes,
            List<String> governanceTags
    ) {
        public Map<String, Object> asMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("code", code);
            out.put("displayName", displayName);
            out.put("description", description);
            out.put("defaultTemplateCode", defaultTemplateCode);
            out.put("requiresDeliveryConfirmation", requiresDeliveryConfirmation);
            out.put("allowedRecipientKinds", allowedRecipientKinds.stream().map(Enum::name).toList());
            out.put("deliveryModes", deliveryModes);
            out.put("governanceTags", governanceTags);
            return Collections.unmodifiableMap(out);
        }
    }

    public record TemplateDefinition(
            String code,
            String displayName,
            String typeCode,
            List<String> requiredFields,
            String templateBody
    ) {
        public Map<String, Object> asMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("code", code);
            out.put("displayName", displayName);
            out.put("typeCode", typeCode);
            out.put("requiredFields", requiredFields);
            out.put("templateHash", Hashes.sha256Hex(templateBody));
            return Collections.unmodifiableMap(out);
        }
    }
}
