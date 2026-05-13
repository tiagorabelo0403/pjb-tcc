package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;

@Service
public class EstruturaCaixaInstitucionalService {

    public List<CaixaInstitucional> expandir(UnidadeInstitucional unidade) {
        LinkedHashMap<String, CaixaInstitucional> caixas = new LinkedHashMap<>();
        CaixaInstitucional principal = unidade.caixaPrincipal();
        caixas.put(principal.codigo(), principal);
        if (deveGerarTriagem(unidade.destinatarioKind())) {
            put(caixas, derivar(principal, unidade, TipoCaixaInstitucional.CAIXA_TRIAGEM, "Triagem"));
        }
        if (deveGerarCoordenacao(unidade.destinatarioKind())) {
            put(caixas, derivar(principal, unidade, TipoCaixaInstitucional.CAIXA_COORDENACAO, "Coordenação"));
        }
        if (deveGerarGabineteFuncional(unidade.destinatarioKind())) {
            put(caixas, derivar(principal, unidade, TipoCaixaInstitucional.CAIXA_GABINETE_FUNCIONAL, "Gabinete Funcional"));
        }
        if (deveGerarSubstituicao(unidade.destinatarioKind())) {
            put(caixas, derivar(principal, unidade, TipoCaixaInstitucional.CAIXA_SUBSTITUICAO, "Substituição"));
        }
        return List.copyOf(caixas.values());
    }

    private boolean deveGerarTriagem(DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO,
                    DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA,
                    DELEGACIA_POLICIA,
                    DELEGACIA_POLICIA_CIVIL,
                    DELEGACIA_POLICIA_FEDERAL,
                    POLICIA_PENAL,
                    UNIDADE_PRISIONAL,
                    CEJUSC,
                    PERICIA_JUDICIAL,
                    PERITO_JUDICIAL,
                    CONTADORIA_JUDICIAL,
                    EQUIPE_PSICOSSOCIAL,
                    ASSISTENTE_SOCIAL_JUDICIAL,
                    CARTORIO_EXTRAJUDICIAL,
                    ORGAO_TECNICO_CONVENIADO,
                    JUIZO_DEPRECADO,
                    ORGAO_JUDICIAL_EXTERNO -> true;
            default -> false;
        };
    }

    private boolean deveGerarCoordenacao(DestinatarioInstitucionalKind kind) {
        return kind != DestinatarioInstitucionalKind.CONSELHO_TUTELAR;
    }

    private boolean deveGerarGabineteFuncional(DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO,
                    DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA,
                    JUIZO_DEPRECADO,
                    ORGAO_JUDICIAL_EXTERNO -> true;
            default -> false;
        };
    }

    private boolean deveGerarSubstituicao(DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO,
                    DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA,
                    DELEGACIA_POLICIA,
                    DELEGACIA_POLICIA_CIVIL,
                    DELEGACIA_POLICIA_FEDERAL,
                    POLICIA_PENAL,
                    UNIDADE_PRISIONAL,
                    CEJUSC,
                    CONTADORIA_JUDICIAL,
                    EQUIPE_PSICOSSOCIAL,
                    ASSISTENTE_SOCIAL_JUDICIAL,
                    PERICIA_JUDICIAL,
                    PERITO_JUDICIAL,
                    ORGAO_TECNICO_CONVENIADO,
                    JUIZO_DEPRECADO,
                    ORGAO_JUDICIAL_EXTERNO -> true;
            default -> false;
        };
    }

    private CaixaInstitucional derivar(CaixaInstitucional principal,
                                       UnidadeInstitucional unidade,
                                       TipoCaixaInstitucional tipo,
                                       String sufixo) {
        String codigo = principal.codigo() + ":" + tipo.name();
        String nome = principal.nomeExibicao() + " — " + sufixo;
        return new CaixaInstitucional(
                codigo,
                nome,
                tipo,
                unidade.codigo(),
                unidade.destinatarioKind(),
                principal.recebimentoEmLote(),
                principal.permiteTriagem() || tipo == TipoCaixaInstitucional.CAIXA_TRIAGEM
        );
    }

    private void put(Map<String, CaixaInstitucional> caixas, CaixaInstitucional caixa) {
        caixas.put(caixa.codigo(), caixa);
    }
}
