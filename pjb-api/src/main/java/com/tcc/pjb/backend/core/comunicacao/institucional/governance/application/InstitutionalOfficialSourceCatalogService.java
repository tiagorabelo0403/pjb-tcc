package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceCatalogProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOfficialSourceCatalogService {

    private final Map<String, InstitutionalOfficialSourceCatalogProfile> profiles = profiles();
    private final InstitutionalOfficialSourceCatalogProfile fallback = fallback();

    public InstitutionalOfficialSourceCatalogProfile profileFor(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return fallback;
        }
        return profiles.getOrDefault(sourceCode.trim().toUpperCase(Locale.ROOT), fallback);
    }

    public List<InstitutionalOfficialSourceCatalogProfile> list() {
        return List.copyOf(profiles.values());
    }

    private static Map<String, InstitutionalOfficialSourceCatalogProfile> profiles() {
        LinkedHashMap<String, InstitutionalOfficialSourceCatalogProfile> map = new LinkedHashMap<>();
        map.put("CNJ_DATAJUD", new InstitutionalOfficialSourceCatalogProfile(
                "CNJ_DATAJUD",
                "Base CNJ/DataJud",
                "CNJ",
                "PODER_JUDICIARIO",
                "API_PUBLICA_OU_INTEGRACAO_TRIBUNAL",
                "AUTOMATICA",
                true,
                true,
                80,
                "https://www.cnj.jus.br/sistemas/datajud/api-publica/",
                List.of("consultar_datajud_ou_catalogo_cnj", "versionar_resposta_oficial_cnj"),
                List.of("fonte_oficial=CNJ_DATAJUD", "connector_ready=CNJ_API_OR_REST_PROXY")));
        map.put("SIORG", new InstitutionalOfficialSourceCatalogProfile(
                "SIORG",
                "Cadastro SIORG",
                "MGI_SEGES",
                "EXECUTIVO_FEDERAL",
                "CONSULTA_PUBLICA_E_SERVICOS_WEB",
                "AUTOMATICA",
                true,
                true,
                78,
                "https://www.gov.br/gestao/pt-br/assuntos/estruturas-organizacionais/Sistema-informatizado-siorg",
                List.of("consultar_siorg_cidadao_ou_servicos_web", "versionar_estrutura_oficial_federal"),
                List.of("fonte_oficial=SIORG", "connector_ready=SIORG_WEB_SERVICE")));
        map.put("RECEITA_CNPJ", new InstitutionalOfficialSourceCatalogProfile(
                "RECEITA_CNPJ",
                "Receita/CNPJ",
                "RECEITA_FEDERAL",
                "IDENTIDADE_JURIDICA",
                "CONSULTA_CNPJ_E_COMPROVANTE_CADASTRAL",
                "AUTOMATICA",
                true,
                true,
                82,
                "https://www.gov.br/receitafederal/pt-br/servicos/cadastro/cnpj",
                List.of("validar_situacao_cadastral_no_cnpj", "versionar_comprovante_oficial_do_cnpj"),
                List.of("fonte_oficial=RECEITA_CNPJ", "connector_ready=RECEITA_QUERY_GATE")));
        map.put("CANAL_OFICIAL", new InstitutionalOfficialSourceCatalogProfile(
                "CANAL_OFICIAL",
                "Canal institucional oficial",
                "CANAL_INSTITUCIONAL_VERIFICADO",
                "ATIVACAO_E_CONTATO",
                "OUT_OF_BAND_ASSINADO",
                "HUMANO_ASSISTIDO",
                false,
                false,
                60,
                null,
                List.of("verificar_canal_institucional_fora_da_sessao", "registrar_desafio_de_ativacao"),
                List.of("fonte_oficial=CANAL_OFICIAL", "connector_ready=OUT_OF_BAND_GATE")));
        map.put("DNS_E_GOVERNANCA_INSTITUCIONAL", new InstitutionalOfficialSourceCatalogProfile(
                "DNS_E_GOVERNANCA_INSTITUCIONAL",
                "Domínio institucional governado",
                "GOVERNANCA_DE_DOMINIO",
                "DOMINIO_INSTITUCIONAL",
                "DNS_HTTP_TLS_E_VALIDACAO_DE_DOMINIO",
                "AUTOMATICA",
                false,
                true,
                68,
                null,
                List.of("validar_dominio_controlado_pela_instituicao", "registrar_fingerprint_de_dominio"),
                List.of("fonte_oficial=DNS_E_GOVERNANCA_INSTITUCIONAL", "connector_ready=DOMAIN_PROBE")));
        map.put("ATO_PUBLICADO", new InstitutionalOfficialSourceCatalogProfile(
                "ATO_PUBLICADO",
                "Ato publicado ou delegação formal",
                "ATO_NORMATIVO_OU_DELEGACAO",
                "BASE_LEGAL",
                "DIARIO_OFICIAL_E_DOCUMENTO_VALIDADO",
                "HUMANO_ASSISTIDO",
                true,
                false,
                64,
                null,
                List.of("anexar_ato_ou_portaria_publicada", "homologar_base_legal_da_unidade"),
                List.of("fonte_oficial=ATO_PUBLICADO", "connector_ready=LEGAL_ACT_CRAWLER")));
        map.put("IBGE_OU_TOPOLOGIA_CNJ", new InstitutionalOfficialSourceCatalogProfile(
                "IBGE_OU_TOPOLOGIA_CNJ",
                "Topologia territorial oficial",
                "IBGE_OU_CNJ",
                "TOPOLOGIA_TERRITORIAL",
                "MALHA_TERRITORIAL_E_CATALOGO_JURISDICIONAL",
                "AUTOMATICA",
                true,
                true,
                76,
                "https://servicodados.ibge.gov.br/api/docs/localidades",
                List.of("conferir_codigo_ibge_e_topologia_jurisdicional", "versionar_mapa_territorial_aplicado"),
                List.of("fonte_oficial=IBGE_OU_TOPOLOGIA_CNJ", "connector_ready=IBGE_TOPOLOGY_GATE")));
        map.put("GOVBR", new InstitutionalOfficialSourceCatalogProfile(
                "GOVBR",
                "Identidade gov.br do representante",
                "GOVBR",
                "IDENTIDADE_PESSOAL_RAIZ",
                "LOGIN_UNICO_COM_EVENTO_ASSINADO",
                "AUTOMATICA",
                true,
                true,
                84,
                "https://www.gov.br/governodigital/pt-br/acessibilidade-e-usuario/atendimento-gov.br/duvidas-na-conta-gov.br/duvidas-no-login-com-certificado-no-gov.br/duvidas-no-login-com-certificado-no-gov.br",
                List.of("exigir_evento_de_login_govbr_ouro", "versionar_prova_de_autenticacao_forte"),
                List.of("fonte_oficial=GOVBR", "connector_ready=GOVBR_ASSERTION_GATE")));
        map.put("ITI_ICP_BRASIL", new InstitutionalOfficialSourceCatalogProfile(
                "ITI_ICP_BRASIL",
                "Certificado ICP-Brasil do representante",
                "ITI_ICP_BRASIL",
                "CERTIFICACAO_DIGITAL",
                "MEU_CERTIFICADO_E_VALIDAR",
                "AUTOMATICA",
                true,
                true,
                86,
                "https://www.gov.br/pt-br/servicos/meu-certificado",
                List.of("validar_certificado_pf_no_iti", "versionar_serial_e_validade_do_certificado"),
                List.of("fonte_oficial=ITI_ICP_BRASIL", "connector_ready=ITI_CERTIFICATE_GATE")));
        map.put("HERANCA_DE_CONFIANCA_INSTITUCIONAL", new InstitutionalOfficialSourceCatalogProfile(
                "HERANCA_DE_CONFIANCA_INSTITUCIONAL",
                "Herança de confiança da instituição-pai",
                "PJB_GOVERNANCA_INSTITUCIONAL",
                "ENCAPSULAMENTO_DE_SUBUNIDADE",
                "AVALIACAO_INTERNA_AUDITAVEL",
                "AUTOMATICA",
                false,
                true,
                74,
                null,
                List.of("correlacionar_subunidade_com_instituicao_pai", "versionar_heranca_de_confianca"),
                List.of("fonte_oficial=HERANCA_DE_CONFIANCA_INSTITUCIONAL", "connector_ready=PJB_PARENT_TRUST_GRAPH")));
        return Map.copyOf(map);
    }

    private static InstitutionalOfficialSourceCatalogProfile fallback() {
        return new InstitutionalOfficialSourceCatalogProfile(
                "NAO_MAPEADA",
                "Fonte não mapeada",
                "NAO_MAPEADA",
                "GENERICA",
                "MANUAL",
                "HUMANO_ASSISTIDO",
                false,
                false,
                50,
                null,
                List.of("classificar_fonte_na_malha_soberana"),
                List.of("fonte_oficial=NAO_MAPEADA", "connector_ready=PENDENTE_MODELAGEM")
        );
    }
}
