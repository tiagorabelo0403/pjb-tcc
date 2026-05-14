package com.tcc.pjb.backend.service.conciliation.intelligence;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public final class AcordoRamoDireitoPolicy {

    private AcordoRamoDireitoPolicy() {}

    public record RamoPolicy(
            boolean acordoPermitido,
            boolean mediadorObrigatorio,
            boolean mpIntervencaoObrigatoria,
            String observacao
    ) {}

    public RamoPolicy avaliar(RitoProcessual rito) {
        if (rito.name().contains("PENAL") || rito.name().contains("CRIMINAL")) {
            return new RamoPolicy(false, false, false,
                    "Matéria penal: acordo patrimonial não homologável — somente ANPP (art. 28-A CPP) e penas restritivas.");
        }
        if (rito.isFamiliaSucessoes()) {
            boolean menorPossivel = rito.name().contains("GUARDA") || rito.name().contains("FAMILIA");
            return new RamoPolicy(true, true, menorPossivel,
                    "Direito de família: mediação preferível à conciliação (NCPC art. 694).");
        }
        if (rito.name().contains("TRABALHIST")) {
            return new RamoPolicy(true, false, false,
                    "Trabalhista: acordo possível — verificar verbas irrenunciáveis (CLT art. 9º).");
        }
        if (rito.name().contains("FAZENDA") || rito.name().contains("FISCAL")) {
            return new RamoPolicy(true, false, false,
                    "Fazenda Pública: acordo permitido com autorização legal — verificar lei específica.");
        }
        return new RamoPolicy(true, false, false,
                "Matéria disponível — acordo livremente permitido.");
    }
}
