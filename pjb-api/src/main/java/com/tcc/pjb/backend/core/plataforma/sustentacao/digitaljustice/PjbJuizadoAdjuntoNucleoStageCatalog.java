package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class PjbJuizadoAdjuntoNucleoStageCatalog {

    private static final List<PjbJuizadoAdjuntoNucleoStage> STAGES = List.of(
            new PjbJuizadoAdjuntoNucleoStage(
                    "TJCE-JECA-2026-ETAPA-01",
                    LocalDate.of(2026, 2, 2),
                    LocalDate.of(2026, 2, 2),
                    LocalDate.of(2026, 2, 13),
                    List.of(
                            "1ª Vara da Comarca de Camocim",
                            "1ª e 2ª Vara Cível da Comarca de Acopiara",
                            "Vara Única da Comarca de Várzea Alegre",
                            "Vara Única da Comarca de Alto Santo",
                            "Vara Única da Comarca de Itarema",
                            "Vara Única da Comarca de Aracoiaba",
                            "1ª Vara da Comarca de Marco",
                            "Vara Única da Comarca de Jardim",
                            "Vara Única da Comarca de Cariré")),
            new PjbJuizadoAdjuntoNucleoStage(
                    "TJCE-JECA-2026-ETAPA-02",
                    LocalDate.of(2026, 3, 9),
                    LocalDate.of(2026, 3, 9),
                    LocalDate.of(2026, 3, 20),
                    List.of(
                            "1ª e 2ª Vara Cível da Comarca de Brejo Santo",
                            "Vara Única da Comarca de Pedra Branca",
                            "1ª Vara da Comarca de Beberibe",
                            "Vara Única da Comarca de Chaval",
                            "1ª e 2ª Vara Cível da Comarca de Limoeiro do Norte",
                            "Vara Única da Comarca de Lavras da Mangabeira",
                            "Vara Única da Comarca de Independência",
                            "Vara Única da Comarca de Missão Velha",
                            "Vara Única da Comarca de Monsenhor Tabosa")),
            new PjbJuizadoAdjuntoNucleoStage(
                    "TJCE-JECA-2026-ETAPA-03",
                    LocalDate.of(2026, 4, 13),
                    LocalDate.of(2026, 4, 13),
                    LocalDate.of(2026, 4, 24),
                    List.of(
                            "Vara Única da Comarca de Ipaumirim",
                            "1ª Vara da Comarca de Jaguaribe",
                            "1ª e 2ª Vara Cível da Comarca de Santa Quitéria",
                            "Vara Única da Comarca de Jijoca de Jericoacoara",
                            "1ª Vara da Comarca de Nova Russas",
                            "Vara Única da Comarca de Nova Olinda",
                            "Vara Única da Comarca de Caridade",
                            "Vara Única da Comarca de Farias Brito",
                            "Vara Única da Comarca de Ocara")),
            new PjbJuizadoAdjuntoNucleoStage(
                    "TJCE-JECA-2026-ETAPA-04",
                    LocalDate.of(2026, 5, 18),
                    LocalDate.of(2026, 5, 18),
                    LocalDate.of(2026, 5, 29),
                    List.of(
                            "Vara Única da Comarca de Caririaçu",
                            "Vara Única da Comarca de Jucás",
                            "Vara Única da Comarca de Jaguaruana",
                            "Vara Única da Comarca de Mauriti",
                            "1ª e 2ª Vara Cível da Comarca de Morada Nova",
                            "Vara Única da Comarca de Araripe",
                            "Vara Única da Comarca de Santana do Acaraú",
                            "Vara Única da Comarca de Tamboril"))
    );

    public List<PjbJuizadoAdjuntoNucleoStage> stages() {
        return STAGES;
    }

    public Optional<PjbJuizadoAdjuntoNucleoStage> stageForCourtUnit(String courtUnit) {
        return STAGES.stream().filter(stage -> stage.covers(courtUnit)).findFirst();
    }
}
