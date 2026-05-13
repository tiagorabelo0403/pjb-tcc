package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralPartyProfileResolver {

    NationalProceduralPartyProfile resolve(Map<String, Object> payload, String corpus) {
        String reu = normalize(firstNonBlank(text(payload.get("parteReuNome")), text(payload.get("reu")), text(payload.get("partePassiva"))));
        String autor = normalize(firstNonBlank(text(payload.get("parteAutoraNome")), text(payload.get("autor")), text(payload.get("parteAtiva"))));
        boolean federal = containsAny(reu, "UNIAO", "INSS", "IBAMA", "ANVISA", "ANATEL", "AUTARQUIA FEDERAL", "CAIXA ECONOMICA FEDERAL", "EMPRESA PUBLICA FEDERAL", "TRF")
                || bool(payload.get("envolveUniao")) || bool(payload.get("envolveAutarquiaFederal")) || bool(payload.get("envolveEmpresaPublicaFederal"));
        boolean autarquiaFederal = containsAny(reu, "AUTARQUIA FEDERAL", "INSS", "IBAMA", "INCRA", "DNIT", "ANVISA", "ANATEL") || bool(payload.get("envolveAutarquiaFederal"));
        boolean empresaPublicaFederal = containsAny(reu, "CAIXA ECONOMICA FEDERAL", "CORREIOS", "EMPRESA PUBLICA FEDERAL") || bool(payload.get("envolveEmpresaPublicaFederal"));
        boolean state = containsAny(reu, "ESTADO DE", "SECRETARIA DE ESTADO", "AUTARQUIA ESTADUAL", "DETRAN", "TJ", "POLICIA MILITAR") || bool(payload.get("envolveEstado"));
        boolean municipal = containsAny(reu, "MUNICIPIO DE", "PREFEITURA", "CAMARA MUNICIPAL", "AUTARQUIA MUNICIPAL") || bool(payload.get("envolveMunicipio"));
        boolean trabalho = containsAny(corpus, "CLT", "EMPREGADO", "EMPREGADOR", "VERBAS RESCISORIAS", "HORAS EXTRAS", "VINCULO EMPREGATICIO") || bool(payload.get("envolveRelacaoTrabalho"));
        boolean eleitoral = containsAny(corpus, "TSE", "TRE", "ZONA ELEITORAL", "ELEICAO", "CANDIDATO", "PARTIDO POLITICO") || bool(payload.get("envolveEleitoral"));
        boolean militar = containsAny(corpus, "JUSTICA MILITAR", "IPM", "CPPM", "AUDITORIA MILITAR", "CRIME MILITAR", "CPM") || bool(payload.get("envolveMilitar"));
        boolean publicParty = federal || autarquiaFederal || empresaPublicaFederal || state || municipal;
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (federal) tags.add("PARTE_FEDERAL");
        if (autarquiaFederal) tags.add("AUTARQUIA_FEDERAL");
        if (empresaPublicaFederal) tags.add("EMPRESA_PUBLICA_FEDERAL");
        if (state) tags.add("PARTE_ESTADUAL");
        if (municipal) tags.add("PARTE_MUNICIPAL");
        if (trabalho) tags.add("RELACAO_TRABALHO");
        if (eleitoral) tags.add("MATERIA_ELEITORAL");
        if (militar) tags.add("MATERIA_MILITAR");
        return new NationalProceduralPartyProfile(federal, autarquiaFederal, empresaPublicaFederal, state, municipal, trabalho, eleitoral, militar, publicParty, List.copyOf(tags), autor, reu);
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }

    private static boolean bool(Object value) {
        return NationalProceduralRoutingSupport.bool(value);
    }
}
