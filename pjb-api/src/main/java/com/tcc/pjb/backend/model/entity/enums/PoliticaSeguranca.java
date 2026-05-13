package com.tcc.pjb.backend.model.entity.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum PoliticaSeguranca {

    
    
    
    CONTROLE_ACESSO_RIGOROSO(
            "PS-ACCESS-001",
            Categoria.ACESSO,
            Criticidade.ALTA,
            "Controle de acesso estrito (menor privilégio).",
            Set.of(),                                  
            Set.of("PS-VALID-001", "PS-AUDIT-002")      
    ),

    BLOQUEIO_DELEGACAO(
            "PS-GOV-001",
            Categoria.GOVERNANCA,
            Criticidade.CRITICA,
            "Veda delegação de atos sensíveis; exige atuação direta do responsável formal.",
            Set.of("PS-GOV-002"),                       
            Set.of("PS-AUDIT-001", "PS-INTEG-001")       
    ),

    
    
    
    AUDITORIA_INTEGRAL(
            "PS-AUDIT-001",
            Categoria.AUDITORIA,
            Criticidade.CRITICA,
            "Auditoria completa: toda ação relevante deve ser rastreável e atribuível.",
            Set.of("PS-AUDIT-002"),                      
            Set.of("PS-INTEG-001", "PS-GOV-002")          
    ),

    LOG_TEMPORAL(
            "PS-AUDIT-002",
            Categoria.AUDITORIA,
            Criticidade.ALTA,
            "Registro temporal confiável (timestamp) para eventos e decisões.",
            Set.of(),
            Set.of()
    ),

    RESPONSABILIDADE_FUNCIONAL(
            "PS-GOV-002",
            Categoria.GOVERNANCA,
            Criticidade.ALTA,
            "Atribuição formal de responsabilidade por atos, decisões e autorizações.",
            Set.of(),
            Set.of("PS-AUDIT-002", "PS-AUDIT-001")        
    ),

    
    
    
    REGISTRO_IMUTAVEL(
            "PS-INTEG-001",
            Categoria.INTEGRIDADE,
            Criticidade.CRITICA,
            "Registros imutáveis (append-only/WORM) para evidência e cadeia de custódia.",
            Set.of("PS-AUDIT-002"),                       
            Set.of("PS-AUDIT-001")                        
    ),

    
    
    
    VALIDACAO_DUPLA(
            "PS-VALID-001",
            Categoria.VALIDACAO,
            Criticidade.ALTA,
            "Exige dupla validação/dupla assinatura em atos sensíveis.",
            Set.of(),
            Set.of("PS-GOV-002", "PS-AUDIT-002")           
    );

    private final String codigo;
    private final Categoria categoria;
    private final Criticidade criticidade;
    private final String descricao;

    
    private final Set<String> requerCodigos;

    
    private final Set<String> recomendaCodigos;

    PoliticaSeguranca(
            String codigo,
            Categoria categoria,
            Criticidade criticidade,
            String descricao,
            Set<String> requerCodigos,
            Set<String> recomendaCodigos
    ) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.criticidade = criticidade;
        this.descricao = descricao;
        this.requerCodigos = Set.copyOf(requerCodigos);
        this.recomendaCodigos = Set.copyOf(recomendaCodigos);
    }

    

    private static final Map<String, PoliticaSeguranca> POR_CODIGO =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    PoliticaSeguranca::getCodigo,
                    p -> p
            ));

    public static PoliticaSeguranca fromCodigo(String codigo) {
        PoliticaSeguranca p = POR_CODIGO.get(codigo);
        if (p == null) throw new IllegalArgumentException("PoliticaSeguranca inválida: " + codigo);
        return p;
    }

    

    public String getCodigo() {
        return codigo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Criticidade getCriticidade() {
        return criticidade;
    }

    public String getDescricao() {
        return descricao;
    }

    
    public Set<PoliticaSeguranca> getRequer() {
        if (requerCodigos.isEmpty()) return Set.of();
        return requerCodigos.stream().map(PoliticaSeguranca::fromCodigo).collect(Collectors.toUnmodifiableSet());
    }

    
    public Set<PoliticaSeguranca> getRecomenda() {
        if (recomendaCodigos.isEmpty()) return Set.of();
        return recomendaCodigos.stream().map(PoliticaSeguranca::fromCodigo).collect(Collectors.toUnmodifiableSet());
    }

    

    public boolean isCritica() {
        return criticidade == Criticidade.CRITICA;
    }

    public boolean isAltaOuMaior() {
        return criticidade == Criticidade.ALTA || criticidade == Criticidade.CRITICA;
    }

    
    public boolean dependeDe(PoliticaSeguranca outra) {
        if (outra == null) return false;

        
        if (requerCodigos.contains(outra.codigo)) return true;

        
        for (String req : requerCodigos) {
            PoliticaSeguranca r = POR_CODIGO.get(req);
            if (r != null && r.requerCodigos.contains(outra.codigo)) return true;
        }
        return false;
    }

    
    public static Set<PoliticaSeguranca> dependenciasFaltantes(Set<PoliticaSeguranca> ativas) {
        if (ativas == null || ativas.isEmpty()) return Set.of();

        Set<String> ativosCod = ativas.stream().map(PoliticaSeguranca::getCodigo).collect(Collectors.toSet());

        Set<PoliticaSeguranca> faltantes = ativas.stream()
                .flatMap(p -> p.requerCodigos.stream())
                .filter(req -> !ativosCod.contains(req))
                .map(POR_CODIGO::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return faltantes.isEmpty() ? Set.of() : Collections.unmodifiableSet(faltantes);
    }

    

    public enum Categoria {
        ACESSO,
        AUDITORIA,
        INTEGRIDADE,
        GOVERNANCA,
        VALIDACAO
    }

    public enum Criticidade {
        BAIXA,
        MEDIA,
        ALTA,
        CRITICA
    }
}
