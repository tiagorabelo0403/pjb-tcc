package com.tcc.pjb.backend.core.quality.codebase.application;

public final class PjbCodebaseSanityMessages {

    private PjbCodebaseSanityMessages() {
    }

    public static String directVirtualThreadOutsideSpine() {
        return "Uso direto de virtual thread fora da espinha bounded do projeto";
    }

    public static String legacyJudgeImportIsolationBridge() {
        return "Pacote legado judge deve permanecer isolado como ponte transitória";
    }

    public static String jacocoAbsentFromMavenCycle() {
        return "JaCoCo ausente do ciclo Maven";
    }

    public static String checkstyleAbsentFromMavenCycle() {
        return "Checkstyle ausente do ciclo Maven";
    }

    public static String checkstyleConfigsMustBeWiredToMaven() {
        return "Configurações de higiene e bounded contexts precisam estar acopladas ao Maven";
    }
}
