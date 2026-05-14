package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstituicaoConfiguradorService {

    public InstituicaoJudicial configurarVaraUnicaInterior(
            UUID id, String nome, String uf, UUID comarcaId) {
        InstituicaoConfiguracao cfg = new InstituicaoConfiguracao(
                EnumSet.allOf(RitoProcessual.class),
                LocalTime.of(8, 0), LocalTime.of(14, 0),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                false, null, Set.of(), true, Set.of(), true, true, 150);
        return new InstituicaoJudicial(id, nome, "VU-" + uf,
                new InstituicaoJudicialTipo.VaraUnica(),
                "COMARCA_" + uf, comarcaId, uf, EnumSet.allOf(RitoProcessual.class), cfg);
    }

    public InstituicaoJudicial configurarJuizadoEspecial(
            UUID id, String nome, String uf, UUID comarcaId) {
        Set<RitoProcessual> ritos = Set.of(
                RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO,
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL,
                RitoProcessual.JUIZADO_ESPECIAL_FEDERAL);
        InstituicaoConfiguracao cfg = new InstituicaoConfiguracao(
                ritos, LocalTime.of(8, 0), LocalTime.of(18, 0),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                false, null, Set.of(), false, Set.of(), false, false, 300);
        return new InstituicaoJudicial(id, nome, "JEC-" + uf,
                new InstituicaoJudicialTipo.JuizadoEspecial(),
                "COMARCA_" + uf, comarcaId, uf, ritos, cfg);
    }

    public InstituicaoJudicial configurarJecItinerante(
            UUID id, String nome, String uf, UUID comarcaId, String municipioAlvo) {
        Set<RitoProcessual> ritos = Set.of(
                RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO,
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
        InstituicaoConfiguracao cfg = new InstituicaoConfiguracao(
                ritos, LocalTime.of(8, 0), LocalTime.of(14, 0),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                false, null, Set.of(), true, Set.of(), true, true, 50);
        return new InstituicaoJudicial(id, nome, "JEC-IT-" + uf,
                new InstituicaoJudicialTipo.JecItinerante(municipioAlvo),
                "COMARCA_" + uf, comarcaId, uf, ritos, cfg);
    }

    public InstituicaoJudicial configurarPlantao(
            UUID id, String nome, String uf, UUID comarcaId, String periodoId) {
        Set<RitoProcessual> ritos = Set.of(
                RitoProcessual.PROCEDIMENTO_PENAL_COMUM,
                RitoProcessual.PROCEDIMENTO_PENAL_SUMARIO,
                RitoProcessual.ESPECIAL_HABEAS_CORPUS,
                RitoProcessual.ESPECIAL_MANDADO_SEGURANCA);
        Set<DayOfWeek> todosDias = EnumSet.allOf(DayOfWeek.class);
        InstituicaoConfiguracao cfg = new InstituicaoConfiguracao(
                ritos, LocalTime.of(0, 0), LocalTime.of(23, 59),
                todosDias, true, periodoId, Set.of(), false, Set.of(), false, false, 30);
        return new InstituicaoJudicial(id, nome, "PLANTAO-" + uf,
                new InstituicaoJudicialTipo.Plantao(periodoId),
                "COMARCA_" + uf, comarcaId, uf, ritos, cfg);
    }

    public InstituicaoJudicial configurarCamara(
            UUID id, String nome, String uf, UUID comarcaId, int numeroCamara) {
        InstituicaoConfiguracao cfg = InstituicaoConfiguracao.padraoPrimeiroGrau();
        return new InstituicaoJudicial(id, nome, "CAM-" + numeroCamara + "-" + uf,
                new InstituicaoJudicialTipo.Camara(numeroCamara),
                "TRIBUNAL_" + uf, comarcaId, uf, EnumSet.allOf(RitoProcessual.class), cfg);
    }

    public CompetenciaInstitucional definirCompetenciaJEC(UUID instituicaoId, String territorio) {
        return new CompetenciaInstitucional(
                instituicaoId,
                Set.of(MateriaJurisdicao.CIVIL, MateriaJurisdicao.CONSUMIDOR,
                        MateriaJurisdicao.TRIBUTARIA),
                Set.of(RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO,
                        RitoProcessual.JUIZADO_ESPECIAL_CIVEL),
                BigDecimal.ZERO,
                new BigDecimal("40000"),
                territorio
        );
    }
}
