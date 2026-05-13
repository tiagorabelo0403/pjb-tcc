package com.tcc.pjb.backend.model.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Data;

@Data
public class PropostaFinanceiraDTO {


    private BigDecimal valorTotal;          
    private BigDecimal valorEntrada;        
    private int parcelas;                   
    private BigDecimal valorParcela;        
    private double multaPercentual;         
    private String indiceCorrecao;          


    private UUID propostaUuid;              
    private Long processoId;                
    private Long usuarioId;                 
    private String usuarioNome;             
    private String advogadoResponsavel;     
    private String advogadoOab;             
    private String tribunal;                
    private String comarca;                 
    private String jurisdicao;              
    private String esfera;                  
    private LocalDateTime dataCriacao;      
    private LocalDateTime ultimaAtualizacao;
    private LocalDateTime dataValidade;     
    private boolean ativa;                  
    private boolean homologada;             
    private LocalDateTime dataHomologacao;  
    private boolean arquivada;              
    private LocalDateTime dataArquivamento; 
    private boolean recorrivel;             
    private String status;                  
    private String moeda;                   
    private BigDecimal valorCorrigido;      
    private BigDecimal jurosMensal;         
    private BigDecimal jurosAnual;          
    private BigDecimal descontoPercentual;  
    private BigDecimal valorDesconto;       
    private BigDecimal valorLiquido;        
    private BigDecimal custasProcessuais;   
    private BigDecimal honorariosAdvocaticios;
    private BigDecimal valorIndenizacao;    
    private BigDecimal valorMulta;          
    private BigDecimal valorTotalComMulta;  
    private BigDecimal valorTotalCorrigido; 
    private String formaPagamento;          
    private String meioPagamento;           
    private boolean pagamentoDigital;       
    private boolean pagamentoParcelado;     
    private boolean pagamentoAntecipado;    
    private boolean pagamentoAutomatico;    
    private String referenciaExterna;       
    private String origemSistema;           
    private String hashIntegridade;         
    private boolean criptografada;          
    private boolean validaAssinatura;       
    private String assinaturaDigital;       
    private boolean sigilosa;               
    private int nivelSigilo;                
    private boolean urgente;                
    private boolean prioridadeAlta;         
    private String categoriaRisco;          
    private double confiancaIA;             
    private String resumoIA;                
    private List<String> tagsIA;            
    private boolean detectouFraude;         
    private boolean detectouSpam;           
    private boolean detectouFakeNews;       
    private boolean detectouPlagio;         
    private boolean detectouViolencia;      
    private boolean detectouDiscursoOdio;   
    private boolean detectouAssedio;        
    private boolean detectouRacismo;        
    private boolean detectouSexismo;        
    private boolean detectouPhishing;       
    private boolean detectouMalware;        
    private boolean detectouConteudoAdulto; 
    private boolean detectouDadosPessoais;  
    private boolean detectouSegredoJustica; 

    
    public String getValorTotalFormatado() {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valorTotal);
    }

    
    public String getValorEntradaFormatado() {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valorEntrada);
    }

    public String getValorParcelaFormatado() {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valorParcela);
    }

    public BigDecimal calcularValorTotalComDesconto() {
        if (valorDesconto != null) {
            return valorTotal.subtract(valorDesconto);
        }
        return valorTotal;
    }

    public BigDecimal calcularValorTotalComMulta() {
        if (multaPercentual > 0) {
            return valorTotal.add(valorTotal.multiply(BigDecimal.valueOf(multaPercentual / 100)));
        }
        return valorTotal;
    }
}