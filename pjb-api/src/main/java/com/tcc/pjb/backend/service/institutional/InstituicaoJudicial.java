package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Set;
import java.util.UUID;

public record InstituicaoJudicial(
        UUID id,
        String nome,
        String sigla,
        InstituicaoJudicialTipo tipo,
        String jurisdicao,
        UUID comarcaId,
        String uf,
        Set<RitoProcessual> ritosSuportados,
        InstituicaoConfiguracao configuracao
) {

    public InstituicaoJudicial {
        ritosSuportados = Set.copyOf(ritosSuportados);
    }

    public boolean aceitaRito(RitoProcessual rito) {
        return ritosSuportados.contains(rito);
    }

    public boolean eVaraUnica() {
        return tipo instanceof InstituicaoJudicialTipo.VaraUnica;
    }

    public boolean ePlantao() {
        return tipo instanceof InstituicaoJudicialTipo.Plantao;
    }

    public boolean eJecItinerante() {
        return tipo instanceof InstituicaoJudicialTipo.JecItinerante;
    }

    public boolean eComarcaDoInterior() {
        return configuracao.configuracaoComarcaInterior();
    }

    public boolean suportaJuizSubstituto() {
        return tipo.suportaJuizSubstituto();
    }

    public String descricaoTipo() {
        return switch (tipo) {
            case InstituicaoJudicialTipo.VaraUnica v -> "Vara Única";
            case InstituicaoJudicialTipo.VaraEspecializada ve -> "Vara Especializada — " + ve.materia();
            case InstituicaoJudicialTipo.JuizadoEspecial je -> "Juizado Especial Cível";
            case InstituicaoJudicialTipo.JuizadoEspecialFederal jef -> "Juizado Especial Federal";
            case InstituicaoJudicialTipo.TurmaRecursal tr -> "Turma Recursal";
            case InstituicaoJudicialTipo.Camara c -> "Câmara " + c.numero();
            case InstituicaoJudicialTipo.TribunalPleno tp -> "Tribunal Pleno";
            case InstituicaoJudicialTipo.JecItinerante ji -> "JEC Itinerante — " + ji.municipioAlvo();
            case InstituicaoJudicialTipo.Plantao p -> "Plantão — " + p.periodoId();
        };
    }
}
