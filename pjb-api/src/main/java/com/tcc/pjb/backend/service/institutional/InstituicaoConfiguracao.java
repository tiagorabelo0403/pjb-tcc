package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

public record InstituicaoConfiguracao(
        Set<RitoProcessual> ritosSuportados,
        LocalTime inicioExpediente,
        LocalTime fimExpediente,
        Set<DayOfWeek> diasUteis,
        boolean plantaoAtivo,
        String periodoPlantaoId,
        Set<LocalDate> feriadosLocais,
        boolean distribuicaoManual,
        Set<String> varasEspecializadas,
        boolean suportaJecItinerante,
        boolean configuracaoComarcaInterior,
        int limiteProcessosPorServidor
) {

    public InstituicaoConfiguracao {
        ritosSuportados = Set.copyOf(ritosSuportados);
        diasUteis = Set.copyOf(diasUteis);
        feriadosLocais = Set.copyOf(feriadosLocais);
        varasEspecializadas = Set.copyOf(varasEspecializadas);
    }

    public boolean estaEmExpediente(LocalTime agora) {
        return !agora.isBefore(inicioExpediente) && !agora.isAfter(fimExpediente);
    }

    public boolean aceitaRito(RitoProcessual rito) {
        return ritosSuportados.contains(rito);
    }

    public static InstituicaoConfiguracao padraoPrimeiroGrau() {
        return new InstituicaoConfiguracao(
                EnumSet.allOf(RitoProcessual.class),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                false,
                null,
                Set.of(),
                false,
                Set.of(),
                false,
                false,
                200
        );
    }
}
