package com.tcc.pjb.backend.core.validator;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FaseValidatorService {

    private static final Set<RamoDireito> RAMOS_PENAIS =
            EnumSet.of(RamoDireito.PENAL, RamoDireito.MILITAR);

    public void validarMudancaFase(Processo processo, FaseProcessual novaFase) {
        Objects.requireNonNull(processo, "processo é obrigatório");
        Objects.requireNonNull(novaFase, "novaFase é obrigatória");

        String rito = processo.getRito() != null ? processo.getRito().name() : null;
        RamoDireito ramo = processo.getRamoDireito();

        validarCompatibilidadeComRito(rito, novaFase);
        validarCompatibilidadeComRamo(ramo, novaFase);
        validarCoerenciaMinima(rito, ramo, novaFase);
    }

    private void validarCompatibilidadeComRito(String rito, FaseProcessual fase) {
        if (rito == null || rito.isBlank()) {
            return;
        }
        if (isRito(rito, "JUIZADO_ESPECIAL_CIVEL") && fase.isTecnicoPericial()) {
            throw new IllegalStateException("JEC não admite perícia técnica complexa como regra de fluxo.");
        }
        if (isRito(rito, "EXECUCAO_FISCAL") && fase == FaseProcessual.AUDIENCIA_CUSTODIA) {
            throw new IllegalStateException("Execução fiscal é incompatível com audiência de custódia.");
        }
        if (fase.exigeRitoJuri() && !isRito(rito, "TRIBUNAL_JURI")) {
            throw new IllegalStateException("Ato do júri exige rito TRIBUNAL_JURI. Fase: " + fase);
        }
        if (fase == FaseProcessual.PLENARIO_JURI && !isRito(rito, "TRIBUNAL_JURI")) {
            throw new IllegalStateException("Plenário do júri exige rito TRIBUNAL_JURI.");
        }
    }

    private void validarCompatibilidadeComRamo(RamoDireito ramo, FaseProcessual fase) {
        if (ramo == null) {
            return;
        }
        if (fase.isPenalOnly() && !RAMOS_PENAIS.contains(ramo)) {
            throw new IllegalStateException("Fase penal incompatível com ramo: " + ramo + " (fase: " + fase + ")");
        }
        if (fase.isExecucaoPatrimonial() && ramo == RamoDireito.PENAL) {
            throw new IllegalStateException("Penhora não é fase padrão no ramo penal (restrição do motor).");
        }
    }

    private void validarCoerenciaMinima(String rito, RamoDireito ramo, FaseProcessual fase) {
        if (fase == FaseProcessual.CUMPRIMENTO_SENTENCA) {
            boolean ritoPenal = isPenalRito(rito);
            boolean ramoPenal = ramo != null && RAMOS_PENAIS.contains(ramo);
            if (ritoPenal || ramoPenal) {
                throw new IllegalStateException("Cumprimento de sentença (civil) incompatível com processo penal.");
            }
        }
        if (fase.isExecucaoPatrimonial() && isPenalRito(rito)) {
            throw new IllegalStateException("Penhora é incompatível com ritos penais.");
        }
    }

    private boolean isPenalRito(String rito) {
        return isRito(rito,
                "PROCEDIMENTO_PENAL_COMUM",
                "PROCEDIMENTO_PENAL_SUMARIO",
                "PROCEDIMENTO_PENAL_SUMARISSIMO",
                "TRIBUNAL_JURI");
    }

    private boolean isRito(String rito, String... expected) {
        if (rito == null) {
            return false;
        }
        String normalized = rito.trim().toUpperCase(Locale.ROOT);
        for (String candidate : expected) {
            if (normalized.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
