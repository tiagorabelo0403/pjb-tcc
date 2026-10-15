package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RegimeDistribuicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Component;

@Component
public class RegimeDistribuicaoResolver {

    public RegimeDistribuicaoDecisao resolver(RitoProcessual rito, GrauJurisdicao grau) {
        return resolver(rito, grau, null, RegimeDistribuicaoSinais.nenhum());
    }

    public RegimeDistribuicaoDecisao resolver(RitoProcessual rito, GrauJurisdicao grau, TpuClasseCnj classe) {
        return resolver(rito, grau, classe, RegimeDistribuicaoSinais.nenhum());
    }

    public RegimeDistribuicaoDecisao resolver(RitoProcessual rito, GrauJurisdicao grau, TpuClasseCnj classe, RegimeDistribuicaoSinais sinais) {
        RitoProcessual ritoEfetivo = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        GrauJurisdicao grauEfetivo = grau == null ? GrauJurisdicao.PRIMEIRO_GRAU : grau;
        RegimeDistribuicaoSinais sinaisEfetivos = sinais == null ? RegimeDistribuicaoSinais.nenhum() : sinais;
        RamoDireito ramoDireito = classe == null ? ritoEfetivo.suggestedRamo() : classe.ramoDireito();
        TpuClasseCnj.RamoJustica ramoJustica = classe == null ? ramoJusticaDoRito(ritoEfetivo) : classe.ramoJustica();
        RegimeDistribuicao regime = decidir(ritoEfetivo, grauEfetivo, ramoJustica, sinaisEfetivos);
        return new RegimeDistribuicaoDecisao(regime, ramoDireito, ramoJustica, grauEfetivo, ritoEfetivo, regime.fundamentoLegal());
    }

    public RegimeDistribuicaoDecisao resolverPorCodigos(String ritoCodigo, String classeTpuCodigo, String grauCodigo) {
        RitoProcessual rito = RitoProcessual.tryParse(ritoCodigo).orElse(RitoProcessual.COMUM_ORDINARIO);
        TpuClasseCnj classe = resolverClasse(classeTpuCodigo);
        return resolver(rito, grauInformadoOuDaClasse(grauCodigo, classe), classe, RegimeDistribuicaoSinais.nenhum());
    }

    private RegimeDistribuicao decidir(RitoProcessual rito, GrauJurisdicao grau, TpuClasseCnj.RamoJustica ramoJustica, RegimeDistribuicaoSinais sinais) {
        if (sinais.precatorioOuRpv()) {
            return RegimeDistribuicao.CRONOLOGICO;
        }
        if (sinais.temaRepetitivoAfetado()) {
            return RegimeDistribuicao.AFETACAO_SOBRESTAMENTO;
        }
        if (sinais.dependenciaOuPrevencao()) {
            return RegimeDistribuicao.PREVENCAO;
        }
        if (grau.exigeColegiado()) {
            return sinais.rescisoriaExcluindoJulgador() ? RegimeDistribuicao.EXCLUSAO_LEGAL : RegimeDistribuicao.SORTEIO_RELATORIA;
        }
        if (sinais.unidadeUnica()) {
            return RegimeDistribuicao.UNIDADE_UNICA;
        }
        if (ramoJustica == TpuClasseCnj.RamoJustica.ELEITORAL || rito.isEleitoral()) {
            return RegimeDistribuicao.DESIGNACAO_LEGAL;
        }
        return RegimeDistribuicao.ALEATORIO_ALTERNADO;
    }

    private TpuClasseCnj.RamoJustica ramoJusticaDoRito(RitoProcessual rito) {
        if (rito.isEleitoral()) {
            return TpuClasseCnj.RamoJustica.ELEITORAL;
        }
        if (rito.isMilitar()) {
            return TpuClasseCnj.RamoJustica.MILITAR;
        }
        if (rito.isTrabalhista()) {
            return TpuClasseCnj.RamoJustica.TRABALHO;
        }
        if (rito.isPrevidenciario()) {
            return TpuClasseCnj.RamoJustica.FEDERAL;
        }
        if (rito.isTribFazenda()) {
            return TpuClasseCnj.RamoJustica.ESTADUAL_FEDERAL;
        }
        return TpuClasseCnj.RamoJustica.ESTADUAL;
    }

    private TpuClasseCnj resolverClasse(String classeTpuCodigo) {
        if (classeTpuCodigo == null || classeTpuCodigo.isBlank()) {
            return null;
        }
        String token = classeTpuCodigo.trim();
        if (token.chars().allMatch(Character::isDigit)) {
            try {
                return TpuClasseCnj.porCodigo(Integer.parseInt(token)).orElse(null);
            } catch (NumberFormatException overflow) {
                return TpuClasseCnj.porNome(token).orElse(null);
            }
        }
        return TpuClasseCnj.porNome(token).orElse(null);
    }

    private GrauJurisdicao grauInformadoOuDaClasse(String grauCodigo, TpuClasseCnj classe) {
        if (grauCodigo != null && !grauCodigo.isBlank()) {
            try {
                return GrauJurisdicao.fromString(grauCodigo);
            } catch (IllegalArgumentException naoReconhecido) {
                return grauDaClasse(classe);
            }
        }
        return grauDaClasse(classe);
    }

    private GrauJurisdicao grauDaClasse(TpuClasseCnj classe) {
        if (classe == null) {
            return GrauJurisdicao.PRIMEIRO_GRAU;
        }
        if (classe.faixaProcedimental() == TpuClasseCnj.FaixaProcedimental.CONTROLE_CONCENTRADO) {
            return GrauJurisdicao.CONSTITUCIONAL;
        }
        if (classe.ramoJustica() == TpuClasseCnj.RamoJustica.SUPERIOR) {
            return GrauJurisdicao.SUPERIOR;
        }
        if (ehCompetenciaOriginariaDeTribunal(classe)) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private boolean ehCompetenciaOriginariaDeTribunal(TpuClasseCnj classe) {
        return classe.faixaProcedimental() == TpuClasseCnj.FaixaProcedimental.PENAL_REVISAO
                || classe == TpuClasseCnj.ACAO_RESCISORIA
                || classe == TpuClasseCnj.ACAO_RESCISORIA_TRABALHISTA;
    }
}
