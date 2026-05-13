package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

@Service
public class AtoCanonicoComunicacaoMapper {

    private final Map<AtoCanonicoProcessual, PoliticaAtoCanonicoProcessual> policies;

    public AtoCanonicoComunicacaoMapper() {
        EnumMap<AtoCanonicoProcessual, PoliticaAtoCanonicoProcessual> map = new EnumMap<>(AtoCanonicoProcessual.class);
        register(map, AtoCanonicoProcessual.ABRIR_VISTA_MP_INTERESSE_INCAPAZ,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                true,
                "CPC art. 178, II; CPC art. 698, parágrafo único; ECA e proteção integral",
                List.of("vista obrigatória ao Ministério Público em hipótese com incapaz ou interesse de criança/adolescente"));
        register(map, AtoCanonicoProcessual.ABRIR_VISTA_MP_ACAO_COLETIVA,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                true,
                "CPC art. 178, I e III; tutela coletiva e interesse público relevante",
                List.of("intervenção do Ministério Público em ação coletiva ou interesse público qualificado"));
        register(map, AtoCanonicoProcessual.ABRIR_VISTA_MP_FALENCIA_RECUPERACAO,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                true,
                "Lei 11.101/2005 e CPC art. 178 quando incidente interesse público relevante",
                List.of("falência ou recuperação com necessidade de vista institucional ao Ministério Público"));
        register(map, AtoCanonicoProcessual.INTIMAR_DEFENSORIA_CURADORIA_ESPECIAL,
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.INTIMACAO_INSTITUCIONAL_DEFENSORIA,
                true,
                "CPC art. 72; LC 80/1994; curadoria especial",
                List.of("curadoria especial exige intimação institucional da Defensoria Pública"));
        register(map, AtoCanonicoProcessual.INTIMAR_FAZENDA_PUBLICA_REPRESENTACAO,
                DestinatarioInstitucionalKind.FAZENDA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.INTIMACAO_FAZENDA_PUBLICA,
                true,
                "CPC art. 183; representação judicial da Fazenda Pública",
                List.of("polo fazendário identificado"));
        register(map, AtoCanonicoProcessual.REQUISITAR_ESTUDO_PSICOSSOCIAL,
                DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "CPC; ECA; regras de apoio técnico interdisciplinar ao juízo",
                List.of("estudo psicossocial necessário para suporte decisório"));
        register(map, AtoCanonicoProcessual.COMUNICAR_CONSELHO_TUTELAR,
                DestinatarioInstitucionalKind.CONSELHO_TUTELAR,
                PapelProcessualInstitucional.ORGAO_REQUISITADO,
                TipoComunicacaoJudicial.COMUNICACAO_CONSELHO_TUTELAR,
                false,
                "ECA e rede de proteção integral",
                List.of("comunicação à rede protetiva da criança e do adolescente"));
        register(map, AtoCanonicoProcessual.ENCAMINHAR_CEJUSC,
                DestinatarioInstitucionalKind.CEJUSC,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoComunicacaoJudicial.ENCAMINHAMENTO_CEJUSC,
                false,
                "CPC art. 334; política nacional de autocomposição",
                List.of("derivação para CEJUSC ou autocomposição institucional"));
        register(map, AtoCanonicoProcessual.NOMEAR_PERITO_E_ABRIR_ACEITE,
                DestinatarioInstitucionalKind.PERICIA_JUDICIAL,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "CPC art. 156 e seguintes; nomeação e aceite do perito",
                List.of("produção de prova pericial"));
        register(map, AtoCanonicoProcessual.REQUISITAR_APRESENTACAO_REU_PRESO,
                DestinatarioInstitucionalKind.POLICIA_PENAL,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoComunicacaoJudicial.OFICIO_UNIDADE_PRISIONAL,
                false,
                "CPP; LEP; requisição de apresentação de réu preso",
                List.of("réu preso ou custodiado com necessidade de apresentação"));
        register(map, AtoCanonicoProcessual.COMUNICAR_UNIDADE_PRISIONAL_AUDIENCIA,
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoComunicacaoJudicial.OFICIO_UNIDADE_PRISIONAL,
                false,
                "CPP; LEP; comunicação de audiência à unidade custodiante",
                List.of("audiência designada para pessoa custodiada"));
        register(map, AtoCanonicoProcessual.EXPEDIR_OFICIO_CONTADORIA,
                DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "CPC; atos de liquidação e cálculo judicial",
                List.of("cálculo judicial ou conferência contábil necessária"));
        register(map, AtoCanonicoProcessual.EXPEDIR_OFICIO_CARTORIO,
                DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL,
                PapelProcessualInstitucional.DESTINATARIO_OFICIO,
                TipoComunicacaoJudicial.NOTIFICACAO_JUDICIAL,
                false,
                "Lei 6.015/1973; cooperação com serventias extrajudiciais",
                List.of("ato dependente de averbação, registro ou informação cartorária"));
        register(map, AtoCanonicoProcessual.EXPEDIR_COOPERACAO_JUIZO,
                DestinatarioInstitucionalKind.JUIZO_DEPRECADO,
                PapelProcessualInstitucional.JUIZO_COOPERANTE,
                TipoComunicacaoJudicial.COMUNICACAO_COOPERACAO_NACIONAL,
                true,
                "CPC art. 67 a 69; Resolução CNJ 350/2020",
                List.of("necessidade de cooperação judicial ou juízo deprecado"));
        register(map, AtoCanonicoProcessual.COMUNICAR_ORGAO_TECNICO_CONVENIADO,
                DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "convênios técnicos e apoio especializado ao juízo",
                List.of("demanda de apoio técnico conveniado"));
        register(map, AtoCanonicoProcessual.INTIMAR_ADVOCACIA_PUBLICA_REPRESENTACAO,
                DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_DEFENSOR,
                true,
                "CPC art. 183; representação institucional da advocacia pública",
                List.of("representação judicial institucional da advocacia pública"));
        register(map, AtoCanonicoProcessual.ABRIR_VISTA_MP_FISCAL_ORDEM_JURIDICA,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                true,
                "CPC art. 179; vista institucional do Ministério Público como fiscal da ordem jurídica",
                List.of("ato canônico genérico de vista ao Ministério Público fiscal da ordem jurídica"));
        register(map, AtoCanonicoProcessual.INTIMAR_DEFENSORIA_REU_SEM_ADVOGADO,
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.INTIMACAO_INSTITUCIONAL_DEFENSORIA,
                true,
                "LC 80/1994 art. 4º, I; CPC art. 72; ausência de advogado constituído",
                List.of("réu ou parte vulnerável sem advogado constituído"));
        register(map, AtoCanonicoProcessual.INTIMAR_FAZENDA_CITACAO_INICIAL,
                DestinatarioInstitucionalKind.FAZENDA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.CITACAO_INICIAL,
                true,
                "CPC art. 246, §3º; citação inicial institucional da Fazenda Pública",
                List.of("citação inicial envolvendo ente fazendário"));
        register(map, AtoCanonicoProcessual.REQUISITAR_ESTUDO_SOCIAL,
                DestinatarioInstitucionalKind.ASSISTENTE_SOCIAL_JUDICIAL,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "CPC art. 699; ECA art. 151; estudo social especializado",
                List.of("necessidade de estudo social judicial"));
        register(map, AtoCanonicoProcessual.ENCAMINHAR_CEJUSC_MEDIACAO,
                DestinatarioInstitucionalKind.CEJUSC,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoComunicacaoJudicial.ENCAMINHAMENTO_CEJUSC,
                false,
                "Res. CNJ 125/2010; CPC art. 334; mediação/conciliação institucional",
                List.of("encaminhamento institucional para mediação ou conciliação"));
        register(map, AtoCanonicoProcessual.NOMEAR_PERITO_E_FIXAR_QUESITOS,
                DestinatarioInstitucionalKind.PERITO_JUDICIAL,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoComunicacaoJudicial.REQUISICAO_ORGAO_TECNICO,
                false,
                "CPC art. 465; nomeação pericial com quesitos",
                List.of("nomeação de perito com definição de quesitos"));
        register(map, AtoCanonicoProcessual.EXPEDIR_MANDADO_PRISAO,
                DestinatarioInstitucionalKind.POLICIA_PENAL,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoComunicacaoJudicial.MANDADO_ARRESTO_SEQUESTRO,
                true,
                "CPP art. 283; execução do mandado de prisão",
                List.of("expedição de mandado de prisão com execução institucional"));
        register(map, AtoCanonicoProcessual.REQUISITAR_REU_PRESO,
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoComunicacaoJudicial.OFICIO_UNIDADE_PRISIONAL,
                false,
                "CPP art. 360; requisição de réu preso",
                List.of("requisição de preso ou custodiado à unidade prisional"));
        register(map, AtoCanonicoProcessual.TRAVAR_HOMOLOGACAO_ATE_VISTA_MP,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                true,
                "Lógica interna de gate processual sensível antes da homologação",
                List.of("homologação condicionada à vista institucional prévia do Ministério Público"));
        register(map, AtoCanonicoProcessual.NENHUM,
                DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.NOTIFICACAO_JUDICIAL,
                false,
                "sem ato canônico institucional obrigatório identificado",
                List.of("ausência de gatilho canônico obrigatório"));
        this.policies = Map.copyOf(map);
    }

    public PoliticaAtoCanonicoProcessual resolve(AtoCanonicoProcessual atoCanonico) {
        PoliticaAtoCanonicoProcessual politica = policies.get(atoCanonico);
        if (politica == null) {
            throw new IllegalArgumentException("Ato canônico sem política: " + atoCanonico);
        }
        return politica;
    }

    private static void register(Map<AtoCanonicoProcessual, PoliticaAtoCanonicoProcessual> map,
                                 AtoCanonicoProcessual atoCanonico,
                                 DestinatarioInstitucionalKind destinatarioKind,
                                 PapelProcessualInstitucional papelProcessual,
                                 TipoComunicacaoJudicial tipoComunicacao,
                                 boolean exigeCienciaPessoal,
                                 String fundamentoLegal,
                                 List<String> justificativas) {
        map.put(atoCanonico, new PoliticaAtoCanonicoProcessual(
                atoCanonico,
                destinatarioKind,
                papelProcessual,
                tipoComunicacao,
                exigeCienciaPessoal,
                atoCanonico.bloqueiaMarcoProcessual(),
                atoCanonico.gateCode(),
                fundamentoLegal,
                justificativas
        ));
    }
}
